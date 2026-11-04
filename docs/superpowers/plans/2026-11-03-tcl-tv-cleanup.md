# TCL TV Cleanup (Project B) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Disable the TCL and Google background packages the family does not use, in small tested rounds, so the TV stops freezing, without risking its network, remote or tiles.

**Architecture:** Plain-text package lists (one file per round) are applied by a guarded shell script that records exactly what it disabled; a second script restores from that record. A measurement script runs one fixed protocol before and after. Tooling is tested against a fake `adb`; the rounds themselves run on the real TV with the owner.

**Tech Stack:** bash, `adb`, `pm disable-user` (never uninstall), the fake-`adb` test style of `scripts/tests/test-install.sh`.

**Spec:** `docs/superpowers/specs/2026-11-03-tcl-tv-cleanup-design.md`

## Global Constraints

Copied from the spec.

- Disable with `pm disable-user` only. Never `pm uninstall`, `pm hide` or root.
- Keep list (owner): Play Store, Google Play services, the Chromecast receiver, YouTube, Netflix. Added: `com.google.android.tv.remote.service`, our launcher `family.tvhome`, `com.tcl.tv`, `com.tcl.tvinput`, `com.tcl.waterfall.overseas` (the "TCL Movies" tile). TCL's USB media apps (`com.tcl.ui_mediaCenter` and the other TCL USB players) stay enabled.
- Never-touch: Wi-Fi, Ethernet, Settings and their providers; `com.android.bluetooth`, `com.tcl.tcl_bt_rcu_service`, `com.tcl.autopair`, `com.tcl.system.server`; `com.android.providers.tv`; `com.google.android.tungsten.setupwraith` until proven to hold no saved network; whatever re-enables network debugging once found.
- Rounds are small. After each: reboot, adb reconnect over Ethernet, memory measured, and the owner checks Home, all four tiles, YouTube, Netflix, casting and the phone remote. A failed round is restored before the next starts.
- One restore script puts every package back and is tested once on the TV.
- Any step that changes what the TV shows, restarts it or changes a setting needs the owner's go-ahead at that moment.
- Memory protocol, identical before and after: reboot, three idle minutes on Home, YouTube one minute, Netflix one minute, back to Home; read total RAM, free RAM and swap from `dumpsys meminfo`.
- A package saving under 2 MB of PSS with an unclear role stays enabled.
- Credit CleanYourTV (MIT, github.com/Gathin23/cleanyourtv) in each script header; `NOTICE` already holds its licence.
- Commit only when the owner asks. Every sub-agent call sets `model` explicitly; never `fable` without asking.
- UI text and docs may state only what has been verified. Mark anything else `[NEEDS SOURCE]`.

## Review Focus

Each line has a test in the task named.

1. A round file names a keep-list or never-touch package. Expected: the whole file is refused before any package is touched, exit 2. (Task 1)
2. A package is absent or was already disabled before Project B. Expected: skipped with a message, not recorded, and never re-enabled by restore. (Tasks 1, 2)
3. adb is unreachable at the start, after a reboot, or partway through. Expected: stop, exit 4, print the by-hand recovery steps (Developer options, then Settings > Apps > Enable); earlier work stays recorded. (Tasks 1 to 3)
4. One `pm disable-user` fails or is not confirmed in the middle of a round. Expected: stop, record what succeeded, exit 3. (Task 1)
5. Restore runs twice, or after a partial round. Expected: only recorded packages are enabled, a second run changes nothing. (Task 2)

## File Structure

- `cleanup/never-touch.txt`, `cleanup/keep.txt`: one package per line, `#` comments.
- `cleanup/baseline-disabled.txt`: packages already disabled before Project B (Task 4).
- `cleanup/rounds/NN-name.txt`: one round per file, `package  # role; evidence; saving`.
- `cleanup/state/disabled.tsv`: what was disabled (`ROUND<TAB>PACKAGE<TAB>time`), written by `cleanup-apply.sh`; untracked.
- `scripts/cleanup-apply.sh`, `scripts/cleanup-restore.sh`, `scripts/cleanup-wait.sh`, `scripts/cleanup-measure.sh`.
- `scripts/tests/test-cleanup.sh`, `scripts/tests/fixtures/meminfo-sample.txt`.
- `docs/cleanup-results.md`, `docs/cleanup-measurements/<label>.txt`.

### Task 1: Guarded apply script

**Files:**
- Create: `cleanup/never-touch.txt`, `cleanup/keep.txt`, `scripts/cleanup-apply.sh`, `scripts/tests/test-cleanup.sh`
- Modify: `.gitignore` (add `cleanup/state/`)

**Interfaces:**
- Produces: `scripts/cleanup-apply.sh ROUND_FILE`. Env: `ADB` (default `adb`), `TV_HOST`, `STATE_FILE` (default `cleanup/state/disabled.tsv`), `NEVER`, `KEEP`, `BASELINE`. Exit 0 done, 2 guard refused (nothing changed), 3 a disable failed or was not confirmed by `pm list packages -d`, 4 adb unreachable (checked with `adb -s $TV get-state`).
- Produces: the test harness in `test-cleanup.sh`: a fake `adb` keeping `installed.txt` and `disabled.txt` in `$WORK`, with `FAKE_FAIL=<package>` making that disable fail and `FAKE_DOWN=1` making `get-state` fail. Tasks 2 and 3 add cases to this file.

- [ ] **Step 1: Write the failing tests** in `scripts/tests/test-cleanup.sh`, same `run`, `ok`, `bad` style as `test-install.sh`. Cases and assertions:
  - `refuses_never_touch_and_changes_nothing`: round file lists `a.ok` then `com.android.bluetooth`; exit 2, no `pm disable-user` in the log (including for `a.ok`).
  - `refuses_keep_list_package`: round lists `com.android.vending`; exit 2.
  - `skips_missing_and_already_disabled`: round lists a package not installed and one in `disabled.txt`; exit 0, neither is in the state file, output says skipped.
  - `records_state_lines`: two installed packages; exit 0; state file has two lines `round<TAB>pkg<TAB>time`; both are in `disabled.txt`.
  - `partial_failure_stops_and_keeps_record`: three packages, `FAKE_FAIL` on the second; exit 3; state file has only the first; the third was never attempted.
  - `unreachable_exits_4_with_recovery_text`: `FAKE_DOWN=1`; exit 4; output contains `Developer options` and `Settings > Apps`; state file absent.
  - `comments_and_blank_lines_ignored`.
- [ ] **Step 2: Run to verify it fails.** `bash scripts/tests/test-cleanup.sh`. Expected: FAIL lines because `cleanup-apply.sh` does not exist.
- [ ] **Step 3: Write the two lists and `scripts/cleanup-apply.sh`.** Lists: the packages named in Global Constraints (`com.google.android.apps.mediashell`, `com.android.vending`, `com.google.android.gms`, `com.google.android.youtube.tv`, `com.netflix.ninja`, and the rest). Script order: reachability check; read the round file; refuse if any line is in either list (before any change); for each package, skip if missing or in `pm list packages -d`; run `pm disable-user --user 0`; confirm in `pm list packages -d`; append the state line; header credits CleanYourTV.
- [ ] **Step 4: Run to verify it passes.** Expected: `7 passed, 0 failed`.
- [ ] **Step 5: Commit (only when the owner asks).**

### Task 2: Restore script

**Files:** Create `scripts/cleanup-restore.sh`; modify `scripts/tests/test-cleanup.sh`.

**Interfaces:**
- Consumes: the state file format and the fake `adb` from Task 1.
- Produces: `scripts/cleanup-restore.sh [ROUND]`. With no argument it enables every package in the state file; with `ROUND` only that round's. After each `pm enable --user 0` it confirms the package is no longer in `pm list packages -d`, removes the line from the state file, and exits 0. Exit 4 when adb is unreachable (same recovery text).

- [ ] **Step 1: Write the failing tests:** `restore_enables_only_recorded` (a pre-existing disabled package in `disabled.txt` and `baseline-disabled.txt` stays disabled); `restore_one_round_only`; `restore_twice_changes_nothing` (second run: exit 0, no `pm enable` in the second log); `restore_after_partial_round` (state from the Task 1 partial case restores cleanly); `restore_unreachable_exits_4`.
- [ ] **Step 2: Run; expect FAIL** (script missing).
- [ ] **Step 3: Implement `scripts/cleanup-restore.sh`** as above.
- [ ] **Step 4: Run; expect all cases pass** (`12 passed, 0 failed`).
- [ ] **Step 5: Commit (only when the owner asks).**

### Task 3: Wait and measurement scripts

**Files:** Create `scripts/cleanup-wait.sh`, `scripts/cleanup-measure.sh`, `scripts/tests/fixtures/meminfo-sample.txt`; modify `scripts/tests/test-cleanup.sh`.

**Interfaces:**
- Produces: `scripts/cleanup-wait.sh` waits up to 300 s (env `WAIT_SECONDS`) for `adb connect` to succeed and `getprop sys.boot_completed` to read 1; exit 0 when ready, 4 on timeout with the recovery text.
- Produces: `scripts/cleanup-measure.sh LABEL` runs the protocol (env `IDLE_SECONDS`=180, `APP_SECONDS`=60, set to 0 in tests): `KEYCODE_WAKEUP`, Home, idle, launch YouTube, wait, Home, launch Netflix, wait, Home, then writes `docs/cleanup-measurements/LABEL.txt` with the `Total RAM`, `Free RAM`, `Used RAM`, `ZRAM` lines of `dumpsys meminfo` and its `Total PSS by process` top 20. `scripts/cleanup-measure.sh --summarize FILE` prints `total_kb=... free_kb=... zram_physical_kb=... swap_kb=...`.
- Fixture: the TV's `dumpsys meminfo` from 2026-11-03 trimmed to those sections.

- [ ] **Step 1: Write the failing tests:** `summarize_parses_sample` expects `total_kb=943688 free_kb=166165 zram_physical_kb=114852 swap_kb=573812` from the fixture; `measure_protocol_order` (the fake log shows wake, Home, YouTube launch, Home, Netflix launch, Home, `dumpsys meminfo`, in that order); `wait_times_out_with_recovery_text` (`FAKE_DOWN=1`, `WAIT_SECONDS=2`, exit 4).
- [ ] **Step 2: Run; expect FAIL.**
- [ ] **Step 3: Implement both scripts** (and cut the fixture from `dumpsys meminfo`).
- [ ] **Step 4: Run; expect `15 passed, 0 failed`.**
- [ ] **Step 5: Commit (only when the owner asks).**

### Task 4: Preconditions and baseline on the TV

Each step needs the owner's go-ahead. Results go in `docs/cleanup-results.md`.

**Files:** Create `cleanup/baseline-disabled.txt`, `docs/cleanup-results.md`, `docs/cleanup-measurements/baseline.txt`.

**Interfaces:** Consumes Task 3's scripts. Produces the baseline file Tasks 1 and 2 use, and the "before" measurement.

- [ ] **Step 1: Baseline list.** `adb -s $TV shell pm list packages -d` to `cleanup/baseline-disabled.txt`. Expected: the five known (Stan, stock launcher, Google videos, Rakuten TV, TCL Alexa) plus anything new.
- [ ] **Step 2: Reboot test.** With the owner's go-ahead run `adb -s $TV reboot` then `scripts/cleanup-wait.sh`. Expected: exit 0 with no help from the owner, Home resolves to `family.tvhome/.HomeActivity`. If adb does not return, the owner turns debugging back on in Developer options and the cause goes in the results file.
- [ ] **Step 3: What re-enables adb.** Read `settings get global adb_enabled`, the `persist.*` and `service.adb.*` properties and any TCL receiver that sets them (search `dumpsys package` of `com.tcl.*` for `adb`). Add the responsible package to `cleanup/never-touch.txt`, or record `[NEEDS SOURCE]` if not found.
- [ ] **Step 4: Wi-Fi owner.** Find the owner of saved networks (`dumpsys wifi`, `cmd wifi list-networks`); record whether `setupwraith` holds any. Record the owner's decision on a Wi-Fi backup network.
- [ ] **Step 5: Baseline measurement.** `scripts/cleanup-wait.sh && scripts/cleanup-measure.sh baseline`. Expected: the file exists and `--summarize` prints four numbers.
- [ ] **Step 6: Commit (only when the owner asks).**

### Task 5: Candidate verification and round files

**Files:** Create `cleanup/rounds/01-voice-and-recommendations.txt` and further `cleanup/rounds/NN-*.txt`.

**Interfaces:** Consumes the lists and baseline. Produces round files that `cleanup-apply.sh` accepts (exit 0 on a dry run against the fake `adb`).

- [ ] **Step 1: Gather candidates** from CleanYourTV's `examples/tcl-40s5400a/debloat.txt` and the measured top processes in the spec.
- [ ] **Step 2: For each candidate record** its role evidence (`dumpsys package <pkg>`: components, receivers, whether a keep-list app needs it), its measured PSS, and an include or leave decision by the 2 MB rule. Prime Video (`com.amazon.amazonvideo.livingroom`) is included; `com.tcl.ttvs` is left enabled.
- [ ] **Step 3: Write round 1** (`katniss` processes, the Assistant stub, `com.google.android.tvrecommendations`) and further rounds, grouped small (a handful of packages each), one comment per line giving role, evidence and saving.
- [ ] **Step 4: Check each round file** against the guards: `scripts/cleanup-apply.sh` with the fake `adb` exits 0 and none of the lines are refused. Expected: exit 0 for every round.
- [ ] **Step 5: Commit (only when the owner asks).**

### Task 6: Run the rounds on the TV (repeat per round)

Needs the owner's go-ahead at each step and a pause for sign-off.

**Interfaces:** Consumes Tasks 1 to 5. Produces one entry per round in `docs/cleanup-results.md` and one measurement file per round.

- [ ] **Step 1: Go-ahead** for the round (name the packages).
- [ ] **Step 2: Apply.** `scripts/cleanup-apply.sh cleanup/rounds/NN-name.txt`. Expected: exit 0.
- [ ] **Step 3: Reboot, wait, measure.** With the owner's go-ahead `adb -s $TV reboot`, then `scripts/cleanup-wait.sh && scripts/cleanup-measure.sh round-NN`.
- [ ] **Step 4: Checks with the owner.** Home is ours; adb reconnected unaided; all four tiles open their apps; YouTube plays; Netflix opens a title; the phone remote controls the TV including volume; casting from a phone works; a USB drive still plays if any package touched media.
- [ ] **Step 5: Decide.** If every check passes and the owner signs off, record the round (packages, `--summarize` before and after, checks, sign-off). If any check fails, `scripts/cleanup-restore.sh NN`, reboot, and record the failure and the package suspected.
- [ ] **Step 6: Commit (only when the owner asks).**

### Task 7: Restore test, final report, and living with it

Needs the owner's go-ahead.

**Files:** Modify `docs/cleanup-results.md`.

- [ ] **Step 1: Restore test.** `scripts/cleanup-restore.sh`, reboot, `scripts/cleanup-wait.sh`. Expected: `pm list packages -d` equals `cleanup/baseline-disabled.txt` and Home still resolves to `family.tvhome/.HomeActivity`.
- [ ] **Step 2: Re-apply** the signed-off rounds in order, reboot, confirm Home is ours.
- [ ] **Step 3: Final measurement** `scripts/cleanup-measure.sh final`; write before, after and the difference in total, free and swap to the results file.
- [ ] **Step 4: Owner lives with it for several days** and reports whether the freezing has stopped; record the report and the date.
- [ ] **Step 5: Commit (only when the owner asks).**
