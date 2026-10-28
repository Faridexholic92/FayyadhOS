# Transparent weather widget — implementation and verification

## Outcome

Implemented a passive, transparent home-screen forecast below Jarvis/Settings. No weather card, panel, border, blur, elevation or background drawable is added. White text and subtle shadows match the existing dashboard; a small cyan condition icon sits on the left. The existing wallpaper and all other dashboard coordinates/styles remain unchanged.

**Final result: debug APK build successful; 181 tests passed, 0 failures, 0 errors, 0 skipped.** Final evidence is under `verification/weather/`, especially `build-and-tests.log`, `test-summary.json` and `test-results/`.

## Inspection before editing

The source already contained `WeatherForecast`, `MetWeatherRepository`, `RainController`, `RainOptions`, a MET location catalogue and rain-effect settings/tests. This integration was used only for the Rain-on-Glass effect; it did not display weather text on Home. The existing repository already provided HTTPS timeouts, a 64 KiB response cap, an atomic disk cache, six-hour refresh, 24-hour expiry, offline/retry handling and HTTP 429 backoff.

The actual home screen is built programmatically in `DashboardActivity.java`; `activity_main.xml` is not its layout. No second weather service, network library, timer, executor or API integration was introduced.

## Data correctness and configuration

- Official documentation checked: https://developer.data.gov.my/realtime-api/weather
- Both the complete and exact projected production query were requested successfully for Sabah (`St502`), returning seven actual daily forecast rows. Raw responses are retained in `verification/weather/`.
- This endpoint supplies daily forecasts and `min_temp`/`max_temp` daily extrema. **It does not supply instantaneous current temperature or current observations.**
- The display therefore says **Ramalan** and shows an available daily range, not a purported current temperature. This is an intentional, evidence-based limitation relative to the request for current weather.
- Sabah is the widget fallback configuration, not device geolocation. An existing location selected in the Rain-on-Glass weather settings is reused and its name is displayed accurately. No saved rain location, prayer zone, wallpaper or app preference is overwritten.
- Forecast text is the API's Bahasa Melayu period description. Longer descriptions ellipsize visually; the full description is available in the content description.
- The existing application-specific MYT period policy is preserved: morning 06:00–12:00, afternoon 12:00–19:00, night 19:00–06:00; before 06:00, the description uses the previous date's night. The displayed daily temperature range is for today's MYT calendar date. These period cutoffs are app policy, not observation timestamps.
- Missing, malformed, non-numeric, implausible or reversed temperature ranges are omitted. Legacy caches without temperature fields remain readable. Missing/expired/unmatched forecast data displays `Ramalan tidak tersedia`, with no invented temperature or condition.
- Fresh cached data remains usable after an offline/network failure and is marked `cache` in the condition text when the controller has an error. Expired and future-dated cache values are not presented as valid weather.
- Rain animation remains independently controlled by its original settings and defaults off. Attaching the Home widget enables foreground forecast scheduling without silently enabling rain or selecting a rain location.

## Every source/test file changed

All paths below are relative to the project root. These are the only nine changes under `app/src/`.

| File | Reason |
| --- | --- |
| `app/src/main/java/com/gathin/tvhome/DashboardActivity.java` | Add the passive widget at x=650, y=82, width=270, height=42 in the existing 960×540 design space; attach it to the existing controller. |
| `app/src/main/java/com/gathin/tvhome/WeatherWidget.java` (new) | Two-line background-free layout; white shadowed text, static cyan condition drawable, change-only text updates, accessibility description, and no click/focus target. |
| `app/src/main/java/com/gathin/tvhome/WeatherForecast.java` | Parse optional validated daily temperature ranges while retaining existing rain/period semantics and old-cache support. |
| `app/src/main/java/com/gathin/tvhome/MetWeatherRepository.java` | Include `min_temp,max_temp` in the existing bounded request. Endpoint, cache and transport are reused. |
| `app/src/main/java/com/gathin/tvhome/RainController.java` | Share the existing worker, cache and refresh lifecycle with the attached widget; use Sabah when there is no chosen weather location; retain rain-specific enable/preview behavior. Explain the distinction in status. |
| `app/src/test/java/com/gathin/tvhome/RainFeatureTest.java` | Update two scheduling expectations for the newly attached independent Home forecast; wait for its asynchronous worker. Rain-frame and other regression assertions remain. |
| `app/src/test/java/com/gathin/tvhome/WeatherWidgetTest.java` (new) | Nine tests covering rendered transparency/geometry at three resolutions, actual recorded API parsing, invalid/absent temperatures, missing/expired/mismatched data, fetch reuse/lifecycle, offline/failures and change-only updates. Simulated scenarios reset cooldown preferences so they are independent. |
| `app/src/test/resources/met-sabah-2026-10-25.json` (new) | Actual official API response used only as a test fixture; never a production fallback. |
| `app/src/test/resources/API-FIXTURE-PROVENANCE.txt` | Record retrieval URL/date, forecast interpretation and attribution for the new fixture. |

Build files, manifest, dependencies, wallpaper assets, prayer implementation, app order, clock/greeting code and navigation code were not changed. Structural LinearLayouts position the icon/text; they have no visible surface. No Notion workspace content was edited.

## Build and tests actually performed

Environment: Linux x86_64; Amazon Corretto JDK 17; Gradle 8.9; Android Gradle Plugin 8.5.2; Android SDK platform/build-tools 34. SDK/tool downloads were outside the delivered project; no SDK path or environment-specific build edit was added.

Equivalent reproducible command with JDK 17 and the Android SDK configured:

```sh
./gradlew --no-daemon :app:assembleDebug :app:testDebugUnitTest
```

The sandbox ran the same Gradle 8.9 distribution directly. Final result was `BUILD SUCCESSFUL in 1m 4s`. XML results total **181 tests, zero failures/errors/skips**, including nine new widget tests and the existing launcher, remote navigation, application launch, prayer, voice, personalization and rain regression suites. Android 26 and 34 are exercised by the existing suite; native-graphics widget render assertions run on Robolectric Android 34.

The initial integration run built the APK but failed two obsolete “no weather wake while rain disabled” assertions. They were updated for the intended independent forecast. An intermediate run exposed shared cooldown preferences between simulated failure scenarios in the new test; the helper now isolates those scenarios. Both diagnostic logs are retained separately. The final passing log/XML, not the earlier logs or the archive's historical verification files, define the result.

`git diff --check` passed. `apksigner verify --verbose --print-certs` passed with APK Signature Scheme v2 and the Android debug signer. Package ID remains `dev.farred.fayyadhos.tv.jarvis`, version `2.8-master` / code 10, minSdk 26, targetSdk 34.

APK SHA-256:

```text
c1b556f4ffc86319bab449b364ca3af98302be5bda424d5bfc19d541007b134f
```

## UI verification and performance scope

- Native Robolectric render captures at 1280×720, 1920×1080 and 3840×2160 were generated. Automated bounds assertions check no overlap with Jarvis, Settings, clock/date or greeting and that both text rows fit.
- Recursively asserted null backgrounds/foregrounds, zero elevation and non-focusable/non-clickable weather views; compared a widget-corner pixel with the underlying wallpaper.
- Visually inspected the 720p and 1080p Kinabalu captures plus white/black wallpaper captures: icon and text float over the wallpaper without a weather box. The 4K layout was rendered and passed geometry assertions; it was not separately inspected on a 4K television.
- Captures use test-only registered apps with generic Android icons and the recorded forecast fixture. Prayer remains deliberately unconfigured in weather captures; the weather feature does not select a prayer zone or invent prayer values. Captured clock/greeting reflect the test host, not a user-TV screenshot.
- Network requests and JSON parsing remain on the existing single background worker. Refresh is six hours while Home is active, with period-boundary/cache-expiry wakes and existing offline/retry delays; no request is triggered by drawing. Background/screen-off suspend scheduling. Fifteen repeated layout/render calls were tested without increasing the fake network request count.
- No weather animation, bitmap, blur, continuous polling, heavy dependency or repeated layout inflation was added. Unchanged forecast updates were tested not to request text layout. This is structural/test evidence, **not** a device CPU/GPU/FPS/memory/power benchmark. No physical-device performance claim is made.

## Remaining errors and unverified work

- No remaining compile errors or failing tests in the final debug build/test run. The compiler reports existing use of deprecated APIs; unrelated code was not rewritten to suppress those notes. Lint was not rerun in this task; historical lint artifacts in the source archive are not new validation.
- No physical Android TV, hardware remote, on-device network/TLS/offline transition, OEM font scaling, long-duration cache behavior or actual TV rendering/performance was tested. Robolectric is not a substitute for that acceptance pass.
- Current observations/current temperature are not available from the verified provider endpoint and are intentionally not fabricated. Adding another provider requires separate approval/design rather than duplicating the integration here.
- A production-signed/R8 release APK was not built or verified: the configured release keystore and credentials were not supplied. The delivered APK is **debug-signed and debuggable**, for testing only; it is not a production release.
- The debug key differs from any existing release signer. Android will normally reject an in-place update of a differently signed installation; do not uninstall a user's launcher or delete its data just to work around that. For an update preserving data, rebuild with the owner's existing signing key.
- The supplied archive contained earlier screenshots/reports but no separate new weather-specific mockup image. This implementation follows the written weather requirements and the existing dashboard composition; exact comparison against an unavailable additional mockup is unverified.

## Delivery contents

The updated source ZIP omits local Git/Gradle/build caches and includes this report, the complete source patch and all new verification evidence. The APK is delivered separately. All original source/assets and historical verification files are retained unchanged except the source/test edits listed above.

Additional generated files are enumerated with their purpose in `verification/weather/generated-files.txt`. They do not add runtime launcher functionality.
