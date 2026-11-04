#!/usr/bin/env bash
# Memory measurement with one fixed protocol, so before and after are comparable:
# wake the TV, Home, idle, YouTube, Home, Netflix, Home, then read `dumpsys meminfo`.
# (Reboot and wait first, only when you are ready to restart the TV: `adb reboot`, then scripts/cleanup-wait.sh.)
# Usage: scripts/cleanup-measure.sh LABEL           writes cleanup/measurements/LABEL.txt
#        scripts/cleanup-measure.sh --summarize FILE  prints total_kb, free_kb, zram_physical_kb, swap_kb
# Exit: 0 written; 2 the label already has a file (FORCE=1 overwrites it); 3 the TV returned no memory data, nothing written;
# 4 the TV is not reachable.
# Env: IDLE_SECONDS (default 180), APP_SECONDS (default 60), OUT_DIR.
# Approach follows CleanYourTV's tooling (MIT). See NOTICE.
set -u
source "$(dirname "$0")/cleanup-lib.sh"
OUT_DIR="${OUT_DIR:-$ROOT/cleanup/measurements}"
IDLE_SECONDS="${IDLE_SECONDS:-180}"
APP_SECONDS="${APP_SECONDS:-60}"

num() { tr -d ','; }
summarize() {
  local f="$1" total free zram swap
  total="$(sed -n 's/^ *Total RAM: *\([0-9,]*\)K.*/\1/p' "$f" | num)"
  free="$(sed -n 's/^ *Free RAM: *\([0-9,]*\)K.*/\1/p' "$f" | num)"
  zram="$(sed -n 's/^ *ZRAM: *\([0-9,]*\)K physical used for *\([0-9,]*\)K in swap.*/\1/p' "$f" | num)"
  swap="$(sed -n 's/^ *ZRAM: *\([0-9,]*\)K physical used for *\([0-9,]*\)K in swap.*/\2/p' "$f" | num)"
  echo "total_kb=$total free_kb=$free zram_physical_kb=$zram swap_kb=$swap"
}
if [ "${1:-}" = "--summarize" ]; then summarize "${2:?usage: $0 --summarize FILE}"; exit 0; fi

LABEL="${1:?usage: $0 LABEL}"
if [ -e "$OUT_DIR/$LABEL.txt" ] && [ "${FORCE:-0}" != 1 ]; then
  echo "$OUT_DIR/$LABEL.txt already exists; pick another label or set FORCE=1 to overwrite it."; exit 2
fi
if ! reachable; then recovery_text; exit 4; fi
launch() { tv shell monkey -p "$1" -c android.intent.category.LEANBACK_LAUNCHER 1 >/dev/null 2>&1; }

tv shell input keyevent KEYCODE_WAKEUP; tv shell input keyevent KEYCODE_HOME
sleep "$IDLE_SECONDS"
launch com.google.android.youtube.tv; sleep "$APP_SECONDS"; tv shell input keyevent KEYCODE_HOME
launch com.netflix.ninja; sleep "$APP_SECONDS"; tv shell input keyevent KEYCODE_HOME
sleep 3

mkdir -p "$OUT_DIR"
raw="$(tv shell dumpsys meminfo | tr -d '\r')"
# A dropped connection returns nothing; a header-only file would read as a measurement.
if ! grep -q "Total RAM" <<<"$raw"; then echo "No memory data came back from the TV; nothing was written."; exit 3; fi
{
  echo "# $LABEL, measured $(date '+%Y-%m-%dT%H:%M:%S')"
  grep -E "^ *(Total|Free|Used|Lost) RAM:|^ *ZRAM:" <<<"$raw"
  grep -A20 "^Total PSS by process" <<<"$raw"
} > "$OUT_DIR/$LABEL.txt"
echo "Wrote $OUT_DIR/$LABEL.txt"
summarize "$OUT_DIR/$LABEL.txt"
