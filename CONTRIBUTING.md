# Contributing

Thanks for helping. This project changes the software on a TV, so the bar is "can it be undone, and was it actually checked on a device", not "does it look right".

The most useful contributions, roughly in order:

1. **Reports and lists for other TVs.** What worked, what broke, what you had to keep.
2. **Bug reports** with enough detail to reproduce (see below).
3. **Fixes and small features** to the launcher or the scripts.
4. **Documentation** that would have saved you time.

If you are unsure whether an idea fits, open an issue first.

## Ground rules

- **Never uninstall.** Cleanup uses `pm disable-user --user 0` and nothing else. No `pm uninstall`, `pm hide`, root, or DNS/firewall blocking of a vendor's servers.
- **A change must be undoable from the computer.** Anything that alters the TV needs a matching restore path and a test for it.
- **Home must never be left broken.** If the launcher fails to start or does not become Home, the stock launcher must get Home back automatically.
- **Do not touch the network path or the remote path.** Anything for Wi-Fi, Ethernet, Bluetooth remotes, Settings and their providers is on `cleanup/never-touch.txt`. A round that names such a package is refused whole.
- **The launcher stays small.** Plain Android views, no UI framework, no background services, no analytics, no ads. New permissions need an issue first. Today it holds `INTERNET` and `READ_TV_LISTINGS` only, and `scripts/check-manifest.sh` enforces that.
- **No secrets, no personal data.** Never commit `secrets.properties`, `scripts/env.local.sh`, an API key, your TV's IP address, your account names, or device identifiers. Check `git diff --cached` before you commit.
- **Report outcomes as they are.** If a test fails, show the output. If you could not test something on a device, say so in the pull request.

## Setup

See [README.md](README.md) for requirements. To work on the code you do not need a TV:

```bash
bash scripts/tests/test-cleanup.sh
bash scripts/tests/test-install.sh
./gradlew testDebugUnitTest
```

Set `ANDROID_HOME` (or create `local.properties` with `sdk.dir=...`) if Gradle cannot find the Android SDK. To try changes on a TV, create `scripts/env.local.sh` from the example and run `adb connect <TV_HOST>:5555`.

## Workflow

1. Fork, then branch from the default branch. One concern per pull request.
2. **Write the failing test first**, watch it fail for the right reason, then make it pass. This is how the code base was built, and the tests are the only record of why a guard exists.
   - Launcher logic that does not touch Android classes gets a JUnit test in `app/src/test/java/family/tvhome/`.
   - Script behaviour gets a case in `scripts/tests/test-cleanup.sh` or `test-install.sh`. The tests run the real scripts against a fake `adb`; extend the fake (for example with a new failure mode) rather than mocking inside a script.
   - Code that draws views or starts other apps' screens cannot be unit-tested here. Keep the decision logic in a small class you can test, keep the Android part thin, and describe in the pull request how you checked it on a TV.
3. Run all three suites. All must pass.
4. Match the surrounding code: naming, comment density and idiom. Comments say why, not what.
5. Commit with a short imperative subject ("Make rollback verify Home") and a body that says what and why. Each commit is one complete, self-contained change, with no work-in-progress or "fixup" commits left on the branch.
6. Open the pull request using the template. Say what you tested, on which TV and Android version, and what you did not test.

## Contributing a list for another TV

The cleanup lists in `cleanup/` are for the reference TCL TV. For a different TV, do not edit them: add your own set, for example `cleanup/devices/<brand>-<model>/` with its own `never-touch.txt`, `keep.txt`, `baseline-disabled.txt` and `rounds/`. The scripts read those paths from the environment, so you can point them at your set:

```bash
NEVER=cleanup/devices/my-tv/never-touch.txt KEEP=cleanup/devices/my-tv/keep.txt \
BASELINE=cleanup/devices/my-tv/baseline-disabled.txt \
scripts/cleanup-apply.sh cleanup/devices/my-tv/rounds/01-first-round.txt
```

(This layout is a proposal and has not been used by anyone yet. If it gets in your way, open an issue.)

A good list has:

- **A comment on every package**: what it is, how you know (a name, `dumpsys package`, a vendor note, a process list), and the memory it held (PSS, in kB) when you looked.
- **Small rounds.** A handful of related packages per round, so a failure points at its cause. A package whose measured saving is under about 2 MB and whose role is unclear should stay enabled.
- **A real round-trip.** For each round: applied, waited 60 seconds, rebooted, packages still disabled, then the TV used for a while (Home, every tile, playback, phone remote or whatever you rely on). Say which of these you checked.
- **Measurements taken the same way** each time (`scripts/cleanup-measure.sh`), repeated, with the noise said out loud. On the reference TV a single run moved swap use by up to about 100 MB with nothing changed, so one reading proves nothing.
- **What came back or failed.** Packages the system re-enabled after a reboot, rounds you reverted, and anything that broke. These are as valuable as the successes.
- **Sources marked.** Mark anything you did not verify on your own TV as unverified. Notes from other projects are candidates, not proof.

Never-touch and keep entries from your TV (what cut the network, what broke the remote, what the Play Store needs) are the most valuable part of a list.

## Reporting a bug or a device result

Use the issue templates. Include:

- TV make, model and Android version (`adb shell getprop ro.product.model`, `ro.build.version.release`, `ro.build.fingerprint`).
- What you ran (exact command), what you expected, what happened, with the script output.
- For the launcher: whether the stock launcher is still disabled, and what `adb shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME` prints.

Remove your IP address, account names and any API key before posting.

## Conduct and security

Be kind and specific. See [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md). For a security problem, see [SECURITY.md](SECURITY.md).

## Licence

By contributing you agree that your contribution is licensed under the MIT licence in [LICENSE](LICENSE). Contributions that copy code from elsewhere must say where it came from and be compatible with MIT; credit goes in [NOTICE](NOTICE).
