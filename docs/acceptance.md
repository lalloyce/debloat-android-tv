# Project A acceptance record (home launcher)

Written 2026-11-03 from the build ledger, which is git-ignored, so that what was checked on the TV is recorded in tracked files. The launcher's original design and plan are not published. Memory: `docs/memory-report.md`. Published link shapes: `docs/spike-results.md`.

## Checked on the TV

| Check | Result | Evidence |
|---|---|---|
| Header and four tiles look right | Owner approved | Owner's reply during Task 8, again after the glass styling and the borderless tiles |
| Enter on a tile opens YouTube, TCL Channel (`com.tcl.waterfall.overseas`) and TV (`com.tcl.tv/.TVActivity`) | Passed | Key presses over adb, owner watching |
| Back returns to our Home from YouTube and from TCL Movies | Passed | Same session. Netflix and the TV app consume Back themselves, so the Home key is the way out there |
| Back at our Home | Does nothing, as designed | Same session |
| YouTube suggestion card, cold start | Passed | After `am force-stop` of YouTube, Down and Enter opened the video; the owner saw it play |
| Netflix suggestion cards | Passed | The first build showed "Not available" (Netflix publishes a link with no `intent:` prefix). Fixed in `PublishedUri`; the owner confirmed all three cards opened that title |
| Apps list look | Owner approved | Owner's reply |
| Reboot test | Passed | Uptime 42 s at the read; Home was ours; the stock launcher stayed disabled; the owner confirmed the first tile was selected and the header and suggestions were correct |
| Rollback test | Passed | `scripts/rollback.sh` gave Home back to the stock launcher and the owner saw the original Google TV home; `scripts/install.sh` restored ours |
| Release APK permissions | INTERNET and READ_TV_LISTINGS only | `scripts/check-manifest.sh` |
| Memory at idle | 44.5 MB (45.7 MB worst) against the trial launcher's 33 MB | `docs/memory-report.md`. The owner accepted it on 2026-11-03. The origin of the 33 MB figure is [NEEDS SOURCE] |

## Not verified

- A YouTube "Recommended" card (the `intent:` link form) was parsed correctly and the system log showed a start from our uid, but a cold start of YouTube from that card was not confirmed. The cold start above used the popular-chart card, whose link is the `https:` form.
- Behaviour of a Netflix card after its title link has expired in the real service.
- The header icons for Inputs, Network and Settings, and the dimmed state of a tile whose app is missing, were listed in the ledger as untested on the TV after Task 8. Later ledger lines do not record a test, so treat them as unverified [NEEDS SOURCE].
- Code changed after the whole-branch review (2026-11-03) and not yet installed on the TV: the stale-row dimming and the separate thumbnail threads, the crash guards in `SuggestionRepository`, `FallbackSource` and `ThumbnailLoader`, the single fetch on first start, the row no longer being rebuilt when fresh results equal the shown ones (and focus staying near where it was when the row shrinks), `SafeStart` around every `startActivity` to another app, Down from a tile going to the first card, and https-only thumbnails. They build and the JVM tests pass (107 tests); the view-level parts (`HomeActivity`, `SafeStart`) have no JVM test and have not been seen on the TV.

## Deliberate changes from the design

- Permissions: READ_TV_LISTINGS was added beside INTERNET (owner approved: "Yes, if it works"), so Netflix and YouTube rows can be read. `scripts/install.sh` grants it.
- The YouTube tile prefers the family's own "Recommended for you" rows, then "Trending", then the API popular chart. The design lists personalised recommendations as a non-goal; this follows the owner's earlier answer (personalised if signed in, popular if not).
- The suggestion row scrolls and holds up to 20 cards.
- Visuals: glass panels on the header icons, cards, the Inputs dialog and the Apps rows, with no blur (Android 11); tiles are borderless artwork and grow 1.08 times when focused.
- Memory: 44.5 MB against the 33 MB figure, accepted by the owner.

## Open

- Network debugging is still on. The owner turns it off in Developer options, then port 5555 must be confirmed as refusing connections.
- The launcher's design carries an Amendments section matching the list above; the design and plan themselves are not published.
