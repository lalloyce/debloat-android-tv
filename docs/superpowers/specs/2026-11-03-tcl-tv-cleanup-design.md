# TCL TV cleanup (Project B): design

Date: 2026-11-03. Status: draft for review. Follows the launcher design (Project A, built and installed; not published).

## Why

The TV freezes. It has about 0.9 GB of RAM and is swapping heavily. Project A replaced the home screen and made it lighter than the stock one. The rest of the memory is held by background packages from TCL and Google that the family never uses. The owner wants everything that can safely go to go, without risking the TV's network or remote.

## Scope

| Project | Status |
|---|---|
| A. Home screen | Built and installed |
| B. Cleanup | This document |
| C. Control without the remote | Settled: Google's phone remote works on the Pixel and on an iPhone (owner-tested 2026-11-03). Nothing to build. The setup guide is `docs/family-remote-guide.md` |

## The TV (read on 2026-11-03)

- TCL "Smart TV", device `C06`, product `C06_2K_GB`, Android 11, build `AR2101`, 943,688 kB of RAM. It sits on Ethernet (`eth0`, <TV_HOST>); the debugging connection runs over that.
- One snapshot with YouTube and Netflix both open after tests (not a clean baseline): free RAM 166,165 kB, swap holding 573,812 kB of data in 114,852 kB of RAM.
- Largest processes outside the family's apps, in kB of PSS: `com.google.android.katniss:search` 75,989; `com.google.android.gms` 57,619; `com.google.android.katniss:interactor` 31,119; `com.android.vending` 30,535; `com.google.android.apps.mediashell` 24,491; `com.tcl.ttvs` 23,102; `com.google.android.tvrecommendations` 11,800; `com.tcl.guard` 11,061; `com.tcl.system.server` 11,043; `com.android.providers.tv` 9,203; `com.google.android.tv.remote.service` 8,095; `com.android.bluetooth` 8,072; `com.google.android.tungsten.setupwraith` 7,350.
- Already disabled: the stock launcher (by us), and Stan, Google's videos app, Rakuten TV and TCL Alexa. Who disabled the last four is unknown.
- The package roles above are inferred from names and CleanYourTV's notes, not confirmed on this TV.

## Reference

CleanYourTV (MIT, github.com/Gathin23/cleanyourtv, credited in `NOTICE`) has `examples/tcl-40s5400a/debloat.txt` for the same device code, product and build, plus `tools/disable.sh` and `tools/restore.sh`. Its notes report one to a few days of use per unit, and its own guidance asks for longer. Treat it as a candidate list, not as proof. Notes in it that matter here:

- `com.tcl.ttvs` is the firmware updater, not speech. `com.tcl.system.server` is remote-key plumbing.
- Disabling `com.google.android.tv.remote.service` also breaks the phone remote and voice from the Bluetooth remote.
- On some units `setupwraith` owned the saved Wi-Fi network, and disabling it dropped Wi-Fi.

## Goal

Free as much memory as safely possible and reduce swapping, so the TV stops freezing in normal use (YouTube, Netflix, casting, live TV, phone remote).

## Non-goals

- Uninstalling anything, root access, or blocking TCL servers by DNS or firewall.
- Touching YouTube, Netflix, the Play Store, Google Play services or the Chromecast receiver.
- Changing our launcher (Project A) beyond what a cleanup finding requires.
- Voice features: the owner confirms nobody uses the microphone button.
- USB playback: the owner says the family sometimes plays media from USB drives, so TCL's USB media apps (Media Center `com.tcl.ui_mediaCenter` and the other TCL USB players) stay enabled.

## Anti-goals (a failure even if memory improves)

- Losing the debugging connection or any network path, with no way back.
- Breaking the physical remote, the phone remote, casting, the TV tuner or Inputs.
- A boot loop, or a TV that no longer opens to our home screen.
- A change that cannot be undone from one script.

## Constraints

- Disable with `pm disable-user` only. Never `pm uninstall`, `pm hide` or root.
- Keep list, decided by the owner: Play Store, Google Play services, the Chromecast receiver, YouTube, Netflix. Added by me: `com.google.android.tv.remote.service`, our launcher, `com.tcl.tv`, `com.tcl.tvinput`, and TCL Channel (`com.tcl.waterfall.overseas`), because the "TCL Movies" home tile opens it. Every round's check includes opening each of the four home tiles, so a disabled app cannot hide behind a working Home.
- Never-touch list (network path, remote path, system core): anything for Wi-Fi, Ethernet, Settings and their providers; the Bluetooth stack and TCL's remote services (`com.android.bluetooth`, `com.tcl.tcl_bt_rcu_service`, `com.tcl.autopair`, `com.tcl.system.server`); `com.android.providers.tv`; `com.google.android.tungsten.setupwraith` until it is proven to hold no saved network.
- Work in small rounds. After each round: reboot, adb reconnect over Ethernet, memory measured, and the owner checks that Home, YouTube, Netflix, casting and the phone remote still work. A round that fails any check is restored before the next one starts.
- One restore script must put every package back to its state before Project B and be tested once.
- Any step that changes what the TV shows, restarts it or changes a setting needs the owner's go-ahead at that moment.
- Memory is measured the same way before and after: a fixed protocol (reboot, three idle minutes on Home, then YouTube one minute, Netflix one minute, back to Home), reading `dumpsys meminfo` total RAM, free RAM and swap.

## What the builder delivers

Both the tooling and the running of the rounds on the TV, with the owner. The scripts stop and wait for the owner's confirmation after each round. The schedule allows for days of owner time, because Done means item 5 is several days of use.

## How it gets made

- Shell scripts in `scripts/`, in the style of Project A's `install.sh` and `rollback.sh`: a fake-`adb` test for the scripts, credit to CleanYourTV in the header.
- A plain-text list of packages per round, with a comment per line saying what it is, how it was verified and the measured saving.
- Results recorded in `docs/cleanup-results.md`.

## Done means

1. Before the first round: (a) the owner confirmed on 2026-11-03 that Settings > Apps shows an Enable button for a disabled system app (so Settings can undo a disabled package without adb); (b) the TV has been rebooted once and network debugging came back with no help (it did after the Project A reboot, but the cause is unknown, so whatever re-enables it joins the never-touch list once found); (c) the owner can turn network debugging back on from Developer options; (d) a Wi-Fi network is saved as a backup, or the owner decides to go without one. The owner accepts turning debugging back on and re-enabling packages by hand as the fallback if a round cuts adb off (2026-11-03), so rounds can be moderate in size.
2. Every round has a recorded result: packages, memory before and after, checks passed, owner sign-off.
3. A final report states before and after memory, swap and free RAM for the same protocol.
4. The restore script is tested once on the TV, and a reboot still opens our home screen.
5. The owner lives with the result for several days and reports whether the freezing has stopped, and the report goes into `docs/cleanup-results.md`.

## Left to the builder

The exact package list and round order, how each package's role is verified, the script layout, and how the fake-`adb` test covers the failure paths.

## My calls (change any you disagree with)

- Keep `com.tcl.ttvs` (firmware updater) enabled. Disabling it means the TV never updates itself, and the owner has not decided. Easy to change later.
- Disable Prime Video (`com.amazon.amazonvideo.livingroom`), since the family only uses YouTube and Netflix. [NEEDS SOURCE] for its memory cost: it was not among the largest processes listed above.
- The first round covers the voice stack (`katniss`, the Assistant stub) and `com.google.android.tvrecommendations`, because they are large and the owner does not use voice.
- Report the before and after measurements; no numeric target, because the baseline is not clean yet.
- Stopping rule: work down the candidate list in rounds. A package whose measured saving is under 2 MB of PSS and whose role is unclear is left enabled, since a small saving is not worth any risk. Success is judged by the measured numbers and by the owner's report after several days.
- Owner's "good" to the recovery check was ambiguous, so it stays as a precondition (done means item 1).
