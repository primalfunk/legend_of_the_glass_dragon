# LGD-003 Completion Report

Date: 2026-08-28

## Result

LGD-003 controlled defeat, recovery, Physical Training, and progression balance are complete. A fresh character normally reaches Level 2 on Day 1, an ordinary campaign reaches Level 5-6 around Day 12 and Level 9 late in a 30-day campaign, and defeat primarily costs the current Forest day rather than damaging long-term progression.

The shipped application also contains the already-authorized LGD-004 community systems. This qualification changed only the player combat/progression balance and the save fields needed to preserve defeat state.

## Final balance constants

- Starting character: Level 1, 0 XP, 20/20 HP, Attack 4, Defense 2, 25 gold.
- Forest fights: 10 per realm day.
- Damage: `max(1, attack + random(0..2) - defense)`.
- Escape success: 65%; a failed escape permits one monster attack.
- Healer: 1 gold per missing HP.
- Defeat: lose 25% of carried gold, rounded up; gold floors at zero.
- Defeat recovery state: `max(1, maximumHp / 2)` HP, zero fights remaining, and an explicit defeated-for-today flag.
- Overnight recovery: full HP, 10 fights, and the defeated-for-today flag cleared.
- Promotion growth: +5 maximum HP, +2 Attack, +1 Defense, followed by full healing.
- Encounter weighting above Level 1: current-level monsters are selected twice as often as one-tier-older monsters. Older enemies therefore become visibly easier without eliminating current-level danger.
- Campaign roster: 22 data-driven monsters spanning intended Levels 1-10. LGD-003 added the minimum higher-level tiers needed to qualify campaign pacing; no broader content system was introduced.

### Cumulative XP thresholds

| Level | Cumulative XP |
| ---: | ---: |
| 1 | 0 |
| 2 | 50 |
| 3 | 250 |
| 4 | 600 |
| 5 | 1,250 |
| 6 | 2,800 |
| 7 | 5,600 |
| 8 | 9,500 |
| 9 | 14,500 |
| 10 | 20,500 |

Progression beyond Level 10 remains lawful through `20,500 + 7,000n + 500n(n-1)`, where `n` is the number of levels beyond 10. This avoids an accidental terminal level without affecting the qualified 30-day arc.

## Controlled defeat and recovery

Defeat is now an explicit durable daily state rather than merely an exhausted fight counter. Reaching 0 HP resolves combat once, grants no reward, retains XP/level/equipment/stats, applies the gold penalty once, restores half HP, sets fights to zero, and refuses further encounters until world advancement.

The UI reports the gold loss, safe progression, same-day Forest closure, and End Day recovery path. Town retains the warning, and Forest displays `[1] Search the Forest  <DEFEATED>`.

Schema v4 persists this flag. Save/reload after defeat, End Day after defeat, and next-day recovery are covered automatically and were reproduced on the tablet. Schema v1-v3 saves remain supported. The v3-to-v4 migration preserves identity, day, wealth, equipment, wounds, and community state while raising only old level-dependent XP/Attack values that would be unlawful under the new balance.

Deterministic scenarios cover defeat on the first, middle, and final fight; negligible and substantial gold; threshold-adjacent defeat; defeat after promotions; repeated defeats across separate days; Forest re-entry refusal; save/load; End Day; recovery; and gold-floor rounding.

## Deterministic balance qualification

The JVM harness uses the production `GameEngine`, monster selection, deterministic persisted random source, healer, Physical Training, validation, and `WorldAdvancer`. It does not use an abstract combat approximation.

- Cohort: 128 seeds per policy.
- Policies: cautious, ordinary, aggressive.
- Horizon: 30 realm days per seed.
- Total: 384 campaigns and 11,520 realm days.
- Each record includes level, XP, gold, HP, attempted/won fights, escapes, defeat/fight number, training eligibility/attempts/promotions, level-reached days, and consecutive defeated days.

| Policy | Day 1 median wins / XP | Level 2 on Day 1 | Defeated realm days | Median fight at defeat | Day 12 levels | Day 30 levels | Median gold Day 12 / 30 |
| --- | --- | ---: | ---: | ---: | --- | --- | ---: |
| Cautious | 9 / 91 | 100.0% | 1.5% | 5 | L2: 1, L4: 2, L5: 27, L6: 98 | L7: 1, L8: 4, L9: 104, L10: 19 | 687 / 3,278 |
| Ordinary | 9 / 93 | 97.7% | 10.9% | 6 | L5: 20, L6: 108 | L9: 127, L10: 1 | 592 / 2,830 |
| Aggressive | 10 / 100 | 98.4% | 17.3% | 6 | L5: 24, L6: 104 | L8: 1, L9: 124, L10: 3 | 597 / 2,800 |

Ordinary-policy median/P90 days to levels 2-9 were respectively:

`L2 1/1, L3 3/4, L4 5/6, L5 7/9, L6 11/13, L7 16/18, L8 22/23, L9 27/29`.

Level 10's median and P90 were both beyond Day 30. No late saturation was observed: only 1 of 128 ordinary campaigns reached Level 10 by Day 30. No abrupt late grind appeared; the representative run promoted at Days 1, 3, 5, 7, 11, 15, 21, and 26.

Repeated defeated days did occur under continued risk-taking, as intended: cohort maxima were 2 cautious, 4 ordinary, and 6 aggressive. No defeat-driven death spiral was observed. Every new day restored full combat capability, and no defeat removed XP, level, stats, or equipment.

Gold never became negative. Ordinary median carried gold was 592 on Day 12 and 2,830 on Day 30. This remains useful for healing and meaningfully exposed to the 25% penalty. Long-campaign wealth will need the planned LGD-005 shop/bank sinks, but it does not currently block or destabilize the LGD-003 loop.

## Play-feel review

### One day

The fresh-seed representative Day 1 completed 10 encounters, won 9, escaped once, promoted to Level 2, and ended at 92 XP, 39 gold, and 3 HP. Across the ordinary cohort the median was 9 wins and 93 XP. This provides several meaningful actions, danger from accumulated wounds, visible progress, the complete Training lesson, and a natural End Day boundary.

The physical tablet pass preserved an existing character rather than erasing user data. Starting from 7 XP with 8 fights left, ordinary play used the Forest, escape, and healer, reached exactly 50 XP after five victories, displayed the training-ready messages, and completed a clearly celebratory Level 2 promotion.

### Twelve days

An accelerated but genuine production-rule replay (ordinary policy, seed 42) reached Levels 2/3/4/5/6 on Days 1/3/5/7/11. Day 12 ended at Level 6, 3,944 XP, 819 gold, and 7 HP after 10 victories. Manual review of the full day-by-day trace found regular promotions and meaningful HP pressure without exhausting upper progression.

### Thirty-day campaign

The same complete trace reached Level 7 on Day 15, Level 8 on Day 21, and Level 9 on Day 26. Day 30 ended at 19,666 XP, 3,634 gold, and 24 HP, still 834 XP short of the noteworthy Level 10 promotion. Growth remained active in the latter half and current-tier monsters preserved combat relevance while older foes demonstrated gained power.

The 12- and 30-day reviews were accelerated JVM production-rule replays inspected by the Builder, complemented by physical checks of the affected combat, training, defeat, persistence, and End Day presentation. They were not represented as hundreds of manual tablet taps.

## Automated verification

Final combined command:

~~~text
gradlew test lint assembleDebug
~~~

Result: `BUILD SUCCESSFUL`.

- 71 unit tests passed; 0 failed, 0 errors, 0 skipped.
- New focused suites cover deterministic cohorts, controlled defeat, recovery, training thresholds, earliest promotion, stale repeated training, before/after promotion power, multi-promotion progression, persistence, and schema-v3 migration with already-leveled NPCs.
- Existing combat, character creation, save/load, world advancement, navigation, community simulation, and 1,000-day soak tests remain green.
- Android lint passed.
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`.
- APK size: 12,328,875 bytes.
- APK SHA-256: `67E0F41BC923328D3DE2A87303AFEF4757E04C4DCAC71D9B12A89850A6BAE183`.

## Physical TCL NXTPAPER qualification

Connected device: TCL NXTPAPER model `9491G`, serial `BC9424B4E3C5002`, 1600 x 2400 portrait.

Verified on-device:

- In-place APK install and cold launch (approximately 1.2-1.3 seconds).
- Existing schema-v3 Day 5 save migrated without erasure after a device-discovered NPC invariant defect was repaired.
- Real Forest combat, escape, repeated healing, XP accumulation, training readiness, Physical Training, and Level 2 result presentation.
- Promotion persisted across force-stop/relaunch/Continue at Level 2 and 25/25 HP.
- Controlled defeat after pushing onward at low HP: 48 gold became 36, exactly 25% lost; recovery was 12/25 HP; XP/level remained.
- Defeat survived force-stop/relaunch; Town retained the warning and Forest refused a new encounter with `<DEFEATED>`.
- End Day advanced exactly once to Day 7, restored 25/25 HP and 10 fights, retained 36 gold and Level 2, and cleared the restriction.
- Portrait readability, touch targets, scrollable terminal presentation, and promotion/defeat text fit were satisfactory.
- No LGD fatal exception or ANR signature appeared in the sampled device log.

The tablet is left in a lawful state at Day 7 Town: Arcturus Level 2, 25/25 HP, 36 gold, and 10 fights.

## Materially changed files

- `game/GameState.kt`, `GameRules.kt`, `MonsterDefinition.kt`, `GameStateValidator.kt`, `WorldAdvancer.kt`
- `data/GameStateCodec.kt`
- `ui/screens/GameplayScreens.kt`, `TerminalScreens.kt`
- `balance/BalanceHarness.kt`, `BalanceQualificationTest.kt`
- `ControlledDefeatQualificationTest.kt`, `TrainingQualificationTest.kt`
- Existing progression, first-loop, and persistence tests adjusted for schema v4 and the final thresholds.
- This report, documentation index, and preserved supplemental work order.

## Remaining balance concern

No material LGD-003 experience defect remains. The one known forward-looking concern is that gold naturally accumulates over a 30-day run because the player equipment economy is intentionally not part of LGD-003. LGD-005 is the appropriate place to add meaningful spending rather than distorting combat/healing now.

## Terminal state

~~~text
LGD003_CONTROLLED_DEFEAT_COMPLETE
LGD003_RECOVERY_LOOP_QUALIFIED
LGD003_PHYSICAL_TRAINING_QUALIFIED
LGD003_FIRST_PROMOTION_PACING_QUALIFIED
LGD003_12_DAY_PROGRESSION_QUALIFIED
LGD003_CAMPAIGN_PROGRESSION_QUALIFIED
LGD003_MOBILE_PLAY_FEEL_QUALIFIED
LGD003_COMPLETE
~~~
