#!/usr/bin/env bash
# Prints the launcher's PSS and RSS (KB) in two phases: idle on Home, then after cycling across all four tiles.
# Usage: scripts/measure-memory.sh [package]   (default family.tvhome; pass com.gathin.tvhome for the trial launcher)
set -u
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
source "$ROOT/scripts/env.sh"
PKG="${1:-family.tvhome}"
tv() { adb -s "$TV" shell "$@"; }
reading() { tv dumpsys meminfo "$PKG" | grep -E "TOTAL PSS|^ *TOTAL " | head -2 | tr -s ' '; }

echo "== $PKG idle on Home"
for i in 1 2 3; do reading; sleep 3; done
if [ "$PKG" = family.tvhome ]; then
  echo "== $PKG after cycling the tiles (suggestions showing)"
  for i in 1 2 3 4 5 6; do
    tv input keyevent KEYCODE_DPAD_RIGHT; sleep 6
    tv input keyevent KEYCODE_DPAD_LEFT; sleep 6
  done
  tv input keyevent KEYCODE_DPAD_DOWN; sleep 3
  reading
fi
