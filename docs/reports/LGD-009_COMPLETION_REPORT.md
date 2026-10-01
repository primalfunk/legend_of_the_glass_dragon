# LGD-009 Completion Report

Date: 2026-08-28

## Disposition

`LGD009_COMPLETE`

Legend of the Glass Dragon now runs as one finite, deterministic season. Dragon
pressure escalates through visible stages; residents react, prepare, declare
intent, attempt the Dragon, fail and recover, or legally become the victor. A
ready player may fight the Dragon through persisted turn-based combat. Resolution
locks ordinary world advancement and preserves the complete campaign for a
victory notice, actual-fact Final Glass Chronicle, and Season Record.

This workspace is not a Git worktree, so no implementation commit/current HEAD
exists (`git status` reports `not a git repository`).

## Dragon Season state model

Save schema version 7 adds `DragonSeasonState` and optional persisted
`DragonCombatState`.

Season state includes:

- `DragonStage`: `WHISPERS`, `SIGNS`, `FEAR`, `CRISIS`,
  `CHALLENGE_WINDOW`, `RESOLVED`;
- pressure and stage-start day;
- bounded structured incident history;
- declared challenger IDs;
- bounded structured attempt history;
- player's last attempt day;
- authoritative victor and resolution day;
- persisted conclusion phase: victory notice, Final Chronicle, Season Record,
  or complete.

`DragonIncidentRecord` and `DragonAttemptRecord` retain factual types, day,
resident, power/outcome, and stable identity. New event types feed the shared
LGD-008 narrative system: `DRAGON_INCIDENT`, `DRAGON_INTENT`, `DRAGON_ATTEMPT`,
`DRAGON_VICTORY`, and `DRAGON_SUPPORT`.

## Final escalation rules

Centralized values in `DragonSeasonBalance`:

| Rule | Value |
| --- | ---: |
| Signs pressure | 18 |
| Fear pressure | 40 |
| Crisis pressure | 68 |
| Challenge Window pressure | 92 |
| Maximum pressure | 120 |
| Fallback declaration day | 21 |
| Fallback eligible-attempt day | 23 |
| Guaranteed qualified-candidate resolution boundary | Day 32 |
| Dragon-attempt recovery | 2 days |
| Retained incidents | 60 |
| Retained attempts | 24 |

Pressure grows from game-day passage, current stage, community strength, and
controlled deterministic variation. Stages are monotonic. Incident generation
is bounded and stage-gated: distant glass chimes and fragments precede missing
travelers/burned woodland, wounded scouts and shattered homesteads, open
sightings, and the public call for a champion. Incidents do not occur every day
merely to fill space.

The same starting state, world seed, and player actions produce the same stage
history, incidents, candidates, attempts, outcome, and narrative selection.
Presentation never consumes gameplay RNG.

## Champion-readiness model

Combat power is derived from real state:

```text
level × 4
+ attack × 2
+ defense × 2
+ maximum HP / 4
+ (weapon tier + armor tier) × 2
```

Player readiness requires:

- Challenge Window;
- Level 7 or higher;
- weapon tier 4 or higher;
- armor tier 4 or higher;
- full health and no same-day defeat;
- combat power at least 125.

NPC readiness requires the same type of authoritative state with balanced NPC
minimums: Level 5, weapon/armor tier 2, active status, and power at least 95.
Potential candidates begin at power 85. The player sees direct reasons—more
training, weapon, armor, health, or strength—rather than a hidden coefficient.

The player may compete through progression/rank/challenges, support a declared
candidate with an atomic 50 carried-gold action that transfers real wealth and
raises familiarity, obstruct through the existing duel/rivalry economy, or
ignore the crisis. There is no artificial `SABOTAGE HERO` command.

## NPC Dragon decisions

Disposition derives from existing personality: cautious, obsessed, ambitious,
opportunistic, supportive, or fearful. Candidate ordering considers actual
power, state, personality, and declaration history rather than selecting the
player or highest level unconditionally.

During End Day, likely candidates may declare intent, use existing canonical
equipment and real gold to prepare, and schedule an attempt only after all actual
readiness requirements pass. The fallback days increase decision pressure; they
do not make an unqualified NPC legal. Attempts use challenger power versus
Dragon power with the saved deterministic random stream. Hidden NPC battles are
outcome-oriented, not simulated through UI combat turns.

NPC failure removes 20% of carried gold, changes status to `RECOVERING` for two
days, and records one major event. The resident remains the same persistent
person and cannot retry immediately. Victory sets one authoritative victor and
resolves the season. Validation disallows multiple victory records or a victor
in an unresolved season.

## Player qualification and Dragon combat

The Dragon Watch is always reachable from Town and changes from a status screen
to a clear confrontation route when the window opens. It lists current stage,
plain readiness guidance, declared champions, support opportunities, and the
enabled/disabled confrontation command.

`DragonCombatEngine` reuses the existing atomic action/revision model. Combat is
persisted and resumable. The Glass Dragon has:

- 100 maximum HP;
- Attack 20;
- intact crystal defense 15;
- fractured defense 11 after half HP;
- refracted breath every third retaliation for +4 damage.

The player attacks first. A killing hit prevents retaliation. Recent logs remain
bounded. The ANSI combat screen separates player/Dragon state and uses the
LGD-007 danger/crystal language without health bars or a new combat framework.

A retreat records and consumes the day's attempt. Defeat records exactly once,
loses 25% carried gold under the established defeat rule, keeps banked gold, XP,
level, and equipment, restores half HP, closes Forest activity for the day, and
requires End Day before another attempt. Victory stores the player as victor,
emits a current-day Chronicle record immediately, and enters conclusion.

## Season resolution

Both NPC and player victories use the same `RESOLVED` authority. Resolved saves:

- reject further ordinary End Day simulation;
- preserve community, Chronicle, economy, relationships, attempts, and player;
- resume the exact persisted conclusion phase;
- remain available through Continue;
- require explicit New Game replacement instead of silent deletion.

The conclusion flow is:

```text
THE GLASS DRAGON HAS FALLEN
→ THE FINAL GLASS CHRONICLE
→ SEASON RECORD
→ existing Main Menu boundary
```

`SeasonNarrator` derives the duration, actual failed attempts, actual public
leader/rival where warranted, actual victor, and resolution day. The Season
Record shows days, victor, player level/rank, carried/banked gold, equipment,
challenge record, Dragon attempts, and a nonjudgmental summary when an NPC won.

## Chronicle, Dawn, and gossip integration

Dragon simulation emits structured events into LGD-008's event pipeline. The
Chronicle sees private and public significant facts. Dawn sees only public/direct
facts. Realm News sees public history. Inn gossip discusses public incidents,
declared candidates, and opinions without leaking private campaign state.

Physical-save example:

```text
THE GLASS CHRONICLE — DAY 22
Vesper challenged the Glass Dragon and was cast broken from the crystal wastes.

DAWN BRIEF — DAY 23
Vesper returned wounded from the Dragon's territory.
```

This demonstrates the intended omniscient-versus-character-knowledge boundary.

## Save migration and safety

`GameStateCodec` version 7 serializes the season, incidents, declarations,
attempts, outcome, conclusion, and full active Dragon encounter/log. The
validator covers pressure/stage bounds, unique incident/attempt IDs, legal
resident references, attempt/outcome consistency, one victor, resolved-state
locking, combat HP/status/log bounds, and mutual exclusion from ordinary combat
or duels.

Version 6 saves initialize a sensible campaign stage from their current game day
without losing the player/community. Earlier supported migrations remain intact.
A dedicated v6 migration test passes. Stale-revision tests cover duplicate begin,
attack, support, resolution, and conclusion actions.

## Automated qualification

Final command:

```text
gradlew test lint assembleDebug
```

Result: `BUILD SUCCESSFUL`

- JVM suite: 110 tests; 0 failed, 0 errors, 0 skipped.
- `DragonSeasonTest`: 9 targeted tests covering deterministic monotonic stages,
  allowed incidents, Chronicle integration, player/NPC readiness, rejection of
  unqualified attempts, atomic support, player victory, player defeat/retry,
  save round trip, conclusion phases, resolved-world lock, private Dawn boundary,
  and the 25-season cohort.
- Existing combat, economy, community, Chronicle, navigation, persistence,
  1,000-day community soak, and 384 production-rule player balance campaigns
  remain green.
- Connected instrumentation previously passed 2 tests on this physical Android
  15 device, covering packaged audio and selection resources; LGD-009 adds no
  device-only dependency.
- Android lint succeeds with only the two existing launcher-template
  `IconXmlAndPng` warnings. LGD-009 adds no lint warning.
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`, 77,790,933 bytes.
- APK SHA-256:
  `A0D0E83E373BA711CB27527BF295195C60B6A7984D72686C5EB0E7B0DA897A3D`.

## 25-season deterministic cohort

Actual final test output:

```text
DRAGON_COHORT seasons=25 minDay=21 medianDay=25 maxDay=28
distinctVictors=25 failedAttempts=82 declaredRange=3..8
strongestLevelRange=5..6
```

Results:

- 25/25 seasons resolved; zero deadlocks.
- Resolution range Day 21–28; median Day 25.
- Every season ended through NPC agency with a persistent lawful NPC victor
  because this was the passive-player cohort.
- 82 failures occurred before victories (3.28 per season), demonstrating that
  attempts are dangerous rather than ceremonial.
- 3–8 declared candidates emerged per world.
- All attempts passed actual power/readiness assertions; no fallback cheated an
  unqualified resident into combat.
- Strongest residents ended at Levels 5–6; different personalities/equipment and
  seeded histories avoided lockstep results.
- IDs, residents, gold, stages, attempts, Chronicle retention, and save
  invariants remained lawful.

## Player-facing qualification

Five required perspectives were exercised proportionately:

1. **Normal progression:** the production-rule ordinary policy reaches Level 7
   around Day 16 and Level 8 around Day 22 (median), with carried wealth
   sufficient for tier-4 equipment. Dragon Watch gives explicit remaining
   requirements. The ready-player combat test completes a lawful victory and
   persists it.
2. **Aggressive progression:** the existing 128-seed aggressive production
   cohort reaches comparable late-campaign levels while accepting more defeats;
   Dragon attempt/retry rules prevent same-day brute-force loops.
3. **Passive campaign:** a genuine NXTPAPER playthrough performed no meaningful
   player progression. NPCs advanced the season and Vesper resolved it on Day
   24. The player was not blocked or falsely judged as a failure.
4. **Player defeat:** deterministic marginal combat produced one authoritative
   defeat, 25% carried-gold loss, half-HP recovery, zero Forest fights, retained
   permanent progression, persisted attempt history, and no duplicate action.
5. **NPC victory:** the physical Vesper campaign presented the complete
   Chronicle/Dawn/victory/Final Chronicle/Season Record flow and restored the
   final Season Record after force-stop and Continue.

The long physical run advanced the same save through Whispers, Signs (Day 5),
Fear (Day 9), Crisis (Day 15), Challenge Window (Day 20), declarations and
failed attempts (Days 21–23), and resolution (Day 24). Escalation was legible
without wall-clock time.

## Representative actual NPC-victory ending

From the physical campaign:

```text
VESPER IS THE DRAGON VICTOR

The realm is safe, and another adventurer's name passes into legend.
```

Season Record:

```text
Days............ 24
Dragon Victor... Vesper
Name............ Arcturus
Final Level..... 1
Final Rank...... #17
Carried Gold.... 28
Banked Gold..... 0
Weapon.......... Rusty Sword
Armor........... Worn Leather
Challenges...... 0 won / 0 lost
Dragon Attempts. 3
```

Actual Final Chronicle:

```text
For 24 days the Glass Dragon haunted the realm.
2 brave attempts failed before the crystal threat was ended.
Lyra led the public rankings when the final crisis came.
On Day 24, Vesper entered the crystal wastes.
The Glass Dragon did not return.
```

No name, deed, leader, attempt, or day in that account was fabricated.

## Physical NXTPAPER qualification

Connected TCL NXTPAPER serial `BC9424B4E3C5002`, model `9491G`, Android 15,
physical 1600 × 2400 density 320:

- new character and durable save;
- Dragon Watch in Whispers, Fear, and Challenge Window;
- explicit player readiness reasons;
- consecutive End Day transitions through a full season;
- public/private Dragon Chronicle and Dawn output;
- NPC declarations, failures, recovery, and victory;
- portrait and landscape touch/scroll behavior;
- no accidental duplicate day under rapid automation;
- victory notice, Final Chronicle, and Season Record readability;
- force-stop/relaunch/Continue on the completed season.

The device remains installed with the completed Vesper/Arcturus season at its
Season Record. Player-victory and player-defeat Dragon mechanics were exercised
deterministically through the production engine; the passive NPC-victory path
was the full manual-facing physical campaign.

## Evidence

Screenshots in `screenshots/lgd-008-009-device/` include:

- `dragon-watch-whispers.png`
- `dragon-watch-fear-landscape.png`
- `dragon-watch-challenge-window-landscape.png`
- `dragon-chronicle-day-21-landscape.png`
- `dragon-chronicle-day-22-landscape.png`
- `dragon-dawn-after-day-22-landscape.png`
- `dragon-chronicle-day-24-landscape.png`
- `dragon-dawn-after-day-24-landscape.png`
- `npc-victory-vesper-landscape.png`
- `final-chronicle-vesper-landscape.png`
- `season-record-vesper-landscape.png`

## Material files

- `game/GameState.kt`, `CommunityState.kt`, `GameStateValidator.kt`
- new `game/DragonSeasonRules.kt`
- new `game/ChronicleRules.kt`
- `game/WorldAdvancer.kt`, `ChallengeRules.kt`, `EconomyRules.kt`
- `simulation/CommunitySimulator.kt`
- `data/GameStateCodec.kt`
- `navigation/AppNavigation.kt`
- new `ui/screens/DragonScreens.kt`
- `ui/screens/GameplayScreens.kt`, `CommunityScreens.kt`, `TerminalScreens.kt`
- new `DragonSeasonTest.kt`, `ChronicleTransitionTest.kt`, and migration/regression
  test updates
- the LGD-008/LGD-009 work orders, reports, README index, and device evidence

## Constraints and next recommendation

No genuine Dragon Season defect remains. The workspace has no Git metadata, so
no commit/HEAD can be supplied. The full Main Menu/product wrapper remains
outside LGD-009; the existing Main Menu correctly preserves and reopens a
completed season. The current final narrative is deliberately concise and
fact-derived rather than a branching cinematic ending.

Recommended next pass: **Main Menu/product wrapper and final campaign balancing**.
The finite society/campaign loop is structurally complete; a full factions pass
would add breadth but is not required to make this season playable or
conclusive.

No additional audio scope was introduced: there is no boss music, roar, combat
sound, victory fanfare, or crisis ambience.

`LGD009_DRAGON_PRESSURE_COMPLETE`

`LGD009_NPC_CHAMPION_AGENCY_COMPLETE`

`LGD009_PLAYER_DRAGON_COMBAT_COMPLETE`

`LGD009_NPC_VICTORY_COMPLETE`

`LGD009_FINITE_SEASON_CONCLUSION_COMPLETE`

`LGD009_25_SEASON_ZERO_DEADLOCK_QUALIFIED`

`LGD009_NXTPAPER_CAMPAIGN_QUALIFIED`

`LGD009_COMPLETE`
