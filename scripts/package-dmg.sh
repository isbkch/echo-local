#!/bin/zsh
set -euo pipefail

SCRIPT_DIR=${0:A:h}
DEFAULT_PROJECT_DIR=${SCRIPT_DIR:h}
PROJECT_DIR=${ECHOLOCAL_PROJECT_DIR:-$DEFAULT_PROJECT_DIR}
PROJECT_DIR=${PROJECT_DIR:A}
RAW_VERSION=${1:-${ECHOLOCAL_VERSION:-}}
VERSION=${RAW_VERSION#v}
BUILD_NUMBER=${ECHOLOCAL_BUILD_NUMBER:-${VERSION##*.}}
ARTIFACT_DIR=${ECHOLOCAL_ARTIFACT_DIR:-$PROJECT_DIR/dist}
SIGNING_IDENTITY=${ECHOLOCAL_SIGNING_IDENTITY:-}

if [[ -z "$RAW_VERSION" || ! "$VERSION" =~ '^[0-9]+\.[0-9]+\.[0-9]+$' ]]; then
  print -u2 "Usage: $0 <X.Y.Z or vX.Y.Z>"
  exit 1
fi

if [[ -z "$SIGNING_IDENTITY" ]]; then
  SIGNING_IDENTITY="-"
fi

APP_PATH="$PROJECT_DIR/.build/Build/Products/Release/Echolocal.app"
DMG_PATH="$ARTIFACT_DIR/Echolocal-$VERSION.dmg"
CHECKSUM_PATH="$DMG_PATH.sha256"
STAGING_DIR=$(mktemp -d /tmp/echolocal-dmg.XXXXXX)

cleanup() {
  if [[ -n "${STAGING_DIR:-}" && -d "$STAGING_DIR" && "$STAGING_DIR" == /tmp/echolocal-dmg.* ]]; then
    rm -rf -- "$STAGING_DIR"
  fi
}
trap cleanup EXIT INT TERM

ECHOLOCAL_PROJECT_DIR="$PROJECT_DIR" \
ECHOLOCAL_VERSION="$VERSION" \
ECHOLOCAL_BUILD_NUMBER="$BUILD_NUMBER" \
ECHOLOCAL_SIGNING_IDENTITY="$SIGNING_IDENTITY" \
  "$SCRIPT_DIR/build.sh"

mkdir -p "$ARTIFACT_DIR"
ditto "$APP_PATH" "$STAGING_DIR/Echolocal.app"
ln -s /Applications "$STAGING_DIR/Applications"

hdiutil create \
  -volname "Echolocal $VERSION" \
  -srcfolder "$STAGING_DIR" \
  -format UDZO \
  -ov \
  "$DMG_PATH"

if [[ "$SIGNING_IDENTITY" != "-" ]]; then
  codesign --force --timestamp --sign "$SIGNING_IDENTITY" "$DMG_PATH"
  codesign --verify --verbose=2 "$DMG_PATH"
fi

if [[ -n "${ECHOLOCAL_NOTARY_PROFILE:-}" ]]; then
  if [[ "$SIGNING_IDENTITY" == "-" ]]; then
    print -u2 "Packaging failed: notarization requires a Developer ID signature."
    exit 1
  fi
  xcrun notarytool submit \
    "$DMG_PATH" \
    --keychain-profile "$ECHOLOCAL_NOTARY_PROFILE" \
    --wait
elif [[ -n "${ECHOLOCAL_NOTARY_KEY:-}" || -n "${ECHOLOCAL_NOTARY_KEY_ID:-}" || -n "${ECHOLOCAL_NOTARY_ISSUER_ID:-}" ]]; then
  if [[ "$SIGNING_IDENTITY" == "-" ]]; then
    print -u2 "Packaging failed: notarization requires a Developer ID signature."
    exit 1
  fi
  if [[ -z "${ECHOLOCAL_NOTARY_KEY:-}" || -z "${ECHOLOCAL_NOTARY_KEY_ID:-}" || -z "${ECHOLOCAL_NOTARY_ISSUER_ID:-}" ]]; then
    print -u2 "Packaging failed: the notary key, key ID, and issuer ID must be provided together."
    exit 1
  fi
  xcrun notarytool submit \
    "$DMG_PATH" \
    --key "$ECHOLOCAL_NOTARY_KEY" \
    --key-id "$ECHOLOCAL_NOTARY_KEY_ID" \
    --issuer "$ECHOLOCAL_NOTARY_ISSUER_ID" \
    --wait
fi

if [[ -n "${ECHOLOCAL_NOTARY_PROFILE:-}" || -n "${ECHOLOCAL_NOTARY_KEY:-}" ]]; then
  xcrun stapler staple "$DMG_PATH"
  xcrun stapler validate "$DMG_PATH"
fi

hdiutil verify "$DMG_PATH"
(
  cd "$ARTIFACT_DIR"
  shasum -a 256 "${DMG_PATH:t}" > "${CHECKSUM_PATH:t}"
  shasum -a 256 --check "${CHECKSUM_PATH:t}"
)

print
print "Packaged: $DMG_PATH"
print "Checksum: $CHECKSUM_PATH"
if [[ "$SIGNING_IDENTITY" == "-" ]]; then
  print "Trust: ad-hoc signed; configure Developer ID secrets before public distribution."
elif [[ -n "${ECHOLOCAL_NOTARY_PROFILE:-}" || -n "${ECHOLOCAL_NOTARY_KEY:-}" ]]; then
  print "Trust: Developer ID signed and notarized."
else
  print "Trust: Developer ID signed but not notarized."
fi
