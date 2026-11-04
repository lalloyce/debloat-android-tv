#!/usr/bin/env bash
# Installs this launcher on the TV and makes it Home. The stock launcher is disabled, never removed.
# The order (install, start, disable stock, set Home, verify, roll back on failure) follows the approach in
# CleanYourTV's launcher/build-and-install.sh (MIT, github.com/Gathin23/cleanyourtv). See NOTICE.
# Grants READ_TV_LISTINGS (needed for Netflix/YouTube suggestion rows).
# Usage: scripts/install.sh [--remove-trial]   (SKIP_BUILD=1 reuses the existing release APK)
set -u
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
[ -z "${ADB:-}" ] && source "$ROOT/scripts/env.sh"
ADB="${ADB:-adb}"
TV="${TV_HOST:?Set TV_HOST to the IP address of the TV, see scripts/env.local.sh.example}:5555"
OURS="family.tvhome/.HomeActivity"
STOCK="com.google.android.tvlauncher"
STOCK_HOME="$STOCK/.MainActivity"
TRIAL="com.gathin.tvhome"
APK="${APK:-$ROOT/app/build/outputs/apk/release/app-release.apk}"
RECOVERY_DIR="${RECOVERY_DIR:-$ROOT}"
REMOVE_TRIAL=0
[ "${1:-}" = "--remove-trial" ] && REMOVE_TRIAL=1
tv() { "$ADB" -s "$TV" "$@"; }

if [ "${SKIP_BUILD:-0}" != 1 ]; then
  (cd "$ROOT" && ./gradlew assembleRelease -q) || { echo "Build failed"; exit 1; }
fi
[ -f "$APK" ] || { echo "APK not found: $APK"; exit 1; }

recover() {
  tv shell pm enable --user 0 "$STOCK" >/dev/null 2>&1
  tv shell cmd package set-home-activity --user 0 "$STOCK_HOME" >/dev/null 2>&1
  cat > "$RECOVERY_DIR/recovery-${TV%%:*}.txt" <<REC
If the TV has no working Home screen, run these from the Mac:
  adb connect $TV
  adb -s $TV shell pm enable --user 0 $STOCK
  adb -s $TV shell cmd package set-home-activity --user 0 $STOCK_HOME
  adb -s $TV shell pm uninstall --user 0 family.tvhome
REC
  echo "Rolled back. Recovery steps saved to $RECOVERY_DIR/recovery-${TV%%:*}.txt"
}

tv install -r "$APK" 2>&1 | grep -q Success || { echo "Install failed"; exit 1; }
# The only permission beyond INTERNET: lets the launcher read the suggestion rows other apps publish. Dangerous-level, so it is granted here.
tv shell pm grant family.tvhome android.permission.READ_TV_LISTINGS >/dev/null 2>&1
# On an update the stock launcher is already disabled, so a build that fails to start would leave no working Home.
STOCK_WAS_DISABLED=0
tv shell pm list packages -d 2>/dev/null | tr -d '\r' | grep -qx "package:$STOCK" && STOCK_WAS_DISABLED=1
if ! tv shell am start -W -n "$OURS" 2>&1 | grep -q "Status: ok"; then
  if [ "$STOCK_WAS_DISABLED" = 1 ]; then echo "Launcher did not start; giving Home back to the stock launcher"; recover; else echo "Launcher did not start; stock launcher left untouched"; fi
  exit 1
fi
tv shell pm disable-user --user 0 "$STOCK" >/dev/null 2>&1
tv shell cmd package set-home-activity --user 0 "$OURS" >/dev/null 2>&1
HOME_NOW="$(tv shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME 2>&1 | tail -n 1 | tr -d '\r')"
case "$HOME_NOW" in
  family.tvhome/*) echo "Home is now $HOME_NOW" ;;
  *) echo "Home resolved to '$HOME_NOW', not our launcher"; recover; exit 1 ;;
esac
if [ "$REMOVE_TRIAL" = 1 ]; then
  tv shell pm uninstall --user 0 "$TRIAL" >/dev/null 2>&1 && echo "Trial launcher removed"
fi
exit 0
