#!/usr/bin/env bash
# Builds the signed release APK (dist/StreamCompanion-<version>.apk).
# Needs: JDK 17+ and the Android SDK (platform 35 + build-tools 35.0.0). Set ANDROID_HOME if it is not auto-detected.
set -euo pipefail
HERE="$(cd "$(dirname "$0")/.." && pwd)"
DIST="$(cd "$HERE/.." && pwd)/dist"
cd "$HERE"

# ---- locate the Android SDK
SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
[ -z "$SDK" ] && [ -f local.properties ] && SDK="$(sed -n 's/^sdk.dir=//p' local.properties | head -1)"
for guess in "$HOME/Android/Sdk" "$HOME/Library/Android/sdk" /opt/android-sdk; do [ -z "$SDK" ] && [ -d "$guess" ] && SDK="$guess"; done
[ -d "$SDK" ] || { echo "Android SDK not found. Install Android Studio or the command-line tools and set ANDROID_HOME."; exit 1; }
export ANDROID_HOME="$SDK"
echo "sdk.dir=$SDK" > local.properties

# ---- release signing key (created once; keep keystore.properties + the .jks safe, updates must use the same key)
if [ ! -f keystore.properties ]; then
  echo "==> Creating a release signing key"
  mkdir -p signing
  PASS="$(head -c 24 /dev/urandom | base64 | tr -dc 'A-Za-z0-9' | head -c 20)"
  keytool -genkeypair -v -keystore signing/stream-companion.jks -storetype PKCS12 -alias stream-companion \
    -keyalg RSA -keysize 4096 -validity 36500 -storepass "$PASS" -keypass "$PASS" \
    -dname "CN=Stream Companion, O=Stream Companion, C=US" >/dev/null 2>&1
  printf 'storeFile=signing/stream-companion.jks\nstorePassword=%s\nkeyAlias=stream-companion\nkeyPassword=%s\n' "$PASS" "$PASS" > keystore.properties
fi

echo "==> Unit tests + release build"
./gradlew --no-daemon testDebugUnitTest assembleRelease

APK="app/build/outputs/apk/release/app-release.apk"
BT="$(ls -d "$SDK"/build-tools/* | sort -V | tail -1)"
echo "==> Verifying signature"
"$BT/apksigner" verify --verbose --print-certs "$APK" | grep -E "Verifies|Signer #1 certificate (DN|SHA-256)|Verified using v[0-9]"

VERSION="$("$BT/aapt2" dump badging "$APK" | sed -n "s/.*versionName='\([^']*\)'.*/\1/p" | head -1)"
mkdir -p "$DIST"
cp "$APK" "$DIST/StreamCompanion-$VERSION.apk"
echo "==> Done: $DIST/StreamCompanion-$VERSION.apk ($(du -h "$APK" | cut -f1))"
