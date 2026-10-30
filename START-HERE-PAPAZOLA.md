# FayyadhOS 3.1 · Dunia PapaZola

Implementation of the approved transparent TV-home, PapaZola/BoBoiBoy action world and Fayyadh fifth-birthday concept. This is the existing native launcher, not firmware or a new WebView app.

## New in 3.1 · Dunia PapaZola dusk

- Kinabalu-inspired dusk/lake backdrop, mountain logo, “A QUIETER KIND OF SMART.” header, JARVIS/SETTINGS pills, Malay date, gold title swoosh and icon-only dock with cyan remote-focus glow.
- **Pose hero** is the default: PapaZola and BoBoiBoy hold the reference composition with restrained run-in-place bounce. **Lari bebas** retains the separate-lane sprite animation; choose it in Theme & motion → Watak.
- 25 short Malay motivational quotes rotate offline every 30 seconds. No quote service, tracking or extra permission.
- Five fardu prayer times remain from **JAKIM**, with the selected zone, provenance/cache, next-prayer countdown and Azan status. Configure the correct zone; no zone is guessed.
- Weather remains **MET Malaysia / data.gov.my**. The temperature is a **daily forecast min–max range**, not a measured current temperature. Upcoming slots use MET's Pagi/Petang/Malam periods, including “Esok pagi”; they are not invented +3h/+6h forecasts. Full descriptions remain available to accessibility. Kinarut uses the existing Papar forecast scope, not a new hyperlocal observation.
- The separately sourced Open-Meteo row stays hidden until explicitly configured. Source/cache/error distinctions remain intact.
- Birthday adds the native gold headline and ribbons. Automatic fifth birthday remains **30 October 2026 only**, Malaysian time; manual Birthday remains available on other dates.

## Included

- Spacious navy/ice-blue home; left clock, transparent top prayer area, weather at right and app carousel near the bottom. Actual installed app icons and launch targets remain; app labels remain in accessibility, not below dock icons; fixture icons are never bundled.
- Five simultaneous fardu times, next-prayer countdown in minutes, source/cache status, Azan OFF/ON/CHECK/PLAYING label. Syuruk remains in the full schedule, not a sixth fardu prayer. Select the correct JAKIM zone explicitly.
- Optional **Lari bebas** uses a native 12-frame WebP sprite atlas: six PapaZola run poses plus six BoBoiBoy run poses. Each character now follows an arc-length-paced curved route in a separate lane, preserving the Birthday headline and app dock, with different run-cycle timing, changing depth/size, subtle body lean and independent jumps. No combat audio, weapons, full-screen flashes or input capture.
- Native wallpaper drift, forecast-dependent scene brightness and existing rain effects. Clear/cloudy/rain/storm are presentation states from the existing validated current-period MET forecast. Unknown/expired data uses the neutral scene. This does NOT measure current rainfall or promise hyperlocal observations. Open-Meteo remains a separately labelled, separately configured modelled-weather display.
- Birthday uses the clear PapaZola-world scenery as its backdrop; animated sprites are rendered once in front so characters do not double against a baked-in character illustration. The native UI supplies “Happy Birthday, Fayyadh!” and “5 TAHUN!”. Automatic birthday is **30 October 2026 only, Malaysian time**. It is not an annual reminder claiming age five forever. The manual Birthday theme can be opened on any date.
- Full motion targets up to 30 fps and Lite up to 15 fps, paced against display vsync through Android Choreographer; OFF remains static. Each character follows an arc-length-sampled closed Catmull-Rom route in an 11-second loop, with smooth direction changes, non-overlapping lanes, independent jump timings, restrained body lean and depth-based size. The six supplied poses remain discrete artwork; their cycles are deliberately desynchronised at about 15–17 fps. Full interpolates position at up to 30 fps, while Lite targets 15 fps. Movement is time-based and does not accumulate drift after missed frames. Caps are not measured TV performance guarantees.
- Sprite work/drift stop when Home pauses or detaches, the screen turns off, a tracked dialog obscures Home, motion is OFF, Android animator scale is zero, or Rain settings Moving is OFF. The motion timeline freezes and resumes from the same pose; missed frames do not accumulate timing drift. During this app's Azan playback, characters remain visible in a frozen pose and wallpaper drift pauses through an app-scoped playback-state signal. Existing scheduling/audio-focus/volume behavior is unchanged.
- Birthday confetti is bounded to 18 small marks for the first ten seconds after entering that theme. Its timer is independent of the character animation, so PapaZola and BoBoiBoy continue moving after confetti ends. Static gold ribbon accents also remain in Birthday mode.

## Controls

Settings → **PapaZola · Theme & motion**:
1. PapaZola · Weather world
2. Birthday · Fayyadh 5 Tahun
3. Classic · Existing wallpapers
4. Motion (tap to cycle Full → OFF → Lite)
5. Automatic birthday on 30 October 2026 ON/OFF
6. Watak: Pose hero (Dunia PapaZola) / Lari bebas (sprite)

Settings → Wallpaper retains the existing manual collection and weather-auto toggle. Selecting an old wallpaper changes the theme to Classic and disables automatic wallpaper selection, without deleting the saved preference. Re-select PapaZola through Theme & motion to return. Existing wallpaper selection, app order/hidden state, prayer zone, weather configuration, Azan audio and Jarvis keys are not cleared.

D-pad: apps Up → prayer; prayer Up → Jarvis/header; Down returns to the remembered app. Left/right wraps apps. OK opens real apps; hold OK moves/hides or opens App info. Prayer OK opens the existing full schedule/zone/Azan options.

## Build and test

JDK 17, Android SDK 34 / build-tools 34.0.0; Gradle wrapper 8.9.

```sh
export JAVA_HOME=/path/to/jdk17
export ANDROID_HOME=/path/to/android-sdk
bash ./gradlew --no-daemon :app:assembleDebug :app:testDebugUnitTest
```

`art/premium/*.webp.b64` are the canonical artwork sources. Gradle decodes them into `app/build/generated/premiumAssets/premium/` before asset merging. **Only WebP files enter the APK**, not Base64 text; no image download or Base64 work occurs at runtime. This text transport permits reproducible publishing through the connected GitHub tools. No private reference photograph, user's photo, credentials or signing material is included.

See `verification/papazola/DUNIA-3.1.json` for this revision’s build/test evidence; `RESULTS.json` preserves the earlier 3.0 audit, including pre-existing lint failures. Updated legacy assertions reflect approved geometry/header/settings changes; legacy Classic weather coverage is retained, alongside dedicated Premium lifecycle and forecast-state tests. No physical TV was connected. Robolectric screenshots/video are labelled simulations with fixture apps/times/weather, not actual installed apps, live forecasts, benchmarks, audible speaker tests or TV acceptance evidence.

## Safe installation

Package remains **dev.farred.fayyadhos.tv.jarvis.fix**, min SDK 26, target SDK 34. VersionCode **15**, versionName **3.1.0-dunia-papazola**. The old non-Fix package is not migrated.

The delivered APK is **debug/test signed**. It is NOT signed with the installed production certificate. Never uninstall a working launcher, clear its data or disable the stock launcher to bypass a signature mismatch. A data-preserving update needs the original installed signing certificate through your secure build process. Release signing configuration is unchanged; no release key is created, uploaded or substituted.

Test on a spare device/emulator first. Check remote focus, real app launch/return, long-press/move/hide, 720p/1080p/4K and overscan, correct JAKIM zone, offline/stale forecast fallback, motion OFF/Lite/Full, birthday selection, actual Azan playback/Stop and coexistence with other apps. Keep stock Home enabled. Firmware standby, wake, audio and real-frame pacing still need physical-device testing.

## Artwork

PapaZola appearance follows the reference supplied in the approval conversation (red eye mask, black hair/moustache/goatee, white/red/blue costume and P belt), correcting the earlier orange-helmet concept. Art is AI-generated fan-concept artwork, not official MONSTA production assets. Character names/designs belong to their respective rights holders. Public/commercial distribution requires checking applicable rights; this change does not grant a character licence. Background and sprite artwork retain the approved concept direction, but animated poses are not a promise of pixel-identical still-mockup parity.
