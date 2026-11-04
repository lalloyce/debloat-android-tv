#!/usr/bin/env bash
# Fails unless the APK asks for INTERNET and READ_TV_LISTINGS only (the second reads other apps' published rows), has package family.tvhome and launches family.tvhome.HomeActivity.
set -u
APK="${1:-app/build/outputs/apk/debug/app-debug.apk}"
AAPT2="${ANDROID_HOME:-$HOME/Library/Android/sdk}/build-tools/34.0.0/aapt2"
[ -f "$APK" ] || { echo "FAIL: APK not found: $APK"; exit 1; }
perms=$("$AAPT2" dump permissions "$APK" | grep -E "^uses-permission" | sed -E "s/.*name='([^']+)'.*/\1/" | sort)
want=$(printf 'android.permission.INTERNET\nandroid.permission.READ_TV_LISTINGS\n')
if [ "$perms" != "$want" ]; then
  echo "FAIL: permissions must be exactly INTERNET and READ_TV_LISTINGS, found:"; printf '%s\n' "$perms"; exit 1
fi
badging=$("$AAPT2" dump badging "$APK")
printf '%s' "$badging" | grep -q "^package: name='family.tvhome'" || { echo "FAIL: package is not family.tvhome"; exit 1; }
printf '%s' "$badging" | grep -E "launchable-activity|leanback-launchable-activity" | grep -q "name='family.tvhome.HomeActivity'" || { echo "FAIL: HomeActivity is not launchable"; exit 1; }
echo OK
