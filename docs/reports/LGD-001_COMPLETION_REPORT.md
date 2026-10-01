# LGD-001 Completion Report

Date: 2026-08-28

## Result

LGD-001 is complete. The project is a clean native Android application built with Kotlin and Jetpack Compose. It contains no web runtime, networking stack, Internet permission, cloud save, or gameplay beyond the authorized foundation.

The temporary namespace is `com.legendoftheglassdragon` because no established Wingtip Studio catalogue namespace was present in the workspace.

## Foundation delivered

- Android application with minimum SDK 26, compile/target SDK 37, and current stable dependencies identified by the installed Android toolchain.
- Edge-to-edge, system-bar-safe terminal surface with responsive horizontal padding and vertical touch scrolling.
- System-safe monospace typography, reusable ANSI color spans, ASCII rules/boxes, and subtle press feedback.
- Full-width numbered menu rows with a 52dp minimum touch height and no visible Material buttons, cards, pills, or surfaces.
- Startup connection sequence, ANSI title, Main Menu, New Game placeholder, Town, About, and reusable area placeholder screens.
- Coherent Android Back policy and `rememberSaveable` route/selection state across activity recreation.
- Local `SharedPreferences` boundary for intro-seen and placeholder-save markers. Android backup is disabled so save data remains device-local.
- Explicit package boundaries for `game`, `simulation`, `data`, `navigation`, `ui`, and `ui/theme` without speculative systems.

## Automated verification

Command:

```text
gradlew test lint assembleDebug
```

Result: `BUILD SUCCESSFUL`

- Unit tests: 6 passed, 0 failed.
- Persistence tests cover empty initial state and a write/reload round trip through the same boundary used by Android preferences.
- Navigation tests cover first/returning launch and foundational Back behavior.
- Android lint: `No issues found.`
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
- Final APK SHA-256: `FDC5B704D2ECFC48A8A206A4BFD7F84F09E27F7FB128A93C9BA16D597F3FDBD4`
- Merged manifest contains no `android.permission.INTERNET` permission.

## NXTPAPER device qualification

Physical device qualification was completed on the connected primary hardware:

- Public name: TCL NXTPAPER 14
- Model/device: 9491G / Hera_Vis_WIFI
- Android: 15 (API 35)
- Physical display: 1600 × 2400 at 320 dpi, portrait
- Install: streamed debug APK install succeeded
- Final cold launch: succeeded; activity resumed normally

Checks performed:

- First-run startup sequence and immediate touch advancement.
- Startup → Title → Main Menu → New Game → Town touch flow.
- Disabled Continue state before a local marker exists and enabled Continue state afterward.
- Forest placeholder and return to Town.
- Android Back: Placeholder → Town → Main Menu.
- Background/home and hot resume retained the current route.
- Reinstall retained local intro/save markers, proving on-device persistence.
- UI Automator exposed each menu target as a full-width clickable node with a 104px height at 320 dpi (52dp).
- Native portrait layout showed no content behind system bars or the tablet taskbar.
- A reversible 800 × 1000 display override exercised a narrow 400dp content width. The Town surface became scrollable, a touch swipe revealed Character and End the Day, route state survived recreation, and the native display size was restored afterward.
- Visual inspection found no clipping after the responsive header refinement and no visible stock Material controls.

Representative device captures are in [`screenshots/lgd-001-device`](../../screenshots/lgd-001-device/), including [startup](../../screenshots/lgd-001-device/startup.png), [title](../../screenshots/lgd-001-device/title.png), [main menu](../../screenshots/lgd-001-device/main-menu.png), [new game](../../screenshots/lgd-001-device/new-game.png), [town](../../screenshots/lgd-001-device/town.png), [placeholder](../../screenshots/lgd-001-device/forest-placeholder.png), and the [final narrow-width Town layout](../../screenshots/lgd-001-device/phone-size-town-final.png).

The first qualification reset the newly installed development app's local data to guarantee a genuine first-run check. The test then recreated both persistence markers through the normal UI.

## Terminal state

```text
LGD001_NATIVE_FOUNDATION_COMPLETE
LGD001_ANSI_SHELL_ESTABLISHED
LGD001_TOUCH_NAVIGATION_PROVEN
LGD001_LOCAL_PERSISTENCE_FOUNDATION_PROVEN
LGD002_CHARACTER_CREATION_READY
```

Next planned work: LGD-002 — Character Creation, Persistent Player Save, and First Real Town State.
