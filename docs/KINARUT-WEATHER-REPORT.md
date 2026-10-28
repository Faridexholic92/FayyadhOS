# Kinarut / MET Malaysia weather verification and fix

## Outcome

**Ds548 is verified as Papar. The production repository now fetches and parses its real forecast successfully, and the application widget displays Kinarut using that district forecast.**

Final debug build: **BUILD SUCCESSFUL in 1m 40s**. Final suite: **201 tests, 0 failures, 0 errors, 0 skipped, 19 suites**. This includes an explicitly enabled real HTTPS repository → parser → atomic cache → controller → widget integration test, not only saved-fixture tests.

No physical Android TV was available. Verification is on the actual Java repository/controller/widget code under Robolectric, with real HTTPS for the live integration test. It is not a claim of deployment to or measurement on the user's TV.

## 1. Location verification and geographic scope

- The existing 440-entry catalogue contains `Ds548` / `Papar` and no dedicated Kinarut entry.
- The live API returned seven rows, each with `location.location_id = Ds548` and `location.location_name = Papar`.
- The official weather documentation identifies `Ds` IDs as district forecasts: https://developer.data.gov.my/realtime-api/weather
- The Sabah Government's Papar District Council Instrument lists Kinarut in the Papar district council's township schedule: https://sagc.sabah.gov.my/sites/default/files/law/Papar-Instrument1961_1.pdf
- A Papar district forecast is therefore a reasonable regional proxy for Kinarut, **not a Kinarut-specific forecast station, current observation or precise hyperlocal measurement**. This limitation is explicitly recorded in code (`RainOptions.HOME_SCOPE`), the weather location dialog and diagnostics.
- A one-time, weather-only configuration migration selects `Ds548` with the display name `Kinarut`. It does not change prayer-zone or any other feature's location preferences. The original catalogue remains intact; its entry is not renamed to pretend that MET calls Papar “Kinarut”.

## 2. Exact requests and observed HTTP responses

Documented query syntax: https://developer.data.gov.my/request-query

The initial documented slashless path is:

```text
https://api.data.gov.my/weather/forecast?filter=Ds548%40location__location_id&sort=date&limit=14
```

**Direct request with redirects disabled, recorded at 2026-10-25T02:04:38Z:**

- HTTP **301**
- Content-Type **text/html; charset=utf-8**
- Location header: `/weather/forecast/?filter=Ds548%40location__location_id&sort=date&limit=14`
- This redirect body is not forecast records.

The verified canonical URL used by the fixed app is:

```text
https://api.data.gov.my/weather/forecast/?filter=Ds548%40location__location_id&sort=date&limit=14
```

**Direct request with redirects disabled, recorded at 2026-10-25T02:04:38Z:**

- HTTP **200**
- Content-Type **application/json**
- **7 records**
- Actual IDs/names: **Ds548 / Papar**, throughout
- Fields present: `location`, `date`, `morning_forecast`, `afternoon_forecast`, `night_forecast`, `summary_forecast`, `summary_when`, `min_temp`, `max_temp`
- Dates in the saved response: 2026-10-25 through 2026-10-31.

The actual application repository then independently requested the canonical URL at **2026-10-25T02:05:30Z**, with redirect-following still disabled. Its recorded result was:

```text
HTTP 200; Content-Type: application/json; parsed 7 records for Ds548 / Papar
Display heading: Kinarut · 24–33°C
Display condition: Ramalan · Ribut petir di beberapa tempat
```

Those temperature/condition strings are the live response at that time, not production constants. `min_temp`/`max_temp` remain daily forecast extrema, never a current thermometer reading.

Raw request/header metadata, bodies and the live integration result are in `verification/kinarut/`. The first Python verification calls allowed automatic redirects and reported a final HTTP 200. Their metadata now explicitly labels that behavior. They must **not** be mistaken for evidence that the original slashless URL returned a direct 200. The separate no-follow requests and Java repository test establish the distinction.

## 3. Confirmed root cause and additional failure paths

### Primary reproduced failure: unhandled HTTP 301

The old repository requested `/weather/forecast` without the trailing slash, called `setInstanceFollowRedirects(false)`, and threw `FetchFailure` for every status other than 200. A live integration test using that repository path failed with **MET HTTP 301**. A no-follow request against the exact old Sabah production URL, including its old `include` projection, independently returned 301 with the canonical slash URL in the Location header.

Consequently, the app could never parse the redirect response. Without a usable cache, it displayed “Ramalan tidak tersedia”. The fix is the verified canonical HTTPS endpoint, **not** indiscriminately enabling redirects or inventing weather values. Redirects remain disabled.

This is a confirmed source/repository failure matching the persistent unavailable symptom. The user's individual TV logs were not available, so additional TV-specific problems (offline state, TLS, wrong clock, expired cache) are not ruled out; the revised diagnostics now expose them.

### `include` investigation

The old projected request was also fetched successfully after its redirect. It retained the nested `location` object and all requested forecast/temperature fields. Thus, **`include` did not flatten or corrupt the response in the verified API behavior** and is not the demonstrated 301 cause.

The projection did omit daily summaries. The fixed code requests the documented default structure instead, preserving exact location filtering, ascending `sort=date` and bounded `limit=14`. The default contains all needed fields plus daily summaries for an explicitly labelled fallback.

### Additional reproduced weaknesses fixed

- **Overly strict optional fields:** the original parser rejected the entire otherwise valid seven-day response if one period field was absent. A standalone original-parser probe reproduced this by removing only `night_forecast`. Period text, summary and temperatures are now individually optional; valid dates/other periods are retained. Invalid optional temperatures are omitted, not fabricated. Structurally invalid/mismatched responses still fail distinctly.
- **Early-morning date gap:** at 02:00 MYT, the original period policy requests the previous date's night. The saved live response begins today, so the original parser returns an empty description even with a valid temperature range. Home now uses today's actual summary only when needed and labels it **“Ramalan harian”**. It never labels that daily summary as the missing night/current-period forecast. The existing rain animation still uses only its original period-specific forecast and is not activated by this fallback.
- **Future-dated cached fetch times:** the old refresh calculation could defer recovery until a cached future timestamp. Non-fresh/future caches are now due for recovery rather than treated as normally reusable.
- **Legacy projected caches:** when display data is missing and there is no summary, refresh is shortened to a 15-minute recovery interval rather than waiting six hours. Existing request cooldowns, offline delay and HTTP Retry-After bounds remain in force.

## 4. Diagnostics and user-facing behavior

The home-screen unavailable message stays concise: **Ramalan tidak tersedia**. The existing weather status dialog now distinguishes:

- HTTP/API failure and its actual status;
- offline state without sending a request;
- network timeout, DNS, TLS and other I/O failures;
- invalid JSON;
- unexpected response structure/content type;
- empty results versus no records matching the selected ID;
- missing requested MYT date/period, including whether a daily fallback is used;
- expired cache, a future cache timestamp/TV-clock issue, and missing/wrong-location/invalid cache;
- current request URL, actual source ID/name, retrieved time, MYT clock and next permitted request.

A fresh usable cache survives network failure; expired data is not displayed as valid current weather. Cache-write failure is diagnosed without discarding usable network data. Network and JSON work remain on the existing single background worker; no new polling service or dependency was introduced.

The old multi-category/location picker is replaced with a short Kinarut/Papar explanation and a “Use Kinarut / Papar” action. Other weather/rain controls keep their positions in the settings menu. Legacy/programmatic location changes remain isolated correctly; tests verify that an in-flight old-location response cannot publish under a new location name.

## 5. Every changed source/test file

Compared with the previous icon-corrected delivery, exactly **14 files under app/src** changed or were added:

| File | Purpose |
| --- | --- |
| `app/src/main/java/com/gathin/tvhome/MetWeatherRepository.java` | Canonical trailing-slash endpoint; default response instead of include; transport/cache diagnostics; retained HTTPS bounds/atomic cache. |
| `app/src/main/java/com/gathin/tvhome/WeatherForecast.java` | Tolerant optional fields, explicit parse failures, actual source name, labelled daily fallback and availability diagnostics; original rain-state semantics preserved. |
| `app/src/main/java/com/gathin/tvhome/RainController.java` | Verified home migration, informative status/failure classification and stale/missing-data recovery using the existing worker/scheduler. |
| `app/src/main/java/com/gathin/tvhome/RainOptions.java` | Verified Ds548/Kinarut configuration, weather-only one-time migration and explicit Papar district limitation. |
| `app/src/main/java/com/gathin/tvhome/RainSettingsDialog.java` | Replace catalogue browsing with home-location explanation/reset; leave non-weather feature settings untouched. |
| `app/src/main/java/com/gathin/tvhome/WeatherWidget.java` | Select the actual display description/label/state, allowing clearly labelled daily fallback; no geometry, typography or icon-artwork changes. |
| `app/src/test/java/com/gathin/tvhome/RainFeatureTest.java` | Adapt default-location/migration expectations; preserve explicit test locations for rain regression coverage. |
| `app/src/test/java/com/gathin/tvhome/WeatherForecastTest.java` | Keep malformed/empty-response rejection tests while allowing individually absent optional period fields. |
| `app/src/test/java/com/gathin/tvhome/WeatherWidgetTest.java` | Updated default/location fixtures and default-response URL expectations; retain layout/offline tests. |
| `app/src/test/java/com/gathin/tvhome/KinarutWeatherTest.java` (new) | Fourteen targeted URL, fixture, optional-field, date-gap, diagnostic, migration, cache-recovery, location-race and simplified-settings tests. |
| `app/src/test/java/com/gathin/tvhome/MetLiveVerificationTest.java` (new) | Opt-in actual HTTPS repository/cache/controller/widget test; writes real live result and render. |
| `app/src/test/resources/met-papar-Ds548-2026-10-25.json` (new) | Recorded real default-response fixture, never used as a production weather fallback. |
| `app/src/test/resources/met-papar-Ds548-projected-2026-10-25.json` (new) | Recorded projected-response comparison fixture. |
| `app/src/test/resources/API-FIXTURE-PROVENANCE.txt` | Actual request provenance and redirect/final-status distinction. |

The hash audit confirms the dashboard activity, icon vector resources, wallpaper implementation, prayer panel, manifest and build files are unchanged. No background/card/border, app-tile reordering, clock/greeting alteration, prayer-zone change, dependency or navigation change was introduced.

The incremental `verification/kinarut/kinarut-weather.patch` applies cleanly to the immediately previous icon-corrected source. `git diff --check` passed. Additional generated files are this report and `verification/kinarut/`; their complete inventory is in `generated-files.txt`.

## 6. Build, tests and remaining limits

Build environment: JDK 17, Gradle 8.9, AGP 8.5.2, Android platform/build tools 34; existing minSdk 26 unchanged.

Reproducible command after configuring JDK/Android SDK:

```sh
MET_VERIFY_LIVE=1 ./gradlew --no-daemon :app:assembleDebug :app:testDebugUnitTest
```

The sandbox invoked the equivalent Gradle 8.9 binary directly. **201/201 tests passed with no skipped tests**, including actual live HTTPS verification and the existing weather-icon, UI bounds/transparency, app launch, focus/navigation, prayer, speech, personalization and rain regressions. Without `MET_VERIFY_LIVE=1`, the deliberately opt-in live test is skipped; saved-fixture tests must not be reported as live verification.

An initial test-output compile issue was corrected to use the SDK-compatible `Files.write` method. An intermediate integration failure exposed HTTP 301 and a test fixture timestamp mismatch; both were corrected. Diagnostic logs are retained separately from the final successful log/XML. No remaining compilation error or failing test exists in the final run. Existing deprecated-API compiler notes were not suppressed with unrelated refactoring. Lint and production/R8 release were not run in this task.

APK signature verification passed. Delivered APK is **debug-signed/testing-only**, not a production-signed release. A differently signed installed launcher cannot normally be updated in place with it; obtain the existing signing key for a data-preserving update. Do not uninstall the active launcher or erase its data merely to bypass a signing mismatch.

APK SHA-256:

```text
daf721271f3997f2a669522f759b276e07052b89b24a8c852f5eb97ac4baa47a
```

The live and daily-fallback screenshots were opened and visually inspected: transparent design, original icon styling/position and surrounding launcher layout are preserved. The live render uses actual API data but test-registered app icons; it is a native Robolectric render, not a physical-TV screenshot. Clock/greeting reflect the test host; weather calculations/diagnostics explicitly use Malaysia time. Device-specific networking, installation, long-running TV behavior and physical remote/performance remain unverified. No device benchmark claim is made.
