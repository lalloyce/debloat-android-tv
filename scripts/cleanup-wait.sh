#!/usr/bin/env bash
# Waits for the TV to answer over adb and finish booting (use after a reboot).
# Env: WAIT_SECONDS (default 300), WAIT_INTERVAL (default 5). Exit: 0 ready; 4 timed out, with by-hand steps printed.
# Approach follows CleanYourTV's tooling (MIT). See NOTICE.
set -u
source "$(dirname "$0")/cleanup-lib.sh"
WAIT_SECONDS="${WAIT_SECONDS:-300}"
WAIT_INTERVAL="${WAIT_INTERVAL:-5}"
end=$((SECONDS + WAIT_SECONDS))
while :; do
  "$ADB" connect "$TV" >/dev/null 2>&1
  if reachable && [ "$(tv shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = 1 ]; then echo "TV ready"; exit 0; fi
  [ "$SECONDS" -ge "$end" ] && break
  sleep "$WAIT_INTERVAL"
done
recovery_text
exit 4
