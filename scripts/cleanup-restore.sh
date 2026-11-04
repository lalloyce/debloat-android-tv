#!/usr/bin/env bash
# Re-enables the packages recorded in the state file: all of them, or only one round's.
# Usage: scripts/cleanup-restore.sh [round-name]
# Packages that were already disabled before this tooling ran were never recorded, so they are left alone.
# Round names are the round files' names without ".txt" (for example 03-tguard).
# Exit: 0 done (also when nothing is recorded); 2 no recorded round has that name; 3 an enable was not confirmed (its line stays recorded);
# 4 the TV is not reachable, at the start or partway (lines not yet confirmed stay recorded).
# Approach follows CleanYourTV's tools/restore.sh (MIT). See NOTICE.
set -u
source "$(dirname "$0")/cleanup-lib.sh"
ONLY_ROUND="${1:-}"

if ! reachable; then recovery_text; exit 4; fi
if [ ! -s "$STATE_FILE" ]; then echo "Nothing recorded to restore."; exit 0; fi

if [ -n "$ONLY_ROUND" ] && ! cut -f1 "$STATE_FILE" | grep -qxF "$ONLY_ROUND"; then
  echo "No recorded round named $ONLY_ROUND. Recorded rounds: $(cut -f1 "$STATE_FILE" | sort -u | tr '\n' ' ')"; exit 2
fi

while IFS=$'\t' read -r round pkg _; do
  [ -n "$ONLY_ROUND" ] && [ "$round" != "$ONLY_ROUND" ] && continue
  tv shell pm enable --user 0 "$pkg" >/dev/null 2>&1
  still_disabled="$(tv_packages -d)" || { recovery_text; exit 4; }
  if grep -qxF "$pkg" <<<"$still_disabled"; then echo "failed: $pkg is still disabled"; exit 3; fi
  unrecord "$round" "$pkg"
  echo "enabled: $pkg"
done < <(cat "$STATE_FILE")
exit 0
