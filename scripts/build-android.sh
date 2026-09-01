#!/usr/bin/env bash
set -euo pipefail

REPO_ROOT=$(cd "$(dirname "$0")/.." && pwd)
cd "$REPO_ROOT/android"
./gradlew :app:testDebugUnitTest :app:assembleDebug

APK="$REPO_ROOT/android/app/build/outputs/apk/debug/app-debug.apk"
test -f "$APK"
shasum -a 256 "$APK" > "$APK.sha256"
printf 'APK: %s\nSHA-256: %s\n' "$APK" "$APK.sha256"
