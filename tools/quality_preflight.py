#!/usr/bin/env python3
"""One reproducible quality gate for the owner's Mac and GitHub PR CI.

No formatters, reset/clean/stash, staging, installs, publishing or secret access.
"""
import argparse
from pathlib import Path
import shutil
import subprocess
import sys
import time

ROOT = Path(__file__).resolve().parents[1]
CONTRACTS = (
    "tools/check-structure.py",
    "tools/check-r5-contract.py",
    "tools/check-r51-contract.py",
    "tools/check-r52-contract.py",
    "tools/check-r53-contract.py",
    "tools/check-r54-contract.py",
    "tools/check-m1a-contract.py",
)
GRADLE_TASKS = (
    ":core:test",
    ":core:jacocoTestReport",
    ":app:assembleDebug",
    ":app:assembleRelease",
    ":app:lintDebug",
    ":app:lintRelease",
    "detekt",
    "ktlintCheck",
)


def invoke(args: list[str], *, plan: bool) -> bool:
    print("RUN=" + " ".join(args), flush=True)
    if plan:
        return True
    start = time.monotonic()
    result = subprocess.run(args, cwd=ROOT, check=False)
    print(f"EXIT={result.returncode} SECONDS={round(time.monotonic() - start, 1)}", flush=True)
    return result.returncode == 0


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--ci", action="store_true", help="Use pinned Gradle 8.13 from GitHub setup-gradle")
    parser.add_argument("--quick", action="store_true", help="Static checks and offline tests only")
    parser.add_argument("--plan", action="store_true", help="Print commands without executing")
    options = parser.parse_args()
    commands = [
        ["git", "diff", "--check"],
        *[[sys.executable, path] for path in CONTRACTS],
        [sys.executable, "-m", "unittest", "discover", "-s", "tests", "-p", "test_*.py", "-v"],
    ]
    if not options.quick:
        if options.ci:
            runner = shutil.which("gradle")
            if runner is None:
                print("HOLD_CI_GRADLE_813_NOT_CONFIGURED", file=sys.stderr)
                return 2
            executable = runner
        else:
            wrapper = ROOT / "gradlew"
            if not wrapper.is_file():
                print("HOLD: Gradle wrapper missing; run tools/bootstrap-gradle.sh and pin wrapper files.", file=sys.stderr)
                return 2
            executable = str(wrapper)
        commands.append([executable, "--no-daemon", "--console=plain", *GRADLE_TASKS])
        if options.ci:
            commands.append(
                [
                    sys.executable, "tools/verify-release-apk.py",
                    "--ci-unsigned", "app/build/outputs/apk/release/app-release-unsigned.apk",
                ],
            )
    for command in commands:
        if not invoke(command, plan=options.plan):
            print("RESULT=HOLD_SABLE_HEALTH_QUALITY_PREFLIGHT", file=sys.stderr)
            return 1
    print("RESULT=PASS_QUALITY_PREFLIGHT_PLAN" if options.plan else "RESULT=PASS_QUALITY_PREFLIGHT")
    return 0


if __name__ == "__main__":
    sys.exit(main())
