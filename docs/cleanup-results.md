# Project B cleanup results

## Status (2026-11-03)

Rounds 1, 2, 4 and 5 are applied and signed off. Round 3 was reverted. The tooling has been through the whole-branch review; its findings about an adb connection dropping partway were fixed (suite 24/24).

**Not done yet:** the restore-everything test on the TV, re-applying the rounds after it, the two final measurements with reboots, and the several days of use that the design's done-means items 3 to 5 require. The owner chose "Not now" for these. Until they run, there is no final before-and-after report and no verdict on whether the freezing has stopped. The measurements so far are single runs with about 100 MB of noise in swapped data.

**Current state on the TV:** `com.tcl.usercenter` was enabled again by the system after a reboot (round 3 finding) and appears as a running process in the round 3, 3b and 4 measurements but not in round 5's. Its present state was not re-read for this note, and `cleanup/state/disabled.tsv` still records it as disabled by round 2. Restoring it is harmless. The packages from rounds 1, 4 and 5 and `com.tcl.partnercustomizer` stay disabled.

Design: `docs/superpowers/specs/2026-11-03-tcl-tv-cleanup-design.md`. Plan: `docs/superpowers/plans/2026-11-03-tcl-tv-cleanup.md`.
Measurements: `docs/cleanup-measurements/`. Compare with `scripts/cleanup-measure.sh --summarize FILE`.

## Preconditions (2026-11-03)

- **Baseline disabled packages** (`cleanup/baseline-disabled.txt`): `au.com.stan.and`, `com.google.android.tvlauncher` (disabled by us in Project A), `com.google.android.videos`, `com.tcl.smartalexa`, `tv.wuaki.apptv`.
- **Reboot test:** passed. After `adb reboot` the TV answered over Ethernet with no help (uptime 34 s when read), Home resolved to `family.tvhome/.HomeActivity`, and the disabled set was unchanged.
- **What turns debugging on:** not identified [NEEDS SOURCE]. Read on the TV: `adb_enabled=1` (Developer options setting, persists), `persist.sys.usb.config=adb`, `service.adb.tcp.port=5555` (set at boot by an unidentified component; `persist.adb.tcp.port` is empty), `adb_wifi_enabled=0`. No TCL package was found referring to it. Because the cause is unknown, a round must be followed by a reboot and a reconnect check, and the owner can restore by hand (Developer options, then Settings > Apps > Enable).
- **Wi-Fi:** `cmd wifi list-networks` reports "No networks"; Wi-Fi is enabled but not connected. The TV is on Ethernet (`eth0`). That the shell sees all saved networks is not confirmed, so `setupwraith` stays on the never-touch list. Owner decision (2026-11-03): no Wi-Fi backup network, Ethernet only.
- **Settings recovery:** the owner confirmed Settings > Apps shows an Enable button for a disabled system app.

## Baseline measurement

`docs/cleanup-measurements/baseline.txt`, protocol: reboot, three idle minutes on Home, YouTube one minute, Netflix one minute, Home, then `dumpsys meminfo`.

total 943,688 kB, free 335,440 kB, ZRAM physical 102,428 kB holding 524,356 kB of swapped data. Netflix 127,122 kB and YouTube 54,701 kB were still resident at the read. `com.google.android.katniss:search` was not among the running processes at this read, though it was at 75,989 kB in the earlier snapshot, so its saving may vary by run.

## Round procedure

1. Owner's go-ahead for the round. 2. `scripts/cleanup-apply.sh cleanup/rounds/NN-name.txt`. 3. **Wait 60 seconds** before rebooting: CleanYourTV's notes report that a reboot a few seconds after a batch brought the whole batch back enabled. 4. Owner's go-ahead, `adb reboot`, `scripts/cleanup-wait.sh`. 5. Confirm the packages are still disabled (`pm list packages -d`). 6. `scripts/cleanup-measure.sh round-NN`. 7. Owner checks: Home is ours, all four tiles open, YouTube plays, Netflix opens a title, phone remote including volume, casting from a phone, a USB drive if media apps were touched. 8. Sign-off, or `scripts/cleanup-restore.sh round-NN` and a record of what failed.

## Rounds

### Round 1: voice and recommendations (signed off by the owner, 2026-11-03)

Packages: `com.google.android.katniss`, `com.google.android.tv.assistant`, `com.google.android.tvrecommendations`. Measurement `docs/cleanup-measurements/round-01.txt` (one run each, same protocol):

| | total kB | free kB | ZRAM physical kB | swapped data kB |
|---|---|---|---|---|
| baseline | 943,688 | 335,440 | 102,428 | 524,356 |
| round 1 | 943,688 | 281,513 | 67,204 | 408,868 |

Swap use fell by about 115 MB of swapped data (35 MB less physical RAM held by the swap). Free RAM was lower by about 54 MB, but "free" includes cache and moves between runs, so I read swap as the better signal; one run each cannot separate the effect from run-to-run noise. `katniss` was not running at the round 1 read.
Owner checks passed: four tiles, YouTube, Netflix, phone remote with volume, casting. The voice button no longer works, as expected. Home stayed ours after two reboots.
Note: the first apply disabled only `katniss` because `adb shell` swallowed the loop's input on the real TV; fixed with a test (suite 17/17) and the round re-applied.

### Round 2: TCL account and promo rows (signed off by the owner, 2026-11-03)

Packages: `com.tcl.usercenter`, `com.tcl.partnercustomizer`. `docs/cleanup-measurements/round-02.txt`: total 943,688, free 307,174, ZRAM physical 74,252, swapped data 437,120 kB. Compared with round 1 (swapped 408,868 kB) this is no improvement beyond run-to-run noise; both packages were running (about 12 MB and 8 MB) before the round, so both were disabled. Owner checks passed. Later finding (round 3): the system re-enabled `usercenter` after a reboot, so only `partnercustomizer` stays off.

### Round 3: TGuard (owner checks passed; reverted, no benefit)

Package: `com.tcl.guard`. `docs/cleanup-measurements/round-03.txt`: free 279,595, ZRAM physical 89,652, swapped data 509,404 kB. A second run with the same state (`round-03b-guard-still-disabled.txt`, taken after a restore command that silently did nothing, see below) gave 535,796 kB swapped. Both are worse than rounds 1 and 2 (408,868 and 437,120 kB) and close to the baseline (524,356 kB). The two same-state runs differ by about 26 MB, so run-to-run noise is of that order.
`com.tcl.guard` is a PERSISTENT system-uid app (`dumpsys package`: flags SYSTEM HAS_CODE PERSISTENT, sharedUser android.uid.system, state disabled-user), and its process was running (pid 2232) while disabled, so disabling it did not stop it. Result: no measured benefit, so it was restored with `scripts/cleanup-restore.sh 03-tguard` (the package is enabled again).
Also found: `com.tcl.usercenter` (round 2) was enabled again by the system after a reboot, as CleanYourTV's notes predicted for this firmware. `com.tcl.partnercustomizer` stayed disabled.
Script defect found here: `cleanup-restore.sh round-03` (the wrong name; rounds are named by their file, `03-tguard`) exited 0 and did nothing. It now exits 2 and names the recorded rounds (test added, suite 18/18).

### Round 4: unused apps and extras (signed off by the owner, 2026-11-03)

Packages: `com.amazon.amazonvideo.livingroom` (Prime Video), `com.google.android.play.games`, `com.google.android.youtube.tvmusic`, `com.google.android.syncadapters.calendar`, `com.google.android.feedback`. All five stayed disabled after the reboot. `docs/cleanup-measurements/round-04.txt`: free 292,764, ZRAM physical 90,500, swapped data 472,920 kB. None of the five were running before the round, so little was expected; the number sits between round 1 (408,868) and baseline (524,356), inside the run-to-run spread seen so far. Owner checks passed.

**Run-to-run spread:** swapped data by run was 524,356 (baseline), 408,868, 437,120, 509,404, 535,796 and 472,920 kB. Differences under about 100 MB between single runs cannot be told apart from noise, so the final report must compare repeated runs and not rely on one reading per state.

### Round 5: photo screensaver (signed off by the owner, 2026-11-03)

Before applying, the screensaver setting was changed from `com.google.android.backdrop/.Backdrop` to `com.android.dreams.basic/.Colors` (the owner agreed). Package: `com.google.android.backdrop`, disabled and still disabled after the reboot. `docs/cleanup-measurements/round-05.txt`: free 274,263, ZRAM physical 74,788, swapped data 419,140 kB. The measurement protocol does not run the screensaver, so the reported 105 MB while it runs is not captured and stays [NEEDS SOURCE] for this TV. Owner checks passed, including the plain colour screensaver appearing and waking normally.

Round files: `cleanup/rounds/01-voice-and-recommendations.txt`, `02-tcl-account-and-promos.txt`, `03-tguard.txt`, `04-unused-apps-and-extras.txt`, `05-photo-screensaver.txt` (needs a screensaver setting change first).

Left enabled on purpose (idle at the survey and either unclear or an accessibility feature; the 2 MB rule): `com.tcl.messagebox`, `com.tcl.micmanager`, `com.tcl.initsetup`, `com.tcl.useragreement`, `com.tcl.esticker`, `android.autoinstalls.config.tcl.device`, `com.tcl.copydatatotv`, `com.tcl.t_solo`, `com.google.android.partnersetup`, `com.google.android.tts`, `com.google.android.marvin.talkback`, and `com.tcl.ttvs` (the firmware updater, by the design's call).
