#!/usr/bin/env python3
"""Validate compiled Sable Health APK package, SDK, version and (for release) signer."""
import argparse
import os
from pathlib import Path
import re
import subprocess
import sys

EXPECTED_PACKAGE = "io.github.aimindseye.sablehealth"
MIN_SDK = 30
BUILD_TOOLS = "36.0.0"
FINGERPRINT = re.compile(r"[0-9a-f]{64}")


def sdk_tool(name: str) -> Path:
    locations = [
        os.environ.get("ANDROID_HOME"),
        os.environ.get("ANDROID_SDK_ROOT"),
        str(Path.home() / "Library/Android/sdk"),
    ]
    for root in filter(None, locations):
        binary = Path(root) / "build-tools" / BUILD_TOOLS / name
        if binary.is_file() and os.access(binary, os.X_OK):
            return binary
    raise ValueError(f"Android build-tools/{BUILD_TOOLS}/{name} not available")


def output(*args: str) -> str:
    result = subprocess.run(args, capture_output=True, text=True, check=False, timeout=90)
    if result.returncode:
        raise ValueError(f"Android SDK verifier exited {result.returncode}: {result.stderr[:500]}")
    return result.stdout


def metadata(apk: Path) -> tuple[int, str]:
    data = output(str(sdk_tool("aapt2")), "dump", "badging", str(apk))
    match = re.search(
        r"^package: name='([^']+)' versionCode='([0-9]+)' versionName='([^']+)'",
        data,
        flags=re.MULTILINE,
    )
    sdk = re.search(r"^(?:minSdkVersion|sdkVersion):'([0-9]+)'", data, flags=re.MULTILINE)
    if match is None or sdk is None:
        raise ValueError("Compiled APK package/minSdk identity could not be read")
    package, code, name = match.groups()
    if package != EXPECTED_PACKAGE:
        raise ValueError("Wrong application package identity")
    if int(sdk.group(1)) != MIN_SDK:
        raise ValueError("Unexpected minimum Android SDK")
    if int(code) < 1 or not name.strip():
        raise ValueError("Invalid Android release version metadata")
    return int(code), name


def verify_signature(apk: Path, certificate: Path) -> None:
    if apk.name.endswith("-unsigned.apk"):
        raise ValueError("Unsigned release APK cannot be distributed")
    if not certificate.is_file():
        raise ValueError("Registered certificate fingerprint file is missing")
    expected = certificate.read_text(encoding="utf-8").strip().lower()
    if not FINGERPRINT.fullmatch(expected):
        raise ValueError("Certificate fingerprint must contain one SHA-256 hex string")
    result = output(
        str(sdk_tool("apksigner")),
        "verify",
        "--min-sdk-version",
        str(MIN_SDK),
        "--verbose",
        "--print-certs",
        str(apk),
    )
    if not re.search(r"(?m)^Verifies$", result):
        raise ValueError("APK signature verification not confirmed")
    if not re.search(r"(?m)^Verified using v[23](?:\.\d+)? scheme .*: true$", result):
        raise ValueError("No verified v2/v3 APK signing scheme")
    if not re.search(r"(?m)^Number of signers: 1$", result):
        raise ValueError("APK must have exactly one signing identity")
    certs = re.findall(
        r"(?m)^(?:V[0-9.]+ Signer|Signer #[0-9]+):? "
        r"certificate SHA-256 digest: ([0-9a-fA-F]{64})$",
        result,
    )
    if not certs or any(cert.lower() != expected for cert in certs):
        raise ValueError("APK signing certificate does not match registered fingerprint")
    print(f"PASS_SIGNING_IDENTITY_SHA256={expected}")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("apk", type=Path)
    parser.add_argument("--ci-unsigned", action="store_true")
    parser.add_argument(
        "--certificate",
        type=Path,
        default=Path(__file__).resolve().parents[1] / "config/release-certificate.sha256",
    )
    parser.add_argument("--expected-code", type=int)
    parser.add_argument("--expected-name")
    args = parser.parse_args()
    if not args.apk.is_file():
        raise ValueError(f"APK not found: {args.apk}")
    if args.ci_unsigned and not args.apk.name.endswith("-unsigned.apk"):
        raise ValueError("CI unsigned verification requires an -unsigned.apk artifact")
    code, name = metadata(args.apk)
    if args.expected_code is not None and code != args.expected_code:
        raise ValueError("APK versionCode does not match expected release")
    if args.expected_name is not None and name != args.expected_name:
        raise ValueError("APK versionName does not match expected release")
    if args.ci_unsigned:
        print("RESULT=PASS_SABLE_HEALTH_UNSIGNED_APK_METADATA_ONLY")
        print("SIGNATURE_VERIFIED=false")
    else:
        verify_signature(args.apk, args.certificate)
        print("RESULT=PASS_SABLE_HEALTH_REGISTERED_SIGNED_APK")
    print(f"PACKAGE={EXPECTED_PACKAGE}")
    print(f"MIN_SDK={MIN_SDK}")
    print(f"VERSION_CODE={code}")
    print(f"VERSION_NAME={name}")
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except (OSError, ValueError, subprocess.SubprocessError) as error:
        print(f"HOLD_SABLE_HEALTH_APK_VERIFIER: {error}", file=sys.stderr)
        sys.exit(2)
