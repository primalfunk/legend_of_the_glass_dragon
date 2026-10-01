# LGD-012 Android Release Qualification and MVP Closure

Qualification date: 2026-08-28  
Source tree: `C:\Users\erinm\AndroidStudioProjects\LegendoftheGlassDragon`  
Disposition: **LGD012_MVP_PRIVATE_RELEASE_READY**

The Android MVP is technically qualified for private/local installation. Public redistribution is not yet authorized because the Principal-supplied click recording has no documented redistribution provenance and no production signing key was available. The 15 music recordings have affirmative matching source metadata and are credited, but their Public Domain Mark remains evidence rather than a legal warranty.

## 1. Frozen baseline and final workspace

- Git/HEAD: not applicable; the workspace is not a Git repository, and LGD-012 did not initialize one.
- Final source-manifest fingerprint: SHA-256 `016B41F5D65B5D0BD9A648871FCC3A8AA841B51D98ECA339D46AA7F8F4B9054C` over 109 sorted `app/src` and build-configuration entries.
- Application ID: `com.legendoftheglassdragon`.
- Application label: `Legend of the Glass Dragon`.
- Version: `versionName 1.0.0-rc1`, `versionCode 12`.
- Save schema: 8.
- Android: minSdk 26, targetSdk 37, compileSdk 37.
- Build tools: Gradle 9.5.0, Android Gradle Plugin 9.3.2, Kotlin 2.4.10, Java source/target 11; qualification JBR OpenJDK 25.0.2.
- Direct runtime versions: AndroidX Core KTX 1.19.0, Activity Compose 1.13.0, Lifecycle Runtime KTX 2.11.0, Compose BOM 2026.08.00 (Compose UI/Foundation 1.12.0; Material3 1.4.0).
- Release optimization/shrinking/obfuscation: disabled. This preserves the existing safe, simple release configuration.
- Launcher: exported `.MainActivity`; app is resizable and supports Android rotation because no orientation lock is declared.
- Backup: disabled; save data remains app-private.

### Accepted LGD-011B balance baseline

No gameplay-domain or frozen balance file changed during LGD-012. The accepted Ordinary-policy baseline remains:

| Metric | Accepted and reproduced |
|---|---:|
| Seasons | 5,000 / 5,000 resolved |
| Total game days | 100,706 |
| Median campaign | 20 days |
| P10–P90 | 19–21 days |
| Player Dragon victory | 73.70% |
| NPC Dragon victory | 26.30% |
| First player Dragon-attempt success | 71.88% |
| Player Rank #1 | 73.00% |
| Player Top 3 | 99.30% |
| Median credible candidates | 2 |
| Training first-attempt success | 83.31% |
| Chronicle mean / median | 5.32 / 5 |
| Structured callbacks | 4.51 / season |
| Unresolved | 0 |

Frozen rules include 10 Forest fights/day, 25% carried-gold defeat loss, +5 HP/+2 attack/+1 defense per level, training success base/full-health/equipment values of 80/5/3%, XP thresholds `0, 50, 250, 600, 1250, 2800, 5600, 9500, 14500, 20500` for Levels 1–10, Dragon HP/attack/crystal defense/fractured defense of `108/21/15/11`, and Chronicle limits of 7 items/day and 40 retained days.

Frozen file hashes:

| File | SHA-256 |
|---|---|
| `GameRules.kt` | `F520DC9E1785D0AD1B678666EDC2251A80BC1D860F3D400DC401D703330FB6B8` |
| `DragonSeasonRules.kt` | `9E7960DC33745254D91C58080ADB030DA832CB301AA5AD94032761E5ACBC4575` |
| `ChronicleRules.kt` | `A51F05962D27FBDE29CBEF7A3D3B45F52EF5DF22135AA19112412007C315A54C` |
| `CommunitySimulator.kt` | `69A3141008A3D792634FE17D63C76C81D755EC329C9E1DB93BE11706B1D9D5D9` |

## 2. Release configuration and automated gates

| Gate | Result |
|---|---|
| JVM unit tests | PASS — 128 tests in 27 suites; 0 failed, 0 errored, 0 skipped |
| Android instrumentation | PASS — 6/6 on physical TCL 9491G, Android 15 |
| Lint | PASS — `No issues found` |
| Debug APK | PASS |
| Release APK | PASS |
| Release AAB | PASS |
| Audio decode | PASS — all 15 MP3 resources plus the click derivative decoded on device |
| 5,000-season regression | PASS — 5,000/5,000 resolved; accepted campaign CSV reproduced byte-for-byte |
| Deterministic reruns | PASS — 10/10 representative pairs |
| Save/reload simulation comparisons | PASS — 5/5 |

The release runtime dependency tree contains Kotlin, AndroidX/Compose, JetBrains annotations, kotlinx coroutines/serialization, JSpecify, and the small AndroidX transitive Guava listenable-future artifact. No ad, analytics, account, cloud, HTTP client, or remote-service SDK is present.

## 3. Install, upgrade, wrapper, and save qualification

### Clean install

The existing package was uninstalled, the signed private-test release candidate was installed, and the app was launched cold. Android presented the production splash/title and a clean Main Menu with no stale save or preferences. New Game opened the ANSI naming screen and Android keyboard; a valid name created exactly one Day 1 campaign and entered Town. Killing the process with an uncommitted name left no campaign behind.

### Compatible upgrade

A schema-8 active save from the prior compatible build was captured, the release candidate was installed with `adb install -r`, and the same character/day/route remained available through Continue. A completed-season fixture and application audio settings also survived an in-place release install. No migration or balance rewrite was required because the schema remained 8.

### Save/lifecycle matrix

| State | Qualification and result |
|---|---|
| Main Menu | Hard stop/relaunch returned to the same authoritative campaign availability state. |
| Character naming | Hard stop before confirmation created no campaign. Keyboard and Back followed Android conventions. |
| Town | Hard stop/relaunch showed Continue and restored the same day/player state. |
| Forest / combat boundary | A live Forest encounter, HP, enemy HP, and remaining fights survived hard process death exactly. |
| Training | Seeded success/failure, one-attempt-per-day, HP consequence, and repository round trips pass the production-domain suite. |
| Shops / bank | Revision-gated atomic purchases, deposits, withdrawals, insufficient funds, and save round trips pass; no duplicate money/action path was found. |
| End Day before confirmation | Android Back returned to Town without advancing the day. |
| End Day committed | World advancement applied once; a second resolution was rejected while the transition checkpoint remained pending. |
| Glass Chronicle | Hard stop/relaunch returned to the same Day 1 Chronicle; all authoritative displayed text matched exactly. |
| Dawn Brief | Hard stop/relaunch returned to the same Day 2 brief; health, fights, rank, and notices matched exactly. |
| Dragon attempt pending | Production repository round-trip restored `DragonCombat`; combat engine/state tests reject duplicate begin/attack transitions. |
| Failed Dragon attempt | Release UI restored the same defeat text, 90/108 Dragon HP, 27/55 player HP, 25 gold lost, and same-day lock after process death. |
| Player Dragon victory | Device test saved/reloaded every combat turn, produced exactly one player-victory attempt, one victor, and rejected post-completion attacks. |
| NPC Dragon victory | A 22-day autonomous physical-device test resolved to an NPC, completed the conclusion, archived the record, and exposed no Continue into ordinary play. |
| Final Chronicle / Season Record | Conclusion advanced to `COMPLETE`, archived, returned to Main Menu, and survived hard stop/relaunch. |
| New season after completion | Day, level, gold, Dragon pressure, NPCs, relationships, and active Chronicle reset; application settings and the latest completed Season Record remained. |

The unreadable-save policy also passes focused tests: corrupt/invalid data returns an error instead of throwing, is not overwritten automatically, presents simple recovery language, and requires an explicit player action before the broken slot is deleted or replaced.

## 4. Device, layout, input, and lifecycle qualification

Physical target: TCL 9491G (`Hera_Vis_WIFI`), Android 15 / API 35, arm64-v8a, native 1600×2400 at 320 dpi. This is the available physical NXTPAPER-class target, not an emulator.

- Native tablet portrait: title, menu, Town, scrollable command list, and Season Record were crisp and unclipped.
- Native tablet landscape: clean install, naming, Town, interrupted combat/Chronicle/Dawn, completed season, and settings were exercised.
- Representative phone portrait: Android override 1080×2400 at 420 dpi. Main Menu, Town, combat, Chronicle, Dawn, and Dragon defeat were reachable, legible, and scrollable.
- Text scaling: 115% system font scale remained readable; Town commands continued below the fold through normal scrolling.
- Touch: observed command rows are approximately 52 dp high at both phone and tablet densities, exceeding the 48 dp minimum target.
- Android Back: gameplay safely returns to Main Menu, Main Menu returns to Android, End Day confirmation cancels without advancing, modals close, and naming first dismisses the keyboard. No Back path started New Game, ended a day, or deleted a campaign.
- Orientation: both portrait and landscape were exercised; authoritative game state survived recreation/process restoration. No landscape-only redesign was introduced.
- Offline: with Wi-Fi disabled and Android reporting `Active default network: none`, cold launch, Main Menu, Continue into persisted Dawn, and entry into Town all worked. The manifest requests no Internet permission.

Useful screenshots:

- `screenshots/LGD-012-clean-install.png`
- `screenshots/LGD-012-tablet-portrait.png`
- `screenshots/LGD-012-tablet-font115-town.png`
- `screenshots/LGD-012-phone-main.png`
- `screenshots/LGD-012-phone-town.png`
- `screenshots/LGD-012-phone-combat.png`
- `screenshots/LGD-012-phone-chronicle.png`
- `screenshots/LGD-012-phone-dawn.png`
- `screenshots/LGD-012-phone-dragon-loss.png`
- `screenshots/LGD-012-season-record.png`

## 5. Audio lifecycle and provenance

One activity-scoped `GameAudioController` owns one `MediaPlayer` and one `SoundPool`. The 15 tracks use a shuffle bag, and the click uses the short-effect pool. Unit tests prove complete-bag traversal, deterministic behavior, no same-track repeat across bag reshuffle, lifecycle pause/resume policy, settings gating, and no duplicate-session command. The physical-device decoder test opened every packaged audio resource.

Ten Home/foreground cycles retained one process (`PID 6329`) and logged only one `music_start`; the same track resumed rather than restarting or overlapping. Backgrounding paused active use, hard process death created one fresh controller on relaunch, music and click settings persisted, and rapid click playback did not crash or increase resources. No audio beyond the 15 music tracks and universal click was introduced.

The source MP3s are byte-for-byte package copies. The Principal-supplied `temp/click.mp3` could not be decoded reliably by the target Android stack, so the package uses a losslessly decoded 3,492-byte WAV derivative; runtime never reads `temp`.

| Package/source | Embedded title or role | Available performer/source/license evidence | Category |
|---|---|---|---|
| `muss_01.mp3` | Promenade — Allegro giusto nel modo russico… | Skidmore College Orchestra; Classicals.de/Musopen; CC PDM 1.0 | B — attribution required |
| `muss_02.mp3` | I. Gnomus — Vivo | Same collection metadata/source | B |
| `muss_03.mp3` | Promenade — Moderato comodo e con delicatezza | Same collection metadata/source | B |
| `muss_04.mp3` | III. Tuileries — Allegretto non troppo capriccioso | Same collection metadata/source | B |
| `muss_05.mp3` | IV. Bydlo — Sempre moderato pesante | Same collection metadata/source | B |
| `muss_06.mp3` | Promenade — Tranquillo | Same collection metadata/source | B |
| `muss_07.mp3` | V. Ballet des poussins dans leurs coques | Same collection metadata/source | B |
| `muss_08.mp3` | VI. Samuel Goldenberg and Schmuyle — Andante | Same collection metadata/source | B |
| `muss_09.mp3` | X. La grande porte de Kiev — Allegro alla breve | Same collection metadata/source | B |
| `muss_10.mp3` | IX. La Cabane sur des pattes de poule | Same collection metadata/source | B |
| `muss_11.mp3` | Cum mortuis in lingua mortua | Same collection metadata/source | B |
| `muss_12.mp3` | VIII. Catacombae. Sepulcrum romanum | Same collection metadata/source | B |
| `muss_13.mp3` | VII. Limoges. Le Marché | Same collection metadata/source | B |
| `muss_14.mp3` | Promenade — Moderato non tanto pesante | Same collection metadata/source | B |
| `muss_15.mp3` | II. Il vecchio castello — Andante | Same collection metadata/source | B |
| `ui_click.wav` ← `temp/click.mp3` | Universal valid-selection click; no embedded tags | Principal-supplied only; no source, author, copyright, or license record | C — provenance unclear |

The matching current source page is `https://www.classicals.de/mussorgsky-exhibition`; credits now name Mussorgsky, The Skidmore College Orchestra, Classicals.de, and CC Public Domain Mark 1.0. Creative Commons describes PDM as a status label, not a legally operative dedication like CC0. This report records evidence and does not provide a legal opinion.

## 6. Visual assets, permissions, privacy, and notices

Production identity paths:

- Title: `app/src/main/res/drawable-nodpi/ansi_title_ornament.png`
- Crest/splash art: `app/src/main/res/drawable-nodpi/glass_dragon_crest.png`
- Adaptive icon: `app/src/main/res/drawable/ic_launcher_background.xml`, `ic_launcher_foreground.xml`, `ic_launcher_monochrome.xml`, and `mipmap-anydpi/ic_launcher*.xml`
- Splash themes: `app/src/main/res/values/themes.xml` and `values-v31/themes.xml`

The launcher screenshot confirms the cyan/white dragon silhouette remains identifiable at small size and under the device mask. Raster visuals total 2,544,847 bytes and are packaged resources; no runtime reference to `root/temp` exists.

The app manifest declares no Internet, location, contacts, camera, microphone, or storage permission. `aapt` reports only AndroidX's package-scoped `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`. No analytics, ads, crash upload, account identifier, telemetry, or device-fingerprinting dependency was found. The app is fully offline.

Production Kotlin/AndroidX and transitive support libraries are Apache-2.0-compatible according to their published artifacts. A complete Apache 2.0 notice is packaged at `app/src/main/assets/third_party_notices.txt`, and Credits identify the runtime library family. Test-only JUnit/EPL and Hamcrest/BSD dependencies are not bundled into release runtime. No required notice omission was found from the available metadata; this is an engineering inventory, not legal advice.

## 7. Performance, memory, and storage

- Five signed-release cold launches on the physical target: 368–447 ms, median 401 ms.
- Continue and ordinary UI navigation were visually immediate; the early End Day transition completed within the first sub-second observation interval.
- The full 100,706-day host regression completed in 18.46 seconds (270.87 seasons/second, approximately 5,455 simulated days/second), including early/mid/late seasons and bounded Chronicle history. No progressive End Day slowdown appeared.
- Ten background/foreground cycles retained the same PID and controller. Total PSS changed from 97,369 KB to 89,602 KB; total RSS from 241,096 KB to 236,156 KB. This is not a leak signature.
- Device completion/reset instrumentation, including an autonomous 22-day season, completed in 1.157 seconds of test time.
- Installable private APK: 74,066,884 bytes (70.64 MiB).
- Unsigned APK: 74,049,145 bytes; AAB: 73,112,017 bytes.
- Packaged audio: 62,999,610 bytes (60.08 MiB); packaged raster visuals: 2,544,847 bytes (2.43 MiB).
- Observed active save: 23,807 bytes; completed-season record: 182,498 bytes; local settings: 176 bytes.
- Chronicle history is bounded to 40 days; no save or memory growth issue was observed.

## 8. Release artifacts and signing

| Artifact | Size | SHA-256 | Status |
|---|---:|---|---|
| `release/LegendOfTheGlassDragon-1.0.0-rc1-private-test.apk` | 74,066,884 | `A9B2EA34A7F1081AFA170C24ABF398BC531C8CFEE7D23ACBF360C77539958F69` | Installable private qualification APK; Android debug certificate; v2/v3 verifies |
| `release/LegendOfTheGlassDragon-1.0.0-rc1-unsigned.apk` | 74,049,145 | `AB26BBE87819CAADB28E56A18EBE4CD499D47115B1EBC1AE49969E3E64F14273` | Unsigned production-signing input |
| `release/LegendOfTheGlassDragon-1.0.0-rc1.aab` | 73,112,017 | `8F13386D68279BA35471490D26FB566EC5A9C96658165CA23D1B5DED6207ACB1` | Additional unsigned bundle |

No authorized production keystore was available. No key or password was created, copied into source, or recorded. Public signing therefore remains a Principal-controlled step. The actual private-test APK was installed after the final build; Android reported version 12 / `1.0.0-rc1`, the correct label, no `DEBUGGABLE` flag, and the exact 74,066,884-byte installed base APK.

## 9. Findings, repairs, and closure

LGD-012 made only bounded release-readiness changes:

1. advanced the wrapper from development `0.10.0` to `1.0.0-rc1` / versionCode 12;
2. added exact soundtrack source/mark attribution and packaged Apache-2.0 notices;
3. moved the notice from an inappropriate raw resource to assets after lint identified it as unused;
4. added physical-device production repository/state-machine tests for combat, Chronicle/Dawn, Dragon win/loss, NPC season completion, archive, reset, and settings persistence.

No combat, progression, economy, NPC, Dragon, campaign timing, Chronicle selection, or callback value changed. No new feature, audio, network service, analytics, or gameplay scope was introduced.

P0 release blockers: none for private/local installation.  
P1 release defects: none remain for private/local installation.

Public-distribution gates:

- **DG-01:** the click recording/derivative has unclear redistribution provenance.
- **DG-02:** no production signing key/signature was available; the installable APK is deliberately marked `private-test` and debug-signed.
- **DG-03:** the music set has strong matching PDM evidence and attribution, but final distribution/legal acceptance remains the Principal's decision.

Accepted P2 observations carried forward unchanged:

- OBS-01: Casual synthetic players attempt the Dragon in 16% of seasons because the autonomous world often resolves first.
- OBS-02: Chronicle median is 5, with many days concentrated around five or six items.
- OBS-03: Aggressive and Reckless synthetic Dragon outcomes remain somewhat similar.

Machine-readable regression evidence is under `docs/reports/evidence/LGD-012/regression-5000/`. Device UI dumps are under `docs/reports/evidence/LGD-012/`; screenshots are listed above. Music import hashes/mapping remain in `docs/assets/LGD-007_AUDIO_IMPORT.md`, with the release provenance inventory in `docs/assets/LGD-010_MUSIC_PROVENANCE.md`.

## Final recommendation

**LGD012_MVP_PRIVATE_RELEASE_READY**

The MVP is installable, offline, lifecycle-safe, save-safe, campaign-complete, deterministic, and consistent with the frozen LGD-011B balance. Private/local distribution may use the explicitly debug-signed `private-test` APK. Public distribution must wait for click-asset clearance and authorized production signing; no gameplay work is required to close those gates.
