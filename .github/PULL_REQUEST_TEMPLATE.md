## What and why

<!-- One concern per pull request. -->

## How it was checked

- [ ] `bash scripts/tests/test-cleanup.sh`
- [ ] `bash scripts/tests/test-install.sh`
- [ ] `./gradlew testDebugUnitTest`
- [ ] New behaviour has a test that failed before the change (or I explain below why it cannot)
- [ ] Tried on a real TV: <!-- make, model, Android version; or "not tried" -->

## Safety

- [ ] Nothing is uninstalled; every change to the TV can be undone from the computer
- [ ] No package on `cleanup/never-touch.txt` is touched; no new permission
- [ ] No secrets, IP addresses, account names or device identifiers in the diff

## Not checked

<!-- Say what you could not test. -->
