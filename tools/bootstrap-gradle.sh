#!/usr/bin/env bash
# One-time, explicit official Gradle bootstrap for initial repository (JAR not vendored).
# Requires externally installed Gradle 8.13; never silently installs or executes curl|bash.
set -eu
cd "$(dirname "$0")/.."
if ! command -v gradle >/dev/null 2>&1; then
  echo "Gradle not available. Install Gradle 8.13 (for example via SDKMAN) then rerun." >&2
  exit 2
fi
gradle --version | head -12
gradle wrapper --gradle-version 8.13 --distribution-type bin --gradle-distribution-sha256-sum 20f1b1176237254a6fc204d8434196fa11a4cfb387567519c61556e8710aed78
if [ ! -f gradle/wrapper/gradle-wrapper.jar ]; then
  echo "Wrapper jar was not generated; cannot proceed." >&2
  exit 3
fi
printf 'Wrapper jar generated locally. Review its hash and commit to feature branch after validation.\n'
