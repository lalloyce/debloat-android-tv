# Memory report (2026-11-03)

Method: `scripts/measure-memory.sh`, which reads `adb shell dumpsys meminfo <package>` (the "TOTAL PSS" and "TOTAL RSS" figures, in KB) on the TCL C06. Release build of our launcher; the trial launcher is CleanYourTV's, version as installed on 2026-11-03.

| Launcher | Phase | PSS (KB) | RSS (KB) |
|---|---|---|---|
| Trial (com.gathin.tvhome) | idle, about 10 s after start, reading 1 | 74124 | 135636 |
| Trial | idle, reading 2 | 60488 | 122728 |
| Trial | idle, reading 3 | 60325 | 122616 |
| Ours (family.tvhome) | idle on Home, reading 1 | 44206 | 112660 |
| Ours | idle, reading 2 | 44554 | 115052 |
| Ours | idle, reading 3 | 44528 | 115048 |
| Ours | worst case: after 6 rounds of Right/Left across the tiles, suggestions and thumbnails showing | 45742 | 115964 |

Our breakdown at the end (KB PSS): Java heap 2140, native heap 7408, code 16472, graphics 4536.

## Against the spec's 33 MB
The spec's figure for the trial launcher was 33 MB (PSS) from an earlier measurement. The method behind that figure is `[NEEDS SOURCE]`. Measured today the same way for both launchers, ours is lower than the trial (about 44.5 MB against 60 to 74 MB), but it is above the 33 MB figure. The spec's constraint is "must not exceed the trial's measured 33 MB without a reason the owner agrees to"; that needs the owner's decision. Possible reason: the 33 MB reading was taken under different conditions, such as after a long idle period (the trial figure today is falling from 74 MB to 60 MB right after start). Not verified.

The stock launcher (com.google.android.tvlauncher) was not measured: it is disabled, and enabling it would change the TV's setup.

## Owner decision, 2026-11-03

The owner accepted 44.5 MB idle (45.7 MB worst case) in place of the 33 MB target. The trial launcher measured 60 to 74 MB the same day with the same method.
