# 2.9.0 feature pack — delivery verification

## Delivered scope
- Infinite-style virtual app carousel, center zoom and transparent edge fades. Real installed app components remain the data source; no demo shortcuts are bundled. Move remains bounded/persistent, Hide is retained. iCal is excluded.
- Separate opt-in Open-Meteo current/model +3h/+6h widget with manual coordinate setup, bounded transport, validation, cache and foreground-only scheduling. MET continues to own rain and automatic wallpaper. No user's coordinates/key are in this delivery.
- Approximately 800 ms weather-driven wallpaper dissolve, no focus-triggered changes, latest pending loader, bounded bitmap resolution and reduced-motion/lifecycle handling.
- Explicit opt-in 35% cross-app overlay via a non-interactive specialUse foreground service, OFF notification, permission handling and persisted desired state. Protected/OEM surfaces may exclude overlays.

## Actual checks
- JDK 17, Gradle 8.9, Android SDK/build-tools 34/34.0.0. Serial builds only.
- Final clean build/test invocation: **BUILD SUCCESSFUL**.
- **334 tests: 333 passed, zero failures, zero errors, one skipped**.
- Skipped optional existing test: `com.gathin.tvhome.MetLiveVerificationTest.liveRepositoryParsesPaparCachesAndDisplaysKinarut`. It was not represented as a live MET pass.
- New feature suite: 26 tests. Includes first→last carousel wrap with real launch component mapping, center scale, dissolve pixels/queue/same-key/reduced-motion/pause/destroy, hourly parser/day rollover/freshness/invalid data, no unconfigured fetch, offline cache, dim permission/window flags/OFF notification, appended Settings indices, unchanged focus-independent wallpaper and labelled previews.
- The initial regression run exposed lost Move focus after its dialog dismissed; this was corrected by restoring the moving component key. Existing carousel tests now assert logical app identity across virtual positions instead of treating child 0 as the selected app. Existing menu index 0–8 tests remain, with two entries appended.
- Native Robolectric captures at **1280×720, 1920×1080 and 3840×2160** were rendered and visually inspected. Exported previews are explicitly labelled simulation; app icons, schedule and weather are fixtures. These are NOT screenshots of a physical TV or emulator, NOT live weather, and NOT evidence of audible azan or actual installed apps on the user's device.
- Open-Meteo HTTPS endpoint was separately requested with **0,0 synthetic public coordinates**, not a user's location. Current/hourly unit schema and 48 hourly slots were returned. A recorded response is included only in test resources and cannot be used as production fallback. No live Groq credential or real TV speed test was used.
- Actual APK DEX audit: **373 application class definitions; all 13 manifest components present**. `tools/verify-apk.py` checks class definitions rather than string references.
- APK signature verified: v2, debug signer. Package/version checked in the built APK: `dev.farred.fayyadhos.tv.jarvis.fix`, `2.9.0-feature-pack`, code `13`, min SDK 26, target SDK 34.
- APK SHA-256: `a4e7da537043858b5b5d853ebb24b0079546e6dfbe3f5f9a7fc631c5257d1238`.
- Sixteen protected modules (including Jarvis AI, SpeedProbe, prayer, azan, MET, rain and welcome speech) compare byte-for-byte with the supplied baseline; hashes are in `protected-modules.json`.

## Lint — not clean
The fresh lint run reports **45 errors and 82 warnings**. All errors are the existing **44 MissingTranslation + 1 ByteOrderMark** findings. Both error-bearing files (`strings.xml` and `Config.java`) are byte-identical to the uploaded source. They were not suppressed or silently fixed. New warnings include synchronous durable Dim preference commits, a small draw allocation in the dissolve view, and a redundant minimum-SDK guard. Debug compilation/test success is not a claim of lint or production-release readiness.

## Remaining acceptance / limitations
- No physical Android TV or emulator install/start/Home acceptance was performed for this feature pack. Historical 2.8.1 emulator evidence in other folders is not a test of 2.9.0.
- Validate genuine TV D-pad/long-press/rapid wrap/Move/Hide, installed icons, overscan and accessibility on the target firmware.
- Validate permission UI, Android 13 notification consent, Android 14 foreground service, Dim while another app runs, notification OFF, permission revocation and restart on actual hardware. No claim of panel power savings, guaranteed boot startup or coverage of protected screens.
- Weather conditions are modelled; sources may disagree. Open-Meteo coordinates and MET location are intentionally independent. Public free API terms are non-commercial; review the provider's terms before commercial deployment.
- Validate real JAKIM/azan timing, audio coexistence and standby on the device. Old module regression tests do not substitute for physical testing.
- No production-signed/R8 release was built. A compatible update requires the installed signing key. Do not uninstall/clear a working launcher or disable stock Home to bypass signing mismatch.
- No GitHub remote was supplied or changed; **nothing was pushed**. Complete source ZIP is the primary deliverable. The source patch contains code/docs/compact results; full logs/XML/previews are in this source ZIP, not duplicated into the patch.

Read `START-HERE-FEATURE-PACK.md` for settings and repo import instructions. Historical files are retained as history, not as freshly performed checks.
