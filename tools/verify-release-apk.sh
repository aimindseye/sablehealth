#!/usr/bin/env bash
# Verify an already-signed release APK. No keystore or password is read.
set -eu
apk="${1:-}"
if [ -z "$apk" ] || [ ! -f "$apk" ]; then
    echo "Usage: bash tools/verify-release-apk.sh /absolute/path/to/signed.apk" >&2
    exit 2
fi
expected="9b2047390e5aa77537b8e8875cdfd3c2c72d0a0715e172a7f77457fc4300f292"
package="io.github.aimindseye.sablehealth"
sdk="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Library/Android/sdk}}"
if [ ! -d "$sdk/build-tools" ]; then
    echo "ERROR: Android SDK build-tools not found (set ANDROID_HOME)." >&2
    exit 2
fi
# Build-tools versions have dotted components. Sort version-wise when supported.
if sort -V </dev/null >/dev/null 2>&1; then
    bt="$(find "$sdk/build-tools" -mindepth 1 -maxdepth 1 -type d | sort -V | tail -n1)"
else
    bt="$(find "$sdk/build-tools" -mindepth 1 -maxdepth 1 -type d | sort | tail -n1)"
fi
signer="$bt/apksigner"
aapt="$bt/aapt"
if [ ! -x "$signer" ] || [ ! -x "$aapt" ]; then
    echo "ERROR: apksigner/aapt missing in $bt" >&2
    exit 2
fi
"$signer" verify --verbose "$apk"
cert="$($signer verify --print-certs "$apk" | sed -n 's/^[[:space:]]*Signer #1 certificate SHA-256 digest: //p' | tr '[:upper:]' '[:lower:]' | head -n1)"
if [ "$cert" != "$expected" ]; then
    echo "ERROR: wrong signing certificate. Expected $expected, received $cert" >&2
    exit 3
fi
actual_package="$($aapt dump badging "$apk" | sed -n "s/^package: name='\([^']*\)'.*/\1/p" | head -n1)"
if [ "$actual_package" != "$package" ]; then
    echo "ERROR: wrong APK package. Expected $package, received $actual_package" >&2
    exit 4
fi
echo "PASS: $package release APK signed with Sable Games / TenSeven registered certificate."
