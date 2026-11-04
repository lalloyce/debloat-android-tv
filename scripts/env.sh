# Source this file: build and TV environment for this project.
export JAVA_HOME="$(java -XshowSettings:properties -version 2>&1 | awk '/java.home/ {print $3}')"
export ANDROID_HOME="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
export PATH="/opt/homebrew/bin:$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$PATH"
# Your own settings, not tracked: copy scripts/env.local.sh.example to scripts/env.local.sh and set TV_HOST.
[ -f "$(dirname "${BASH_SOURCE[0]}")/env.local.sh" ] && source "$(dirname "${BASH_SOURCE[0]}")/env.local.sh"
if [ -z "${TV_HOST:-}" ]; then echo "Set TV_HOST to the TV's IP address (see scripts/env.local.sh.example)." >&2; return 1 2>/dev/null || exit 1; fi
export TV="$TV_HOST:5555"
