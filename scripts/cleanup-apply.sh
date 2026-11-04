#!/usr/bin/env bash
# Disables the packages in one round file (never uninstalls) and records each in the state file.
# Usage: scripts/cleanup-apply.sh cleanup/rounds/NN-name.txt
# Exit: 0 done; 2 refused (a line is not a package name, or a package is on the keep or never-touch list; nothing changed); 3 a disable failed
# or was not confirmed (earlier ones stay recorded); 4 the TV is not reachable, at the start or partway
# (a package whose disable may have run stays recorded; restoring an enabled package changes nothing).
# Approach follows CleanYourTV's tools/disable.sh (MIT). See NOTICE.
set -u
source "$(dirname "$0")/cleanup-lib.sh"
ROUND_FILE="${1:-}"
if [ -z "$ROUND_FILE" ] || [ ! -f "$ROUND_FILE" ]; then echo "usage: $0 ROUND_FILE (an existing file)" >&2; exit 2; fi
ROUND="$(basename "$ROUND_FILE" .txt)"

# Guard first (it needs no adb): one bad line refuses the whole round before any package is touched.
refused=0
while read -r pkg; do
  # adb shell hands its arguments to the TV's shell, so only plain package names may get that far.
  if ! [[ "$pkg" =~ ^[A-Za-z0-9_.]+$ ]]; then echo "refused: '$pkg' is not a package name"; refused=1; continue; fi
  if read_packages "$NEVER" | grep -qxF "$pkg"; then echo "refused: $pkg is on the never-touch list"; refused=1; fi
  if read_packages "$KEEP" | grep -qxF "$pkg"; then echo "refused: $pkg is on the keep list"; refused=1; fi
done < <(read_packages "$ROUND_FILE")
if [ "$refused" -ne 0 ]; then echo "Round refused; nothing was changed."; exit 2; fi

if ! reachable; then recovery_text; exit 4; fi

installed="$(tv_packages)" || { recovery_text; exit 4; }
disabled="$(tv_packages -d)" || { recovery_text; exit 4; }
mkdir -p "$(dirname "$STATE_FILE")"
while read -r pkg; do
  if ! grep -qxF "$pkg" <<<"$installed"; then echo "skipped: $pkg is not installed"; continue; fi
  if grep -qxF "$pkg" <<<"$disabled"; then echo "skipped: $pkg is already disabled"; continue; fi
  # Record first: if adb drops while the command runs, the package may be disabled and must stay restorable.
  printf '%s\t%s\t%s\n' "$ROUND" "$pkg" "$(date '+%Y-%m-%dT%H:%M:%S')" >> "$STATE_FILE"
  if ! tv shell pm disable-user --user 0 "$pkg" >/dev/null 2>&1; then
    if ! reachable; then recovery_text; exit 4; fi
    unrecord "$ROUND" "$pkg"; echo "failed: could not disable $pkg"; exit 3
  fi
  now_disabled="$(tv_packages -d)" || { recovery_text; exit 4; }
  if ! grep -qxF "$pkg" <<<"$now_disabled"; then unrecord "$ROUND" "$pkg"; echo "failed: $pkg is not shown as disabled afterwards"; exit 3; fi
  echo "disabled: $pkg"
done < <(read_packages "$ROUND_FILE")
exit 0
