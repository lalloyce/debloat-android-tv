#!/usr/bin/env bash
# Gives Home back to the stock launcher. With --uninstall it also removes this launcher.
# Approach follows CleanYourTV's rollback (MIT). See NOTICE.
set -u
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
[ -z "${ADB:-}" ] && source "$ROOT/scripts/env.sh"
ADB="${ADB:-adb}"
TV="${TV_HOST:?Set TV_HOST to the IP address of the TV, see scripts/env.local.sh.example}:5555"
STOCK="com.google.android.tvlauncher"
STOCK_HOME="$STOCK/.MainActivity"
tv() { "$ADB" -s "$TV" "$@"; }

tv shell pm enable --user 0 "$STOCK" >/dev/null 2>&1
tv shell cmd package set-home-activity --user 0 "$STOCK_HOME" >/dev/null 2>&1
HOME_NOW="$(tv shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME 2>&1 | tail -n 1 | tr -d '\r')"
echo "Home resolves to: $HOME_NOW"
# Fail unless the stock launcher took Home back; removing ours then could leave the TV on a fallback Home.
case "$HOME_NOW" in
  "$STOCK"/*) ;;
  *) echo "Home did not go back to the stock launcher; nothing was removed."; exit 1 ;;
esac
if [ "${1:-}" = "--uninstall" ]; then
  tv shell pm uninstall --user 0 family.tvhome >/dev/null 2>&1 && echo "Launcher removed"
fi
exit 0
