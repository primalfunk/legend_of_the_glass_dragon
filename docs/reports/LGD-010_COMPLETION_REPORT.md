# LGD-010 Completion Report

Date: 2026-08-28

## 1. Disposition

`LGD010_COMPLETE_WITH_BOUNDED_FOLLOWUP`

The product wrapper is complete and qualified. The bounded follow-up is release
clearance for the untagged Principal-supplied click recording and independent
verification of the exact music recordings' embedded public-domain claim. This
does not block development or offline operation, but it remains an LGD-012
distribution gate.

## 2. Implementation commit / HEAD

The workspace is not a Git worktree (`git status` reports `not a git repository`),
so no commit or current HEAD exists.

## 3. Main Menu state model

`ProductWrapperPolicy` provides one pure wrapper authority:

- `NONE`: New Game, Settings, Credits, Exit;
- `ACTIVE`: Continue, New Game, optional Last Season Record, Settings, Credits,
  Exit;
- `COMPLETED`: Season Record, New Game, Settings, Credits, Exit;
- `INVALID`: safe replacement path without pretending Continue is valid.

A resolved campaign with an unfinished victory/conclusion presentation is marked
`conclusionPending` and exposes `Continue Conclusion`. It resumes the exact
Victory Notice, Final Chronicle, or Season Record checkpoint and never enters
ordinary Town gameplay.

The legacy fake connection and separate title gates were removed from production
navigation. Android's short crest splash now leads directly to the title-dominant
Main Menu.

## 4. New Game flow

New Game leads to the existing terminal-styled Android `BasicTextField`, which
autofocuses and opens the soft keyboard. The 24-code-point validator trims outer
whitespace, rejects blank/overlong/control input, retains safe Unicode, and
supports both IME Done and the touch command.

For an active or unreadable slot, a full ANSI warning appears first. Entering the
naming flow does not delete or rewrite the existing save. The old campaign is
replaced only by one successful synchronous save after the player confirms the
final name. A Compose commit guard prevents a rapid double activation.

Starting after a completed campaign initializes a wholly fresh player, community,
day, economy, relationships, Chronicle, and Dragon Season. Application audio
settings remain outside campaign storage.

## 5. Continue / resume behavior

Continue reloads the authoritative save and routes in priority order to:

1. active Forest combat;
2. active adventurer challenge;
3. active Dragon combat;
4. Glass Chronicle checkpoint;
5. Dawn Brief checkpoint;
6. persisted Dragon conclusion phase;
7. Town.

Return to Main Menu performs no game action and never advances the world. Town
offers a visible `[11] Main Menu` command; Android Back from Town also returns to
the menu. Atomic combat/day/Chronicle/Dragon screens retain their existing safe
Back policy.

## 6. Completed-season handling

The active campaign slot remains unchanged, but a second local
`glass_dragon_completed_season/completed_season` record archives the latest
resolved `GameState`. Existing LGD-009 resolved saves are backfilled into that
archive on first LGD-010 launch. Every later resolved-state save refreshes it.

Creating a new active season therefore does not erase the latest Season Record.
The Main Menu can show `Last Season Record` alongside an active campaign. The
archive retains the real final Chronicle/history, community, victor, rankings,
equipment, relationships, and campaign length needed by the current record and
future retrospective work.

## 7. Season Record

The existing real-state Season Record is now a first-class Main Menu destination.
It shows campaign days, Dragon victor, player final level/rank, carried and banked
gold, weapon, armor, duel record, Dragon attempts, and a player-specific closing
statement. Returning from the active conclusion marks the conclusion complete;
opening the archived record from the menu is strictly read-only.

Physical migration evidence preserved the Day 24 Vesper/Arcturus NPC-victory
record and reopened it from the new menu before the connected test runner later
uninstalled its debug fixture.

## 8. Settings

Implemented and persisted independently in `AppPreferences`:

- Music: ON/OFF;
- Click Sound: ON/OFF.

Both default ON and take effect immediately. Settings survive activity/process
restart and campaign replacement. No volume mixer, soundtrack selector, graphics,
account, cloud, difficulty, or unrelated options were introduced.

## 9. Menu/game audio lifecycle

`GameAudioController` remains the one activity-scoped controller. It receives the
saved preferences at construction and exposes the small `AudioControls` boundary.
Navigation between menu and gameplay does not recreate it. Music OFF pauses the
current `MediaPlayer`, releases audio focus, and retains the current shuffle
position; Music ON resumes that player or starts exactly one next track. The
existing lifecycle foreground pause/resume, shuffle bag, completion callback,
resource release, and one `SoundPool` remain unchanged in scope.

The universal selection route checks the click preference before playing. Valid
menu rows remain visually responsive when sound is disabled. No new track,
effect, service, or audio category was added.

## 10. Splash, icon, and application label

- Android 12+ splash: short native black splash with the Glass Dragon crest;
- pre-Android-12 launch window: near-black game background;
- adaptive launcher icon: existing pixel Glass Dragon crest on a hard-edged
  cyan/black/gold grid field, with a dedicated monochrome layer;
- app label: `Legend of the Glass Dragon`.

The old Android-robot foreground and green template background were replaced.
Ten redundant template WebP launcher variants were removed so adaptive XML is the
single icon authority. The launcher rendering and system splash were captured on
the physical target.

## 11. Wrapper visual asset paths

- title art: `app/src/main/res/drawable-nodpi/ansi_title_ornament.png`;
- crest/splash/icon source: `app/src/main/res/drawable-nodpi/glass_dragon_crest.png`;
- adaptive background: `app/src/main/res/drawable/ic_launcher_background.xml`;
- adaptive foreground: `app/src/main/res/drawable/ic_launcher_foreground.xml`;
- monochrome icon: `app/src/main/res/drawable/ic_launcher_monochrome.xml`;
- icon manifests: `app/src/main/res/mipmap-anydpi/ic_launcher*.xml`;
- launch themes: `app/src/main/res/values/themes.xml` and
  `app/src/main/res/values-v31/themes.xml`.

## 12. Package and version

- application ID / namespace: `com.legendoftheglassdragon`;
- installed label: `Legend of the Glass Dragon`;
- minimum SDK: 26;
- target / compile SDK: 37;
- `versionName`: `0.10.0`;
- `versionCode`: `10`.

No `INTERNET` permission or network dependency is present.

## 13. Production cleanup

- removed the legacy Startup, separate Title, and placeholder About routes;
- removed disabled Continue from the no-save menu;
- omitted unavailable options instead of rendering tutorial-style disabled UI;
- removed obsolete intro-seen preference and startup accessibility string;
- removed template Android launcher art and duplicate raster launchers;
- added a route test that rejects startup/debug/seed destinations;
- exposed no seed controls, raw state dumps, test screens, developer resets, or
  temporary navigation in the ordinary player path.

Engineering balance/soak fixtures remain JVM-only.

## 14. Automated qualification

Final local command:

```text
gradlew testDebugUnitTest lintDebug assembleDebug assembleRelease
```

Result: `BUILD SUCCESSFUL`.

- JVM suite: 119 tests; 0 failed, 0 errors, 0 skipped across 22 suites;
- Android lint: 0 issues;
- debug and unsigned release APKs built;
- Android-test APK built;
- connected final-binary instrumentation: 3 tests, all passed on `9491G - 15`;
- instrumentation covers all 15 MP3 resources, click decoding, package identity,
  and launch directly into the production Main Menu without the connection gate.

Added targeted policy coverage for no-save/active/completed/invalid menus,
interrupted conclusion resume, active combat/Chronicle/Dawn routing, archived
record coexistence, Back routing, preference persistence, music setting commands,
and click gating. Existing name, save, campaign, Chronicle, combat, economy,
1,000-day community soak, and Dragon Season cohorts remain green.

Artifacts:

- debug APK: 77,779,339 bytes,
  SHA-256 `373F07AB8E7248A0BD4386FF3219A61957095698822F8BCD914D02BA259AE287`;
- unsigned release APK: 74,028,993 bytes,
  SHA-256 `4F8C820C6C353AA6614031CE78FC86C777943A0021DBAD16D99A50299D432E4F`.

## 15. Clean-install player qualification

On a genuinely clean post-instrumentation install:

1. the native crest splash appeared briefly;
2. the app opened directly to the title-dominant Main Menu;
3. only New Game, Settings, Credits, and Exit appeared;
4. New Game opened the ANSI name screen and Android keyboard;
5. blank submission produced `ERROR: A name must be entered.`;
6. IME Done accepted `Arcturus` and opened confirmation;
7. Enter the Realm created one lawful Day 1 campaign and Town.

There was no connection animation, account, network request, filesystem prompt,
debug screen, or developer terminology.

## 16. Active-save qualification

The physical flow created Lyra, returned to menu without advancing time, displayed
Continue plus Last Season Record, canceled the destructive New Game warning, and
continued back to the same Lyra/Day 1/10-fight state. After the clean-install
pass, Arcturus was force-stopped, relaunched, and continued to the same Day 1
Town. The latest APK is installed with that lawful active campaign.

The warning explicitly states that replacement occurs only after a new
adventurer is confirmed. Cancel preserved the old campaign.

## 17. Completed-season qualification

The pre-existing resolved Vesper season migrated into the archive and produced:

- `Continue Conclusion` while its Season Record checkpoint was unfinished;
- an actual Day 24 Season Record with Vesper as victor and Arcturus as player;
- after returning, a completed menu with Season Record and no ordinary Continue;
- after creating Lyra, an active menu with Continue and Last Season Record.

This proves active and completed records coexist. Gradle connected-test cleanup
subsequently uninstalled the debug package and its generated device data; evidence
was captured before cleanup, and a clean lawful Arcturus save was installed after.

## 18. Phone / tablet / NXTPAPER qualification

Connected physical target:

- serial `BC9424B4E3C5002`;
- TCL model `9491G` / NXTPAPER-class target;
- Android 15;
- physical 1600 x 2400, density 320.

Qualified:

- tablet portrait and device landscape;
- representative phone viewport 1080 x 2400 at density 420, then fully restored;
- title scaling, cyan/gray/gold contrast, frame line thickness, menu scrolling,
  48dp+ command targets, keyboard resize, blank-name error, Continue/restart,
  New Game cancel, completed archive, settings restart, launcher icon, and splash;
- device display size/density and auto-rotation were restored after qualification.

Useful evidence:

- `screenshots/lgd010-fresh-portrait.png`;
- `screenshots/lgd010-phone-portrait.png`;
- `screenshots/lgd010-main-menu.png`;
- `screenshots/lgd010-record.png`;
- `screenshots/lgd010-settings.png`;
- `screenshots/lgd010-name.png`;
- `screenshots/lgd010-new-warning.png`;
- `screenshots/lgd010-town-lyra.png`;
- `screenshots/lgd010-launcher.png`;
- `screenshots/lgd010-splash.png`.

## 19. Music provenance / license inventory

`docs/assets/LGD-010_MUSIC_PROVENANCE.md` records each file and title. All 15
`muss_01.mp3` through `muss_15.mp3` files share embedded metadata identifying:

- Modest Mussorgsky;
- *Mussorgsky's Pictures at an Exhibition*;
- performer: The Skidmore College Orchestra;
- source comment: `https://www.classicals.de`;
- copyright tag: `Public Domain: http://creativecommons.org/licenses/publicdomain/`.

The MP3 sources and packaged files retain the byte-identical LGD-007 mapping.
Creative Commons describes the Public Domain Mark as identifying a work free of
known copyright restrictions but disclaims verification/warranty and notes
jurisdictional caveats. No independent license file or source-page snapshot is in
the workspace.

`click.mp3` has no embedded author/source/copyright/license tags. Android uses its
losslessly decoded `ui_click.wav` derivative. Exact recording verification and
click clearance remain the bounded release follow-up.

## 20. Material files changed

- `MainActivity.kt`, `ui/GlassDragonApp.kt`, `ui/screens/TerminalScreens.kt`;
- `navigation/AppNavigation.kt`, new `navigation/ProductWrapperPolicy.kt`;
- `data/AppPreferences.kt`, `data/GameSaveRepository.kt`;
- `audio/SelectionSound.kt`, `AudioSessionPolicy.kt`, `GameAudioController.kt`;
- `app/build.gradle.kts`, launcher drawables, themes, strings, adaptive icon XML;
- navigation/preferences/audio JVM tests and new product-wrapper JVM/device tests;
- LGD-010 work order, provenance inventory, README indexes, report, screenshots.

## 21. Remaining constraints and LGD-011 recommendation

No genuine product-wrapper defect remains. The release APK is intentionally
unsigned; release signing/store packaging belongs to release work. Audio legal
clearance is the only bounded distribution constraint: music has affirmative
embedded public-domain/source/performer metadata but should be verified against
the exact source, while click provenance is absent.

The project is ready for LGD-011 whole-campaign balance and society-density
qualification. LGD-011 can start from a coherent launch-to-conclusion product,
while LGD-012 should retain the explicit audio clearance gate.

`LGD010_MAIN_MENU_COMPLETE`

`LGD010_NEW_GAME_AND_CONTINUE_COMPLETE`

`LGD010_COMPLETED_SEASON_ARCHIVE_COMPLETE`

`LGD010_AUDIO_SETTINGS_COMPLETE`

`LGD010_ANDROID_PRODUCT_IDENTITY_COMPLETE`

`LGD010_NXTPAPER_WRAPPER_QUALIFIED`

`LGD010_COMPLETE_WITH_BOUNDED_FOLLOWUP`
