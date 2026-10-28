> **Maintenance update: 2.9.1-stability / code 14.** Read `docs/STABILITY-2.9.1.md` for current build/CI instructions. The feature behavior below is preserved; historical 2.9.0 validation is not new 2.9.1 evidence.

# FayyadhOS TV Fix — 2.9.0 feature pack / code 13

This document supersedes older feature descriptions in this archive for the four changes below.
Based on the supplied **FayyadhOS-TV-Fix-2.8.1-source.zip**, not on a replacement launcher.
iCal/calendar was deliberately excluded. No debloating or automatic third-party cache cleaning was added.

## Included

1. **Looping carousel**: repeated virtual positions map to the original installed-app catalog. Selected center approximately 120%, neighboring cards decrease in scale, both edges fade into the wallpaper. Left from the first app reaches the last app in one step. Labels, real icons, Hide, App info and persisted Move remain. Move uses a bounded real list (no wrapping while rearranging). On return Home, the last component key is restored.
2. **Now / +3h / +6h weather**: a new, independent Open-Meteo display below the existing MET widget. Current conditions are **modelled**, not an on-site observation. Future columns use the MYT hourly slot containing now+3h / now+6h. Provider attribution remains visible. No location/key is bundled; default is OFF/unconfigured. Settings → Cuaca · Sekarang / +3j / +6j → enter latitude and longitude → Simpan & ON. Coordinates are sent to api.open-meteo.com only when enabled and Home is active. No GPS, geocoding, IP-location guess or new location permission. Existing MET location is NOT automatically converted to guessed coordinates.
3. **Weather wallpaper dissolve**: keep the existing MET-driven scene selection and manual wallpaper option. On a changed scene, cross-fade for approximately 800 ms. No switching triggered by app focus. Same key does not decode/animate again. Rapid changes retain only the latest pending scene and decode it after the active fade. First image is immediate; reduced motion/system animation scale 0 is immediate. Leaving Home or screen-off finishes the pending state without continued frames. Bitmaps are sampled to at most 1920×1080; this intentionally trades 4K source fidelity for bounded transition memory.
4. **35% screen dim**: Settings → Dim 35%: OFF/ON. Explicit overlay permission and Android 13+ notification permission are required. A user-started foreground service retains the non-focusable/non-touchable overlay while other apps run, with an OFF notification. Turning OFF stops the service and removes the overlay. Permission revocation stops it. Desired state is restored when Home next opens; this is NOT a promise of automatic boot/force-stop recovery. The service uses Android 14's `specialUse` foreground type and declares its purpose. Play Store distribution would require the applicable foreground-service declaration/review. Protected/system/OEM surfaces may block overlays. This is NOT panel brightness, backlight control, or a proven power-saving feature.

## Weather sources intentionally stay separate

- **MET Malaysia / data.gov.my** still drives existing rain effects, the existing forecast widget and automatic Kinabalu wallpaper. Open-Meteo does not override them. Different forecast providers can disagree; they are labelled separately.
- Open-Meteo: HTTPS only, 8 s connect/read timeouts with an elapsed request guard, max 256 KiB JSON response; strict unit/time/array/numeric/WMO validation; atomic location-bound cache. Refresh every 30 minutes while Home is active; re-evaluate hourly slots every minute. Cache display validity is 2 hours. Retry failures after 15 minutes; HTTP 429 respects bounded Retry-After. No background weather job/service was added. In-flight I/O may finish after Home is left, but no polling/animation continues.
- Open-Meteo data attribution: https://open-meteo.com/ and https://creativecommons.org/licenses/by/4.0/ . Review https://open-meteo.com/en/terms before commercial deployment: the public free API is for non-commercial use; commercial service access may require an appropriate subscription/endpoint change. No commercial API key is included.
- Do not use this display as a warning system or rely on a forecast as confirmation of actual rainfall.

## Safe installation

- Application ID **remains** `dev.farred.fayyadhos.tv.jarvis.fix`, min SDK 26, target/compile SDK 34.
- Version `2.9.0-feature-pack`, versionCode `13`. The old non-Fix package is NOT migrated.
- Supplied APK, if included, is a **debug/test build**, not a production-signed release. This environment's debug certificate may differ from your installed 2.8.1 certificate. DO NOT uninstall a working launcher, clear its data, disable the stock launcher, or reset the TV to bypass a signature mismatch. A data-preserving update needs the installed certificate through your original signing process.
- Test Open first, navigation/app launching, weather setup, dim ON/OFF and notification OFF. Only then consider default Home. Keep the stock launcher enabled as recovery.
- Dim is OFF initially. Azan keeps its existing opt-in behavior; no audio/key is included. Old preferences are not intentionally erased. Startup greeting, Jarvis AI, speed test, JAKIM and azan modules are retained.
- Physical-TV standby, firmware overlay permission UI, genuine installed-app focus, audio coexistence and real forecast accuracy still require acceptance testing. JVM/Robolectric UI fixtures are not physical-TV evidence.

## Put into your repository

The complete source ZIP is the primary deliverable. The accompanying patch applies against the exact supplied 2.8.1 source tree, not an arbitrary earlier branch.

```sh
git checkout -b feature/tv-polish-2.9.0
# Option A: copy the contents of FayyadhOS-TV/ from the ZIP over your matching project.
# Option B: from the matching 2.8.1 project root:
git apply --check /path/to/FayyadhOS-2.9.0-feature-pack.patch
git apply /path/to/FayyadhOS-2.9.0-feature-pack.patch
git diff --check
git status --short
```

Do not add signing keys, passwords, local.properties, private calendar/location config, build caches or user data. No GitHub remote was supplied or modified; nothing was pushed automatically.

## Build and test

JDK 17, Gradle 8.9, Android SDK 34 / build-tools 34.0.0. Never overlap Gradle invocations in the same output directory.

```sh
# Optional: point at your existing stable DEBUG keystore, not a newly substituted release key.
# export FAYYADHOS_DEBUG_KEYSTORE=/secure/path/to/debug.keystore
bash build-verified-debug.sh
```

Original release signing environment variables remain in app/build.gradle. No keystore is included in the ZIP.
See **verification/feature-pack/RESULTS.json** and **REPORT.md** for actual checks in this delivery. Older verification folders describe historical builds, not newly performed checks.
