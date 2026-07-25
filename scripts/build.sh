#!/bin/zsh
set -euo pipefail

SCRIPT_DIR=${0:A:h}
PROJECT_DIR=${SCRIPT_DIR:h}

cd "$PROJECT_DIR"

if ! command -v xcodegen >/dev/null 2>&1; then
  print -u2 "XcodeGen is required. Install it with: brew install xcodegen"
  exit 1
fi

xcodegen generate

xcodebuild \
  -project EchoLocal.xcodeproj \
  -scheme EchoLocal \
  -configuration Release \
  -derivedDataPath .build \
  build \
  CODE_SIGNING_ALLOWED=NO

APP_PATH="$PROJECT_DIR/.build/Build/Products/Release/Echolocal.app"
APP_FRAMEWORKS="$APP_PATH/Contents/Frameworks"
PACKAGE_FRAMEWORKS="$PROJECT_DIR/.build/Build/Products/Release/PackageFrameworks"
KOKORO_SOURCE="$PACKAGE_FRAMEWORKS/KokoroSwift.framework"
KOKORO_DESTINATION="$APP_FRAMEWORKS/KokoroSwift.framework"
APP_EXECUTABLE="$APP_PATH/Contents/MacOS/Echolocal"

if [[ ! -x "$KOKORO_SOURCE/Versions/A/KokoroSwift" ]]; then
  print -u2 "Build failed: KokoroSwift.framework was not produced."
  exit 1
fi

# Xcode 27 beta links this dynamic Swift package but intermittently omits its
# top-level framework from the app's embed phase. Copy it explicitly so the
# distributable bundle never depends on DerivedData.
mkdir -p "$APP_FRAMEWORKS"
ditto "$KOKORO_SOURCE" "$KOKORO_DESTINATION"

verify_rpath_dependencies() {
  local binary_path=$1
  local dependency
  local relative_path

  while IFS= read -r dependency; do
    relative_path=${dependency#@rpath/}
    if [[ ! -e "$APP_FRAMEWORKS/$relative_path" ]]; then
      print -u2 "Build failed: $(basename "$binary_path") requires missing $relative_path"
      exit 1
    fi
  done < <(
    otool -arch arm64 -L "$binary_path" \
      | awk '/^[[:space:]]+@rpath\// { print $1 }'
  )
}

verify_rpath_dependencies "$APP_EXECUTABLE"
verify_rpath_dependencies "$KOKORO_DESTINATION/Versions/A/KokoroSwift"

# This script creates a portable local-development build with an ad-hoc
# signature. Hardened runtime library validation requires a real signing team,
# so do not opt an ad-hoc bundle into it. Xcode/archives still use the project's
# ENABLE_HARDENED_RUNTIME setting with the developer's configured identity.
codesign --force --sign - "$APP_FRAMEWORKS/MisakiSwift.framework"
codesign --force --sign - "$KOKORO_DESTINATION"
codesign --force --sign - \
  --entitlements EchoLocal/EchoLocal.entitlements "$APP_PATH"

codesign --verify --deep --strict "$APP_PATH"

print
print "Built: $APP_PATH"
