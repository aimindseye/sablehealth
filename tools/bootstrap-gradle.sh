#!/usr/bin/env bash
# Generate the official Gradle 8.13 wrapper without evaluating this Android project
# using the user's global Gradle (which may be Gradle 9.6+).
# No installer, curl|bash, or privileged system changes.
set -eu

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
GRADLE_VERSION="8.13"
DIST_SHA256="20f1b1176237254a6fc204d8434196fa11a4cfb387567519c61556e8710aed78"
WRAPPER_SHA256="81a82aaea5abcc8ff68b3dfcb58b3c3c429378efd98e7433460610fecd7ae45f"

sha256_file() {
    if command -v shasum >/dev/null 2>&1; then
        shasum -a 256 "$1" | awk '{print $1}'
    elif command -v sha256sum >/dev/null 2>&1; then
        sha256sum "$1" | awk '{print $1}'
    else
        echo "ERROR: shasum or sha256sum is required to verify the wrapper" >&2
        return 1
    fi
}

if [ -f "$ROOT/gradlew" ] && [ -f "$ROOT/gradle/wrapper/gradle-wrapper.jar" ]; then
    current_sha="$(sha256_file "$ROOT/gradle/wrapper/gradle-wrapper.jar")"
    if [ "$current_sha" = "$WRAPPER_SHA256" ] &&
       grep -q 'gradle-8.13-bin.zip' "$ROOT/gradle/wrapper/gradle-wrapper.properties" &&
       grep -q "$DIST_SHA256" "$ROOT/gradle/wrapper/gradle-wrapper.properties"; then
        echo "PASS: Gradle 8.13 wrapper is already present and verified."
        exit 0
    fi
    echo "ERROR: Existing wrapper JAR/version does not match approved Gradle 8.13; refusing overwrite." >&2
    exit 3
fi

if ! command -v gradle >/dev/null 2>&1; then
    echo "ERROR: Gradle CLI not found. Install any suitable Gradle CLI (8.13 or 9.x) and rerun." >&2
    exit 2
fi

TMP="$(mktemp -d "${TMPDIR:-/tmp}/sablehealth-wrapper.XXXXXXXX")"
trap 'rm -rf "$TMP"' EXIT
printf "rootProject.name = 'sablehealth-wrapper-bootstrap'\n" > "$TMP/settings.gradle"
: > "$TMP/build.gradle"

echo "Step 1: Generate a Gradle 8.13 launcher in a temporary plugin-free project."
echo "The installed Gradle CLI will NOT evaluate Sable Health's Android Gradle Plugin."
(
    cd "$TMP"
    gradle --no-daemon wrapper --gradle-version "$GRADLE_VERSION" \
        --distribution-type bin --gradle-distribution-sha256-sum "$DIST_SHA256"
)

echo "Step 2: Run Gradle 8.13 to generate the matching official Gradle 8.13 wrapper JAR."
(
    cd "$TMP"
    bash ./gradlew --no-daemon wrapper --gradle-version "$GRADLE_VERSION" \
        --distribution-type bin --gradle-distribution-sha256-sum "$DIST_SHA256"
)

for required in gradlew gradlew.bat gradle/wrapper/gradle-wrapper.jar; do
    if [ ! -f "$TMP/$required" ]; then
        echo "ERROR: Bootstrap did not produce $required" >&2
        exit 3
    fi
done

actual_sha="$(sha256_file "$TMP/gradle/wrapper/gradle-wrapper.jar")"
if [ "$actual_sha" != "$WRAPPER_SHA256" ]; then
    echo "ERROR: Wrapper JAR checksum mismatch. Expected: $WRAPPER_SHA256, got: $actual_sha" >&2
    exit 4
fi

# Retain the reviewed distribution/checksum settings already committed in the repository.
if ! grep -q 'gradle-8.13-bin.zip' "$ROOT/gradle/wrapper/gradle-wrapper.properties" ||
   ! grep -q "$DIST_SHA256" "$ROOT/gradle/wrapper/gradle-wrapper.properties"; then
    echo "ERROR: Repository wrapper properties changed; refusing to install wrapper." >&2
    exit 4
fi
cp "$TMP/gradlew" "$ROOT/gradlew"
cp "$TMP/gradlew.bat" "$ROOT/gradlew.bat"
cp "$TMP/gradle/wrapper/gradle-wrapper.jar" "$ROOT/gradle/wrapper/gradle-wrapper.jar"
chmod +x "$ROOT/gradlew"
echo "PASS: Gradle 8.13 wrapper generated and verified (SHA-256 $WRAPPER_SHA256)."
echo "Next: ./gradlew --version && ./gradlew :core:test :app:assembleDebug"
echo "Before merging the M0 PR, stage/commit gradlew, gradlew.bat and gradle/wrapper/gradle-wrapper.jar."
