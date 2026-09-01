#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 2 ]]; then
  printf 'Usage: %s ADB_SERIAL OUTPUT_FILE\n' "$0" >&2
  exit 64
fi

DEVICE_SERIAL=$1
OUTPUT_FILE=$2
if ! adb devices | awk -v serial="$DEVICE_SERIAL" '
  NR > 1 && $1 == serial && $2 == "device" { found = 1 }
  END { exit(found ? 0 : 1) }
'; then
  printf 'ADB device is not connected and authorized: %s\n' "$DEVICE_SERIAL" >&2
  exit 69
fi

printf 'timestamp_utc\ttotal_pss_kib\n' > "$OUTPUT_FILE"
MAX_PSS=0
report_maximum() {
  printf 'Maximum TOTAL PSS: %s KiB\n' "$MAX_PSS"
}
trap report_maximum EXIT

while adb -s "$DEVICE_SERIAL" shell pidof com.isbkch.echolocal >/dev/null; do
  TOTAL_PSS=$(adb -s "$DEVICE_SERIAL" shell dumpsys meminfo com.isbkch.echolocal | awk '
    /TOTAL PSS:/ { print $3; exit }
    /^TOTAL[[:space:]]/ { print $2; exit }
  ')
  if [[ "$TOTAL_PSS" =~ ^[0-9]+$ ]]; then
    printf '%s\t%s\n' "$(date -u +%Y-%m-%dT%H:%M:%SZ)" "$TOTAL_PSS" >> "$OUTPUT_FILE"
    if (( TOTAL_PSS > MAX_PSS )); then MAX_PSS=$TOTAL_PSS; fi
  fi
  sleep 0.25
done
