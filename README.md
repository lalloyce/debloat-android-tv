# mytv

A calm home screen for an Android TV, and a careful set of tools for switching off background packages you do not use.

It was built for one TCL TV with about 0.9 GB of RAM that froze often: the stock home screen was busy and heavy, and the TV swapped constantly. The launcher replaces the stock one and uses less memory. The cleanup tools disable unused TCL and Google packages in small, reversible rounds. Both are designed so that a mistake can be undone from your computer in a few commands.

Nothing is uninstalled, nothing is rooted, and the stock launcher stays on the TV (disabled), so it can be brought back at any time.

> **One TV has been tested.** The reference device is a TCL "Smart TV", device `C06`, Android 11, build `AR2101`, connected by Ethernet. Other TVs may work for the launcher. The cleanup lists are specific to that TCL build: read [Cleanup](#cleanup-optional) before using them anywhere else, and see [CONTRIBUTING.md](CONTRIBUTING.md) if you want to add your own TV.

## What you get

- **A plain home screen.** A greeting, a large clock and the date at the top left, icons for Inputs, Network, Settings and Apps, and one row of four large tiles: YouTube, Netflix, TCL Movies and TV. Nothing else: no ads, no promotion rows.
- **Suggestions under the selected tile.** A scrolling row of up to 20 cards. They come from the rows the apps already publish to the TV (what the stock home screen shows), read with the `READ_TV_LISTINGS` permission. If YouTube publishes nothing readable, the card row falls back to YouTube's "most popular" chart, which needs a free API key. The last good row is cached so the screen appears at once.
- **Works with a damaged remote.** Everything is reachable with the arrow keys, select and back. The clock always shows, whatever the system date says. Back at Home does nothing, so the TV never lands on another launcher.
- **Small.** Plain Android views, two permissions (`INTERNET`, `READ_TV_LISTINGS`), no background services, no analytics. On the reference TV it used about 44.5 MB (PSS) idle.
- **A way back.** `scripts/rollback.sh` gives Home back to the stock launcher.

## Requirements

- A computer with `bash`, `adb`, JDK 17 or newer, and the Android SDK (platform 34, build-tools 34.0.0). Android Studio is not needed.
- An Android TV on Android 11 or newer (the launcher's `minSdk` is 30), with **network debugging** turned on in Developer options, reachable from your computer.
- The TV's IP address. Ethernet is recommended: if a cleanup round ever cut the TV's network, there is no saved Wi-Fi to fall back on.

## Quick start

```bash
git clone <this repository> && cd mytv
cp scripts/env.local.sh.example scripts/env.local.sh   # then edit it: set TV_HOST to your TV's IP address
cp secrets.properties.example secrets.properties       # optional: add a YouTube Data API key, set REGION_CODE
adb connect <TV_HOST>:5555
scripts/install.sh
```

`scripts/install.sh` builds the release APK, installs it, grants `READ_TV_LISTINGS`, starts the launcher, and only then disables the stock launcher and makes ours Home. It then checks that Home resolves to ours. If Home does not resolve to ours, or an update fails to start while the stock launcher is already disabled, it gives Home back to the stock launcher and writes `recovery-<ip>.txt` with the by-hand steps. It is safe to run again to update.

Settings are read from `scripts/env.local.sh` (git-ignored) and `secrets.properties` (git-ignored). Never commit either.

If Gradle cannot find the Android SDK, set `ANDROID_HOME` or create `local.properties` with `sdk.dir=...`.

## Going back

```bash
scripts/rollback.sh              # re-enable the stock launcher and make it Home
scripts/rollback.sh --uninstall  # the same, then remove this launcher
```

`rollback.sh` exits with an error, and removes nothing, unless the stock launcher really took Home back. If the TV has no working Home and the scripts cannot help, run these from your computer:

```bash
adb connect <TV_HOST>:5555
adb -s <TV_HOST>:5555 shell pm enable --user 0 com.google.android.tvlauncher
adb -s <TV_HOST>:5555 shell cmd package set-home-activity --user 0 com.google.android.tvlauncher/.MainActivity
adb -s <TV_HOST>:5555 shell pm uninstall --user 0 family.tvhome
```

## Cleanup (optional)

The cleanup tools disable packages with `pm disable-user --user 0` only. They never uninstall, hide or delete anything. Every change is recorded so one command can undo it.

The lists live in `cleanup/`:

| File | Purpose |
|---|---|
| `never-touch.txt` | Network path, remote-control path and system core. A round that names any of these is refused whole, before anything changes. |
| `keep.txt` | Apps this household wants to keep. Also refused. Edit it for your own household. |
| `baseline-disabled.txt` | Packages that were already disabled before any round (never touched, never restored). |
| `rounds/NN-name.txt` | One round: package names, one per line, with a comment saying what each is and how that was verified. |

A round, in order:

1. Apply (this changes the TV, and step 3 restarts it, so do it when that is fine): `scripts/cleanup-apply.sh cleanup/rounds/01-voice-and-recommendations.txt`
2. **Wait 60 seconds.** A reboot a few seconds after disabling can bring the whole batch back.
3. `adb reboot`, then `scripts/cleanup-wait.sh` until the TV answers.
4. Check the packages are still disabled, then measure: `scripts/cleanup-measure.sh round-01`. It runs a fixed protocol (idle on Home, YouTube, Netflix, then `dumpsys meminfo`) and writes `cleanup/measurements/round-01.txt`.
5. Use the TV: Home, every tile, playback, the phone remote, casting. If anything is wrong: `scripts/cleanup-restore.sh round-01`.

Restore everything with `scripts/cleanup-restore.sh`. If the TV stops answering adb, the by-hand fallback is: turn network debugging back on in Developer options, then Settings > Apps, open the disabled app and choose Enable.

Exit codes: `0` done, `2` refused (a package is on a list, or a line is not a plain package name), `3` a change was not confirmed, `4` the TV is not reachable (at the start or partway; work already done stays recorded).

**What to expect.** On the reference TV, five rounds changed swap use by amounts that single measurements could not separate from run-to-run noise (differences under about 100 MB of swapped data), so the benefit is not proven. One round (`com.tcl.guard`, a persistent system-uid app that keeps running when disabled) showed no benefit and was reverted. Measure more than once and judge by how the TV behaves in daily use. Some packages are switched back on by the system after a reboot (`com.tcl.usercenter` was). Do not copy the round files to a different TV without checking each package's role there.

## Tests

```bash
bash scripts/tests/test-cleanup.sh   # cleanup scripts against a fake adb
bash scripts/tests/test-install.sh   # install and rollback scripts against a fake adb
./gradlew testDebugUnitTest          # the launcher's JVM unit tests
```

All three must pass before a change is merged. None of them needs a TV. Code that draws views or starts other apps' screens (`HomeActivity`, `SafeStart`, and similar) has no unit test and has to be checked on a real TV.

## Layout

```
app/                       the launcher (Java, plain views, package family.tvhome)
  src/main/java/...        HomeActivity is the entry point; Tiles.java lists the four tiles
  src/test/                JVM tests and fixtures
scripts/                   install, rollback, memory and cleanup scripts, and their fake-adb tests
cleanup/                   package lists and rounds for the reference TV
NOTICE                     credits and the licence text of CleanYourTV
```

The tiles are TCL-specific (`Tiles.java`: YouTube, Netflix, the TCL Channel app labelled "TCL Movies", and TCL's `com.tcl.tv`). Changing them for another TV is a small edit and a good first contribution.

## When something goes wrong

- **`Set TV_HOST ...`:** create `scripts/env.local.sh` as shown above.
- **`adb: device offline` or the TV does not answer:** wake the TV and run `scripts/cleanup-wait.sh` (it runs `adb connect` and waits for boot). After a reboot, network debugging usually comes back by itself on the reference TV, but that is not guaranteed.
- **The launcher opens but the YouTube row is empty:** the TV may not let the launcher read YouTube's rows, and there is no API key. Add `YOUTUBE_API_KEY` to `secrets.properties` and reinstall.
- **Home shows a chooser after installing:** reinstalling can clear the preferred Home. `scripts/install.sh` sets it again explicitly; run it once more.

## Contributing

Pull requests, bug reports and, above all, reports and lists for other TVs are welcome. Read [CONTRIBUTING.md](CONTRIBUTING.md) first: it explains the safety rules the cleanup tooling follows and what a good device report contains.

## Credits and licence

The install, rollback and disable-and-restore approach follows [CleanYourTV](https://github.com/Gathin23/cleanyourtv) (MIT). Its licence text and credit are in [NOTICE](NOTICE). Its notes for the same TCL device code were used as a candidate list and checked against the TV; they are not proof.

This project is licensed under the MIT licence, see [LICENSE](LICENSE). It modifies a device you own at your own risk: read the scripts before running them.
