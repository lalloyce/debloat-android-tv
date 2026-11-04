#!/usr/bin/env bash
# Tests cleanup-apply.sh, cleanup-restore.sh, cleanup-wait.sh and cleanup-measure.sh against a fake adb.
# Run: bash scripts/tests/test-cleanup.sh
set -u
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT
PASSED=0; FAILED=0
DEV="$WORK/dev"; mkdir -p "$DEV"

# A fake adb that keeps "installed" and "disabled" package lists in $DEV.
#   FAKE_FAIL=<package>  makes that package's disable fail.   FAKE_DOWN=1  makes the TV unreachable.
#   FAKE_DOWN_AFTER=<n>  lets the first n adb calls work, then every call fails (the connection drops mid-run).
#   FAKE_ENABLE_FAIL=<package>  makes that package's enable fail (exit 1, stays disabled).
#   FAKE_LIST_FAIL=1  makes every `pm list packages` exit 1 while the TV still answers get-state.
#   FAKE_NO_MEMINFO=1  makes `dumpsys meminfo` print nothing.
cat > "$WORK/adb" <<'FAKE'
#!/usr/bin/env bash
echo "$*" >> "$FAKE_LOG"
# The real adb shell reads stdin, which swallows the rest of a `while read` loop's input.
case "$*" in *" shell "*) cat > /dev/null ;; esac
n=$(( $(cat "$FAKE_DIR/count" 2>/dev/null || echo 0) + 1 )); echo "$n" > "$FAKE_DIR/count"
if [ -n "${FAKE_DOWN_AFTER:-}" ] && [ "$n" -gt "$FAKE_DOWN_AFTER" ]; then echo "error: device offline" >&2; exit 1; fi
if [ "${FAKE_LIST_FAIL:-0}" = 1 ]; then case "$*" in *"pm list packages"*) echo "error: closed" >&2; exit 1 ;; esac; fi
case "$*" in
  connect*) if [ "${FAKE_DOWN:-0}" = 1 ]; then echo "failed to connect"; exit 1; else echo connected; fi ;;
  *"getprop sys.boot_completed"*) echo 1 ;;
  *"dumpsys meminfo"*) [ "${FAKE_NO_MEMINFO:-0}" = 1 ] || cat "$FAKE_DIR/meminfo.txt" ;;
  *get-state*) if [ "${FAKE_DOWN:-0}" = 1 ]; then echo "error: device offline" >&2; exit 1; else echo device; fi ;;
  *"pm list packages -d"*) sed 's/^/package:/' "$FAKE_DIR/disabled.txt" ;;
  *"pm list packages"*) sed 's/^/package:/' "$FAKE_DIR/installed.txt" ;;
  *"pm disable-user"*)
    args="$*"; pkg="${args##* }"
    if [ "${FAKE_FAIL:-}" = "$pkg" ]; then echo "Exception: cannot disable $pkg"; exit 1; fi
    echo "$pkg" >> "$FAKE_DIR/disabled.txt"; echo "Package $pkg new state: disabled-user" ;;
  *"pm enable"*)
    args="$*"; pkg="${args##* }"
    if [ "${FAKE_ENABLE_FAIL:-}" = "$pkg" ]; then echo "Exception: cannot enable $pkg"; exit 1; fi
    grep -vxF "$pkg" "$FAKE_DIR/disabled.txt" > "$FAKE_DIR/tmp"; mv "$FAKE_DIR/tmp" "$FAKE_DIR/disabled.txt"
    echo "Package $pkg new state: enabled" ;;
esac
exit 0
FAKE
chmod +x "$WORK/adb"

ok() { echo "ok   $1"; PASSED=$((PASSED+1)); }
bad() { echo "FAIL $1"; FAILED=$((FAILED+1)); }
has() { grep -q -- "$1" "$WORK/log"; }

# reset <installed packages...>: a fresh TV with those packages installed, nothing disabled, no state.
reset() {
  : > "$WORK/log"; : > "$DEV/disabled.txt"; : > "$DEV/installed.txt"; rm -f "$DEV/count"
  for p in "$@"; do echo "$p" >> "$DEV/installed.txt"; done
  rm -rf "$WORK/state" "$WORK/meas"; : > "$WORK/baseline.txt"
  cp "$ROOT/scripts/tests/fixtures/meminfo-sample.txt" "$DEV/meminfo.txt"
}
round() { printf '%s\n' "$@" > "$WORK/round-01.txt"; }  # writes the round file
# runit <script> [args...]: runs a script with the fake adb; sets GOT to its exit code, output in $WORK/out.
runit() {
  local script="$1"; shift
  env ADB="$WORK/adb" FAKE_LOG="$WORK/log" FAKE_DIR="$DEV" TV_HOST=10.0.0.9 \
    STATE_FILE="$WORK/state/disabled.tsv" BASELINE="$WORK/baseline.txt" \
    IDLE_SECONDS=0 APP_SECONDS=0 OUT_DIR="$WORK/meas" WAIT_SECONDS="${WAIT_SECONDS:-3}" WAIT_INTERVAL=1 \
    NEVER="$ROOT/cleanup/never-touch.txt" KEEP="$ROOT/cleanup/keep.txt" \
    ${FAKE_FAIL:+FAKE_FAIL="$FAKE_FAIL"} ${FAKE_DOWN:+FAKE_DOWN="$FAKE_DOWN"} ${FAKE_DOWN_AFTER:+FAKE_DOWN_AFTER="$FAKE_DOWN_AFTER"} \
    ${FAKE_ENABLE_FAIL:+FAKE_ENABLE_FAIL="$FAKE_ENABLE_FAIL"} ${FAKE_LIST_FAIL:+FAKE_LIST_FAIL="$FAKE_LIST_FAIL"} ${FAKE_NO_MEMINFO:+FAKE_NO_MEMINFO="$FAKE_NO_MEMINFO"} ${FORCE:+FORCE="$FORCE"} \
    bash "$ROOT/scripts/$script" "$@" < /dev/null > "$WORK/out" 2>&1
  GOT=$?
}
state_pkgs() { [ -f "$WORK/state/disabled.tsv" ] && cut -f2 "$WORK/state/disabled.tsv" || true; }

# ---- cleanup-apply.sh ----
reset a.ok com.android.bluetooth; round a.ok com.android.bluetooth
runit cleanup-apply.sh "$WORK/round-01.txt"
if [ "$GOT" -eq 2 ] && ! has "pm disable-user" && [ -z "$(state_pkgs)" ]; then ok "refuses_never_touch_and_changes_nothing"; else bad "refuses_never_touch_and_changes_nothing (exit $GOT)"; fi

reset com.android.vending; round com.android.vending
runit cleanup-apply.sh "$WORK/round-01.txt"
if [ "$GOT" -eq 2 ] && ! has "pm disable-user"; then ok "refuses_keep_list_package"; else bad "refuses_keep_list_package (exit $GOT)"; fi

reset b.disabled; echo b.disabled > "$DEV/disabled.txt"; round a.missing b.disabled
runit cleanup-apply.sh "$WORK/round-01.txt"
if [ "$GOT" -eq 0 ] && [ -z "$(state_pkgs)" ] && grep -qi skipped "$WORK/out" && ! has "pm disable-user"; then ok "skips_missing_and_already_disabled"; else bad "skips_missing_and_already_disabled (exit $GOT)"; fi

reset a.one a.two; round a.one a.two
runit cleanup-apply.sh "$WORK/round-01.txt"
if [ "$GOT" -eq 0 ] && [ "$(state_pkgs | tr '\n' ' ')" = "a.one a.two " ] \
   && [ "$(cut -f1 "$WORK/state/disabled.tsv" | sort -u)" = "round-01" ] \
   && grep -qx a.one "$DEV/disabled.txt" && grep -qx a.two "$DEV/disabled.txt"; then ok "records_state_lines"; else bad "records_state_lines (exit $GOT)"; fi

reset a.one a.two a.three; round a.one a.two a.three; FAKE_FAIL=a.two
runit cleanup-apply.sh "$WORK/round-01.txt"; unset FAKE_FAIL
if [ "$GOT" -eq 3 ] && [ "$(state_pkgs | tr '\n' ' ')" = "a.one " ] && ! has "disable-user --user 0 a.three"; then ok "partial_failure_stops_and_keeps_record"; else bad "partial_failure_stops_and_keeps_record (exit $GOT)"; fi

reset a.one; round a.one; FAKE_DOWN=1
runit cleanup-apply.sh "$WORK/round-01.txt"; unset FAKE_DOWN
if [ "$GOT" -eq 4 ] && grep -q "Developer options" "$WORK/out" && grep -q "Settings > Apps" "$WORK/out" && [ ! -f "$WORK/state/disabled.tsv" ]; then ok "unreachable_exits_4_with_recovery_text"; else bad "unreachable_exits_4_with_recovery_text (exit $GOT)"; fi

reset a.one; printf '# a comment\n\n   \na.one   # role; evidence; 5 MB\n' > "$WORK/round-01.txt"
runit cleanup-apply.sh "$WORK/round-01.txt"
if [ "$GOT" -eq 0 ] && [ "$(state_pkgs | tr '\n' ' ')" = "a.one " ]; then ok "comments_and_blank_lines_ignored"; else bad "comments_and_blank_lines_ignored (exit $GOT)"; fi

# The connection drops partway: the script stops with exit 4, never reports a half-done step as done or as nothing to do.
reset a.one; round a.one; FAKE_DOWN_AFTER=1
runit cleanup-apply.sh "$WORK/round-01.txt"; unset FAKE_DOWN_AFTER
if [ "$GOT" -eq 4 ] && ! has "pm disable-user" && [ -z "$(state_pkgs)" ] && ! grep -qi "not installed" "$WORK/out"; then ok "apply_drop_before_package_list_exits_4_not_skipped"; else bad "apply_drop_before_package_list_exits_4_not_skipped (exit $GOT: $(cat "$WORK/out"))"; fi

reset a.one; round a.one; FAKE_DOWN_AFTER=2
runit cleanup-apply.sh "$WORK/round-01.txt"; unset FAKE_DOWN_AFTER
if [ "$GOT" -eq 4 ] && ! has "pm disable-user" && [ -z "$(state_pkgs)" ]; then ok "apply_drop_before_disabled_list_exits_4_and_changes_nothing"; else bad "apply_drop_before_disabled_list_exits_4_and_changes_nothing (exit $GOT)"; fi

reset a.one; round a.one; FAKE_DOWN_AFTER=4
runit cleanup-apply.sh "$WORK/round-01.txt"; unset FAKE_DOWN_AFTER
if [ "$GOT" -eq 4 ] && grep -q "Developer options" "$WORK/out" && [ "$(state_pkgs | tr '\n' ' ')" = "a.one " ] && grep -qx a.one "$DEV/disabled.txt"; then ok "apply_drop_after_disable_keeps_the_record"; else bad "apply_drop_after_disable_keeps_the_record (exit $GOT)"; fi

reset a.one; round a.one; FAKE_DOWN_AFTER=3
runit cleanup-apply.sh "$WORK/round-01.txt"; unset FAKE_DOWN_AFTER
if [ "$GOT" -eq 4 ] && [ "$(state_pkgs | tr '\n' ' ')" = "a.one " ]; then ok "apply_drop_during_disable_keeps_the_record"; else bad "apply_drop_during_disable_keeps_the_record (exit $GOT)"; fi

# The guard needs no adb, so it runs before the reachability check.
reset com.android.bluetooth; round com.android.bluetooth; FAKE_DOWN=1
runit cleanup-apply.sh "$WORK/round-01.txt"; unset FAKE_DOWN
if [ "$GOT" -eq 2 ]; then ok "guard_runs_before_reachability_check"; else bad "guard_runs_before_reachability_check (exit $GOT)"; fi

# Packages added to the never-touch list after the review (roles inferred, not read from this TV).
for p in com.android.shell com.google.android.gsf com.android.externalstorage com.android.providers.downloads com.google.android.ext.services; do
  reset "$p"; round "$p"
  runit cleanup-apply.sh "$WORK/round-01.txt"
  if [ "$GOT" -eq 2 ] && ! has "pm disable-user"; then ok "refuses_never_touch_$p"; else bad "refuses_never_touch_$p (exit $GOT)"; fi
done

# A round line that is not a plain package name never reaches the TV's shell.
reset a.one; round 'a.one; reboot'
runit cleanup-apply.sh "$WORK/round-01.txt"
if [ "$GOT" -eq 2 ] && ! grep -q reboot "$WORK/log" && grep -q "not a package name" "$WORK/out" && ! grep -q "shell" "$WORK/log"; then ok "refuses_round_line_that_is_not_a_package_name"; else bad "refuses_round_line_that_is_not_a_package_name (exit $GOT)"; fi
reset a.one; round 'a.one' '$(id)'
runit cleanup-apply.sh "$WORK/round-01.txt"
if [ "$GOT" -eq 2 ] && [ -z "$(state_pkgs)" ]; then ok "refuses_command_substitution_line_and_changes_nothing"; else bad "refuses_command_substitution_line_and_changes_nothing (exit $GOT)"; fi

# Recovery text starts with the simplest step.
reset a.one; round a.one; FAKE_DOWN=1
runit cleanup-apply.sh "$WORK/round-01.txt"; unset FAKE_DOWN
if grep -q "adb connect" "$WORK/out" && grep -q "cleanup-wait.sh" "$WORK/out"; then ok "recovery_text_suggests_reconnect_first"; else bad "recovery_text_suggests_reconnect_first"; fi

# adb answers get-state but a package listing fails: stop with exit 4, never read it as an empty list.
reset a.one; round a.one; FAKE_LIST_FAIL=1
runit cleanup-apply.sh "$WORK/round-01.txt"; unset FAKE_LIST_FAIL
if [ "$GOT" -eq 4 ] && ! has "pm disable-user"; then ok "apply_failing_package_list_exits_4"; else bad "apply_failing_package_list_exits_4 (exit $GOT)"; fi

# ---- cleanup-restore.sh ----
applyround() { # name pkgs...: writes cleanup round file $WORK/<name>.txt and applies it
  local name="$1"; shift; printf '%s\n' "$@" > "$WORK/$name.txt"; runit cleanup-apply.sh "$WORK/$name.txt"
}
reset a.one pre.disabled; echo pre.disabled > "$DEV/disabled.txt"; echo pre.disabled > "$WORK/baseline.txt"
applyround round-01 a.one pre.disabled
runit cleanup-restore.sh
if [ "$GOT" -eq 0 ] && ! grep -qx a.one "$DEV/disabled.txt" && grep -qx pre.disabled "$DEV/disabled.txt" && [ -z "$(state_pkgs)" ]; then ok "restore_enables_only_recorded"; else bad "restore_enables_only_recorded (exit $GOT)"; fi

reset a.one b.one; applyround round-01 a.one; applyround round-02 b.one
runit cleanup-restore.sh round-02
if [ "$GOT" -eq 0 ] && grep -qx a.one "$DEV/disabled.txt" && ! grep -qx b.one "$DEV/disabled.txt" && [ "$(state_pkgs | tr '\n' ' ')" = "a.one " ]; then ok "restore_one_round_only"; else bad "restore_one_round_only (exit $GOT)"; fi

reset a.one; applyround round-01 a.one; runit cleanup-restore.sh; : > "$WORK/log"
runit cleanup-restore.sh
if [ "$GOT" -eq 0 ] && ! has "pm enable"; then ok "restore_twice_changes_nothing"; else bad "restore_twice_changes_nothing (exit $GOT)"; fi

reset a.one a.two; FAKE_FAIL=a.two applyround round-01 a.one a.two
runit cleanup-restore.sh
if [ "$GOT" -eq 0 ] && [ ! -s "$DEV/disabled.txt" ] && [ -z "$(state_pkgs)" ]; then ok "restore_after_partial_round"; else bad "restore_after_partial_round (exit $GOT)"; fi

reset a.one; applyround round-01 a.one; FAKE_DOWN=1 runit cleanup-restore.sh
if [ "$GOT" -eq 4 ] && grep -q "Developer options" "$WORK/out" && [ "$(state_pkgs | tr '\n' ' ')" = "a.one " ]; then ok "restore_unreachable_exits_4"; else bad "restore_unreachable_exits_4 (exit $GOT)"; fi

reset a.one; applyround 03-tguard a.one; runit cleanup-restore.sh round-03
if [ "$GOT" -eq 2 ] && grep -q "03-tguard" "$WORK/out" && [ "$(state_pkgs | tr '\n' ' ')" = "a.one " ] && grep -qx a.one "$DEV/disabled.txt"; then ok "restore_unknown_round_exits_2_and_names_known_rounds"; else bad "restore_unknown_round_exits_2_and_names_known_rounds (exit $GOT)"; fi

# The connection drops partway through a restore: records not confirmed as enabled must stay, and the exit is 4.
reset a.one a.two; applyround round-01 a.one a.two; rm -f "$DEV/count"; FAKE_DOWN_AFTER=3
runit cleanup-restore.sh; unset FAKE_DOWN_AFTER
if [ "$GOT" -eq 4 ] && grep -q "Developer options" "$WORK/out" && [ "$(state_pkgs | tr '\n' ' ')" = "a.two " ]; then ok "restore_drop_mid_run_keeps_unconfirmed_records_and_exits_4"; else bad "restore_drop_mid_run_keeps_unconfirmed_records_and_exits_4 (exit $GOT: state=$(state_pkgs | tr '\n' ' '))"; fi

reset a.one a.two; applyround round-01 a.one a.two; rm -f "$DEV/count"; FAKE_DOWN_AFTER=4
runit cleanup-restore.sh; unset FAKE_DOWN_AFTER
if [ "$GOT" -eq 4 ] && [ "$(state_pkgs | tr '\n' ' ')" = "a.two " ]; then ok "restore_drop_before_confirming_last_package_keeps_its_record"; else bad "restore_drop_before_confirming_last_package_keeps_its_record (exit $GOT: state=$(state_pkgs | tr '\n' ' '))"; fi

# pm enable fails while the TV answers: exit 3, the record stays.
reset a.one; applyround round-01 a.one; FAKE_ENABLE_FAIL=a.one runit cleanup-restore.sh; unset FAKE_ENABLE_FAIL
if [ "$GOT" -eq 3 ] && [ "$(state_pkgs | tr '\n' ' ')" = "a.one " ] && grep -qx a.one "$DEV/disabled.txt"; then ok "restore_enable_failure_exits_3_and_keeps_record"; else bad "restore_enable_failure_exits_3_and_keeps_record (exit $GOT)"; fi

# A failing package listing during restore is a dropped connection, not "nothing disabled".
reset a.one; applyround round-01 a.one; FAKE_LIST_FAIL=1 runit cleanup-restore.sh; unset FAKE_LIST_FAIL
if [ "$GOT" -eq 4 ] && [ "$(state_pkgs | tr '\n' ' ')" = "a.one " ]; then ok "restore_failing_package_list_exits_4_and_keeps_record"; else bad "restore_failing_package_list_exits_4_and_keeps_record (exit $GOT)"; fi

# ---- cleanup-wait.sh and cleanup-measure.sh ----
# in_order <pattern>...: true when the patterns occur in that order in the fake adb log.
in_order() {
  local -a pats=("$@"); local i=0 line
  while IFS= read -r line; do
    [[ "$line" == *"${pats[$i]}"* ]] && i=$((i+1))
    [ "$i" -ge "${#pats[@]}" ] && return 0
  done < "$WORK/log"
  return 1
}
EXPECTED="total_kb=943688 free_kb=166165 zram_physical_kb=114852 swap_kb=573812"

reset
runit cleanup-measure.sh --summarize "$ROOT/scripts/tests/fixtures/meminfo-sample.txt"
if [ "$GOT" -eq 0 ] && [ "$(cat "$WORK/out")" = "$EXPECTED" ]; then ok "summarize_parses_sample"; else bad "summarize_parses_sample (exit $GOT: $(cat "$WORK/out"))"; fi

reset
runit cleanup-measure.sh unit
if [ "$GOT" -eq 0 ] && in_order KEYCODE_WAKEUP KEYCODE_HOME com.google.android.youtube.tv KEYCODE_HOME com.netflix.ninja KEYCODE_HOME "dumpsys meminfo"; then ok "measure_protocol_order"; else bad "measure_protocol_order (exit $GOT)"; fi

reset
runit cleanup-measure.sh unit
runit cleanup-measure.sh --summarize "$WORK/meas/unit.txt"
if [ "$GOT" -eq 0 ] && [ "$(cat "$WORK/out")" = "$EXPECTED" ] && grep -q "Total PSS by process" "$WORK/meas/unit.txt"; then ok "measure_file_can_be_summarized"; else bad "measure_file_can_be_summarized (exit $GOT)"; fi

reset
runit cleanup-measure.sh unit; runit cleanup-measure.sh unit; : > "$WORK/log"
if [ "$GOT" -eq 2 ] && grep -q "already exists" "$WORK/out" && [ -s "$WORK/meas/unit.txt" ]; then ok "measure_refuses_to_overwrite_a_label"; else bad "measure_refuses_to_overwrite_a_label (exit $GOT)"; fi

reset
runit cleanup-measure.sh unit; FORCE=1 runit cleanup-measure.sh unit
if [ "$GOT" -eq 0 ]; then ok "measure_force_overwrites_a_label"; else bad "measure_force_overwrites_a_label (exit $GOT)"; fi

reset; FAKE_NO_MEMINFO=1
runit cleanup-measure.sh unit; unset FAKE_NO_MEMINFO
if [ "$GOT" -eq 3 ] && [ ! -e "$WORK/meas/unit.txt" ]; then ok "measure_without_memory_data_writes_nothing_and_exits_3"; else bad "measure_without_memory_data_writes_nothing_and_exits_3 (exit $GOT)"; fi

reset
runit cleanup-wait.sh
if [ "$GOT" -eq 0 ]; then ok "wait_succeeds_when_up"; else bad "wait_succeeds_when_up (exit $GOT)"; fi

reset; FAKE_DOWN=1 WAIT_SECONDS=2 runit cleanup-wait.sh
if [ "$GOT" -eq 4 ] && grep -q "Developer options" "$WORK/out"; then ok "wait_times_out_with_recovery_text"; else bad "wait_times_out_with_recovery_text (exit $GOT)"; fi

echo; echo "$PASSED passed, $FAILED failed"
[ "$FAILED" -eq 0 ]
