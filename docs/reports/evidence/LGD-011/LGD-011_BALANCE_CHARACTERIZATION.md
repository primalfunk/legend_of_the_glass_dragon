# LGD-011 Campaign Simulation + Balance Characterization

Generated from the accepted LGD-010 production rules. This is measurement, not a declaration that the balance is accepted.

## Disposition

`LGD011_BALANCE_CHARACTERIZED_PRINCIPAL_REVIEW_REQUIRED`

ZERO human playtesting was requested or used. No discretionary balance tuning was performed.

## Baseline and method

- Git baseline/HEAD: unavailable; this workspace is not a Git repository.
- Save schema: 7.
- Cohort: 2500 deterministic complete-season attempts, 54399 total game days.
- Allocation: PASSIVE=500, CASUAL=500, ORDINARY=500, AGGRESSIVE=500, RECKLESS=500.
- Ceiling: 100 game days; unresolved runs are retained rather than forced.
- Runtime: 7.10 seconds (352.09 seasons/second).
- Memory: no maximum-heap profiler was attached; the 2,500-season in-memory cohort completed without an observed allocation failure or production-rule shortcut.
- RNG authority: saved world seed + monotonically increasing random step through `PersistedGameRandom`; narration does not consume gameplay RNG.
- Production systems exercised: NewGameFactory, GameEngine, EconomyEngine, ChallengeEngine, WorldAdvancer/CommunitySimulator, DragonSeasonSimulator and DragonCombatEngine.

### Frozen balance configuration

- Forest fights/day: 10; escape: 65%; defeat carried-gold loss: 25%.
- Promotion growth: +5 HP, +2 base attack, +1 base defense.
- XP thresholds: L2=50, L3=250, L4=600, L5=1250, L6=2800, L7=5600, L8=9500, L9=14500, L10=20500.
- Dragon pressure thresholds: Signs 18, Fear 40, Crisis 68, Window 92; guaranteed NPC resolution boundary Day 32.

## Synthetic policy definitions

- PASSIVE: fights 0 on odd days and at most 2 on even days; no challenges or Dragon attempt; defensive healing/escape; only checks one upgrade every fifth day; banks above 25 gold.
- CASUAL: at most 5 forest fights/day; heals at 75% HP; stops below 55% if healing is unaffordable; escapes at roughly three incoming hits; one challenge every fourth day; one shop upgrade/visit; waits for Level 8, tier-5 gear, power 135 and two open-window days before considering the Dragon.
- ORDINARY: at most 9 fights/day; heals at 52% HP; stops below 30% if unable to heal; escapes at roughly two incoming hits; one challenge every third day; up to two shop upgrades/visit; attempts the Dragon when production-ready and combat power is at least 130.
- AGGRESSIVE: uses all 10 fights when lawful; heals at 38% HP; stops only below 12% if broke; escapes only at roughly one remaining incoming hit; challenges every second day; aggressively purchases up to four upgrades/visit; attempts as soon as production readiness permits.
- RECKLESS: uses all 10 fights, rarely heals (20%), never voluntarily stops/runs/yields, challenges daily, keeps gold carried, and attempts the Dragon at the earliest lawful readiness point.

All policies train immediately when eligible, use lawful town transactions, and never inspect future RNG. Equipment is bought from the canonical next tier; purchases, healing, challenges and Dragon combat invoke production actions.

## Campaign length

| Policy | N | Complete | Unresolved | Min | P10 | P25 | Median | Mean | P75 | P90 | P95 | Max |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| Passive | 500 | 500 | 0 | 19.00 | 23.00 | 24.00 | 25.00 | 24.54 | 26.00 | 26.00 | 27.00 | 29.00 |
| Casual / Conservative | 500 | 500 | 0 | 18.00 | 22.00 | 23.00 | 25.00 | 24.47 | 26.00 | 27.00 | 27.00 | 29.00 |
| Ordinary | 500 | 500 | 0 | 18.00 | 19.00 | 19.00 | 20.00 | 20.21 | 21.00 | 22.00 | 22.00 | 24.00 |
| Aggressive / Optimizing | 500 | 500 | 0 | 18.00 | 19.00 | 19.00 | 20.00 | 19.81 | 20.00 | 21.00 | 21.00 | 21.00 |
| Reckless | 500 | 500 | 0 | 18.00 | 19.00 | 19.00 | 20.00 | 19.77 | 20.00 | 21.00 | 21.00 | 22.00 |

Player-victory resolution days: n=1486, median 20.00, mean 19.91, P10–P90 19.00–21.00, min–max 18.00–24.00. NPC-victory resolution days: n=1014, median 25.00, mean 24.47, P10–P90 22.00–27.00, min–max 18.00–29.00.

### Duration buckets

| Policy | 1–5 | 6–10 | 11–15 | 16–20 | 21–25 | 26–30 | 31–40 | 41–50 | 51+ |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| Passive | 0 | 0 | 0 | 13 | 336 | 151 | 0 | 0 | 0 |
| Casual / Conservative | 0 | 0 | 0 | 6 | 346 | 148 | 0 | 0 | 0 |
| Ordinary | 0 | 0 | 0 | 352 | 148 | 0 | 0 | 0 | 0 |
| Aggressive / Optimizing | 0 | 0 | 0 | 419 | 81 | 0 | 0 | 0 | 0 |
| Reckless | 0 | 0 | 0 | 433 | 67 | 0 | 0 | 0 | 0 |

## Victory and Dragon attempts

| Policy | Player victory | NPC victory | Unresolved | Player attempts/season | First-attempt win | Attempt win rate | NPC attempts/season | Unique NPC challengers |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| Passive | 0.00% (0/500) | 100.00% (500/500) | 0.00% (0/500) | 0.00 | 0.00% (0/0) | 0.00% (0/0) | 4.18 | 2.75 |
| Casual / Conservative | 0.00% (0/500) | 100.00% (500/500) | 0.00% (0/500) | 0.00 | 0.00% (0/0) | 0.00% (0/0) | 4.11 | 2.67 |
| Ordinary | 97.80% (489/500) | 2.20% (11/500) | 0.00% (0/500) | 1.06 | 91.67% (451/492) | 91.92% (489/532) | 0.37 | 0.35 |
| Aggressive / Optimizing | 100.00% (500/500) | 0.00% (0/500) | 0.00% (0/500) | 1.00 | 100.00% (500/500) | 100.00% (500/500) | 0.18 | 0.18 |
| Reckless | 99.40% (497/500) | 0.60% (3/500) | 0.00% (0/500) | 0.99 | 100.00% (497/497) | 100.00% (497/497) | 0.13 | 0.13 |

Ordinary outcome categories: never attempted 1.60% (8/500); attempted but never won 0.60% (3/500); won first attempt 90.20% (451/500); won after a prior failure/retreat 7.60% (38/500).
Ordinary player attempts: mean 1.06, median 1.00, maximum 3. NPC attempts: mean 0.37, failed before resolution 0.35/season, first-attempt victory 3.47% (6/173), repeated same-NPC attempts 13.

## Player progression

### Level at checkpoints (mean / median; active campaigns only)

| Policy | Day 3 | Day 5 | Day 10 | Day 15 | Day 20 | Day 25 | Conclusion |
|---|---:|---:|---:|---:|---:|---:|---:|
| Passive | 1.00 / 1.00 (n=500) | 1.00 / 1.00 (n=500) | 1.97 / 2.00 (n=500) | 2.00 / 2.00 (n=500) | 2.01 / 2.00 (n=495) | 2.20 / 2.00 (n=272) | 2.20 / 2.00 |
| Casual / Conservative | 2.00 / 2.00 (n=500) | 2.02 / 2.00 (n=500) | 2.84 / 3.00 (n=500) | 4.05 / 4.00 (n=500) | 4.99 / 5.00 (n=498) | 5.71 / 6.00 (n=253) | 5.62 / 6.00 |
| Ordinary | 2.25 / 2.00 (n=500) | 3.04 / 3.00 (n=500) | 4.80 / 5.00 (n=500) | 5.92 / 6.00 (n=500) | 6.88 / 7.00 (n=371) | n/a | 7.00 / 7.00 |
| Aggressive / Optimizing | 2.52 / 3.00 (n=500) | 3.68 / 4.00 (n=500) | 5.56 / 6.00 (n=500) | 6.74 / 7.00 (n=500) | 7.28 / 7.00 (n=339) | n/a | 7.19 / 7.00 |
| Reckless | 2.37 / 2.00 (n=500) | 3.51 / 4.00 (n=500) | 5.47 / 5.00 (n=500) | 6.65 / 7.00 (n=500) | 7.29 / 7.00 (n=327) | n/a | 7.19 / 7.00 |

### Level at checkpoints (all campaigns; completed campaigns carried forward at final level)

| Policy | Day 3 | Day 5 | Day 10 | Day 15 | Day 20 | Day 25 |
|---|---:|---:|---:|---:|---:|---:|
| Passive | 1.00 / 1.00 | 1.00 / 1.00 | 1.97 / 2.00 | 2.00 / 2.00 | 2.01 / 2.00 | 2.16 / 2.00 |
| Casual / Conservative | 2.00 / 2.00 | 2.02 / 2.00 | 2.84 / 3.00 | 4.05 / 4.00 | 4.98 / 5.00 | 5.58 / 6.00 |
| Ordinary | 2.25 / 2.00 | 3.04 / 3.00 | 4.80 / 5.00 | 5.92 / 6.00 | 6.91 / 7.00 | 7.00 / 7.00 |
| Aggressive / Optimizing | 2.52 / 3.00 | 3.68 / 4.00 | 5.56 / 6.00 | 6.74 / 7.00 | 7.19 / 7.00 | 7.19 / 7.00 |
| Reckless | 2.37 / 2.00 | 3.51 / 4.00 | 5.47 / 5.00 | 6.65 / 7.00 | 7.19 / 7.00 | 7.19 / 7.00 |

### Ordinary level-up timing

| Level | Reached | Mean day | Median day | P10–P90 |
|---:|---:|---:|---:|---:|
| 2 | 500 / 500 (100.00%) | 1.07 | 1.00 | 1.00–1.00 |
| 3 | 500 / 500 (100.00%) | 4.78 | 4.00 | 3.00–7.00 |
| 4 | 500 / 500 (100.00%) | 6.70 | 6.00 | 5.00–9.00 |
| 5 | 500 / 500 (100.00%) | 8.81 | 8.00 | 7.00–11.00 |
| 6 | 500 / 500 (100.00%) | 12.55 | 12.00 | 11.00–15.00 |
| 7 | 499 / 500 (99.80%) | 17.69 | 17.00 | 16.00–20.00 |
| 8 | 0 / 500 (0%) | n/a | n/a | n/a |
| 9 | 0 / 500 (0%) | n/a | n/a | n/a |
| 10 | 0 / 500 (0%) | n/a | n/a | n/a |

Ordinary final-level distribution: 7=499, 6=1.

## Physical Training

| Policy | Attempts | Successes | Failures | Failure rate | First-try success | Median attempts/promotion | Max failures |
|---|---:|---:|---:|---:|---:|---:|---:|
| Passive | 602 | 602 | 0 | 0.00% | 100.00% | 1 | 0 |
| Casual / Conservative | 2311 | 2311 | 0 | 0.00% | 100.00% | 1 | 0 |
| Ordinary | 2999 | 2999 | 0 | 0.00% | 100.00% | 1 | 0 |
| Aggressive / Optimizing | 3096 | 3096 | 0 | 0.00% | 100.00% | 1 | 0 |
| Reckless | 3096 | 3096 | 0 | 0.00% | 100.00% | 1 | 0 |

Production Physical Training has no random failure mechanic: a lawful eligible attempt always promotes exactly once. Therefore 2/3/4+ attempt-to-promotion counts and failure-delay days are all zero in this baseline.
Observed per-level training success was 100% for every transition that occurred (Levels 2 through 8); no policy or seed became training-trapped.

## Ordinary combat and daily action use

| Policy | Fights/day | Used all fights | Unused/day | Win rate | Defeat rate | Escape rate | Victory HP% | XP/fight | Gold/fight |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| Passive | 0.95 | 0.42% (51/12268) | 9.01 | 80.41% (9404/11695) | 0.44% (51/11695) | 19.15% (2240/11695) | 82.26 | 8.84 | 4.74 |
| Casual / Conservative | 4.51 | 1.16% (142/12236) | 5.41 | 88.72% (48949/55172) | 0.26% (142/55172) | 11.02% (6081/55172) | 83.46 | 28.89 | 13.59 |
| Ordinary | 8.12 | 3.40% (344/10106) | 1.72 | 95.01% (77926/82018) | 0.42% (344/82018) | 4.57% (3748/82018) | 79.97 | 42.36 | 17.59 |
| Aggressive / Optimizing | 9.23 | 93.83% (9292/9903) | 0.56 | 97.95% (89535/91409) | 0.60% (547/91409) | 1.45% (1327/91409) | 79.12 | 47.78 | 19.02 |
| Reckless | 8.98 | 94.97% (9389/9886) | 0.50 | 98.53% (87453/88756) | 1.47% (1303/88756) | 0.00% (0/88756) | 77.69 | 48.15 | 19.22 |

Ordinary combat defeat by player level: L1 13.09% (25/191), L2 1.93% (243/12590), L3 0.80% (67/8338), L4 0.09% (9/9482), L5 0.00% (0/16839), L6 0.00% (0/23193), L7 0.00% (0/11385).
Ordinary combat defeat by campaign-day bucket: Day 1–5 1.40% (266/18968), Day 6–10 0.34% (73/21514), Day 11–15 0.02% (5/22483), Day 16–20 0.00% (0/18135), Day 21–25 0.00% (0/918).
Ordinary severe post-victory health outcomes (≤25% HP): 0.02% (14/77926).

### Enemy characterization — Ordinary policy

| Enemy | Encounters | Win | Defeat | Escape | Avg damage | Avg XP | Avg gold | Avg day | Avg level |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| Giant Rat | 2991 | 100.00% | 0.00% | 0.00% | 0.84 | 6.00 | 3.00 | 2.38 | 1.62 |
| Cave Bat | 2620 | 99.89% | 0.00% | 0.11% | 1.42 | 6.99 | 3.71 | 2.61 | 1.71 |
| Forest Spider | 789 | 95.69% | 0.00% | 4.31% | 3.93 | 8.61 | 4.31 | 1.05 | 1.00 |
| Goblin | 973 | 69.48% | 2.57% | 27.95% | 7.18 | 8.34 | 4.17 | 1.02 | 1.00 |
| Wild Boar | 5086 | 84.19% | 1.08% | 14.73% | 6.62 | 13.47 | 7.19 | 3.90 | 2.27 |
| Highwayman | 5185 | 58.34% | 3.63% | 38.03% | 10.07 | 10.50 | 6.44 | 3.90 | 2.26 |
| Skeleton | 4594 | 97.61% | 0.17% | 2.22% | 4.67 | 23.43 | 15.64 | 6.60 | 3.37 |
| Orc Scout | 4385 | 86.48% | 1.35% | 12.18% | 7.35 | 24.21 | 16.02 | 6.52 | 3.35 |
| Dire Wolf | 5985 | 99.88% | 0.02% | 0.10% | 2.53 | 35.96 | 17.00 | 9.14 | 4.47 |
| Bandit Captain | 5976 | 98.59% | 0.13% | 1.27% | 3.68 | 39.44 | 18.73 | 9.11 | 4.46 |
| Ogre Brute | 9391 | 100.00% | 0.00% | 0.00% | 1.33 | 48.00 | 19.56 | 12.51 | 5.40 |
| Grave Wight | 9519 | 100.00% | 0.00% | 0.00% | 1.62 | 52.00 | 22.48 | 12.55 | 5.41 |
| Bridge Troll | 9324 | 100.00% | 0.00% | 0.00% | 1.14 | 62.00 | 22.02 | 15.67 | 6.16 |
| Dark Knight | 9195 | 100.00% | 0.00% | 0.00% | 1.29 | 68.00 | 25.04 | 15.65 | 6.16 |
| Marsh Wyvern | 2970 | 100.00% | 0.00% | 0.00% | 1.06 | 78.00 | 25.01 | 18.22 | 7.00 |
| Thorn Warlock | 3035 | 100.00% | 0.00% | 0.00% | 1.32 | 84.00 | 27.41 | 18.23 | 7.00 |

## Economy and equipment

| Policy | Combat gold/day | Healing/season | Equipment/season | Defeat loss/season | Median carried | Median banked | Median total wealth | P10–P90 wealth |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| Passive | 4.52 | 47.81 | 55.93 | 0.41 | 21.00 | 8.00 | 27.00 | 8.00–65.00 |
| Casual / Conservative | 61.27 | 226.90 | 1154.33 | 1.24 | 50.00 | 127.00 | 177.00 | 36.00–368.00 |
| Ordinary | 142.74 | 239.90 | 2396.72 | 5.27 | 75.00 | 305.00 | 380.00 | 53.00–610.00 |
| Aggressive / Optimizing | 175.56 | 166.31 | 3028.24 | 11.67 | 100.00 | 315.00 | 415.00 | 108.00–678.00 |
| Reckless | 172.54 | 108.11 | 3071.82 | 34.50 | 447.00 | 0.00 | 447.00 | 146.00–752.00 |

Ordinary final weapon distribution: Battle Axe=379, Steel Longsword=121.
Ordinary final armor distribution: Knight Plate=421, Mithril Mail=79.
Ordinary Dragon-attempt equipment: weapons Battle Axe=408, Steel Longsword=124; armor Knight Plate=454, Mithril Mail=78.
Ordinary equipment acquisition: Iron Sword 100.00% @ median Day 1.00; Steel Blade 100.00% @ median Day 6.00; Knight's Sword 100.00% @ median Day 7.00; Broad Sword 100.00% @ median Day 9.00; Steel Longsword 100.00% @ median Day 13.00; Battle Axe 75.80% @ median Day 17.00; Studded Leather 100.00% @ median Day 5.00; Chain Mail 100.00% @ median Day 6.00; Plate Armor 100.00% @ median Day 8.00; Scale Armor 100.00% @ median Day 11.00; Knight Plate 100.00% @ median Day 15.00; Mithril Mail 15.80% @ median Day 19.00.
Ordinary total-wealth checkpoints (active campaigns, mean / median): Day 3 18.42/16.00 (n=500), Day 5 38.73/27.00 (n=500), Day 10 132.16/113.00 (n=500), Day 15 284.66/281.00 (n=500), Day 20 325.02/324.00 (n=371), Day 25 n/a.

## NPC progression, rankings and churn

| Policy | NPC final level min/median/mean/max | Final player rank median | #1 finish | Top 3 | Top 5 | Bottom half | Rank moves/season | Leaders/season | Leadership changes | Player ever #1 |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| Passive | 1.00/4.00/4.22/6.00 | 17.00 | 0.00% (0/500) | 0.00% (0/500) | 0.00% (0/500) | 100.00% (500/500) | 215.25 | 4.42 | 5.51 | 0.00% (0/500) |
| Casual / Conservative | 1.00/4.00/4.22/6.00 | 1.00 | 88.20% (441/500) | 93.40% (467/500) | 96.40% (482/500) | 1.40% (7/500) | 215.05 | 3.90 | 4.36 | 95.20% (476/500) |
| Ordinary | 1.00/4.00/3.52/6.00 | 1.00 | 100.00% (500/500) | 100.00% (500/500) | 100.00% (500/500) | 0.00% (0/500) | 173.35 | 2.25 | 1.47 | 100.00% (500/500) |
| Aggressive / Optimizing | 1.00/3.00/3.46/5.00 | 1.00 | 100.00% (500/500) | 100.00% (500/500) | 100.00% (500/500) | 0.00% (0/500) | 168.23 | 2.05 | 1.06 | 100.00% (500/500) |
| Reckless | 1.00/3.00/3.46/5.00 | 1.00 | 100.00% (500/500) | 100.00% (500/500) | 100.00% (500/500) | 0.00% (0/500) | 170.89 | 2.23 | 1.28 | 100.00% (500/500) |

Ordinary NPC final-level distribution: 3=3517, 4=3492, 5=552, 2=410, 1=28, 6=1.
Ordinary NPC level checkpoints (min / median / mean / max): Day 3 1.00/1.00/1.29/2.00, Day 5 1.00/2.00/1.62/3.00, Day 10 1.00/2.00/2.21/4.00, Day 15 1.00/3.00/2.96/5.00, Day 20 1.00/4.00/3.54/5.00, Day 25 n/a.
Ordinary player rank checkpoints (active campaigns, mean / median): Day 3 1.11/1.00 (n=500), Day 5 1.18/1.00 (n=500), Day 10 1.07/1.00 (n=500), Day 15 1.00/1.00 (n=500), Day 20 1.00/1.00 (n=371), Day 25 n/a.
Ordinary NPC final gold: median 94.00, mean 104.90, P10–P90 31.00–193.00. Final weapon tiers: 2=3732, 3=2069, 1=1550, 0=636, 4=13; armor tiers: 2=4487, 1=1689, 3=1135, 0=682, 4=7.
One resident led ≥80% of measured days in 97.00% (485/500) of Ordinary seasons.

## Dragon escalation and readiness

| Policy | Signs median | Fear median | Crisis median | Window median | First player ready | First NPC ready | Candidates at window | Candidates at resolution |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| Passive | 4.00 (n=500) | 8.00 (n=500) | 14.00 (n=500) | 19.00 (n=500) | never | 20.00 (n=500) | 1.00 | 2.00 |
| Casual / Conservative | 4.00 (n=500) | 8.00 (n=500) | 14.00 (n=500) | 19.00 (n=500) | never | 20.00 (n=500) | 1.00 | 2.00 |
| Ordinary | 4.00 (n=500) | 8.00 (n=500) | 14.00 (n=500) | 19.00 (n=500) | 19.00 (n=492) | 19.00 (n=222) | 1.00 | 1.00 |
| Aggressive / Optimizing | 4.00 (n=500) | 8.00 (n=500) | 14.00 (n=500) | 19.00 (n=500) | 19.00 (n=500) | 19.00 (n=161) | 1.00 | 1.00 |
| Reckless | 4.00 (n=500) | 8.00 (n=500) | 14.00 (n=500) | 19.00 (n=500) | 19.00 (n=497) | 19.00 (n=125) | 1.00 | 1.00 |

### Ordinary Dragon-stage timing distributions

| Stage | Reached | Min | P10 | Median | Mean | P90 | Max |
|---|---:|---:|---:|---:|---:|---:|---:|
| SIGNS | 500/500 | 3.00 | 4.00 | 4.00 | 4.01 | 4.00 | 5.00 |
| FEAR | 500/500 | 7.00 | 8.00 | 8.00 | 8.41 | 9.00 | 10.00 |
| CRISIS | 500/500 | 13.00 | 13.00 | 14.00 | 13.98 | 15.00 | 16.00 |
| CHALLENGE_WINDOW | 500/500 | 17.00 | 18.00 | 19.00 | 18.79 | 20.00 | 21.00 |
| RESOLVED | 500/500 | 18.00 | 19.00 | 20.00 | 20.21 | 22.00 | 24.00 |

Ordinary readiness at the first player Dragon attempt: n=492; level median 7.00 (P10–P90 7.00–7.00), weapon tier median 6.00, armor tier median 5.00.

## Society, Chronicle and story continuity

| Policy | Chronicle/day mean | Median | P90 | Max | Zero-event days | >7-event days | NPCs spotlighted | Top NPC share | Never spotlighted | Relationship changes | Rivalries | Callbacks |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| Passive | 6.99 | 7.00 | 7.00 | 7.00 | 0.00% (0/12268) | 0.00% (0/12268) | 16.00 | 9.50% | 0.00 | 0.00 | 0.00 | 0.25 |
| Casual / Conservative | 7.00 | 7.00 | 7.00 | 7.00 | 0.00% (0/12236) | 0.00% (0/12236) | 16.00 | 9.51% | 0.00 | 4.53 | 2.53 | 0.40 |
| Ordinary | 6.71 | 7.00 | 7.00 | 7.00 | 0.00% (0/10106) | 0.00% (0/10106) | 16.00 | 9.76% | 0.00 | 4.95 | 3.76 | 0.57 |
| Aggressive / Optimizing | 6.69 | 7.00 | 7.00 | 7.00 | 0.00% (0/9903) | 0.00% (0/9903) | 16.00 | 9.65% | 0.00 | 6.82 | 4.99 | 1.77 |
| Reckless | 6.70 | 7.00 | 7.00 | 7.00 | 0.00% (0/9886) | 0.00% (0/9886) | 16.00 | 9.64% | 0.00 | 13.04 | 6.88 | 5.96 |

Ordinary Chronicle event families: LEVEL_GAIN=41.63/season, DEFEAT=40.96/season, RECOVERY=6.97/season, EQUIPMENT=24.94/season, GREAT_SUCCESS=1.16/season, RANK_CHANGE=0.43/season, CHALLENGE=11.56/season, DRAGON_INCIDENT=5.71/season, DRAGON_INTENT=0.77/season, DRAGON_ATTEMPT=0.42/season, DRAGON_VICTORY=1.00/season.
Ordinary Dragon incidents created: 7.85/season. Structured callbacks: 0.57/season. The current model does not maintain a separate generic incident-expiration ledger, so expiration-without-follow-up is not measurable beyond retained structured callbacks.

## Policy comparison

| Policy | Median days | Player win | NPC win | Median level | Median rank | Median attempts | Combat defeat | Training failure | Median wealth | Fights/day |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| Passive | 25.00 | 0.00% (0/500) | 100.00% (500/500) | 2.00 | 17.00 | 0.00 | 0.44% (51/11695) | 0.00% (0/602) | 27.00 | 0.95 |
| Casual / Conservative | 25.00 | 0.00% (0/500) | 100.00% (500/500) | 6.00 | 1.00 | 0.00 | 0.26% (142/55172) | 0.00% (0/2311) | 177.00 | 4.51 |
| Ordinary | 20.00 | 97.80% (489/500) | 2.20% (11/500) | 7.00 | 1.00 | 1.00 | 0.42% (344/82018) | 0.00% (0/2999) | 380.00 | 8.12 |
| Aggressive / Optimizing | 20.00 | 100.00% (500/500) | 0.00% (0/500) | 7.00 | 1.00 | 1.00 | 0.60% (547/91409) | 0.00% (0/3096) | 415.00 | 9.23 |
| Reckless | 20.00 | 99.40% (497/500) | 0.60% (3/500) | 7.00 | 1.00 | 1.00 | 1.47% (1303/88756) | 0.00% (0/3096) | 447.00 | 8.98 |

## Confidence and sample stability

- Overall completion: 100.00% (2500/2500), approximate 95% Wilson interval 99.85%–100.00%.
- Ordinary player victory: 97.80% (489/500), approximate 95% Wilson interval 96.10%–98.77%.
- Ordinary NPC victory: 2.20% (11/500), approximate 95% Wilson interval 1.23%–3.90%.
- Ordinary combat defeat: 0.42% (344/82018), approximate 95% Wilson interval 0.38%–0.47%.
- Ordinary training success: 100.00% (2999/2999), approximate 95% Wilson interval 99.87%–100.00%.

## Determinism and interruption qualification

- 10 representative policy/seed pairs were rerun from fresh state and produced identical full `CampaignResult` values.
- 5 representative campaigns (one per policy) were compared continuously versus save/codec/reload checkpoints on Days 3, 7, 11, 17 and 23; outcomes and traces were identical.
- No gameplay RNG is consumed by reporting or narrative formatting.

## Outliers

- Shortest campaign: RECKLESS seed 5000026, Day 18, Level 7, rank 1, wealth 160
- Longest campaign: PASSIVE seed 1000021, Day 29, Level 2, rank 17, wealth 31
- Earliest player victory: RECKLESS seed 5000026, Day 18, Level 7, rank 1, wealth 160
- Latest player victory: ORDINARY seed 3000051, Day 24, Level 7, rank 1, wealth 593
- Strongest final player: AGGRESSIVE seed 4000004, Day 20, Level 8, rank 1, wealth 591
- Weakest player who won: ORDINARY seed 3000001, Day 21, Level 7, rank 1, wealth 520
- Most ordinary-combat defeats: RECKLESS seed 5000273, Day 20, Level 7, rank 1, wealth 22
- Most training failures: PASSIVE seed 1000001, Day 25, Level 3, rank 16, wealth 18
- Richest player: RECKLESS seed 5000346, Day 21, Level 8, rank 1, wealth 1088
- Poorest player at conclusion: PASSIVE seed 1000131, Day 21, Level 2, rank 17, wealth 0
- Most NPC Dragon attempts: CASUAL seed 2000096, Day 29, Level 7, rank 1, wealth 48
- Most dominant NPC-led campaign: PASSIVE seed 1000012, Day 21, Level 2, rank 17, wealth 19
- Highest Chronicle density: PASSIVE seed 1000001, Day 25, Level 3, rank 16, wealth 18
- Lowest Chronicle density: ORDINARY seed 3000497, Day 20, Level 7, rank 1, wealth 27

Unresolved seeds: none.

## Objective defects and repairs

No objective correctness defect was exposed by the qualified cohort. No production rule or balance constant was changed. Harness implementation issues found during development were confined to test tooling and do not constitute gameplay repairs.

## Principal Decision Table — Ordinary policy

| Metric | Ordinary result |
|---|---:|
| Median days to season resolution | 20.00 |
| 10th–90th percentile days | 19.00–22.00 |
| Player Dragon victory rate | 97.80% (489/500) |
| NPC Dragon victory rate | 2.20% (11/500) |
| Unresolved campaign rate | 0.00% (0/500) |
| Median player final level | 7.00 |
| 10th–90th player final level | 7.00–7.00 |
| Median final player rank | 1.00 |
| Player #1 finish rate | 100.00% (500/500) |
| Combat defeat rate | 0.42% (344/82018) |
| Training first-attempt success | 100.00% (2999/2999) |
| Median training attempts/level | 1 (when eligible) |
| Median final wealth | 380.00 gold |
| Median first player Dragon-ready day | 19.00 (n=492) |
| Median first NPC Dragon-ready day | 19.00 (n=222) |
| Median Dragon challenge-window day | 19.00 (n=500) |
| Median Chronicle entries/day | 7.00 |
| Median credible candidates at window | 1.00 |

## Explicit answers to the 20 Principal questions

1. Ordinary season length: median 20.00 days; mean 20.21.
2. Variation: P10–P90 19.00–22.00, min–max 18.00–24.00, SD 1.12 days.
3. Ordinary player Dragon victory: 97.80% (489/500).
4. NPC kills first: 2.20% (11/500).
5. Unresolved: 0.00% (0/500) Ordinary; 0.00% (0/2500) overall.
6. Ordinary final level: median 7.00, P10–P90 7.00–7.00.
7. Major level timing: L2=1.00 (n=500), L3=4.00 (n=500), L4=6.00 (n=500), L5=8.00 (n=500), L6=12.00 (n=500), L7=17.00 (n=499), L8=never, L9=never, L10=never (median day among those reaching the level).
8. Training failure: 0.00% (0/2999); production training is deterministic.
9. Ordinary fight loss: 0.42% (344/82018).
10. Player Dragon attempts: median 1.00, mean 1.06, max 3.
11. Ordinary final wealth: median 380.00, P10–P90 53.00–610.00 gold.
12. End equipment: weapons Battle Axe=379, Steel Longsword=121; armor Knight Plate=421, Mithril Mail=79.
13. NPC competitiveness: NPC victory 2.20% (11/500), player median rank 1.00, player #1 finish 100.00% (500/500).
14. Credible candidates: median 1.00 at window and 1.00 at resolution.
15. Serious pressure: Fear median Day 8.00 (n=500); Crisis median Day 14.00 (n=500).
16. Chronicle density: mean 6.71, median 7.00, P90 7.00 entries/day.
17. Narrative prominence: mean 16.00 unique NPCs/season; top NPC mean share 9.76%.
18. Policy effect: see Policy Comparison; player victory ranges 0.00%–100.00%, median final level 2.0–7.0.
19. Statistical outliers: see Outliers and `outliers.json`; the longest campaign is 29 days and maximum combat defeats are 8.
20. Objective defects/repairs: none discovered; no production repairs or tuning performed.

## Machine-readable evidence

- `campaigns.csv`: one record per season.
- `days.jsonl`: one record per simulated game day.
- `monster_metrics.csv`: policy/enemy aggregates.
- `summary.json`: primary machine-readable distributions.
- `outliers.json`: reproducible diagnostic seeds.

## Recommendation

`LGD011_BALANCE_CHARACTERIZED_PRINCIPAL_REVIEW_REQUIRED`

The Principal should review these distributions before any balance changes or LGD-012 work is authorized.
