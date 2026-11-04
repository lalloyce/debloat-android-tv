# Shared by the cleanup-*.sh scripts (source it). Approach follows CleanYourTV's disable/restore tools (MIT). See NOTICE.
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
[ -z "${ADB:-}" ] && source "$ROOT/scripts/env.sh"
ADB="${ADB:-adb}"
TV="${TV_HOST:?Set TV_HOST to the IP address of the TV, see scripts/env.local.sh.example}:5555"
STATE_FILE="${STATE_FILE:-$ROOT/cleanup/state/disabled.tsv}"
NEVER="${NEVER:-$ROOT/cleanup/never-touch.txt}"
KEEP="${KEEP:-$ROOT/cleanup/keep.txt}"
BASELINE="${BASELINE:-$ROOT/cleanup/baseline-disabled.txt}"

# adb shell reads stdin, which would swallow the rest of a `while read` loop's input; give it none.
tv() { "$ADB" -s "$TV" "$@" < /dev/null; }
reachable() { tv get-state >/dev/null 2>&1; }

# Package names in a list file: comments and blank lines dropped.
read_packages() {
  [ -f "$1" ] || return 0
  sed -e 's/#.*//' -e 's/^[[:space:]]*//' -e 's/[[:space:]]*$//' "$1" | grep -v '^$' || true
}
# Package names the TV reports: all (no argument) or only the disabled ones (-d).
# Fails (status 1) when adb fails, or when the full list comes back empty: an empty list from a dropped
# connection must never be read as "nothing installed" or "nothing disabled".
tv_packages() {
  local out
  out="$(tv shell pm list packages "$@")" || return 1
  out="$(printf '%s\n' "$out" | tr -d '\r' | sed -n 's/^package://p')"
  if [ "$#" -eq 0 ] && [ -z "$out" ]; then return 1; fi
  printf '%s\n' "$out"
}
# Drops one round/package line from the state file.
unrecord() { awk -F'\t' -v r="$1" -v p="$2" '!($1==r && $2==p)' "$STATE_FILE" > "$STATE_FILE.tmp" && mv "$STATE_FILE.tmp" "$STATE_FILE"; }

recovery_text() {
  cat <<'T'
The TV is not reachable over adb. First try the simple thing: wake the TV and run scripts/cleanup-wait.sh
(it runs adb connect and waits for the TV to finish booting). If that times out, by hand on the TV:
  1. Settings > Developer options: turn network debugging back on.
  2. Settings > Apps (show system apps): open a disabled app and choose Enable.
  3. Run scripts/cleanup-restore.sh once adb answers again.
T
}
