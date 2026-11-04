# Spike results (TV checks, Task 2)

Device: TCL C06, <TV_HOST>:5555. Dates are 2026-11-03.

## Cold-start YouTube link
PASS. After `am force-stop com.google.android.youtube.tv`, running
`am start -W -a android.intent.action.VIEW -p com.google.android.youtube.tv -d "https://www.youtube.com/watch?v=jNQXAC9IVRw"`
returned `Status: ok`, `LaunchState: COLD`, activity
`com.google.android.youtube.tv/com.google.android.apps.youtube.tv.activity.MainActivity`.
Four seconds later `media_session` showed a YouTube playback state 3 (playing), and the owner confirmed the right video was on screen.
Note: the first run was inconclusive because the screensaver (DreamActivity) was in front after 12 s; the repeat, with a wake key first, passed.
The plain https watch URL with `-p com.google.android.youtube.tv` is the link form Task 9 uses; `vnd.youtube:` was not needed.

## Inputs intent
No system activity opens an input picker from `adb`.
- `am start -W -a com.android.tv.action.VIEW_INPUTS` resolves to `com.tcl.tv/com.tcl.sourcemenu.sourcemanager.MainActivity`, but the command hung with no output and the owner saw no picker (YouTube kept playing).
- `am start -n com.tcl.tv/com.tcl.sourcemenu.sourcemanager.MainActivity` printed "Starting" and nothing appeared: no activity record, no log lines, owner saw no picker.
The TV lists four inputs (`dumpsys tv_input`): one tuner (`com.tcl.tvinput/.TDTVInputService/HW1413742848`, state 0) and three passthrough inputs (`.../passthroughinput.TvPassThroughService/HW1413743104`, `HW1413744128`, `HW1413744384`, the last two state 1). Names such as "HDMI 1" were not read.
CleanYourTV does not rely on the system picker either: its Inputs button opens its own menu built from `TvInputManager.getTvInputList()` (passthrough, not hidden), per `launcher/app/src/main/java/com/gathin/tvhome/MainActivity.java` in the clone.
Decision for Task 5: build our own small Inputs menu the same way; see ledger ruling.

## Readable rows
Still empty after the owner signed in to Netflix and YouTube (owner said signed in; whether each app was opened first was not asked). `content query` on `content://android.media.tv/watch_next_program` and `.../preview_program` both returned "No result found." This is weak evidence because the `adb` shell is not our app; Task 11 holds the real test.

## Netflix title link
Not run: the owner has no Netflix title id yet. Run when one is supplied (`am start -a android.intent.action.VIEW -d "nflx://www.netflix.com/title/<id>" -p com.netflix.ninja`; URI form `[NEEDS SOURCE]`).

## Task 11: channel-row probe from our own app (2026-11-03)
A temporary debug-only activity (since deleted) queried `TvContract.WatchNextPrograms.CONTENT_URI` and `TvContract.PreviewPrograms.CONTENT_URI` from our app's process, logging only row counts per package. The probe build requested `READ_EPG_DATA` (granted) and `ACCESS_ALL_EPG_DATA` (listed as requested; `dumpsys` showed no grant for it). The owner had said they were signed in to Netflix and YouTube.
Result: both queries returned 0 rows and threw no exception. The earlier `adb shell` query also returned no rows.
Reading: nothing readable from Netflix or YouTube. This cannot tell "they publish nothing" from "the system hides other apps' rows from us".
Decision (the spec's fallback): the Netflix tile only opens Netflix and shows no suggestions. TCL Channel and TV suggestions stay untested (the TV guide needs an aerial; the TCL Channel source is unknown); those tiles only open their apps.

### Correction to the Task 11 probe (2026-11-03, later the same day)
The probe above requested `android.permission.ACCESS_ALL_EPG_DATA`. That permission does not exist on this TV (`pm list permissions` shows no such name). The real one is `com.android.providers.tv.permission.ACCESS_ALL_EPG_DATA`, protection level signature|privileged, held by the stock launcher (a system app under /product/priv-app). So the probe never had broad access, and its "0 rows" result does not show that Netflix rows are unreadable. The decision "Netflix tile only opens Netflix" is therefore provisional (and was reversed; see below).
The owner reports the old stock Home showed Netflix previews, recommendations and unfinished titles, so rows exist.
Since tested (later the same day): with `android.permission.READ_TV_LISTINGS` granted (protection level dangerous, `pm grant` works over adb) the launcher read the shared channel data: Netflix published 1 channel with 17 programs, and YouTube 3 channels ("Recommended for you", "Trending" and YouTube Music). `READ_EPG_DATA` was not needed. The owner approved adding READ_TV_LISTINGS beside INTERNET ("Yes, if it works"), so the decision above is superseded; see the addendum at the end of this file and `docs/acceptance.md`.

## Addendum 2026-11-03: published row links

Apps publish row links in two shapes, and both are read in `PublishedUri` / `IntentFactory.fromPublishedUri`:

- YouTube: `intent://www.youtube.com/watch?v=<id>&launch=launcher#Intent;scheme=https;package=com.google.android.youtube.tv;end`
- Netflix: `http://www.netflix.com/title/<id>#Intent;launchFlags=0x20;component=com.netflix.ninja/.MainActivity;...;end`

`Intent.parseUri` with `URI_INTENT_SCHEME` reads only the first shape; the second must be parsed with no flag. Only links whose package or component matches the tile's app are started. Permissions are INTERNET and READ_TV_LISTINGS (granted by `scripts/install.sh`). Rows are read whole and filtered in code, because a selection clause throws a SecurityException without the all-data permission. Observed on the TV: the row scrolls horizontally with up to 20 cards, and Netflix cards open that title's page (owner-confirmed on 2026-11-03).
