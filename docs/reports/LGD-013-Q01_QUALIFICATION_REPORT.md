# LGD-013-Q01 Qualification Report

## Disposition

`LGD_013_MODERN_PLAYER_LAYER_QUALIFIED_PRINCIPAL_REVIEW_REQUIRED`

The Android runtime walkthrough passed on a disposable mobile emulator. The modern player layer is visibly separate from the antique BBS game, Welcome and Guide are usable, the Trophy Cabinet presents the full collection, persistence works across relaunch and New Game, and the first-blood trophy popup was observed over live BBS play. Principal visual/play review remains required; Principal acceptance is not claimed here.

## Revision and worktree

- Starting revision: unavailable; this workspace is not a Git checkout.
- Ending revision: unavailable for the same reason.
- Worktree state: implementation source is present; this qualification added this report and the representative evidence set. No unrelated files were reset or removed.
- Product repair made during Q01: `TrophyCabinetScreen` now uses a horizontally browsable filter row with compact labels. This corrected the observed three-line `Endurance` label on the target display without changing trophy behavior.

Major Q01 files: [`PlayerLayerScreens.kt`](../../app/src/main/java/com/legendoftheglassdragon/ui/screens/PlayerLayerScreens.kt), this qualification report, and the evidence images. The existing LGD-013 domain, persistence, navigation, and test files were not otherwise changed in Q01.

## Android tooling and target

- SDK: `C:\Users\erinm\AppData\Local\Android\Sdk`
- `adb`: `C:\Users\erinm\AppData\Local\Android\Sdk\platform-tools\adb.exe`; it existed locally but was not on PATH.
- Emulator: `C:\Users\erinm\AppData\Local\Android\Sdk\emulator\emulator.exe`.
- No physical device or existing AVD was available.
- No repository adb/emulator qualification script was present; qualification used the conventional SDK tools directly.
- Installed the official command-line tools and API-35 Google APIs x86_64 system image in the user SDK; no unrelated machine-wide toolchain changes were made.
- Created disposable AVD: `LGD013Q01_API35`, Pixel 6 profile.
- Runtime target: Android 15 / API 35, Google APIs x86_64, 1080×2400, density 420, portrait.
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`; install and launch succeeded.

## Human-facing qualification

### Welcome

PASS. A fresh Arcturus campaign showed Welcome automatically. The copy was contemporary, short enough for portrait, readable without clipping, and covered days, limited actions, forest fighting, experience, training, gold/equipment, defeat, other adventurers, the Glass Dragon, and immediate next-day play. Dismissal was obvious and returned to the BBS Town screen. A second fresh Lyra campaign received its own Welcome. After dismissal and relaunch, Welcome did not repeat.

### Player Layer

PASS. The entry is an unobtrusive BBS menu option rather than a permanent modern toolbar. The Player Layer presents `Continue to game`, `Guide`, and `Trophies` with contemporary spacing and touch-sized controls. It returns to the exact prior campaign/day context without advancing time. The target clearly communicated “modern utility around an old game.”

### Guide

PASS. The Guide opened independently, kept Welcome content available, and exposed the required progressive-disclosure sections: Welcome, Playing a Day, Fighting & Healing, Training & Levels, Equipment & Gold, Other Adventurers, and The Glass Dragon. Cards expanded/collapsed cleanly and the content remained readable in portrait.

### Trophy Cabinet

PASS after the filter-row repair. The cabinet showed `8 / 50 earned` after the controlled runtime award, with earned, known-unearned, and hidden entries visually distinct. Descriptions wrapped without truncation in the inspected portrait states. The collection felt substantial rather than dashboard-like. The repaired category row displayed All, Progression, Combat, and Endurance without label wrapping; it remains horizontally browsable for future categories.

### Trophy popup

PASS for the observed runtime case. A controlled First Blood award produced a readable modern `TROPHY EARNED` card over the live BBS victory screen. The BBS remained visible beneath it, critical controls were not permanently covered, the popup auto-dismissed, and the subsequent cabinet showed the trophy as earned. Repository idempotence and deterministic multi-unlock ordering are covered by the automated domain tests. A separate runtime Dragon/secret unlock was not forced during this disposable session and remains appropriate for Principal play review.

### Persistence and campaign independence

PASS. The controlled First Blood award survived process termination and relaunch. The Trophy Layer continued to show `8 / 50`. Creating the Lyra campaign did not erase the eight earned trophies; the new campaign received Welcome independently. The post-relaunch Player Layer showed `Lyra · Day 1` and `Trophies 8 / 50`.

## Evidence

Representative evidence is stored in [`docs/reports/evidence/LGD-013-Q01`](../reports/evidence/LGD-013-Q01):

1. `01-main-menu.png` — antique BBS entry point.
2. `02-welcome.png` — first-campaign Welcome.
3. `03-player-layer.png` — modern utility layer.
4. `04-guide.png` — Guide with progressive sections.
5. `05-trophy-cabinet-mixed.png` — 50-trophy cabinet with earned and unearned states.
6. `06-hidden-trophy.png` — hidden trophy representation beside known entries.
7. `07-trophy-popup.png` — First Blood popup over live BBS play.
8. `08-earned-trophy-cabinet.png` — earned state after the popup.
9. `09-second-campaign-welcome.png` — Welcome for the replacement campaign.

## Registry sanity review

Exactly 50 stable IDs are present and unique. The 12 secret IDs are:

`relentless`, `one_more_fight`, `hard_lessons`, `back_from_the_dead`, `fortune_favors_the_bold`, `prepared_for_anything`, `nemesis`, `seen_it_all`, `stranger_things`, `against_better_judgment`, `road_less_traveled`, `by_a_thread`.

The final registry, in catalog order, is:

`first_blood`, `seasoned_adventurer`, `old_hand`, `veteran_of_the_forest`, `the_long_road`, `student_becomes_master`, `overqualified`, `master_at_arms`, `dressed_for_war`, `fully_armed`, `untouchable`, `relentless`, `against_the_odds`, `last_breath`, `no_retreat`, `perfect_hunt`, `one_more_fight`, `giant_killer`, `hard_lessons`, `back_from_the_dead`, `iron_constitution`, `unbroken`, `scenic_route`, `speedrunner`, `life_well_lived`, `heavy_purse`, `fortune_favors_the_bold`, `patron_of_the_realm`, `arsenal`, `armorers_friend`, `from_rags_to_riches`, `big_spender`, `prepared_for_anything`, `everybody_knows_your_name`, `social_climber`, `number_one`, `living_legend`, `rivalry`, `nemesis`, `local_celebrity`, `seen_it_all`, `monster_scholar`, `stranger_things`, `against_better_judgment`, `road_less_traveled`, `dragon_slayer`, `dragonbane`, `legend`, `by_a_thread`, `legend_of_the_glass_dragon`.

Thresholds remain the calibrated LGD-013 values: 25/100 forest wins, 10/25-day survival, level 10, nine training promotions, tier-7 equipment, 10/25 win streaks, 1,000 held gold, 3,000 cumulative legitimate spend, 1,500 largest purchase, top-three/top-one community milestones, 1/3/10 Dragon victories, Dragon Speedrunner by day 12, Scenic Route at day 35+, and 40 of the other 49 trophies for the capstone. These preserve the established roughly 20-day ordinary campaign target.

Draft concepts without direct current mechanics remain substituted as documented in the LGD-013 completion report: Against Better Judgment uses a dangerous existing simulated-adventurer challenge, Local Celebrity uses retained player-related community events, and Road Less Traveled uses existing Dragon-season rare incidents. No major gameplay system was added for Q01.

## Save and migration behavior

Existing v8 saves remain valid. Optional player-layer fields preserve established campaigns without replaying Welcome. Trophy data remains in the separate campaign-independent SharedPreferences repository; ordinary New Game leaves it intact. No unsupported historical accomplishments were awarded.

## Automated qualification

- `./gradlew test` — PASS.
- `./gradlew :app:compileDebugAndroidTestKotlin` — PASS.
- `./gradlew :app:assembleDebug` — PASS.
- Focused LGD-013 trophy and save tests — PASS as part of the full JVM suite.

## Remaining limitations

- No physical-device or tablet walkthrough was available; qualification is on a Pixel 6-class API-35 emulator in portrait.
- Multi-trophy queue behavior, secret reveal, and Dragon popup paths are deterministically covered by domain/UI-state tests but were not separately forced in this runtime session.
- Principal visual/play acceptance is still required before using the unqualified final disposition.

## Final disposition

`LGD_013_MODERN_PLAYER_LAYER_QUALIFIED_PRINCIPAL_REVIEW_REQUIRED`
