# LGD-011A Targeted Balance Recharacterization

Disposition: `LGD011A_RECHARACTERIZED_PRINCIPAL_REVIEW_REQUIRED`

This is one targeted tuning experiment followed by one 2500-season cohort. No human playtesting or discretionary second-round tuning was used.

## Cohort

- Seasons: 2500 (PASSIVE=500, CASUAL=500, ORDINARY=500, AGGRESSIVE=500, RECKLESS=500).
- Completed: 2500; unresolved: 0.
- Total game days: 49603.
- Runtime: 7.59 seconds (329.59 seasons/second).
- Deterministic reruns: 10; save/reload comparisons: 5.

## Principal Before / After Table

| Ordinary metric | LGD-011 before | LGD-011A after |
|---|---:|---:|
| Median campaign days | 20 | 20.00 |
| P10-P90 campaign days | 19-22 | 18.00-21.00 |
| Player Dragon victory | 97.8% | 12.20% |
| NPC Dragon victory | 2.2% | 87.80% |
| Player Rank #1 | 100% | 98.60% |
| Median credible candidates | 1 | 3.00 |
| Dragon first-attempt win | 91.67% | 9.70% |
| Mean player Dragon attempts | 1.06 | 1.11 |
| Median final player level | 7 | 7.00 |
| Final-level distribution | 499 L7 / 1 L6 | L6=63, L7=437 |
| Training first-try success | 100% | 84.51% |
| Ordinary combat defeat | 0.42% | 0.36% |
| Reckless combat defeat | 1.47% | 1.64% |
| Chronicle mean | 6.71 | 5.80 |
| Chronicle median | 7 | 6.00 |
| Zero-item Chronicle days | 0% | 0.00% |
| Top NPC Chronicle share | 9.76% | 12.26% |
| Structured callbacks / season | 0.57 | 5.41 |

## Policy Comparison

| Policy | Median days | Player win | NPC win | Median level | Median rank | Combat defeat | Median wealth | Fights/day | Mean Dragon attempts | Training failure | Median first-ready day |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| Passive | 20 | 0.00% | 100.00% | 2 | 17 | 0.45% | 23 | 0.95 | 0.00 | 13.92% | - |
| Casual / Conservative | 20 | 0.00% | 100.00% | 5 | 3 | 0.29% | 131 | 5.06 | 0.00 | 16.16% | - |
| Ordinary | 20 | 12.20% | 87.80% | 7 | 1 | 0.36% | 292 | 8.34 | 1.11 | 15.38% | 19 |
| Aggressive / Optimizing | 19 | 61.40% | 38.60% | 7 | 1 | 0.58% | 343 | 9.25 | 0.90 | 15.52% | 18 |
| Reckless | 19 | 67.40% | 32.60% | 7 | 1 | 1.64% | 369 | 8.95 | 0.88 | 15.59% | 19 |

Casual policy after: 6 fights/day; heals at 75% HP; runs with three maximum incoming hits left; buys one clear upgrade per visit; attempts once Level 7, weapon/armor tier 4, combat power 128, and one full challenge-window day have passed. Other policy decision constants were held.

## Campaign Duration

Ordinary range 17-24, mean 19.93, median 20.00, P10/P25/P75/P90/P95 18.00/19.00/21.00/21.00/22.00.

| Days | Count |
|---|---:|
| 1-5 | 0 |
| 6-10 | 0 |
| 11-15 | 0 |
| 16-20 | 352 |
| 21-25 | 148 |
| 26-30 | 0 |
| 31-40 | 0 |
| 41-50 | 0 |
| 51+ | 0 |

## Dragon Outcomes and Attempts

- Player victory: 12.20%; NPC victory: 87.80%; unresolved: 0.00%.
- Player never attempted: 34.00%.
- Player lost then NPC won: 53.80%.
- Player lost then later won: 5.80%.
- Player first-attempt wins: 9.70% among attempters; multiple-attempt player wins: 5.80% of seasons.
- Mean/median/max player attempts: 1.11/1/4.
- NPC first-attempt victory: 28.02%; multiple NPC challengers before resolution: 66.20%.

## Player Progression

- Level 6: 63 / 500.
- Level 7: 437 / 500.

| Level reached | Reached | Median day | P10-P90 day |
|---:|---:|---:|---:|
| 2 | 500/500 | 1.00 | 1.00-2.00 |
| 3 | 500/500 | 5.00 | 3.00-7.00 |
| 4 | 500/500 | 7.00 | 5.00-10.00 |
| 5 | 500/500 | 9.00 | 7.00-12.00 |
| 6 | 500/500 | 12.00 | 11.00-15.00 |
| 7 | 437/500 | 17.00 | 16.00-19.00 |
| 8 | 0/500 | - | - |
| 9 | 0/500 | - | - |

## Physical Training

- Attempts/successes/failures: 3471 / 2937 / 534 (15.38% failure).
- First-attempt promotion: 84.51%; second-attempt promotion: 13.45%; 3+ attempts: 2.04%.
- Maximum attempts for one successful promotion: 6; maximum observed consecutive failure streak: 5.
- Average delay from failed trials per successful promotion: 0.18 realm days; progression-lock cases: 0.

| Target level | Attempts | Successes | Failures | Success rate |
|---:|---:|---:|---:|---:|
| 2 | 599 | 500 | 99 | 83.47% |
| 3 | 604 | 500 | 104 | 82.78% |
| 4 | 611 | 500 | 111 | 81.83% |
| 5 | 587 | 500 | 87 | 85.18% |
| 6 | 574 | 500 | 74 | 87.11% |
| 7 | 496 | 437 | 59 | 88.10% |

## Rankings and NPC Competition

- Player #1: 98.60%; top 3: 99.80%; median rank: 1.
- Top NPC final level median/P10/P90/max: 6/5/6/7.
- NPCs at player level - 1 or higher (mean): 1.90; equal: 0.17; above: 0.00.
- Credible candidate median/P10/P90: 3.00/1.00/5.00; 1=28.40%, 2=18.40%, 3=23.60%, 4+=29.60%.
- NPC Dragon-ready before player: 99.80%.

## Dragon Stage Timing

- WHISPERS: median 1, P10-P90 1-1, range 1-1.
- SIGNS: median 4, P10-P90 4-4, range 3-5.
- FEAR: median 8, P10-P90 8-9, range 7-10.
- CRISIS: median 14, P10-P90 13-15, range 12-16.
- CHALLENGE_WINDOW: median 18, P10-P90 18-19, range 17-21.
- RESOLVED: median 20, P10-P90 18-21, range 17-24.

## Chronicle Density and Cast

| Items | Ordinary days | Share |
|---:|---:|---:|
| 0 | 0 | 0.00% |
| 1 | 61 | 0.61% |
| 2 | 0 | 0.00% |
| 3 | 1 | 0.01% |
| 4 | 93 | 0.93% |
| 5 | 1948 | 19.55% |
| 6 | 7422 | 74.49% |
| 7+ | 439 | 4.41% |

Mean/median/P10-P90/max: 5.80/6.00/5.00-6.00/7.00. Dragon share 12.97%; callback share 4.68%.

- NPCs with any meaningful presence: mean 15.99; repeated presence: 15.78; prominent (4+ mentions): 13.49; absent: 0.01.
- Mean top-NPC share: 12.26%; top-three share: 31.83%.
- Structured callbacks: 5.41 per season; range 0-14.

## Economy and Equipment (Held)

- No player gold rewards, shop prices, banking rules, item definitions, or equipment ladder values changed.
- Ordinary final wealth median/P10-P90: 292/29-592.
- Final weapons: Battle Axe=377, Steel Longsword=121, Broad Sword=2.
- Final armor: Knight Plate=367, Mithril Mail=112, Scale Armor=21.

## Reproducible Outliers

- Shortest season: `RECKLESS:5000011` (17 days).
- Longest season: `PASSIVE:1000154` (25 days).
- Earliest player win: `AGGRESSIVE:4000002` (day 18).
- Latest player win: `ORDINARY:3000090` (day 23).
- Strongest NPC win: `CASUAL:2000343` (top NPC level 7).
- Weakest successful player: `RECKLESS:5000034` (level 7).
- Greatest training-failure streak: `AGGRESSIVE:4000147` (5 failures).
- Greatest combat-defeat count: `RECKLESS:5000217` (9 defeats).
- Most Dragon attempts: `ORDINARY:3000029` (9 attempts).
- Most credible challengers: `AGGRESSIVE:4000064` (8 candidates).
- Most callback-heavy: `RECKLESS:5000241` (20 callbacks).
- Quietest Chronicle: `AGGRESSIVE:4000423` (5.26 entries/day).
- Most dominant NPC: `CASUAL:2000346` (26 appearances).

## Determinism and Save / Load

- Identical seed/policy reruns: 10/10 matched.
- Continuous versus interrupted campaigns: 5/5 matched authoritative results.
  - `training_failure:CASUAL:2000001`
  - `player_dragon_defeat:ORDINARY:3000002`
  - `npc_dragon_victory:PASSIVE:1000001`
  - `multiple_credible_champions:CASUAL:2000001`
  - `rivalry_callback:PASSIVE:1000001`
- No uncontrolled training or narrative randomness was introduced; both consume/derive from persisted deterministic state.

## Defects and Scope Confirmation

- Objective defects discovered in the revised cohort: none.
- Human playtesting requested or used: **ZERO**.
- Unauthorized iterative self-tuning: **none**. This report stops after the single authorized experiment.
- Economy/equipment core values changed: **none**. Only NPC purchasing frequency for the strong personalities changed.

Recommendation: `LGD011A_RECHARACTERIZED_PRINCIPAL_REVIEW_REQUIRED`
