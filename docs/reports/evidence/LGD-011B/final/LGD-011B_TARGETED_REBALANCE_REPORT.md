# LGD-011B Targeted Rebalance to Principal Ranges

Disposition: `LGD011B_TARGET_RANGES_QUALIFIED_PRINCIPAL_REVIEW_REQUIRED`

This report characterizes the final fifth-round candidate. It does not authorize LGD-012.
ZERO human playtesting was requested or used.

## Baseline and final workspace

- Authoritative pre-tuning baseline: LGD-011A, 2,500 seasons, save schema 8.
- Git baseline/current HEAD: unavailable because this workspace is not a Git repository.
- Final save schema: 8; no migration change was needed.
- Application: `com.legendoftheglassdragon`, version `0.10.0`, versionCode `10`.
- Final cohort: 5,000 seasons, 1,000 per policy.
- Completed: 5,000; unresolved: 0.
- Total simulated game days: 100,706.
- Harness runtime: 14.13 seconds, 353.82 seasons/second.
- No objective defect was found in the final cohort.

## Exact final changes

### Dragon combat and readiness

- Glass Dragon maximum HP: 112 -> 108.
- Dragon attack: 22 -> 21.
- Crystal/fractured defense held at 15/11.
- Breath bonus: 6 -> 7 in the final candidate; it still occurs every third turn.
- Shared abstract Dragon power: 118 -> 125, now used by NPC resolution and attempt telemetry.
- NPC Dragon minimum combat power: 95 -> 105.
- NPCs cannot resolve the Dragon on the first night the challenge window opens.
- Readiness is now classified as `NOT_READY`, `DANGEROUS`, `READY`, or `WELL_PREPARED` from authoritative level, equipment, health, stage, and combat power.
- The Ordinary harness retreats only at 10% HP, except when its maximum possible next hit can finish the Dragon.
- There is no player-only combat bonus and no disconnected Dragon win roll.

### NPC upper cohort

- Only Ambitious residents receive the final 9-10 daily action budget.
- Aggressive remains 5, Greedy 4, Cautious/Social 2, and Erratic 3-5.
- The existing lawful stat, XP, training, defeat, gold, and canonical equipment rules remain authoritative.
- Ambitious residents keep a 96% training decision chance and 58% lawful equipment-purchase chance; they do not receive free equipment, stats, or gold.

### Chronicle and continuity

- Events now qualify through importance >= 3, a real callback, direct player impact, rank change, Dragon event, or a genuinely large success.
- Deterministic daily target sizes are 3-6 for normal days and 7 for Dragon victory.
- Continuity remains ahead of otherwise equal events in deterministic ordering.
- NPC rematch preference is 45%, down from LGD-011A's 55%, concentrating callbacks without retaining the earlier saturation.

### Casual policy

- Uses 8 fights/day, heals at 75%, avoids combat with roughly three maximum incoming hits remaining, buys one clear upgrade per visit, and challenges every four days.
- Fixed policy-side level/equipment/power gates were removed.
- Once production rules say the player is legally Dragon-ready, Casual attempts immediately if well prepared or after at most two further days.
- Passive remains non-participatory by design.

### Protected systems

- Training implementation and probabilities were not changed.
- Player gold rewards, equipment prices, banking, equipment catalog, and broad economy were not changed.
- Dragon pressure/stage constants were not changed.
- Save schema 8 architecture and deterministic RNG were preserved.

## Authorized tuning rounds

The instrumented baseline and each exploratory round used 400 Ordinary seasons plus 25 each of the other four policies. The full Round-4 validation used 1,000 per policy and was retained after it exposed sample-size drift.

| Candidate | Median days | P10-P90 | Player win | NPC win | Rank #1 | Candidates | First-attempt win | Mean attempts | Training first | Combat defeat | Chronicle mean/median | Callbacks |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| Instrumented LGD-011A sample | 20 | 19-21 | 9.00% | 91.00% | 98.25% | 3 | 6.49% | 1.11 | 84.11% | 0.39% | 5.81 / 6 | 5.39 |
| Round 1 | 20 | 19-22 | 20.25% | 79.75% | 94.50% | 5 | 10.09% | 1.53 | 82.94% | 0.40% | 5.43 / 5 | 4.73 |
| Round 2 | 20 | 19-22 | 63.25% | 36.75% | 88.25% | 2 | 52.44% | 1.16 | 83.89% | 0.43% | 5.31 / 5 | 4.54 |
| Round 3 | 20 | 19-21 | 76.25% | 23.75% | 78.25% | 2 | 80.69% | 0.95 | 83.89% | 0.42% | 5.30 / 5 | 4.51 |
| Round 4 | 20 | 19-21 | 71.75% | 28.25% | 73.00% | 2 | 71.85% | 1.00 | 82.88% | 0.47% | 5.31 / 5 | 4.48 |
| Round-4 full validation | 20 | 19-21 | 77.60% | 22.40% | 75.80% | 2 | 78.61% | 1.01 | 83.37% | 0.38% | 5.30 / 5 | 4.41 |
| Round 5 exploratory | 20 | 19-21 | 65.50% | 34.50% | 71.00% | 2 | 64.39% | 1.01 | 83.35% | 0.44% | 5.34 / 5 | 4.53 |
| **Round 5 final, 1,000 Ordinary** | **20** | **19-21** | **73.70%** | **26.30%** | **73.00%** | **2** | **71.88%** | **1.02** | **83.31%** | **0.38%** | **5.32 / 5** | **4.51** |

Round progression:

1. Added attempt instrumentation and readiness tiers; aligned abstract Dragon power, softened player combat from LGD-011A, delayed first-night NPC resolution, strengthened Ambitious opportunity use, tightened Chronicle selection, and removed Casual's fixed policy gates.
2. Raised the NPC readiness floor to restrict the candidate field, added bounded Ambitious variance, and corrected Ordinary retreat/finishing behavior.
3. Reduced Dragon attack by one, strengthened the upper cohort, and allowed a calculated finishing strike.
4. Raised breath by one and broadened Ambitious actions to 8-10. The exploratory result passed, but the 5,000-season validation put first-attempt success and Rank #1 just above the target ceilings.
5. Final authorized adjustment: breath 6 -> 7 and Ambitious actions 8-10 -> 9-10. No subsequent tuning occurred.

## Three-generation Principal table

| Metric | LGD-011 | LGD-011A | LGD-011B |
|---|---:|---:|---:|
| Median days | 20 | 20 | **20** |
| P10-P90 days | 19-22 | 18-21 | **19-21** |
| Player Dragon victory | 97.8% | 12.2% | **73.70%** |
| NPC Dragon victory | 2.2% | 87.8% | **26.30%** |
| Player Rank #1 | 100% | 98.6% | **73.00%** |
| Player Top 3 | unavailable | 99.8% | **99.30%** |
| Median credible candidates | 1 | 3 | **2** |
| First Dragon-attempt win | 91.67% | 9.70% | **71.88%** |
| Mean player Dragon attempts | 1.06 | 1.11 | **1.02** |
| Training first-attempt | 100% | 84.51% | **83.31%** |
| Combat defeat | 0.42% | 0.36% | **0.38%** |
| Chronicle mean | 6.71 | 5.80 | **5.32** |
| Chronicle median | 7 | 6 | **5** |
| Callbacks/season | 0.57 | 5.41 | **4.51** |
| Median final level | 7 | 7 | **7** |
| Median final rank | 1 | 1 | **1** |

## Policy comparison

| Policy | Median days | Player win | NPC win | Median level | Median rank | Combat defeat | Median wealth | Fights/day | Mean Dragon attempts | First-attempt win | Training first | Median ready/attempt day |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| Passive | 21 | 0.00% | 100.00% | 2 | 17 | 0.45% | 22 | 0.95 | 0.00 | 0.00% | 84.64% | - / - |
| Casual | 21 | 0.50% | 99.50% | 6 | 3 | 0.24% | 194 | 6.41 | 0.26 | 0.63% | 83.43% | 20 / 21 |
| Ordinary | 20 | 73.70% | 26.30% | 7 | 1 | 0.38% | 293 | 8.23 | 1.02 | 71.88% | 83.31% | 19 / 20 |
| Aggressive | 20 | 97.60% | 2.40% | 7 | 1 | 0.61% | 372 | 9.17 | 1.02 | 96.89% | 84.76% | 19 / 20 |
| Reckless | 20 | 98.40% | 1.60% | 7 | 1 | 1.59% | 428 | 8.90 | 1.00 | 98.29% | 83.37% | 19 / 20 |

Reckless ordinary-combat defeat is 4.18 times Ordinary's rate, but Aggressive and Reckless Dragon outcomes remain close. This is reported as a characterization constraint, not retuned after the fifth round.

## Ordinary campaign duration

- Mean/median: 19.90 / 20 days.
- P10/P25/P75/P90/P95: 19 / 19 / 20 / 21 / 22.
- Range: 18-24 days.

| Resolution day | Seasons |
|---:|---:|
| 18 | 32 |
| 19 | 310 |
| 20 | 457 |
| 21 | 144 |
| 22 | 47 |
| 23 | 8 |
| 24 | 2 |

## Dragon outcomes and attempts

- Player victory: 737/1,000 (73.70%).
- NPC victory: 263/1,000 (26.30%).
- Unresolved: 0/1,000.
- Player made at least one attempt: 889/1,000 (88.90%).
- Player never attempted: 111/1,000 (11.10%).
- First attempts won: 639/889 (71.88%).
- First-attempt wins as a share of all seasons: 63.90%.
- All player attempts won: 737/1,023 (72.04%).
- Player lost/retreated and later won: 98 seasons (9.80%).
- Player lost/retreated and an NPC later won: 152 seasons (15.20%).
- Mean player attempts: 1.02; repeated-attempt wins: 9.80% of seasons.
- NPC first attempts won: 251/420 (59.76%); all NPC attempts: 263/436 (60.32%).
- Multiple distinct NPC challengers appeared before resolution in 7.80% of seasons.

### Attempt readiness comparison

| Metric | Player attempts | NPC attempts |
|---|---:|---:|
| Attempts | 1,023 | 436 |
| Mean / median level | 7.00 / 7 | 6.32 / 6 |
| Mean attack | 27.32 | 21.76 |
| Mean defense | 17.28 | 14.22 |
| Mean maximum HP | 50.00 | 46.65 |
| Mean combat power | 150.92 | 123.89 |
| Readiness tiers | 1,023 Well Prepared | 74 Dangerous / 105 Ready / 257 Well Prepared |
| First-attempt success | 71.88% | 59.76% |
| All-attempt success | 72.04% | 60.32% |
| Median first-ready day | 19 | 19 |
| Median first-attempt day | 20 | 20 |

Player attempt equipment: Battle Axe 789, Steel Longsword 234; Knight Plate 928, Mithril Mail 95.

NPC attempt equipment: Broad Sword 276, Knight's Sword 75, Steel Blade 20, Steel Longsword 65; Chain Mail 17, Scale Armor 242, Plate Armor 120, Knight Plate 57.

### Why NPCs won

Exclusive categories among the 263 Ordinary NPC victories:

| Category | Count | Share of NPC victories |
|---|---:|---:|
| A. Victor stronger than player | 125 | 47.53% |
| B. NPC ready earlier | 78 | 29.66% |
| C. Player lost, NPC won later | 60 | 22.81% |
| D. Player never attempted, without A/B | 0 | 0.00% |
| E. Other | 0 | 0.00% |

Categories are assigned in A->B->C->D->E order. “Stronger” means the actual victor finished above the player, exceeded the player's level, or recorded greater attempt combat power. Thus 77.19% of NPC wins were primarily classified as actual strength or earlier readiness; Dragon over-difficulty is no longer the principal explanation.

## Player ranks and levels

| Final rank | Count | Share |
|---:|---:|---:|
| 1 | 730 | 73.00% |
| 2 | 216 | 21.60% |
| 3 | 47 | 4.70% |
| 4 | 7 | 0.70% |

- Mean/median rank: 1.33 / 1; P10-P90: 1-2.
- Top 3: 99.30%; Top 5: 100.00%.

| Final level | Count | Share |
|---:|---:|---:|
| 5 | 2 | 0.20% |
| 6 | 71 | 7.10% |
| 7 | 926 | 92.60% |
| 8 | 1 | 0.10% |

Median level timing: L2 Day 1, L3 Day 4, L4 Day 6, L5 Day 8, L6 Day 12, L7 Day 17. P10-P90 L7 timing is Day 16-20.

## NPC upper tail and candidates

- Median NPC level across the population: 4; maximum observed: 8.
- Top NPC level: L5 in 39 seasons, L6 in 452, L7 in 481, L8 in 28.
- Top-three median levels: 7 / 6 / 5.
- Mean NPC counts relative to player: 1.65 at player level - 1; 0.63 equal; 0.10 above.
- NPC Dragon-ready before player: 20.40%.

| Credible candidates | Seasons | Share |
|---:|---:|---:|
| 1 | 186 | 18.60% |
| 2 | 397 | 39.70% |
| 3 | 287 | 28.70% |
| 4+ | 130 | 13.00% |

Candidate median/P10/P90: 2 / 1 / 4; range 1-6.

## Training

- Attempts/successes/failures: 7,096 / 5,926 / 1,170.
- First-attempt promotion: 83.31%.
- Second-attempt promotion: 14.14%; 3+ attempts: 2.55%.
- Maximum attempts: 6; maximum consecutive failure streak: 5.
- Average delay introduced by failure: 0.20 day per successful promotion.
- Progression-lock cases: 0.
- First-attempt success by target level: L2 83.00%, L3 79.80%, L4 82.30%, L5 83.40%, L6 86.17%, L7 84.87% (one L8 trial succeeded).

Training architecture and probabilities were preserved from LGD-011A.

## Dragon stages

| Stage | Median day | P10-P90 | Range |
|---|---:|---:|---:|
| Whispers | 1 | 1-1 | 1-1 |
| Signs | 4 | 4-4 | 3-5 |
| Fear | 8 | 8-9 | 7-10 |
| Crisis | 14 | 13-15 | 12-16 |
| Challenge Window | 19 | 18-19 | 17-21 |
| Resolved | 20 | 19-21 | 18-24 |

Stage constants were held.

## Chronicle density, callbacks, and spotlight

| Items | Ordinary days | Share |
|---:|---:|---:|
| 0 | 1 | 0.01% |
| 1 | 733 | 3.68% |
| 2 | 14 | 0.07% |
| 3 | 57 | 0.29% |
| 4 | 493 | 2.48% |
| 5 | 8,960 | 45.03% |
| 6 | 9,385 | 47.17% |
| 7+ | 253 | 1.27% |

- Mean/median/P10-P90: 5.32 / 5 / 5-6.
- Ceiling days: 1.27%, down from LGD-011A's near-saturation.
- Callbacks: 4.51/season; callback share: 4.27% of Chronicle entries.
- Incidents receiving at least one callback: 4.51/season.
- Rivalry/rematch callbacks: 4.51/season; Dragon-preparation callbacks: 0.00/season.
- NPCs with any meaningful appearance: 15.93; repeated appearances: 15.48; absent: 0.07.
- Mean top-NPC share: 14.21%; top-three share: 34.78%.

The numerical Chronicle targets pass, but the distribution remains concentrated at five or six entries and exposure remains broad across nearly all residents. Quiet days now exist, primarily as one-item days rather than the envisioned two-to-three-item pattern. This is a genuine narrative-density constraint for Principal review, not an objective defect and not retuned after the fifth round.

## Casual characterization

- Median first legal Dragon-ready day: 20.
- Median first attempt day among attempters: 21.
- At least one attempt: 160/1,000 (16.00%).
- Player victory: 0.50%; NPC victory: 99.50%.
- Final levels: L2 5, L3 14, L4 31, L5 164, L6 532, L7 254.
- Fights/day: 6.41.

Fixed policy gates are gone, but production eligibility still requires Level 7, tier-4 weapon/armor, full health, and the challenge window. Casual ends 74.6% of seasons below Level 7, and NPCs usually resolve the season before the two-day bounded delay expires. The policy is now willing once lawful but still attempts infrequently; its Dragon win rate should not be treated as representative cautious-player combat odds.

## Economy and equipment

- Player-facing economy, prices, banking, and equipment ladder were held.
- Ordinary final wealth median/P10-P90: 293 / 46-603; range 0-688.
- Final weapons: Battle Axe 834, Steel Longsword 157, Broad Sword 9.
- Final armor: Knight Plate 881, Mithril Mail 99, Scale Armor 20.
- Mean healing/equipment/defeat-loss totals: 260.95 / 2,393.08 / 5.08 gold.

## Reproducible outliers

- Shortest season / earliest player win: `RECKLESS:5000001`, Day 18.
- Longest season: `CASUAL:2000002`, Day 27.
- Latest player win: `CASUAL:2000143`, Day 23.
- Strongest NPC win: `AGGRESSIVE:4000407`, top NPC Level 8.
- Weakest successful player: `RECKLESS:5000057`, Level 7 at 5,669 XP.
- Greatest training-failure streak: `AGGRESSIVE:4000672`, five failures.
- Greatest combat-defeat count: `RECKLESS:5000351`, nine defeats.
- Most Dragon attempts: `CASUAL:2000065`, nine attempts.
- Most credible challengers: `RECKLESS:5000069`, six.
- Most callback-heavy: `RECKLESS:5000393`, 18 callbacks.
- Quietest Chronicle: `PASSIVE:1000791`, 4.90 items/day.
- Most dominant NPC: `ORDINARY:3000270`, 29.70% of NPC Chronicle appearances.
- Strong Ordinary NPC-star examples: `ORDINARY:3000919` (player Rank 4, victor Level 7) and `ORDINARY:3000119` (player Rank 4, top NPC Level 8).
- Unresolved seed: none.

## Determinism and save/reload

- Runner seed/policy reruns: 10/10 exact matches.
- Condition-selected reruns: 10/10 exact matches, covering training failure, player Dragon defeat, NPC victory, player victory, strong NPC #1, callbacks, Ordinary retry victory, Casual attempt, multiple candidates, and Reckless ordinary defeat.
- Continuous versus interrupted save/reload: 5/5 exact authoritative matches.
- Save/reload cases included Ordinary Dragon loss then later victory, NPC beating the player to the Dragon, strong NPC ranking lead, training failure, and a Chronicle callback chain.
- No duplicate Dragon attempt, campaign deadlock, training lock, illegal NPC resource, callback duplication, or save divergence was found.

## Build qualification

- Final JVM suite: 128 tests across 27 suites; 0 failures, 0 errors, 0 skipped.
- `lintDebug`: passed.
- `assembleDebug`: passed.
- `assembleRelease`: passed.
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`.
- Release APK: `app/build/outputs/apk/release/app-release-unsigned.apk`.

## Evidence files

- `lgd011b_campaigns.csv`: all 5,000 season records.
- `lgd011b_days.jsonl`: all 100,706 day records.
- `lgd011b_dragon_attempts.csv`: authoritative player/NPC attempt telemetry.
- `lgd011b_training.csv`: per-season, per-target-level training outcomes.
- `lgd011b_summary.json`: aggregate distributions and qualifications.
- `lgd011b_outliers.json`: reproducible diagnostic seeds.
- `../exploratory/`: instrumented baseline, all five exploratory rounds, and the full Round-4 validation.

## Scope confirmation

- Human testing requested or used: **ZERO**.
- Tuning stopped after the fifth authorized round.
- No factions, content, audio, visual, economy, equipment, stage-clock, or save-schema expansion was introduced.
- LGD-012 was not started.

Recommendation: `LGD011B_TARGET_RANGES_QUALIFIED_PRINCIPAL_REVIEW_REQUIRED`
