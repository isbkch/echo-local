#!/usr/bin/env bash
# Native Android builds for Echolocal. Run with --help for setup and examples.
set -Eeuo pipefail

REPO_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ANDROID_DIR="$REPO_DIR/android"
ARTIFACT_DIR="$REPO_DIR/artifacts/android"
BUILD_TYPE="development"
MODE_SELECTED=false
SHOULD_INSTALL=false
SKIP_TESTS=false
SHOULD_CLEAN=false
DEVICE_SERIAL="${ANDROID_SERIAL:-}"
DEVICE_OPTION=false
VERSION_CODE=""
GRADLE_ARGS=(--no-daemon --console=plain)

info() { printf 'INFO  %s\n' "$*"; }
fail() { printf 'ERROR %s\n' "$*" >&2; exit 1; }

usage() {
  cat <<'EOF'
Usage: ./build-android-native.sh [development|internal|playstore] [options]

Build types:
  development   Debug-signed APK (default)
  internal      Debug-signed APK for local device testing; same app configuration
  playstore     Release-signed AAB; requires an upload keystore

Options:
  --install          Install the APK on one authorized device or emulator
  --device SERIAL    Select the install target (also accepts ANDROID_SERIAL)
  --version-code N   Override versionCode for this build without editing source
  --clean            Clean Android build outputs first
  --skip-tests       Skip lint and unit tests
  --help, -h         Show this help

Examples:
  ./build-android-native.sh
  ./build-android-native.sh internal --install --device DEVICE_SERIAL
  ./build-android-native.sh playstore --version-code 2

Requires JDK 21 and the Android SDK for the project's compileSdk (currently 36).
SDK lookup: android/local.properties, ANDROID_HOME, ANDROID_SDK_ROOT, then the
standard macOS/Linux SDK directories. Gradle uses its checked-in daemon JVM pin.

Set release signing values in ~/.gradle/gradle.properties (or the equivalent
under GRADLE_USER_HOME), or export environment variables with these names:
  ECHOLOCAL_ANDROID_KEYSTORE_PATH=/absolute/path/to/echolocal-upload.jks
  ECHOLOCAL_ANDROID_KEYSTORE_PASSWORD=...
  ECHOLOCAL_ANDROID_KEY_ALIAS=echolocal-upload
  ECHOLOCAL_ANDROID_KEY_PASSWORD=...

Signing environment variables take precedence over Gradle properties.
Artifacts and SHA-256 files are copied to artifacts/android/.
Version codes are never automatically incremented. Choose an unused, higher
code for each Play update. This script builds locally; it does not upload.
EOF
}

parse_arguments() {
  while [ "$#" -gt 0 ]; do
    case "$1" in
      development|internal|playstore)
        [ "$MODE_SELECTED" = false ] || fail "Specify only one build type."
        BUILD_TYPE="$1"
        MODE_SELECTED=true
        shift
        ;;
      --install) SHOULD_INSTALL=true; shift ;;
      --skip-tests) SKIP_TESTS=true; shift ;;
      --clean) SHOULD_CLEAN=true; shift ;;
      --device|--version-code)
        [ "$#" -ge 2 ] && [ -n "$2" ] && [[ "$2" != -* ]] || fail "$1 requires a value."
        if [ "$1" = --device ]; then
          DEVICE_SERIAL="$2"
          DEVICE_OPTION=true
        else
          VERSION_CODE="$2"
        fi
        shift 2
        ;;
      --help|-h) usage; exit 0 ;;
      *) fail "Unknown argument: $1. Run with --help for usage." ;;
    esac
  done
  if [ "$BUILD_TYPE" = playstore ] && [ "$SHOULD_INSTALL" = true ]; then
    fail "--install requires development or internal; an AAB cannot be installed directly."
  fi
  if [ "$DEVICE_OPTION" = true ] && [ "$SHOULD_INSTALL" = false ]; then
    fail "--device requires --install."
  fi
}

read_version() {
  local gradle_file="$ANDROID_DIR/app/build.gradle.kts"
  if [ -z "$VERSION_CODE" ]; then
    VERSION_CODE="$(sed -nE 's/^[[:space:]]*versionCode = ([0-9]+)[[:space:]]*$/\1/p' "$gradle_file")"
  fi
  [[ "$VERSION_CODE" =~ ^[1-9][0-9]{0,9}$ ]] || fail "versionCode must be a positive integer."
  [ "$VERSION_CODE" -le 2100000000 ] || fail "versionCode cannot exceed 2100000000."
  VERSION_NAME="$(sed -nE 's/^[[:space:]]*versionName = "([^"]+)"[[:space:]]*$/\1/p' "$gradle_file")"
  [[ "$VERSION_NAME" =~ ^[A-Za-z0-9][A-Za-z0-9._+-]*$ ]] || fail "Cannot read a filename-safe versionName from $gradle_file."
  GRADLE_ARGS+=("-PECHOLOCAL_ANDROID_VERSION_CODE=$VERSION_CODE")
}

java_major_version() {
  java -XshowSettings:properties -version 2>&1 \
    | awk -F'= ' '/java.specification.version =/ { print $2 }' | tr -d '\r'
}

configure_environment() {
  local java_major="" detected_java_home="" sdk_path="" candidate
  [ -x "$ANDROID_DIR/gradlew" ] || fail "Executable Gradle wrapper not found: $ANDROID_DIR/gradlew"
  if [ -n "${JAVA_HOME:-}" ]; then
    [ -x "$JAVA_HOME/bin/java" ] || fail "JAVA_HOME does not contain bin/java."
    export PATH="$JAVA_HOME/bin:$PATH"
  fi
  java_major="$(java_major_version || true)"
  if [ "$java_major" != 21 ] && [ -x /usr/libexec/java_home ]; then
    detected_java_home="$(/usr/libexec/java_home -v 21 2>/dev/null || true)"
    if [ -n "$detected_java_home" ]; then
      export JAVA_HOME="$detected_java_home"
      export PATH="$JAVA_HOME/bin:$PATH"
      java_major="$(java_major_version || true)"
    fi
  fi
  [ "$java_major" = 21 ] || fail "JDK 21 is required. Set JAVA_HOME to a JDK 21 installation."

  # Match Gradle's local SDK first; accept the usual escaped spaces/colons.
  if [ -f "$ANDROID_DIR/local.properties" ]; then
    sdk_path="$(sed -nE 's/^[[:space:]]*sdk\.dir[[:space:]]*=[[:space:]]*//p' "$ANDROID_DIR/local.properties" \
      | sed 's/\\ / /g; s/\\:/:/g; s/\r$//')"
  fi
  sdk_path="${sdk_path:-${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}}"
  if [ -z "$sdk_path" ]; then
    for candidate in "$HOME/Library/Android/sdk" "$HOME/Android/Sdk"; do
      if [ -d "$candidate" ]; then sdk_path="$candidate"; break; fi
    done
  fi
  [ -n "$sdk_path" ] && [ -d "$sdk_path" ] || fail "Android SDK not found. Set ANDROID_HOME or android/local.properties."
  export ANDROID_HOME="$sdk_path"
  export ANDROID_SDK_ROOT="$sdk_path"

  command -v unzip >/dev/null || fail "unzip is required to verify artifacts."
  if command -v shasum >/dev/null; then
    CHECKSUM_CMD=(shasum -a 256)
  elif command -v sha256sum >/dev/null; then
    CHECKSUM_CMD=(sha256sum)
  else
    fail "Install shasum or sha256sum to write artifact checksums."
  fi
  if [ "$BUILD_TYPE" = playstore ]; then
    command -v jarsigner >/dev/null || fail "jarsigner is required to verify release bundles."
    GRADLE_ARGS+=(-PECHOLOCAL_ANDROID_REQUIRE_SIGNING=true --no-configuration-cache)
  else
    APKSIGNER=""
    for candidate in "$ANDROID_HOME"/build-tools/*/apksigner; do
      if [ -x "$candidate" ]; then APKSIGNER="$candidate"; fi
    done
    [ -n "$APKSIGNER" ] || fail "Install Android SDK Build-Tools (apksigner is missing)."
  fi
  info "JDK 21; Android SDK: $sdk_path"
}

select_device() {
  local devices authorized_count
  ADB="$ANDROID_HOME/platform-tools/adb"
  [ -x "$ADB" ] || fail "Install Android SDK Platform-Tools to use --install."
  devices="$("$ADB" devices)"
  if [ -n "$DEVICE_SERIAL" ]; then
    printf '%s\n' "$devices" | awk -v serial="$DEVICE_SERIAL" \
      '$1 == serial && $2 == "device" { found = 1 } END { exit !found }' \
      || fail "Selected device is not connected and authorized: $DEVICE_SERIAL"
  else
    authorized_count="$(printf '%s\n' "$devices" | awk '$2 == "device" { count++ } END { print count+0 }')"
    [ "$authorized_count" -gt 0 ] || fail "No authorized Android device or emulator is connected."
    [ "$authorized_count" -eq 1 ] || fail "Multiple devices are connected. Use --device SERIAL or ANDROID_SERIAL."
    DEVICE_SERIAL="$(printf '%s\n' "$devices" | awk '$2 == "device" { print $1 }')"
  fi
  info "Install target: $DEVICE_SERIAL"
}

run_gradle() {
  (cd "$ANDROID_DIR" && ./gradlew "${GRADLE_ARGS[@]}" "$@")
}

write_checksum() {
  (cd "$(dirname "$1")" && "${CHECKSUM_CMD[@]}" "$(basename "$1")" > "$(basename "$1").sha256")
}

build_and_package() {
  local variant=Debug task=:app:assembleDebug extension=apk source_artifact signature_report
  local tasks=()
  if [ "$BUILD_TYPE" = playstore ]; then
    variant=Release
    task=:app:bundleRelease
    extension=aab
    source_artifact="$ANDROID_DIR/app/build/outputs/bundle/release/app-release.aab"
    # Fail before tests/build work if signing material is missing or incomplete.
    run_gradle :app:validateSigningRelease
  else
    source_artifact="$ANDROID_DIR/app/build/outputs/apk/debug/app-debug.apk"
  fi
  if [ "$SHOULD_CLEAN" = true ]; then run_gradle clean; fi
  if [ "$SKIP_TESTS" = false ]; then
    tasks+=(":app:lint${variant}" :app:testDebugUnitTest)
  else
    info "Skipping lint and unit tests (--skip-tests)."
  fi
  tasks+=("$task")
  info "Building $BUILD_TYPE $VERSION_NAME ($VERSION_CODE)"
  run_gradle "${tasks[@]}"
  [ -s "$source_artifact" ] || fail "Gradle completed without the expected artifact: $source_artifact"
  unzip -tq "$source_artifact" >/dev/null || fail "Artifact ZIP integrity check failed."
  if [ "$BUILD_TYPE" = playstore ]; then
    signature_report="$(LC_ALL=C jarsigner -J-Duser.language=en -J-Duser.country=US -verify "$source_artifact" 2>&1)" \
      || fail "Release bundle signature verification failed."
    [[ "$signature_report" == *'jar verified.'* ]] || fail "Release bundle is unsigned."
  else
    "$APKSIGNER" verify "$source_artifact" || fail "APK signature verification failed."
  fi
  mkdir -p "$ARTIFACT_DIR"
  COPIED_ARTIFACT="$ARTIFACT_DIR/echolocal-${BUILD_TYPE}-${VERSION_NAME}-${VERSION_CODE}.${extension}"
  cp "$source_artifact" "$COPIED_ARTIFACT"
  write_checksum "$source_artifact"
  write_checksum "$COPIED_ARTIFACT"
  printf '\nArtifact: %s\nSHA-256:  %s\n' "$COPIED_ARTIFACT" "$COPIED_ARTIFACT.sha256"
}

main() {
  parse_arguments "$@"
  read_version
  configure_environment
  if [ "$SHOULD_INSTALL" = true ]; then select_device; fi
  build_and_package
  if [ "$SHOULD_INSTALL" = true ]; then
    "$ADB" -s "$DEVICE_SERIAL" install -r "$COPIED_ARTIFACT"
    info "APK installed on $DEVICE_SERIAL"
  fi
  info "Android $BUILD_TYPE build completed."
}

main "$@"
