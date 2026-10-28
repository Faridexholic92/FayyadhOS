# Five-prayer glow correction + existing Jarvis Speedtest

This build corrects the omitted prayer layout from the earlier Speedtest handoff. It builds on FayyadhOS-Jarvis-Speedtest-source.zip, preserving JarvisPanel.java and SpeedProbe.java byte-for-byte.

## Delivered behavior
- Five fardu prayers shown together, ordered Subuh, Zohor, Asar, Maghrib, Isyak.
- Transparent row directly below the FayyadhOS wordmark at reference design coordinates x=40, y=84, width=390, height=40 (960x540 design space).
- No filled card/border, rotating slot, Syuruk, zone heading or source metadata on the dashboard row. Full six-time schedule, zone selection, refresh and source information remain accessible with remote OK. Long OK still opens wallpapers.
- Only the current prayer NAME is cyan with a softly pulsing halo. Times remain white. Pulse varies the text shadow, not the text opacity, widget opacity or background.
- Existing validated JAKIM schedule and Malaysia-time current-prayer calculation retained. Glow moves at exact schedule boundaries, including Zohor to Asar. Subuh is not highlighted after Syuruk; before today's Subuh and when today's data is missing, no name is current. No timing is hardcoded into production.
- Missing data stays --:--. No zone is picked automatically. Cache/refresh/time tick logic unchanged.
- Animation pauses while obscured by a dialog/Jarvis, hidden, detached or stopped, and respects reduced-motion/system animation settings. Current-name color/static halo remains when animation is paused.
- No boxed focus background. A thin underline appears only while the whole prayer group has remote focus; OK opens options. Arrow keys no longer rotate times.
- Existing wallpaper, app launcher, weather, AI configuration, audio/azan and Jarvis Speedtest preserved. No dependencies, permissions, build settings, version codes or signing configuration changed.

## Modified/new Java files in this correction
- `app/src/test/java/com/gathin/tvhome/PrayerWeatherUpgradeTest.java`
- `app/src/main/java/com/gathin/tvhome/DashboardActivity.java`
- `app/src/main/java/com/gathin/tvhome/PrayerPanel.java`
- `app/src/test/java/com/gathin/tvhome/FivePrayerGlowTest.java`

## Verification
- Gradle 8.9, JDK 17, Android SDK 34. Command: :app:assembleDebug :app:testDebugUnitTest.
- BUILD SUCCESSFUL. 275 tests: 274 passed, 0 failed, 1 skipped.
- Skipped: existing optional MetLiveVerificationTest.liveRepositoryParsesPaparCachesAndDisplaysKinarut. No new live-weather verification claimed.
- Tests cover order and visibility of all five times; no Syuruk/metadata/card in the home strip; official captured schedule parsing; exact boundary changes; current name only; all time values white; missing-zone/day behavior; focus; stop/overlay/hidden/reduced-motion behavior; 720p/1080p/4K fit; and existing Jarvis/Speedtest/full-project regressions.
- Native Android UI previews generated with Robolectric at 1280x720, 1920x1080 and 3840x2160. Video contains native pulse frames, a magnified crop of the same row, an Asar transition example and the existing Jarvis Speedtest page. This is controlled-date UI test footage, NOT a physical-TV recording, live prayer validation or live internet-speed test.
- Thirty-six 1080p pulse frames were compared pixel-by-pixel. Changes are confined to the Zohor name halo, bounding box (219,168)-(318,212); the rest of the frame is unchanged.
- Date, schedule, weather and installed-app entries in previews are explicit fixtures. App icons are test placeholders, not a replacement for real package icons. Production uses installed-app discovery and the existing weather source.

## APK and limits
- Native debug APK output: app/build/outputs/apk/debug/app-debug.apk
- Delivery: FayyadhOS-5-Waktu-Glow-Jarvis-debug.apk
- Not installed or tested on a physical Android TV. Firmware-specific text-shadow rendering, actual D-pad and live device network/azan behavior remain unverified.
- Debug signing only. A debug certificate may differ from an installed app. Do not uninstall an existing installation simply to bypass signature mismatch: uninstalling can erase saved settings/API keys. A production update needs the original release key through the normal secure build flow.
- Application ID/version unchanged from the supplied project; release versioning and release signing still required for distribution.
- Full buildable source archive includes original assets/tests, both stages of changes, patch and reports, and test evidence. Build caches and large generated preview frames are excluded. Earlier Speedtest report remains as a historical stage report; this report supersedes its note that the five-prayer row was not yet implemented.

APK SHA-256: 0c11bbf88e3c2a7df0bd5feed2e205e67f9dda679d7d376dc4ddbee6c909674e
