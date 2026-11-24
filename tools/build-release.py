#!/usr/bin/env python3
"""Owner-only signed release build: verify registered cert, APK signature and source identity."""
import argparse
import hashlib
from pathlib import Path
import re
import shutil
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]


def run(args: list[str], *, capture: bool = False) -> str:
    result = subprocess.run(args, cwd=ROOT, capture_output=capture, text=True, check=False)
    if result.returncode:
        raise ValueError(f"Command failed (exit {result.returncode}): {args[0]}")
    return result.stdout.strip() if capture else ""


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", type=Path, help="Parent directory for release artifacts")
    options = parser.parse_args()
    if run(["git", "rev-parse", "--show-toplevel"], capture=True) != str(ROOT):
        raise ValueError("Run from the exact Sable Health repository root")
    if run(["git", "status", "--porcelain", "--untracked-files=all"], capture=True):
        raise ValueError("Release requires clean committed source and exact traceability")
    commit = run(["git", "rev-parse", "HEAD"], capture=True)
    if not (ROOT / "gradlew").is_file():
        raise ValueError("Gradle wrapper is missing or not committed")
    # Any part of the expected certificate must be public, exactly pinned and in Git.
    certificate = ROOT / "config/release-certificate.sha256"
    if not certificate.is_file():
        raise ValueError("Registered public release certificate not pinned in config/release-certificate.sha256")
    fingerprint = certificate.read_text(encoding="utf-8").strip().lower()
    if re.fullmatch("[0-9a-f]{64}", fingerprint) is None:
        raise ValueError("Invalid registered certificate fingerprint")
    if run(["git", "ls-files", "--error-unmatch", "config/release-certificate.sha256"], capture=True) != "config/release-certificate.sha256":
        raise ValueError("Public release fingerprint must be tracked in Git")

    print("RUN=local release preflight and signed build", flush=True)
    run([sys.executable, "tools/quality_preflight.py"])
    run(["./gradlew", "--no-daemon", "--console=plain", ":app:verifyRegisteredReleaseKey", ":app:assembleRelease"])

    metadata = (ROOT / "app/build.gradle.kts").read_text(encoding="utf-8")
    code_match = re.search(r"(?m)^\s*versionCode\s*=\s*(\d+)\s*$", metadata)
    name_match = re.search(r'(?m)^\s*versionName\s*=\s*"([^"]+)"\s*$', metadata)
    if not code_match or not name_match:
        raise ValueError("Cannot pin exact versionCode/versionName from app/build.gradle.kts")
    code, version = int(code_match.group(1)), name_match.group(1)
    if not re.fullmatch(r"[A-Za-z0-9._-]+", version):
        raise ValueError("Unexpected versionName characters")
    apk = ROOT / "app/build/outputs/apk/release/app-release.apk"
    run(
        [
            sys.executable, "tools/verify-release-apk.py",
            str(apk), "--expected-code", str(code), "--expected-name", version,
        ],
    )

    output = (options.output or Path.home() / "Downloads/sablehealth-release").expanduser().resolve() / version
    output.mkdir(parents=True, exist_ok=True)
    filename = f"SableHealth-{version}-vc{code}.apk"
    target = output / filename
    if target.exists():
        raise ValueError(f"Refusing to overwrite existing release: {target}")
    shutil.copy2(apk, target)
    sha256 = hashlib.sha256(target.read_bytes()).hexdigest()
    (output / (filename + ".sha256")).write_text(f"{sha256}  {filename}\n", encoding="utf-8")
    (output / "SOURCE_COMMIT.txt").write_text(commit + "\n", encoding="utf-8")
    (output / "REGISTERED_CERT_SHA256.txt").write_text(fingerprint + "\n", encoding="utf-8")
    print(f"RESULT=PASS_SABLE_HEALTH_SIGNED_RELEASE_PACKAGE")
    print(f"APK={target}")
    print(f"APK_SHA256={sha256}")
    print(f"SOURCE_COMMIT={commit}")
    print("PUBLISHED_TO_GITHUB=false")
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except (OSError, ValueError) as error:
        print(f"HOLD_SABLE_HEALTH_RELEASE: {error}", file=sys.stderr)
        sys.exit(2)
