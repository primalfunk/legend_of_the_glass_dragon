# LGD-011 Completion Report

Date: 2026-08-28

## 1. Disposition

`LGD011_BALANCE_CHARACTERIZED_PRINCIPAL_REVIEW_REQUIRED`

The accepted LGD-010 rules have been characterized quantitatively. This report
does not declare the game well balanced and does not authorize LGD-012.

ZERO human playtesting was requested or used. No discretionary production
balance tuning was performed.

## 2. Baseline

- Git commit / HEAD: unavailable because the workspace is not a Git repository.
- Save schema: 7.
- Player/world RNG: `PersistedGameRandom`, using the saved world seed and random
  step.
- Daily forest fights: 10.
- Qualification ceiling: 100 game days.
- Production balance constants and level thresholds are reproduced in the full
  characterization report.

No production Kotlin source or balance constant changed during LGD-011.

## 3. Harness architecture

The development-only JVM harness starts lawful games through `NewGameFactory`
and drives the production-domain actions used by the Android application:

- `GameEngine` for forest combat, healing and Physical Training;
- `EconomyEngine` for equipment and banking;
- `ChallengeEngine` for lawful Inn challenges;
- `WorldAdvancer` for Community and Dragon Season resolution;
- `DragonCombatEngine` for player Dragon attempts;
- `GameStateCodec` for representative interrupted/save-reload comparisons.

It supports five documented deterministic policies: Passive, Casual,
Ordinary, Aggressive and Reckless. Policy rules are included in the full
report and are test fixtures rather than production AI.

## 4. Qualified cohort

- Seasons attempted: 2,500.
- Allocation: 500 per policy.
- Complete seasons: 2,500.
- Unresolved seasons: 0.
- Total game days: 54,399.
- In-harness execution time: approximately 6.9 seconds on this workstation.
- Representative identical reruns: 10 seed/policy pairs.
- Continuous versus save/reload comparisons: 5 campaigns, one per policy,
  with checkpoints on Days 3, 7, 11, 17 and 23.

## 5. Ordinary-policy decision snapshot

| Metric | Result |
|---|---:|
| Median season duration | 20 days |
| P10–P90 duration | 19–22 days |
| Player Dragon victory | 97.8% (489/500) |
| NPC Dragon victory | 2.2% (11/500) |
| Unresolved | 0% |
| Median final player level | 7 |
| Median final player rank | 1 |
| Player rank-1 finish | 100% |
| Ordinary-combat defeat | 0.42% (344/82,018) |
| Training failure | 0% (0/2,999) |
| Median player Dragon attempts | 1 |
| Median final wealth | 380 gold |
| Median first player-ready day | 19 |
| Median challenge-window day | 19 |
| Median Chronicle entries/day | 7 |
| Median credible candidates at window | 1 |

Notable measured characteristics for Principal review include the high
Ordinary player-victory rate, the player's universal rank-1 Ordinary finish,
the low ordinary-combat defeat rate, deterministic/no-failure training, and a
Chronicle commonly at its seven-entry daily curation ceiling. These are
measurements, not Builder balance judgments.

## 6. Objective defects and repairs

No objective game-rule defect was exposed by the 2,500-season cohort. No
campaign failed to terminate, no progression lock occurred, lawful values
remained bounded, and deterministic reruns matched.

Harness-only accounting/typing issues found while building the tooling were
repaired before the qualified cohort. They did not change production behavior
or pre/post balance metrics.

## 7. Evidence

The full decision report is:

- `reports/evidence/LGD-011/LGD-011_BALANCE_CHARACTERIZATION.md`

Machine-readable evidence:

- `reports/evidence/LGD-011/campaigns.csv` — one row per season;
- `reports/evidence/LGD-011/days.jsonl` — one row per simulated day;
- `reports/evidence/LGD-011/monster_metrics.csv` — policy/enemy aggregates;
- `reports/evidence/LGD-011/summary.json` — headline distributions;
- `reports/evidence/LGD-011/outliers.json` — reproducible diagnostic seeds.

## 8. Source and tests

Material development-only sources:

- `app/src/test/java/com/legendoftheglassdragon/campaign/CampaignSimulationHarness.kt`;
- `CampaignCharacterizationReporter.kt`;
- `CampaignSimulationHarnessTest.kt`;
- `CampaignCharacterizationTest.kt`.

The harness tests cover lawful completion, full deterministic equality, codec
checkpoint equality, unique seed allocation, evidence creation and the
100-day qualification boundary.

Final qualification command:

`./gradlew testDebugUnitTest lintDebug assembleDebug assembleRelease`

- 123 JVM tests in 24 suites: 0 failures, 0 errors, 0 skipped;
- Android lint: no issues;
- debug APK assembled;
- unsigned release APK assembled.

## 9. Recommendation

`LGD011_BALANCE_CHARACTERIZED_PRINCIPAL_REVIEW_REQUIRED`

The Principal should decide whether any measured distributions require a
targeted tuning order. Do not proceed automatically to LGD-012.
