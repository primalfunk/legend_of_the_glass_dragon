# LGD-005 Completion Report

Date: 2026-08-28

## Result

LGD-005 is complete, including automated verification and qualification on the connected physical TCL NXTPAPER tablet. The Village now has a shared player/NPC equipment economy, functional weapon and armor shops, protected banked wealth, an expanded Inn, persistent adventurer duels, rivalry/history, NPC-vs-NPC challenge activity, and structured challenge news.

The implementation remains native Kotlin and Jetpack Compose, local-only, touch-driven, and visually ANSI/BBS-first. No inventory grid, item artwork, network service, multiplayer stack, cloud save, modern store presentation, or unrelated feature system was added.

## Canonical equipment economy

Player and NPC records now store stable canonical equipment IDs. Attack and Defense are calculated as base progression plus the catalog contribution exactly once. Both shops use one-way upgrades with the currently equipped item's fixed resale value applied as trade-in credit.

Final weapon ladder:

| Tier | Weapon | Attack | Price | Resale | Minimum level |
|---:|---|---:|---:|---:|---:|
| 0 | Rusty Sword | +0 | 0 | 0 | 1 |
| 1 | Iron Sword | +1 | 35 | 15 | 1 |
| 2 | Steel Blade | +3 | 80 | 35 | 1 |
| 3 | Knight's Sword | +5 | 160 | 70 | 2 |
| 4 | Broad Sword | +7 | 300 | 135 | 3 |
| 5 | Steel Longsword | +9 | 550 | 250 | 4 |
| 6 | Battle Axe | +12 | 900 | 410 | 6 |
| 7 | Runed Blade | +15 | 1,500 | 700 | 8 |

Final armor ladder:

| Tier | Armor | Defense | Price | Resale | Minimum level |
|---:|---|---:|---:|---:|---:|
| 0 | Worn Leather | +0 | 0 | 0 | 1 |
| 1 | Studded Leather | +1 | 35 | 15 | 1 |
| 2 | Chain Mail | +3 | 85 | 35 | 1 |
| 3 | Plate Armor | +5 | 180 | 80 | 2 |
| 4 | Scale Armor | +7 | 325 | 145 | 3 |
| 5 | Knight Plate | +9 | 575 | 260 | 4 |
| 6 | Mithril Mail | +12 | 950 | 430 | 6 |
| 7 | Dragon Scale | +15 | 1,600 | 750 | 8 |

NPC equipment improvement now selects an affordable canonical upgrade, applies the same real catalog price/trade-in relationship, and deducts its cost once. Profiles expose the resulting weapon, armor, challenge record, status, and readable relationship label.

## Bank and transaction safety

- Persistent carried and banked balances are separate.
- Touch denominations are 25, 50, and 100 gold, plus Half and All.
- Deposit, withdraw, Deposit All, and Withdraw All conserve total wealth.
- Source-balance checks and integer-overflow checks reject unlawful transactions.
- Forest defeat retains the existing 25% carried-gold loss and never touches banked gold.
- Weapon/armor purchases and bank actions require the expected save revision and reject stale repeated submissions.
- Active combat or an active duel blocks unrelated money operations.
- Fixed-height terminal message space prevents acknowledgement text from moving commands underneath a rapid second tap. This usability correction was found and verified during the physical rapid-tap pass.

## Adventurer challenges

The Dragon's Rest now exposes daily visitors, Talk, profile inspection, and Challenge. Inn presence remains deterministic for a given realm day and shows four community members. Recovering visitors may be seen but cannot be challenged.

Final challenge constants and behavior:

- One accepted player challenge per realm day.
- Acceptance percentages: Aggressive 100%, Cautious 65%, Ambitious 90%, Greedy 80%, Social 85%, Erratic 75%.
- Player attack and retaliation use the established combat formula, minimum damage, persisted random state, and the NPC's actual base stats and catalog equipment.
- Victory XP: `5 + 3 * opponent level`.
- Victory stake: up to `8 + 4 * opponent level`, limited by the NPC's available gold.
- Player-loss stake: up to `8 + 2 * opponent level`, limited by carried gold.
- Duel loss restores the player to at least half maximum HP and deliberately leaves the Forest day available.
- Yield grants no XP or gold and records a challenge loss.
- Rivalry gains: +3 player victory, +2 player loss, +1 Yield; rivalry is bounded at 100.
- Refusal does not consume the daily opportunity.
- A completed result rejects further attacks and can grant rewards only once.

The player and each involved NPC retain aggregate wins/losses, head-to-head player history, rivalry, familiarity, and last challenge day. Player and NPC duel results create structured `CHALLENGE` events. Realm News and gossip format those real results without exposing private bank balances.

World advancement may resolve at most one bounded NPC-vs-NPC challenge event per realm day. It does not run hidden turn-by-turn combat. NPC equipment purchases and duel records remain part of the same deterministic, in-memory, once-per-day world transition.

## Save compatibility and interruption policy

Save schema version 5 stores:

- player base stats, catalog equipment IDs, banked gold, and challenge record;
- daily challenge allowance;
- NPC base stats, catalog equipment IDs, rivalry and remembered challenge history;
- the complete active/resolved challenge record and bounded combat messages;
- challenge WorldEvents alongside the existing community/news state.

Lawful schema versions 1 through 4 are accepted deliberately. Schema-v4 total Attack/Defense values are converted back to base values by subtracting the migrated catalog contribution, preventing equipment from being counted twice. Legacy display equipment names map to stable IDs; new bank/history values receive lawful defaults. Unsupported or corrupt saves retain the explicit recovery path.

An accepted challenge is saved immediately. Every turn, Yield, loss, victory, and departure is also saved. Continue resumes the exact stored combat or duel. The revision guard makes money, reward, challenge-limit, and End Day operations atomic from the player's perspective.

## Automated verification

Final command:

~~~text
gradlew test lint assembleDebug connectedDebugAndroidTest
~~~

Result: `BUILD SUCCESSFUL`

- JVM unit suite: 85 passed, 0 failed, 0 errors, 0 skipped across 17 suites.
- Physical connected instrumentation: 1 passed on Android-reported device `9491G - 15`.
- Android lint: `No issues found.`
- Merged application manifest contains no `android.permission.INTERNET`.
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk` (12,328,875 bytes).
- APK SHA-256: `9EE7E011FC5234AD2451475586B062B60BD129EB61BE8888AE937A8703BCAD64`.

New/extended tests cover catalog lawfulness, base-plus-equipment totals, price/trade-in arithmetic, stale-action rejection, all bank operations, insufficient balances, overflow rejection, bank protection from Forest defeat, schema-v5 round trip, explicit schema-v4 stat migration, challenge eligibility/refusal, daily limit, victory, loss, Yield, exactly-once rewards, rivalry/history, private-bank gossip protection, bounded NPC duels, canonical NPC purchases, navigation, and active-challenge blocking.

The deterministic 1,000-day soak reaches Day 1001 while validating every daily state. Stable identities persist, all equipment IDs remain canonical, gold and records stay nonnegative, rankings contain every eligible character exactly once, recovery terminates, retained events remain bounded to 84 / 14 days, NPC duel wins equal NPC duel losses, challenge news occurs, equipment upgrades occur, and every day transition terminates lawfully. The existing 100-day diversity test continues to show non-uniform experience/levels, defeats, and upgrades.

## Physical NXTPAPER qualification

Qualification was completed on the connected TCL NXTPAPER tablet, serial `BC9424B4E3C5002`, Android model `9491G`, at 1600 x 2400 portrait resolution.

Verified on device:

- Cold launch in approximately 1.10-1.19 seconds with no crash or ANR.
- A live schema-v4 Arcturus save migrated to schema 5 without losing its player, Day 7 progression, equipment totals, community, or local identity.
- The ten-row Town menu remained readable, fully tappable, and unclipped.
- Weapon Shop displayed all eight canonical tiers. A real Iron Sword purchase reduced carried gold from 36 to 1 and changed only one save revision despite rapid repeated tapping.
- Armor Shop displayed all eight tiers, base-plus-armor Defense, fixed trade-in value, explicit `<NEED gold>` and `<LEVEL n>` refusal states, and clean return navigation.
- Bank Deposit All and Withdraw All conserved wealth and persisted. A rapid post-fix double tap changed exactly one revision. One banked gold remained protected through a live duel loss.
- Expanded Inn, four deterministic visitors, active/recovering eligibility, NPC equipment, and challenge selection were readable and touch comfortable.
- A rapid double Attack produced only one duel exchange/revision.
- Yield against Rook consumed exactly one challenge, recorded the loss and rivalry, saved, and resumed at the exact yielded result after force-stop/relaunch/Continue.
- Android Back from the completed duel cleared only transient challenge state and returned coherently to the Inn.
- A controlled duel loss to Rook restored Arcturus to 12/25 HP, lost zero of zero carried gold, retained the protected banked gold, and left all ten Forest fights available.
- A real duel victory over Garrick ended at 6/25 HP and awarded 11 XP plus 16 gold exactly once. Garrick's persistent Iron Sword, Studded Leather, Level 2 HP/stats, and gold supplied the encounter and reward.
- The daily limit blocked another challenge until End Day.
- Rapid End Day submission advanced Day 7 to Day 8 exactly once and reset one challenge.
- Morning reports and Realm News showed player Yield/loss, player victory, NPC-vs-NPC challenges, NPC equipment purchases, recoveries, defeats, and rank changes. Day 9 news explicitly displayed `Arcturus defeated Garrick in a challenge at the Inn!`.
- App background/resume, force-stop/Continue, scrollable Realm News, and Android Back remained coherent.
- No modern card, graphical inventory, currency icon, health bar, profile art, or matchup UI leaked into the terminal presentation.

Evidence images are retained in `screenshots/lgd-005-device/`, including `armor-shop.png`, `inn-after-challenge.png`, `duel-loss.png`, `duel-victory.png`, and `realm-news.png`.

The connected Android test runner uninstalled its target package during normal test cleanup, which also cleared the active pre-release qualification adventure. The verified APK was immediately reinstalled, cold-launched successfully, and left on the tablet with a new lawful local Arcturus save at Day 1 in Town: Level 1, 20/20 HP, 25 carried gold, 0 banked gold, Rusty Sword, Worn Leather, ten Forest fights, and one challenge.

The TCL two-hour wellbeing overlay appeared once near the end of qualification. Dismissing it immediately returned to the same LGD activity; it was a tablet-level interruption, not an application hang.

## Materially changed files

Core models/rules/persistence:

- `game/EquipmentCatalog.kt` (new)
- `game/EconomyRules.kt` (new)
- `game/ChallengeRules.kt` (new)
- `game/GameState.kt`
- `game/GameRules.kt`
- `game/CommunityState.kt`
- `game/CommunityFactory.kt`
- `game/CommunityRules.kt`
- `game/NewGameFactory.kt`
- `game/GameStateValidator.kt`
- `game/WorldAdvancer.kt`
- `data/GameStateCodec.kt`
- `simulation/CommunitySimulator.kt`

UI/navigation:

- `ui/screens/EconomyScreens.kt` (new)
- `ui/screens/ChallengeScreens.kt` (new)
- `ui/screens/CommunityScreens.kt`
- `ui/screens/GameplayScreens.kt`
- `ui/components/TerminalComponents.kt`
- `ui/GlassDragonApp.kt`
- `navigation/AppNavigation.kt`

Tests/docs:

- `game/EconomyEngineTest.kt` (new)
- `game/ChallengeEngineTest.kt` (new)
- `data/GameSaveRepositoryTest.kt`
- `simulation/CommunitySimulatorTest.kt`
- `simulation/CommunitySoakTest.kt`
- `navigation/AppNavigationTest.kt`
- this work order, report, README index, and physical screenshots.

## Known balance concern

The equipment ladder is intentionally an early economy pass. Its first upgrade is reachable quickly and later tiers span multiple days, but LGD-006 and later campaign play may expose price/reward adjustments. Duel rewards are deliberately modest and daily-limited; no exploit, money duplication, negative balance, long-run overflow, progression saturation, or NPC economy instability was observed in the current tests or device pass.

## Terminal state

~~~text
LGD005_PLAYER_EQUIPMENT_ECONOMY_COMPLETE
LGD005_WEAPON_SHOP_FUNCTIONAL
LGD005_ARMOR_SHOP_FUNCTIONAL
LGD005_BANK_FUNCTIONAL
LGD005_INN_EXPANDED
LGD005_ADVENTURER_CHALLENGES_FUNCTIONAL
LGD005_RIVALRY_V1_FUNCTIONAL
LGD005_COMMUNITY_ECONOMY_COHERENT
LGD006_WORLD_EVENTS_AND_SOCIAL_DEPTH_READY
~~~

Next planned work: LGD-006 - Special Forest Events, Richer Inn Encounters, Relationships, and Persistent Community Stories.
