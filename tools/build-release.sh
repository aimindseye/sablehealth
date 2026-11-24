#!/usr/bin/env bash
# Sable Health owner-only interactive release builder.
# Usage: bash tools/build-release.sh [KEYSTORE ALIAS]
# Derived workflow: Sable Watch/Games: unsigned Gradle APK -> apksigner stdin -> verify.
set -u
umask 077

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)" || exit 2
cd "$ROOT" || exit 2

fail() {
    printf 'HOLD_SABLE_HEALTH_RELEASE: %s\n' "$1" >&2
    exit 2
}
cleanup() {
    unset store_pass key_pass
    if [[ -n "${stage_dir:-}" && -d "$stage_dir" ]]; then
        rm -rf "$stage_dir"
    fi
}
trap cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM

if [[ $# != 0 && $# != 2 ]]; then
    echo 'Usage: bash tools/build-release.sh [KEYSTORE ALIAS]' >&2
    exit 2
fi
[[ -t 0 ]] || fail "Interactive terminal required for owner-only signing"
[[ "$(git rev-parse --show-toplevel 2>/dev/null)" == "$ROOT" ]] || fail "Expected Sable Health checkout root"
[[ -z "$(git status --porcelain --untracked-files=all)" ]] || fail "Commit and review source changes before release"
commit="$(git rev-parse HEAD)" || fail "Unable to determine source commit"
[[ -x ./gradlew ]] || fail "Gradle 8.13 wrapper missing; fix before release"

pin=config/release-certificate.sha256
[[ -f "$pin" ]] || fail "Registered public certificate pin missing: $pin"
git ls-files --error-unmatch "$pin" >/dev/null 2>&1 ||
    fail "Registered certificate pin must be committed and tracked"
expected_cert="$(tr -d '\r\n' < "$pin" | tr '[:upper:]' '[:lower:]')"
[[ "$expected_cert" =~ ^[0-9a-f]{64}$ ]] || fail "Invalid certificate SHA-256; require 64 hex characters"

version="$(sed -n 's/^[[:space:]]*versionName = "\([^"]*\)".*/\1/p' app/build.gradle.kts | head -n1)"
code="$(sed -n 's/^[[:space:]]*versionCode = \([0-9]*\).*/\1/p' app/build.gradle.kts | head -n1)"
[[ "$version" =~ ^[a-zA-Z0-9._-]+$ && "$code" =~ ^[0-9]+$ ]] ||
    fail "Invalid Gradle version metadata"
out="${SABLE_HEALTH_RELEASE_DIR:-$HOME/Downloads/sablehealth-release/$version/$commit}"
asset="SableHealth-$version-vc$code.apk"
[[ ! -e "$out" && ! -L "$out" ]] || fail "Release output directory already exists; refusing overwrite"

sdk="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Library/Android/sdk}}"
apksigner="$sdk/build-tools/36.0.0/apksigner"
[[ -x "$apksigner" ]] || fail "Android SDK 36.0.0 apksigner unavailable"
[[ -x "$sdk/build-tools/36.0.0/aapt2" ]] || fail "Android SDK 36.0.0 aapt2 unavailable"

# Guard against accidentally signing before the explicit user prompt.
[[ ! -f keystore.properties ]] ||
    fail "Move ignored keystore.properties out of this checkout: signing is interactive only"
for name in SABLE_HEALTH_KEYSTORE SABLE_HEALTH_KEYSTORE_PASSWORD SABLE_HEALTH_KEY_ALIAS SABLE_HEALTH_KEY_PASSWORD; do
    if [[ -n "${!name:-}" ]]; then
        fail "Signing environment variables are not used by this interactive release script"
    fi
done

echo 'STEP_1=LOCAL_QUALITY_PREFLIGHT_AND_UNSIGNED_RELEASE'
python3 tools/quality_preflight.py ||
    fail "Quality preflight failed"
unsigned=app/build/outputs/apk/release/app-release-unsigned.apk
[[ -s "$unsigned" ]] || fail "Expected unsigned release APK is missing"
[[ -s app/build/outputs/mapping/release/mapping.txt ]] || fail "R8 mapping missing"
python3 tools/verify-release-apk.py --ci-unsigned \
    --expected-code "$code" --expected-name "$version" ||
    fail "Unsigned APK metadata verification failed"

echo 'STEP_2=INTERACTIVE_SIGNING_INPUTS'
if [[ $# == 2 ]]; then
    keystore="$1"
    alias_name="$2"
else
    printf 'Keystore location (absolute path, .jks/.keystore): '
    IFS= read -r keystore || fail "Keystore path input unavailable"
    printf 'Key alias: '
    IFS= read -r alias_name || fail "Key alias input unavailable"
fi
keystore="${keystore/#\~/$HOME}"
[[ -n "$alias_name" && -n "${alias_name//[[:space:]]/}" ]] || fail "Key alias must not be blank"
[[ -f "$keystore" ]] || fail "Keystore does not exist"
IFS= read -r -s -p 'Keystore password (hidden): ' store_pass || fail "Password input unavailable"
printf '\n'
[[ -n "$store_pass" ]] || fail "Keystore password cannot be blank"
IFS= read -r -s -p 'Key password (hidden; Enter if same): ' key_pass || fail "Key password input unavailable"
printf '\n'
[[ -n "$key_pass" ]] || key_pass="$store_pass"

for part in app app/build app/build/outputs app/build/outputs/apk app/build/outputs/apk/release; do
    [[ ! -L "$part" ]] || fail "Symlinked release directory refused"
done
stage_target=app/build/outputs/apk/release/.owner-signing
# Exclusive creation; do not remove an existing staging directory.
mkdir -m 700 "$stage_target" || fail "Cannot reserve private signing directory"
stage_dir="$stage_target"
signed="$stage_dir/app-release-signed.apk"

echo 'STEP_3=SIGN_UNSIGNED_APK_WITH_OWNER_KEY'
# Both secrets provided only on stdin, never through argv, env or files.
if ! printf '%s\n%s\n' "$store_pass" "$key_pass" |
    "$apksigner" sign \
        --ks "$keystore" \
        --ks-key-alias "$alias_name" \
        --ks-pass stdin \
        --key-pass stdin \
        --out "$signed" "$unsigned"; then
    fail "apksigner refused the keystore, alias or password"
fi
unset store_pass key_pass
rm -f "$signed.idsig"

echo 'STEP_4=VERIFY_COMPILED_SIGNED_APK_AND_REGISTERED_CERTIFICATE'
python3 tools/verify-release-apk.py \
    --expected-code "$code" --expected-name "$version" ||
    fail "Signed APK certificate, version, package or signature validation failed"

# Create a new directory exclusively so neither APK nor receipts are overwritten.
mkdir -p "$(dirname "$out")" || fail "Cannot create output parent"
mkdir -m 700 "$out" || fail "Cannot reserve new release output directory; refusing overwrite"
cp -p "$signed" "$out/$asset" || fail "Could not copy verified signed APK"
if command -v shasum >/dev/null 2>&1; then
    checksum="$(shasum -a 256 "$out/$asset" | awk '{print $1}')"
else
    checksum="$(sha256sum "$out/$asset" | awk '{print $1}')"
fi
[[ "$checksum" =~ ^[0-9a-f]{64}$ ]] || fail "Unable to calculate APK checksum"
printf '%s  %s\n' "$checksum" "$asset" > "$out/$asset.sha256" || fail "Cannot write APK checksum"
printf '%s\n' "$commit" > "$out/SOURCE_COMMIT.txt" || fail "Cannot write source receipt"
printf '%s\n' "$expected_cert" > "$out/REGISTERED_CERT_SHA256.txt" || fail "Cannot write certificate receipt"
echo 'RESULT=PASS_SABLE_HEALTH_OWNER_SIGNED_RELEASE'
printf 'APK=%s\nAPK_SHA256=%s\nSOURCE_COMMIT=%s\n' "$out/$asset" "$checksum" "$commit"
echo 'PUBLISHED_TO_GITHUB=false'
