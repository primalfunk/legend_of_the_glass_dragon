# LGD-011A Completion Report

Disposition: `LGD011A_RECHARACTERIZED_PRINCIPAL_REVIEW_REQUIRED`

## Workspace state

- Pre-experiment authority: the accepted LGD-011 2,500-season evidence baseline in `docs/reports/evidence/LGD-011`.
- Git commit / HEAD: unavailable; this workspace is not a Git repository.
- Save schema: 8 (schema 7 migrates with an unused daily training trial and no invented result).
- App version remains `0.10.0` (`versionCode 10`).

## Single authorized experiment

- NPC upper cohort: Ambitious residents receive 6 daily actions, Aggressive 5, Greedy 4, and Erratic 3-5. High-performing personalities train and buy equipment more purposefully; Ambitious/Aggressive defeat exposure was reduced without hidden stat bonuses.
- Dragon: HP 100 -> 112, attack 20 -> 22, breath bonus 4 -> 6. Readiness now distinguishes a dangerous legal attempt from a well-prepared one in player-facing copy.
- Training: one seeded trial per game day; base success 80%, +5% at 75%+ health, +3% for suitable equipment, capped at 95%. Failure preserves XP/level, costs one quarter of maximum HP (minimum 1, never below 1 HP), and permits retry next day.
- Chronicle: deterministic importance/callback-based daily cap of 3-7 replaced the equal-resident quota; callbacks outrank otherwise equal events.
- Continuity: NPC challenge chance 35% -> 55%; recent lawful challenge pairs receive a 55% deterministic rematch preference for up to six days.
- Casual policy: 5 -> 6 fights/day and Dragon intent changed from Level 8/tier 5/power 135/two-day delay to Level 7/tier 4/power 128/one-day delay.
- Economy/equipment catalog, player rewards, prices, banking, progression thresholds, and Dragon-stage pressure constants were held.

## Revised cohort

- 2,500 seasons; 500 each Passive, Casual, Ordinary, Aggressive, Reckless.
- 2,500 completed; 0 unresolved.
- 49,603 simulated game days.
- 7.59 seconds measured harness runtime; 329.59 seasons/second.
- 10/10 identical seed/policy reruns matched.
- 5/5 continuous versus save/reload executions matched, including training failure, player Dragon defeat, NPC victory, multiple champions, and callback history.
- Objective defects discovered: none.

## Principal findings

The experiment made campaigns less deterministic in training and Dragon ownership, but it overshot several requested exploratory ranges and did not solve ranking dominance:

| Ordinary metric | Before | After |
|---|---:|---:|
| Median days | 20 | 20 |
| P10-P90 days | 19-22 | 18-21 |
| Player Dragon victory | 97.8% | 12.2% |
| NPC Dragon victory | 2.2% | 87.8% |
| Player Rank #1 | 100% | 98.6% |
| Median credible candidates | 1 | 3 |
| Dragon first-attempt win | 91.67% | 9.70% |
| Training first-attempt success | 100% | 84.51% |
| Final levels | 499 L7 / 1 L6 | 437 L7 / 63 L6 |
| Chronicle mean / median | 6.71 / 7 | 5.80 / 6 |
| Structured callbacks / season | 0.57 | 5.41 |

Additional findings:

- Casual still attempted the Dragon in 0% of seasons because NPCs resolved the campaign before the cautious policy reached Level 7. Its decision rule is more plausible, but the measured policy remains non-participatory.
- Chronicle density remained concentrated at six items (74.49% of Ordinary days); only 0.62% of days carried one to three items.
- NPC narrative concentration increased modestly (top NPC share 12.26%, top-three share 31.83%), but almost all 16 residents still appeared repeatedly.
- Training met the exploratory range and remained bounded: 84.51% first-attempt success, 13.45% second-attempt promotions, 2.04% at 3+ attempts, maximum six attempts, no progression locks.
- Ordinary final wealth remained broadly comparable: median 292, P10-P90 29-592; the item ladder and core economy were unchanged.

These are balance findings, not correctness defects. Per the one-experiment stop rule, no second tuning pass was performed.

## Evidence

- Full decision report: `docs/reports/evidence/LGD-011A/LGD-011A_TARGETED_BALANCE_RECHARACTERIZATION.md`
- Per-season CSV: `docs/reports/evidence/LGD-011A/lgd011a_campaigns.csv`
- Per-day JSONL: `docs/reports/evidence/LGD-011A/lgd011a_days.jsonl`
- Per-level training CSV: `docs/reports/evidence/LGD-011A/lgd011a_training.csv`
- Machine summary: `docs/reports/evidence/LGD-011A/lgd011a_summary.json`
- Reproducible outliers: `docs/reports/evidence/LGD-011A/lgd011a_outliers.json`

## Qualification boundary

- Final JVM suite: 126 tests across 25 suites; 0 failures, 0 errors, 0 skipped.
- Android gates: `lintDebug`, `assembleDebug`, and `assembleRelease` passed.
- APKs: `app/build/outputs/apk/debug/app-debug.apk` and `app/build/outputs/apk/release/app-release-unsigned.apk`.
- Human playtesting requested or used: **ZERO**.
- Unauthorized iterative self-tuning: **none**.
- LGD-012 was not started.

Recommendation: `LGD011A_RECHARACTERIZED_PRINCIPAL_REVIEW_REQUIRED`
