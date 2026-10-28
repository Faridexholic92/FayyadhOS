# FayyadhOS TV — Rain-on-Glass implementation report

## Delivered scope
An isolated, optional rain-on-glass feature has been added to the supplied redesigned **FayyadhOS-TV-Master** project, retaining its existing native Java / Android View architecture. The home screen has not been redesigned again. No Kotlin plugin, graphics engine, runtime dependency, Android permission, application ID, SDK level, signing configuration or build variant was changed.

A checkpoint was retained at `FayyadhOS-before-rain` in the build workspace before editing. The supplied archive had no Git repository. A file-hash audit found **only one existing project file modified: DashboardActivity.java**; no existing files were deleted. All other changes are feature-specific additions and verification evidence. Existing wallpaper assets, cards, clock/date code, prayer provider/panel, app catalog/order/hide/launch logic, Jarvis logic, manifest and Gradle configuration are unchanged.

## Files modified or created
All code paths are relative to the project root.

| File | Purpose |
| --- | --- |
| `app/src/main/java/com/gathin/tvhome/DashboardActivity.java` (modified) | Minimal lifecycle/screen callbacks, one appended Rain-on-Glass Settings entry, and suspension while modal menus obscure Home. Existing Settings indices and home layout retained. |
| `app/src/main/java/com/gathin/tvhome/WeatherForecast.java` | Validated dated/located forecasts, conservative description mapping, MYT period selection and 24-hour freshness. |
| `app/src/main/java/com/gathin/tvhome/MetWeatherRepository.java` | Official bounded HTTPS requests and atomic single-location disk cache. |
| `app/src/main/java/com/gathin/tvhome/RainOptions.java` | Isolated SharedPreferences settings, safe defaults and quality clamps. |
| `app/src/main/java/com/gathin/tvhome/RainController.java` | Foreground/screen-aware coordination, asynchronous cache/network work, duplicate prevention, cooldown/retry and state deduplication. |
| `app/src/main/java/com/gathin/tvhome/RainGlassView.java` | Transparent Canvas droplets/trails above the existing UI, bounded motion, fades and adaptive quality. |
| `app/src/main/java/com/gathin/tvhome/RainSettingsDialog.java` | Small D-pad-friendly configuration dialogs using existing dialog styling. |
| `app/src/main/res/values/rain_strings.xml` | Feature labels consistent with the existing English settings. |
| `app/src/main/assets/met-malaysia-locations.json` | 440 official supported locations from a verified dated data.gov.my forecast response; no location selected automatically. |
| `app/src/test/java/com/gathin/tvhome/WeatherForecastTest.java` | Classification, periods, freshness, malformed/missing data, filtering and rate-limit parsing tests. |
| `app/src/test/java/com/gathin/tvhome/RainFeatureTest.java` | Weather states, cache/offline/error/429, preview, lifecycle, screen-off, settings, navigation/launch intent, adaptive quality, SDK 26 and render tests. |
| `app/src/test/java/com/gathin/tvhome/RainPerformanceTest.java` | Host software-Canvas measurements, sprite budget, static-mode and inactive callback checks. |
| `docs/RAIN-ON-GLASS-REPORT.md`, `verification/rain/` | This report, source audit, logs, native-view screenshots, API responses and test results. Older redesign verification remains separate. |

## Official source and actual fields
Sources consulted and endpoint requests verified without authentication:
- Weather documentation: https://developer.data.gov.my/realtime-api/weather
- Query syntax: https://developer.data.gov.my/request-query
- Limits: https://developer.data.gov.my/rate-limit
- Attribution/licensing: https://developer.data.gov.my/faq

Provider: **MET Malaysia via data.gov.my**, CC BY 4.0. Forecast data is documented as updated daily; the Weather API limit is four requests/minute. HTTP 200 responses, nested location fields and exact filters were verified. Raw response evidence is included under `verification/rain/official-*.json`.

Runtime endpoint:

```text
https://api.data.gov.my/weather/forecast?filter=Ds554%40location__location_id&include=location,date,morning_forecast,afternoon_forecast,night_forecast&sort=date&limit=14
```

`Ds554` above is only an example of an explicitly selected ID, not a default. The runtime query encodes the selected ID. Used fields: `location.location_id`, `date`, `morning_forecast`, `afternoon_forecast`, `night_forecast`. The location name shown in configuration comes from the official bundled catalogue. No min/max temperature, daily summary, warning, inferred intensity, GPS coordinate or rainfall observation is used.

The endpoint does **not** supply an instantaneous rainfall observation or a forecast issuance timestamp. `fetched_at` is the app's retrieval time, not a MET-issued timestamp. **Activation is forecast-driven, never confirmed current rainfall.** The warning endpoint is not requested or treated as proof of rain. No API key or secret is embedded.

Catalogue provenance: verified query for forecast date 2026-10-24 with `include=location&limit=1000` returned 440 locations. State, district, town, division and recreation-centre selectors preserve the official IDs. Kota Kinabalu district/town and Taman Nasional Kinabalu are separate choices. Wallpaper and prayer zone do not silently select weather location. The catalogue is a bundled snapshot; future provider location changes may require an asset update.

## Mapping and period policy
Descriptions are case/whitespace-normalized before classification:

| Description | State | Visual behavior |
| --- | --- | --- |
| `Tiada hujan` | CLEAR | Fade off. |
| `Berawan`, `Mendung`, `Berjerebu` | CLOUDY / non-rainy haze | Off; no condensation feature is enabled. |
| `Hujan` and recognized isolated/scattered/coastal/inland variants | RAIN_EXPECTED | Rain only if enabled and forecast activation is permitted. |
| `Ribut petir` and recognized variants | THUNDERSTORM_EXPECTED | Same restrained glass effect; no flashes or invented meteorological intensity. |
| Unrecognized, missing, malformed, wrong-location or stale data | UNKNOWN | No invented weather; last valid schedule used only while fresh. Otherwise fade off. |

The live API also returned `Ribut petir di kebanyakan tempat` and `Ribut petir menyeluruh`; these variants and corresponding rain variants are handled explicitly. Unknown conditional text is not treated as rain.

Application-defined MYT (`Asia/Kuala_Lumpur`) windows: morning 06:00–12:00, afternoon 12:00–19:00, night 19:00–06:00. Before 06:00, the previous date's night forecast is required; if unavailable, UNKNOWN. These cutoffs are an explicit app policy, **not exact period timestamps supplied by MET**. Daily summary text is not used to infer the current period.

## Configuration and preview
1. Open **Settings > Rain-on-Glass · Weather**.
2. Switch **Enable Rain-on-Glass** ON (default OFF).
3. Select an explicit MET Malaysia location.
4. Enable **Activate for period forecast** (default OFF) to allow forecast-driven rain.
5. For a weather-independent visual test, use **Manual preview (not weather)** after enabling the feature. Preview does not require a location or weather request.

Visual intensity, density, speed and moving droplets are adjustable and persisted in the isolated `rain-on-glass` preferences. Defaults: low visual intensity, 48 droplets, slow movement, two moving droplets. Density choices are 48/72/96; visual intensity is a cosmetic choice, not reported rainfall intensity. Preview is intentionally session-only and clears on background/screen-off. Turning preview off restores normal forecast behavior. Disabling the feature stops it immediately.

## Rendering and performance safeguards
- Uses public `ViewGroupOverlay` on the existing launcher root: above wallpaper, clock, app cards and prayer UI, but absent from focus/input traversal. No separate system window or overlay permission.
- Transparent custom Canvas view; small lens-edge sprites, highlights, a handful of larger droplets, up to three slowly sliding drops, short trails and occasional bounded merging. The image is not rain falling inside the landscape.
- No wallpaper processing, full-screen blur, fluid simulation, full-screen refraction, lightning or new rendering engine. Optional local refraction is deliberately omitted for performance.
- Maximum 96 droplets / three moving drops. Fixed position arrays and reused Paint/RectF/sprites; application drawing code allocates no new objects in its per-frame path. This does not claim the Android/framework bridge makes zero allocations.
- Three shared sprites total **122,880 bytes (120 KiB)** when active. Sprites are released when faded out, disabled, obscured, backgrounded or destroyed; no full-screen bitmap is allocated by the feature.
- Default animation interval 50 ms (20 updates/sec), only while active. Weather changes fade over about 700 ms. Static mode stops scheduling once faded in. Disabled/clear/stale/inactive states do not maintain an animation loop.
- Repeated expensive CPU draw callbacks or scheduling lateness reduce motion to one drop / 100 ms interval, then mostly static droplets. This is a CPU/lateness heuristic, not a GPU frame-timing monitor.
- Existing menus suspend the cosmetic overlay while obscuring Home. Root layout, system bars and navigation remain as before.

## Networking/cache/lifecycle
Weather work runs separately from rendering on a bounded single worker with at most one request in flight and an idle thread timeout. No foreground service, background polling service or wake lock was added.

- HTTPS connection/read timeouts: 8 seconds each; response/cache reads limited to 64 KiB; redirects disabled.
- Exact location filtering and 14-record cap. Invalid bodies are not allowed to overwrite valid cached data.
- Atomic one-location cache: `files/met-rain-forecast.json`, storing rows, location ID and retrieval timestamp.
- Refresh target: six hours while Home is active. Cache validity: strictly less than 24 hours, matching location and requested forecast date/period. Future retrieval timestamps after a clock correction are not considered fresh.
- Global persisted request gate: at most one request/minute, including manual/location changes. Routine traffic is much lower (six-hour refresh).
- Offline check: no HTTP request; next check after 30 minutes while foregrounded. Normal errors retry after 15 minutes. HTTP 429 respects parsed Retry-After within a 1-minute–24-hour safety bound. Manual refresh does not bypass cooldown.
- On failure, preserve last valid data only within its freshness limit; expiration switches to UNKNOWN/off. Status text is retained during cooldown.
- Period/cache-expiry/request deadlines are lifecycle-aware Handler events, independent of rendering. No repeated UI invalidation when the relevant effect/settings state is unchanged.
- Backgrounding/screen-off cancels scheduled weather wake-ups and rendering. An already-started bounded worker/network operation may finish/cache its result; no subsequent polling starts while backgrounded. Late results do not restart hidden animation. Destruction removes listeners/callbacks and shuts down resources.

## Verified build/tests
Build environment: JDK 17, Gradle 8.9 / AGP 8.5.2, Android platform/build-tools 34. Existing min SDK 26 / target SDK 34 are unchanged.

```bash
bash gradlew :app:assembleDebug :app:testDebugUnitTest :app:compileReleaseSources --console=plain
bash gradlew :app:lintDebug --console=plain
```

- Debug APK: **BUILD SUCCESSFUL**; APK signature verified using the debug certificate.
- Release **source compilation**: passed. A signed/minified production release APK was not generated because original release signing credentials were not supplied.
- **172 tests passed, 0 failures, 0 errors, 0 skipped**, including all 137 previous regression tests and 35 weather/rain/measurement tests.
- Tests cover clear/cloudy/rain/storm, unavailable network, invalid/incomplete responses, stale/future cache, disabled state, background/resume, screen-off, preview restoration, active settings, rate limiting, remote focus and outgoing app-launch intent through rain. A native-view test confirms the original home pixels are identical after preview fades out. The Settings handoff/preview flow and adaptive fallback were verified.
- Native Android view preview renders at 1280×720 and 1920×1080 were inspected. They are **Robolectric renders, not hardware screenshots**. Generic app icons are test-only app registrations; prayer data remains unavailable until a real zone/schedule is configured. Preview images are not live weather reports.
- Lint still reports **45 errors / 51 warnings**, identical in count to the supplied redesigned project. All 45 error identities/messages match the previous report: 44 existing MissingTranslation errors and the existing ByteOrderMark in unchanged Config.java. No new lint errors/warnings were introduced. Lint was not disabled or masked with a new baseline.

## Measured host-side diagnostics — not TV benchmarks
The following are 60 warm software-Canvas draws per mode at 1280×720 in Robolectric on this Linux host. CPU is current-thread CPU; allocations include framework/measurement overhead. GPU timing, jank and app-startup timing on a TV were unavailable.

| Mode | Whole-home wall ms/draw | Thread CPU ms/draw | Overlay median ms | Overlay p95 ms | Java bytes/draw incl. framework |
| --- | --- | --- | --- | --- | --- |
| disabled_baseline | 20.963 | 20.736 | 0.000 | 0.000 | 1585.9 |
| 48_drops_default_preview | 18.792 | 18.782 | 0.363 | 0.487 | 107.7 |
| 96_drops_static_preview | 16.775 | 16.775 | 0.385 | 0.557 | 90.7 |

**Do not interpret the lower whole-home numbers after activation as a speed improvement.** Sequential warm-up/JIT/cache effects make the before/after totals unsuitable for proving launcher overhead or a frame-rate claim. Raw data is retained in `verification/rain/rain-performance.json`. The isolated overlay timing is also host software drawing, not GPU cost.

Verified resource/lifecycle checks: active sprite storage 120 KiB; zero sprite bytes after disable; storage unchanged across 120 simulated updates/draws; zero additional rain callbacks over five inactive seconds; static mode stops scheduling. These checks do not establish long-session process-wide memory behavior on a physical TV.

## APKs and remaining limitations
Generated APK path, relative to this project:

```text
app/build/outputs/apk/debug/app-debug.apk
```

The downloadable APK ZIP contains `FayyadhOS-TV-Rain-on-Glass-debug.apk`, copied from that output. Package remains `dev.farred.fayyadhos.tv.jarvis`, versionCode 10 / versionName 2.8-master.

**Debug signing warning:** this is not signed with the original production certificate, and this build environment may also have a different debug key from earlier test APKs. It cannot update a differently signed installed launcher. Do not uninstall a working launcher to bypass signature errors. Rebuild/sign with the original release credentials for a production update; signing configuration is unchanged and no private key is included.

No Android TV/emulator was attached (`adb devices` empty). Actual TV CPU/GPU, jank, long-session memory, physical remote, app-start latency, real third-party launch, firmware standby and production certificate update remain unverified. The effect has therefore passed source/build/software tests, **not a claim of zero performance impact or final hardware acceptance**. Test on the target box with default low quality, verify navigation/startup responsiveness, and disable/reduce motion if needed. Live endpoint/schema verification does not prove the weather at the user's location.
