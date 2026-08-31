#!/bin/zsh
set -euo pipefail

SCRIPT_DIR=${0:A:h}
DEFAULT_PROJECT_DIR=${SCRIPT_DIR:h}
PROJECT_DIR=${ECHOLOCAL_PROJECT_DIR:-$DEFAULT_PROJECT_DIR}
PROJECT_DIR=${PROJECT_DIR:A}
SIGNING_IDENTITY=${ECHOLOCAL_SIGNING_IDENTITY:-}

if [[ -z "$SIGNING_IDENTITY" ]]; then
  SIGNING_IDENTITY="-"
fi

cd "$PROJECT_DIR"

if ! command -v xcodegen >/dev/null 2>&1; then
  print -u2 "XcodeGen is required. Install it with: brew install xcodegen"
  exit 1
fi

xcodegen generate

typeset -a build_setting_overrides
build_setting_overrides=()

if [[ -n "${ECHOLOCAL_VERSION:-}" ]]; then
  if [[ ! "$ECHOLOCAL_VERSION" =~ '^[0-9]+\.[0-9]+\.[0-9]+$' ]]; then
    print -u2 "Build failed: ECHOLOCAL_VERSION must use X.Y.Z format."
    exit 1
  fi
  build_setting_overrides+=(MARKETING_VERSION="$ECHOLOCAL_VERSION")
fi

if [[ -n "${ECHOLOCAL_BUILD_NUMBER:-}" ]]; then
  if [[ ! "$ECHOLOCAL_BUILD_NUMBER" =~ '^[0-9]+(\.[0-9]+){0,2}$' ]]; then
    print -u2 "Build failed: ECHOLOCAL_BUILD_NUMBER must contain one to three integers."
    exit 1
  fi
  build_setting_overrides+=(CURRENT_PROJECT_VERSION="$ECHOLOCAL_BUILD_NUMBER")
fi

xcodebuild \
  -project EchoLocal.xcodeproj \
  -scheme EchoLocal \
  -configuration Release \
  -derivedDataPath .build \
  clean \
  build \
  CODE_SIGNING_ALLOWED=NO \
  "${build_setting_overrides[@]}"

APP_PATH="$PROJECT_DIR/.build/Build/Products/Release/Echolocal.app"
APP_FRAMEWORKS="$APP_PATH/Contents/Frameworks"
APP_EXECUTABLE="$APP_PATH/Contents/MacOS/Echolocal"

# Kokoro and Misaki are deliberately static. Embedding either framework would
# load a second copy of MLX and recreate the Objective-C class collisions this
# packaging topology is designed to prevent.
for forbidden_framework in KokoroSwift.framework MisakiSwift.framework; do
  if [[ -e "$APP_FRAMEWORKS/$forbidden_framework" ]]; then
    print -u2 "Build failed: static package was unexpectedly embedded as $forbidden_framework"
    exit 1
  fi
done

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

sign_code() {
  local target=$1
  shift
  local -a sign_arguments

  sign_arguments=(--force --sign "$SIGNING_IDENTITY")
  if [[ "$SIGNING_IDENTITY" != "-" ]]; then
    sign_arguments+=(--options runtime --timestamp)
  fi

  codesign "${sign_arguments[@]}" "$@" "$target"
}

# Local builds default to an ad-hoc signature. Release automation can provide
# a Developer ID Application identity through ECHOLOCAL_SIGNING_IDENTITY; that
# path enables hardened runtime and a secure timestamp for notarization.
sign_code "$APP_PATH" --entitlements EchoLocal/EchoLocal.entitlements

codesign --verify --deep --strict --verbose=2 "$APP_PATH"

print
print "Built: $APP_PATH"
if [[ "$SIGNING_IDENTITY" == "-" ]]; then
  print "Signing: ad-hoc (local/test distribution)"
else
  print "Signing: $SIGNING_IDENTITY"
fi
