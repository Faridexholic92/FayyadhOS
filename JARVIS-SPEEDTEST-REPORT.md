> Historical Speedtest-stage report. The later FIVE-PRAYER-GLOW-REPORT.md supersedes the prayer-layout status below.

# Jarvis Speedtest — implementation and verification

## Scope
Based on FayyadhOS-TV-Solat-Weather-source.zip supplied in this chat.
Only two production Java files changed. No build dependencies, manifest, application ID, launcher, weather, prayer, wallpaper, backend or AI credential storage changes.
The uploaded source still has the rotating boxed prayer widget. The earlier five-prayer transparent mockup is NOT implemented by this change.

## Changes
- Rename Test Internet to Speedtest. Open a download page INSIDE the existing opaque Jarvis sidebar, not a browser or another application.
- Explicit start only; 100 MiB (104,857,600 bytes) maximum response body read, 64 KiB streaming buffer. HTTP/TLS overhead is additional network traffic.
- Single HTTPS GET to https://speed.cloudflare.com/__down?bytes=104857600. Cloudflare, NOT Ookla/Speedtest.net.
- No upload, ping, packet-loss test or background polling. Groq AI is not required for Speedtest.
- Show progress in MiB, ring, provisional Mbps and final average. Average includes connection/TLS/response setup; this is a single-connection estimate, not a multi-stream benchmark or Wi-Fi negotiated link rate.
- 10-second connect timeout, 5-second read timeout, 120-second overall deadline. Cancellation, timeout or incomplete download does not produce a new successful result.
- Batal ujian cancels the request. Back cancels an active test and returns to diagnostics; next Back closes Jarvis. Close and activity lifecycle release resources and ignore stale callbacks. Network route changes invalidate the in-flight test.
- Disabled buttons visibly dim; focus moves to Batal during the test and Mula after completion/cancellation.
- Jarvis radar animation only runs while visible, respects disabled system animations, stops on detach.
- Removed Jarvis spoken-output/TTS controls and initialization. Unrelated existing startup-greeting functionality is preserved.

## Modified / new files
- `app/src/main/java/com/gathin/tvhome/JarvisPanel.java`
- `app/src/main/java/com/gathin/tvhome/SpeedProbe.java`
- `app/src/test/java/com/gathin/tvhome/DashboardTest.java`
- `app/src/test/java/com/gathin/tvhome/JarvisRepairTest.java`
- `app/src/test/java/com/gathin/tvhome/JarvisSidebarTest.java`
- `app/src/test/java/com/gathin/tvhome/SpeedProbeTest.java`
- `app/src/test/java/com/gathin/tvhome/JarvisSpeedtestTest.java`

Additional delivery files: JARVIS-SPEEDTEST.patch, JARVIS-SPEEDTEST-REPORT.md and verification/jarvis-speedtest evidence.

## Verification
- Final command: Gradle 8.9 / JDK 17 / Android SDK 34, :app:assembleDebug :app:testDebugUnitTest.
- BUILD SUCCESSFUL. 268 tests discovered: 267 passed, 0 failed, 1 skipped.
- Skipped: MetLiveVerificationTest.liveRepositoryParsesPaparCachesAndDisplaysKinarut (live weather verification not enabled).
- Tests cover data cap, final progress, no-request pre-cancellation, mid-test cancellation, incorrect status/type/encoding/length, truncated responses, formula, successful fixture flow, error state, focus, Back/cancel, stale callbacks, unchanged home geometry and existing Jarvis/AI/settings/launcher regressions.
- Native Robolectric renders: new Speedtest page at 1920x1080 and 1280x720; sidebar regressions also cover 4K.
- New previews were visually checked. Video is a 20-second sequence of native UI stills from local fixture tests, NOT a continuous screen recording or a real throughput test. App entries/icons and prayer data are fixtures, not an inventory of the user's TV. Every frame is labelled simulation.
- Cloudflare endpoint smoke check from the build computer: 1 KiB request, HTTP 200, application/octet-stream, Content-Length 1024 and 1024 bytes received. A live 100 MiB test on the Android TV was NOT performed.
- APK verified with apksigner: APK Signature Scheme v2 valid.

## APK and remaining checks
- Build output: app/build/outputs/apk/debug/app-debug.apk
- Delivery: FayyadhOS-Jarvis-Speedtest-debug.apk
- Debug build only. No production release was signed or installed. The debug signing certificate can differ from an installed release; do not uninstall an existing app merely to bypass a signature mismatch, as that can erase saved settings/keys. A production update needs the original release signing key through the normal secure build process.
- Not tested on a physical TV: firmware networking, real D-pad behavior, on-device throughput, VPN transitions, real 120-second timeout and streaming impact.
- Groq live credentials, weather correctness and azan behavior were not newly validated by this change.
- Version name/code and application ID remain unchanged from the supplied source. Update versioning/signing before distribution as a release.

## Source archive
The clean source ZIP includes buildable source/assets/tests/Gradle wrapper, report, patch and fresh test evidence. Generated build/cache directories and old historical verification screenshots/logs are omitted to keep the download small. Original uploaded ZIP remains untouched.

APK SHA-256: 58abf6b4b568d44a58a3b98c86d3e5883bb9a61132492ed75514b6e92356e2ff
