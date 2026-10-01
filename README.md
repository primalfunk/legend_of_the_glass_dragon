# Legend of the Glass Dragon

This repository is the clean-slate native Android generation of **Legend of the Glass Dragon**, a touch-operated ANSI/BBS-style fantasy door game from Wingtip Studio.

The application is built with Kotlin and Jetpack Compose and is designed for local, offline play. The previous web implementation is reference-only: it is not an architectural dependency, no source has been migrated from it, and this application does not use a web runtime.

The application namespace is `com.legendoftheglassdragon`.

The current `1.0.0-rc1` build includes the complete combat/progression/day loop, persistent simulated community, Chronicle/Dawn transitions, finite Glass Dragon season, and product wrapper: state-sensitive Main Menu, safe New Game/Continue flows, completed Season Record archive, persistent audio settings, and production Android icon/splash presentation. The accepted LGD-011B balance baseline is preserved by the development-only deterministic whole-campaign harness. LGD-012 qualifies the MVP for private/local Android installation; public redistribution still requires click-audio provenance clearance and authorized production signing.

Governing documents and implementation notes are indexed in [`docs/README.md`](docs/README.md).

## Local build

Use Android Studio's bundled JDK, then run:

```text
./gradlew test lint assembleDebug assembleRelease bundleRelease connectedDebugAndroidTest
```

The debug APK is produced at `app/build/outputs/apk/debug/app-debug.apk`. Release-candidate artifacts and their signing/provenance status are documented in [`docs/reports/LGD-012_RELEASE_QUALIFICATION_REPORT.md`](docs/reports/LGD-012_RELEASE_QUALIFICATION_REPORT.md).
