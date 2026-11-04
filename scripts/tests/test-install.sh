#!/usr/bin/env bash
# Tests install.sh and rollback.sh against a fake adb. Run: bash scripts/tests/test-install.sh
set -u
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT
PASSED=0; FAILED=0

cat > "$WORK/adb" <<'FAKE'
#!/usr/bin/env bash
echo "$*" >> "$FAKE_LOG"
case "$*" in
  *" install "*) echo "Success" ;;
  *"pm list packages -d"*) [ "${FAKE_STOCK_DISABLED:-0}" = 1 ] && echo "package:com.google.android.tvlauncher" ;;
  *"am start"*) [ "${FAKE_START:-ok}" = fail ] && echo "Error: Activity class does not exist" || { echo "Status: ok"; echo "Activity: family.tvhome/.HomeActivity"; } ;;
  *"resolve-activity"*) [ "${FAKE_HOME:-ours}" = ours ] && echo "family.tvhome/.HomeActivity" || echo "com.google.android.tvlauncher/.MainActivity" ;;
esac
exit 0
FAKE
chmod +x "$WORK/adb"
touch "$WORK/fake.apk"

run() { # name, expected_exit, env assignments..., -- script args...
  local name="$1" want="$2"; shift 2
  local envs=()
  while [ "$1" != "--" ]; do envs+=("$1"); shift; done; shift
  : > "$WORK/log"; rm -f "$WORK"/recovery-*.txt
  env ${envs[@]+"${envs[@]}"} ADB="$WORK/adb" FAKE_LOG="$WORK/log" SKIP_BUILD=1 APK="$WORK/fake.apk" TV_HOST=10.0.0.9 RECOVERY_DIR="$WORK" \
    bash "$ROOT/scripts/${SCRIPT:-install.sh}" "$@" > "$WORK/out" 2>&1
  GOT=$?
  if [ "$GOT" -ne "$want" ]; then echo "FAIL $name: exit $GOT, wanted $want"; cat "$WORK/out"; FAILED=$((FAILED+1)); return 1; fi
  return 0
}
line() { grep -n -- "$1" "$WORK/log" | head -1 | cut -d: -f1; }
has() { grep -q -- "$1" "$WORK/log"; }
ok() { echo "ok   $1"; PASSED=$((PASSED+1)); }
bad() { echo "FAIL $1"; FAILED=$((FAILED+1)); }

# (a) happy path order
if run happy 0 -- ; then
  i=$(line " install "); s=$(line "am start"); d=$(line "pm disable-user"); h=$(line "set-home-activity")
  if [ -n "$i" ] && [ -n "$s" ] && [ -n "$d" ] && [ -n "$h" ] && [ "$i" -lt "$s" ] && [ "$s" -lt "$d" ] && [ "$d" -lt "$h" ] && ! has "pm enable"; then ok "happy path order"; else bad "happy path order ($i $s $d $h)"; fi
fi
# (a2) the TV-listings permission is granted after install and before the launcher starts
if run grant 0 -- ; then
  i=$(line " install "); g=$(line "pm grant family.tvhome android.permission.READ_TV_LISTINGS"); s=$(line "am start")
  if [ -n "$g" ] && [ "$i" -lt "$g" ] && [ "$g" -lt "$s" ]; then ok "grants READ_TV_LISTINGS after install, before start"; else bad "grant order ($i $g $s)"; fi
fi
# (b) start fails: never disable stock
if run startfail 1 FAKE_START=fail -- ; then
  if has "am start" && ! has "pm disable-user"; then ok "start failure leaves stock launcher alone"; else bad "start failure"; fi
fi
# (b2) an update (stock launcher already disabled) whose new build does not start: Home must go back to stock
if run updatefail 1 FAKE_START=fail FAKE_STOCK_DISABLED=1 -- ; then
  if has "pm enable --user 0 com.google.android.tvlauncher" && has "set-home-activity --user 0 com.google.android.tvlauncher" \
     && ls "$WORK"/recovery-*.txt >/dev/null 2>&1 && ! grep -q "left untouched" "$WORK/out"; then ok "failed update recovers stock Home"; else bad "failed update did not recover"; fi
fi
# (c) Home resolves to stock: roll back
if run homestock 1 FAKE_HOME=stock -- ; then
  if has "pm enable --user 0 com.google.android.tvlauncher" && has "set-home-activity --user 0 com.google.android.tvlauncher" && ls "$WORK"/recovery-*.txt >/dev/null 2>&1; then ok "rollback and recovery file when Home is not ours"; else bad "homestock rollback"; fi
fi
# (d) trial removal only after the check passes
run notrial 0 -- ; if ! has "pm uninstall"; then ok "no trial uninstall by default"; else bad "unexpected uninstall"; fi
if run trial 0 -- --remove-trial; then
  u=$(line "pm uninstall --user 0 com.gathin.tvhome"); r=$(line "resolve-activity")
  if [ -n "$u" ] && [ -n "$r" ] && [ "$r" -lt "$u" ]; then ok "trial removed after resolve check"; else bad "trial removal order"; fi
fi
if run trialfail 1 FAKE_HOME=stock -- --remove-trial; then
  if has "resolve-activity" && ! has "pm uninstall"; then ok "trial kept when check fails"; else bad "trial removed despite failed check"; fi
fi
# rollback.sh
SCRIPT=rollback.sh
if run rollback 0 FAKE_HOME=stock -- ; then
  if has "pm enable --user 0 com.google.android.tvlauncher" && has "set-home-activity --user 0 com.google.android.tvlauncher" && ! has "pm uninstall"; then ok "rollback re-enables stock"; else bad "rollback steps"; fi
fi
if run rollbackun 0 FAKE_HOME=stock -- --uninstall; then
  if has "pm uninstall --user 0 family.tvhome"; then ok "rollback --uninstall removes ours"; else bad "rollback uninstall"; fi
fi
# rollback must fail, and must not remove ours, when Home did not go back to the stock launcher
if run rollbackfail 1 FAKE_HOME=ours -- --uninstall; then
  if ! has "pm uninstall"; then ok "rollback fails and keeps ours when Home is not stock"; else bad "rollback uninstalled ours although stock did not take Home"; fi
fi
echo "$PASSED passed, $FAILED failed"
[ "$FAILED" -eq 0 ]
