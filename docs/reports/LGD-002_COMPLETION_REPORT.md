# LGD-002 Completion Report

Date: 2026-08-28

## Result

LGD-002 is complete. New Game now creates a validated character through a terminal-styled Android text prompt, saves a versioned local game record, and enters a real Day 1 Town. Continue restores that same player and day after a force-stop and cold application restart.

No combat, day advancement, networking, account system, cloud save, or other later-phase gameplay was introduced.

## Game and persistence foundation

- `GameState` schema version 1 contains the current day, player, daily state, and a deterministic world seed container.
- `PlayerCharacter` has a stable ID, name, level, experience, HP, attack, defense, gold, weapon, and armor.
- The centralized starting rules create a Level 1 character with 0 experience, 20/20 HP, 4 attack, 2 defense, 25 gold, a Rusty Sword, Worn Leather, Day 1, and 10 future fight slots.
- Names are stripped of control characters and surrounding whitespace, must be non-empty, and are limited to 24 Unicode code points. Normal spaces, punctuation, and Unicode remain valid.
- A strict, versioned local record is stored with Android `SharedPreferences`. The codec and repository are isolated from Compose and expose load, save/replace, existence, and delete operations.
- Missing, malformed, unsupported-version, and invariant-violating records are distinguished. A broken save opens an ANSI recovery screen and is never silently erased.
- Intro presentation preferences remain separate from durable game state. Android backup stays disabled and the manifest requests no Internet permission.

## Player flow delivered

- Terminal name prompt backed by a visually unadorned Compose `BasicTextField` and the Android software keyboard.
- Keyboard Done and the numbered Accept command both advance to name confirmation.
- Existing valid or broken records require an ANSI Start Over warning before replacement.
- Main Menu Continue accurately represents missing, valid, and unreadable save states.
- Town displays the saved name, day, level, HP, and gold.
- Character Record displays all persistent player fields and the game day.
- Forest, Inn, Training, and Rankings are player-aware placeholders.
- End the Day is an explicit future boundary and does not change Day 1.
- Android Back returns through the intended terminal hierarchy without deleting the active adventure.

## Automated verification

Command:

```text
gradlew test lint assembleDebug
```

Result: `BUILD SUCCESSFUL`

- Unit tests: 19 passed, 0 failed, 0 skipped.
- New-game tests cover trimming, validation, canonical lawful values, full starting HP, Day 1, and equipment.
- Persistence tests cover full save/load equality, Unicode, replacement, deletion/Continue availability, corrupt data, unsupported versions, and invalid invariants.
- Navigation tests cover startup policy and the LGD-002 Back hierarchy.
- Android lint: no issues.
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`.
- Final APK SHA-256: `B9826245C9EF0BA86F88E4B78E60E7A931A79CC5170CBDBAD395582BB637E2F2`.
- Merged manifest contains no `android.permission.INTERNET` permission.

## NXTPAPER device qualification

Physical qualification was completed on the connected primary target:

- Public name: TCL NXTPAPER 14
- Model/device: 9491G / Hera_Vis_WIFI
- Android: 15 (API 35)
- Physical display: 1600 × 2400 at 320 dpi

Checks completed:

- Fresh launch and no-save Main Menu state.
- Tapping the prompt summoned Gboard; the prompt, Accept, and Return commands remained reachable above the keyboard.
- Editing, touch confirmation, and keyboard Done confirmation worked; Done dismissed the keyboard normally.
- Creating `Arcturus` produced the lawful Day 1 state and immediately wrote the local game record.
- Town and Character Record displayed the persisted values without clipping.
- Forest and End the Day displayed player-aware, non-functional boundaries and returned correctly.
- Home/background and hot resume retained the active route and character.
- Force-stop, cold launch, Main Menu Continue, and Town restoration retained `Arcturus`, Day 1, and all displayed stats.
- New Game showed the required destructive replacement warning; Android Back chose the safe path and preserved Continue.
- A reversible 800 × 1000 display override exercised a narrow 400dp-class viewport. Content reflowed without horizontal clipping and touch scrolling exposed every Town command. The physical display size was restored afterward.
- No stock Material text field, button, card, or dialog was visible in the qualified flow.

Representative captures are in [`screenshots/lgd-002-device`](../../screenshots/lgd-002-device/), including [name entry with keyboard](../../screenshots/lgd-002-device/name-entry-keyboard-portrait.png), [name confirmation](../../screenshots/lgd-002-device/name-confirmation.png), [created Town](../../screenshots/lgd-002-device/town-created.png), [Character Record](../../screenshots/lgd-002-device/character-record.png), [cold-restart Continue result](../../screenshots/lgd-002-device/town-after-restart-continue.png), [replacement warning](../../screenshots/lgd-002-device/new-game-replacement-warning.png), and [narrow Town after scrolling](../../screenshots/lgd-002-device/town-narrow-scrolled.png).

The qualification reset only this development application's local data to establish a genuine no-save baseline. The final installed debug build retains the newly created `Arcturus` save.

## Terminal state

```text
LGD002_CHARACTER_CREATION_COMPLETE
LGD002_PLAYER_IDENTITY_PERSISTENT
LGD002_CONTINUE_GAME_FUNCTIONAL
LGD002_REAL_TOWN_STATE_ESTABLISHED
LGD003_FIRST_PLAYABLE_DAY_READY
```

Next planned work: LGD-003 — Forest Combat, Monsters, Experience, Gold, and the First Playable Day.
