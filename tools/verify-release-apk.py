#!/usr/bin/env python3
"""Verify only fixed, repository-owned release artifacts and registered signer."""
import argparse
import os
from pathlib import Path
import re
import subprocess
import sys
import zipfile

ROOT = Path(__file__).resolve().parents[1]
EXPECTED_PACKAGE = "io.github.aimindseye.sablehealth"
MIN_SDK = 30
BUILD_TOOLS = "36.0.0"
FINGERPRINT = re.compile(r"[0-9a-f]{64}")
RELEASE = "app/build/outputs/apk/release"


def fixed_file(relative: str) -> Path:
    path = ROOT / relative
    if not path.is_relative_to(ROOT) or '..' in Path(relative).parts:
        raise ValueError("Path outside repository")
    if any(part.is_symlink() for part in (path, *path.parents) if part.is_relative_to(ROOT)):
        raise ValueError("Symlinked release input refused")
    if not path.is_file() or not path.resolve().is_relative_to(ROOT):
        raise ValueError("Missing or unsafe release input")
    return path.resolve()


def sdk_tool(name: str) -> Path:
    if name not in ("aapt2", "apksigner"):
        raise ValueError("Unsupported Android SDK command")
    locations = [os.environ.get("ANDROID_HOME"), os.environ.get("ANDROID_SDK_ROOT"),
                 str(Path.home() / "Library/Android/sdk")]
    for root in filter(None, locations):
        binary = Path(root).resolve() / "build-tools" / BUILD_TOOLS / name
        if binary.is_file() and os.access(binary, os.X_OK):
            return binary.resolve()
    raise ValueError(f"Android build-tools/{BUILD_TOOLS}/{name} not available")


def checked_output(result: subprocess.CompletedProcess) -> str:
    if result.returncode:
        raise ValueError(f"Android SDK verifier exited {result.returncode}")
    return result.stdout


def metadata(apk: Path) -> tuple[int, str]:
    data = checked_output(subprocess.run(
        [str(sdk_tool("aapt2")), "dump", "badging", str(apk.resolve())],
        capture_output=True, text=True, check=False, timeout=90,
    ))
    match = re.search(r"^package: name='([^']+)' versionCode='(\d+)' versionName='([^']+)'", data, re.MULTILINE)
    sdk = re.search(r"^(?:minSdkVersion|sdkVersion):'(\d+)'", data, re.MULTILINE)
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


def verify_signature(apk: Path) -> None:
    expected = fixed_file("config/release-certificate.sha256").read_text(encoding="utf-8").strip().lower()
    if not FINGERPRINT.fullmatch(expected):
        raise ValueError("Certificate fingerprint must contain one SHA-256 hex string")
    result = checked_output(subprocess.run(
        [str(sdk_tool("apksigner")), "verify", "--min-sdk-version", str(MIN_SDK),
         "--verbose", "--print-certs", str(apk.resolve())],
        capture_output=True, text=True, check=False, timeout=90,
    ))
    if not re.search(r"(?m)^Verifies$", result):
        raise ValueError("APK signature verification not confirmed")
    if not re.search(r"(?m)^Verified using v[23](?:\.\d+)? scheme .*: true$", result):
        raise ValueError("No verified v2/v3 APK signing scheme")
    if not re.search(r"(?m)^Number of signers: 1$", result):
        raise ValueError("APK must have exactly one signing identity")
    certs = re.findall(r"(?m)^(?:V[\d.]+ Signer|Signer #\d+):? certificate SHA-256 digest: ([0-9a-fA-F]{64})$", result)
    if not certs or any(cert.lower() != expected for cert in certs):
        raise ValueError("APK signing certificate does not match registered fingerprint")
    print(f"PASS_SIGNING_IDENTITY_SHA256={expected}")


def verify_unsigned(apk: Path) -> None:
    """Reject ZIP signature entries and APK Signing Blocks; do not trust filename."""
    with zipfile.ZipFile(apk) as archive:
        if "AndroidManifest.xml" not in archive.namelist():
            raise ValueError("Missing APK manifest")
        for name in archive.namelist():
            upper = name.upper()
            if upper.startswith("META-INF/") and (
                upper.endswith((".SF", ".RSA", ".DSA", ".EC"))
                or upper.split("/")[-1].startswith("SIG-")
            ):
                raise ValueError("Unsigned APK contains signature entries")
        offset = archive.start_dir
        if offset < 16:
            raise ValueError("Invalid APK central directory")
        with apk.open("rb") as stream:
            stream.seek(offset - 16)
            if stream.read(16) == b"APK Sig Block 42":
                raise ValueError("Unsigned APK contains APK Signing Block")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__, allow_abbrev=False)
    parser.add_argument("--ci-unsigned", action="store_true")
    parser.add_argument("--expected-code", type=int)
    parser.add_argument("--expected-name")
    args = parser.parse_args()
    if args.ci_unsigned:
        apk = fixed_file(RELEASE + "/app-release-unsigned.apk")
    else:
        apk = fixed_file(RELEASE + "/.owner-signing/app-release-signed.apk")
    code, name = metadata(apk)
    if args.expected_code is not None and code != args.expected_code:
        raise ValueError("APK versionCode does not match expected release")
    if args.expected_name is not None and name != args.expected_name:
        raise ValueError("APK versionName does not match expected release")
    if args.ci_unsigned:
        verify_unsigned(apk)
        print("RESULT=PASS_SABLE_HEALTH_UNSIGNED_APK_METADATA_ONLY")
        print("SIGNATURE_VERIFIED=false")
    else:
        verify_signature(apk)
        print("RESULT=PASS_SABLE_HEALTH_REGISTERED_SIGNED_APK")
    print(f"PACKAGE={EXPECTED_PACKAGE}")
    print(f"MIN_SDK={MIN_SDK}")
    print(f"VERSION_CODE={code}")
    print(f"VERSION_NAME={name}")
    return 0


def entrypoint() -> int:
    try:
        return main()
    except (OSError, ValueError, zipfile.BadZipFile, subprocess.SubprocessError) as error:
        print(f"HOLD_SABLE_HEALTH_APK_VERIFIER: {error}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(entrypoint())
