# FayyadhOS 2.10.1 device fixes

Version code 16. Continues the 2.10 home-refresh branch; no main merge or production release.

## Weather location

Settings → Cuaca → **Detect lokasi · Android permission** → **Benarkan & detect**, then allow Android location access. Approximate location is enough. A new unconfigured installation offers this setup once; existing manual coordinates are retained.

- Uses Android network/fused/GPS providers, only while Home is foreground. No IP geolocation, background-location permission, embedded location service or continuous tracking.
- Coordinates are sent to Open-Meteo for modelled current conditions. The UI explicitly explains this before requesting permission.
- Bounded 25-second acquisition; cancelled when Home pauses/destroys or weather is switched OFF. Stale, invalid and excessively inaccurate fixes are rejected.
- Permission denial, disabled Location, missing TV providers and acquisition failure are surfaced. Permission alone cannot make a TV with no location provider return coordinates. Open Android Location settings or choose manual coordinates if necessary.
- Saved successful automatic fixes are reused for up to one hour between foreground requests. Provider fixes must be no older than two hours. Weather refresh remains 15 minutes, with existing cache/backoff rules.
- Detected coordinates do not invent a city name: the label is **Lokasi TV**. MET district/rain/wallpaper and JAKIM prayer zone remain independent and unchanged.

## Speedtest

- Replaces one 100 MiB request with ten sequential 10 MiB downloads, still capped at **100 MiB total** and 120 seconds, download-only and explicitly user-started.
- The development environment rejected the old 100 MiB request with HTTP 403, while a complete 10 MiB GET returned HTTP 200 and exactly 10,485,760 bytes. This is endpoint compatibility evidence, not a physical-TV measurement or speed claim.
- Distinct HTTP/TLS/DNS/timeout/incomplete-response errors; redirects, HTML/JSON, compression and mismatched lengths are rejected. TLS checks are not bypassed.
- All segments must succeed before a final measured result is shown. Cancel/back still stops the test and closes its connection. Elapsed time includes setup and gaps; the result is an end-to-end single-stream estimate, not Wi-Fi link speed.

## Projectivy Icon Pack

Settings → Artwork → **Auto** or the installed pack. Applying a theme in Projectivy Launcher does not change another launcher's preferences.

- Explicit Android 11+ package visibility and direct detection of the known Projectivy package, in addition to theme/launcher discovery.
- Reads installed pack resources without loading its executable code. Compiled `xml/appfilter` takes priority over asset samples.
- Normalises relative/full activity names, supports activity aliases and drawable/mipmap references. Same-package artwork can cover a changed launcher activity.
- Missing mappings/resources fall back to the original TV banner/app icon. Not all apps or regional variants have artwork in the pack.
- **Status / refresh** shows active package, mapping source/count, matching counters and loading failures. No third-party APK or artwork is bundled, installed or redistributed.

## Installation and acceptance

Debug test builds are not production signed. Do not uninstall a working launcher or clear data to bypass a signature mismatch. A data-preserving update requires the certificate used for the installed app.

Physical-TV location-provider availability, permission flow, installed pack access, remote navigation and real Speedtest throughput require device acceptance. Existing prayer/azan, weather-driven wallpaper/dissolve and Dim behavior are retained.

## Local verification

Clean `bash build-verified-debug.sh`: PASS. 378 JVM/Robolectric tests: 377 passed, 1 skipped, 0 failures, 0 errors. Lint: 0 errors, 89 warnings (remaining warnings are not claimed fixed). Actual DEX audit: 405 app class definitions; all 13 declared components present. Static locale/placeholder/wrapper/executable guards and `git diff --check`: PASS.

New coverage includes Android location providers, runtime request code/permissions, fresh/stale fixes, provider-off/unavailable/timeout/cancellation, ignored paused callbacks, real drawable/alias/package-fallback loading, diagnostic fallback, segmented byte caps and distinct Speedtest failure cases. Tests use synthetic location/artwork fixtures, not personal coordinates or physical-TV acceptance.
