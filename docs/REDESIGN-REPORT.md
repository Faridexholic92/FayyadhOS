# FayyadhOS TV — Reference UI Redesign

## Outcome and scope
The supplied **FayyadhOS-TV-Master** project is modified in place. The active exported `DashboardActivity` remains the real HOME / LEANBACK launcher. Native Java, programmatic Android Views, RecyclerView, Gradle 8.9 and AGP 8.5.2 are retained. No new runtime dependency, application ID, SDK level, signing key, release configuration or build variant was introduced. This is the supplied 2.8-master application, not a new TV Plus 2.6 package.

## Inspection and implementation plan followed
1. Inspect the complete supplied source/resources/tests and guide; identify the exported Home activity, app catalog/launch intents, wallpaper collection, Jarvis lifecycle and prayer repository/provider.
2. Recompose the actual dashboard in a uniform 960×540 design space that scales to 16:9 TV resolutions; preserve branding and a three-zone header.
3. Replace the multi-row app grid with a horizontal, focusable RecyclerView; retain discovery, saved ordering/hiding, long-press options, explicit launch components and Back semantics.
4. Restyle the existing PrayerPanel rather than substituting sample times; add vector-like native icons and a local, UI-free sunset wallpaper.
5. Build, run the full existing/extended test suite, compare lint with untouched source, inspect Android view renders and verify APK identity/signature.

## Source files changed
Paths below are relative to the project root.

| File | Reason |
| --- | --- |
| `app/src/main/java/com/gathin/tvhome/DashboardActivity.java` | Three-zone header, cyan wordmark suffix, TV EDITION, locale-aware live clock/date, adaptive greeting, horizontal carousel, actual icon loading/fallback, empty-app state, remembered focus, explicit vertical navigation, horizontal move mode, immersive TV flags, reference spacing and safe-area prayer placement. |
| `app/src/main/java/com/gathin/tvhome/GlassUi.java` | Honor translucent panel fills; cyan layered focus glow and quiet header shortcuts; dark readable menu surfaces. No animated/GPU blur dependency. |
| `app/src/main/java/com/gathin/tvhome/PrayerPanel.java` | New dashboard-only glass strip using the existing values/provider/settings/cache/lifecycle; six icon-led entries, state/zone identity and truthful status text. Legacy MainActivity presentation remains supported. |
| `app/src/main/java/com/gathin/tvhome/FayyadhWallpapers.java` | Append the new local sunset artwork and define the dashboard default; preserve all previous filenames and saved selections. |
| `app/src/main/java/com/gathin/tvhome/PrayerGlyph.java` (new) | Lightweight resolution-independent mosque, dawn/sun/horizon and crescent line icons, with preallocated drawing objects. |
| `app/src/main/res/values/dashboard_strings.xml` (new) | English prayer-strip branding resource. |
| `app/src/main/assets/fayyadh-wallpapers/11-Kinabalu-Sunset.jpg` (new) | Bundled 1920×1080 JPEG, created by AI-editing the supplied reference to remove all UI. Nature artwork only; no network fetch or baked-in text. |
| `app/src/test/java/com/gathin/tvhome/DashboardRedesignTest.java` (new) | 13 tests for carousel structure/focus/scrolling, OK/Enter launch intents, vertical navigation, return focus, Back, move/hide, preserved wallpaper, localized dynamic clock, unconfigured prayer state, 720p and minimum SDK 26. |
| `app/src/test/java/com/gathin/tvhome/TransparentPanelTest.java` | Update only obsolete visual expectations (transparent-only glass and exactly ten wallpapers) to the new design. Retain legacy transparency, asset decode/path restrictions and wallpaper persistence coverage. |
| `README-FAYYADHOS-TV.md`, `BUILD-REPORT-TV.txt` | Current UI, installation/signing precautions, build/test results and limitations. |
| `docs/REDESIGN-REPORT.md` (new), `verification/redesign/` (new) | This report, changed-file manifest, current logs, test XML, lint comparison, screenshots and APK verification. |

`AppCatalog`, `DashboardApps`, `AppVisibility`, package mappings, explicit launch component logic, `PrayerRepository`, `WaktuSolatApi`, Jarvis network/voice logic, startup speech, AndroidManifest, Gradle build files and release signing configuration remain intact.

## Behavior and data integrity
- Only apps returned by the existing installed-app discovery are shown. Labels and icons come from PackageManager, with Android's default icon as a failure fallback. No mpvNova/OnStream/Projectivy or other fake package shortcut is added.
- Left/Right moves across installed apps and scrolls beyond the first seven. OK/Enter uses the existing explicit MAIN component intent. Missing/stopped/unlaunchable apps retain the existing error dialog rather than crashing.
- The first available app is initially focused. A normal return from an app restores the remembered selection. Header Down / prayer Up return to that app. App Up goes to Jarvis, app Down to the prayer panel. Long-press Move uses horizontal arrows; OK/Back finishes without launching. Normal Back still focuses Settings rather than exiting Home.
- Clock/date use device-local time and locale, refreshed every 30 seconds while foregrounded and immediately on resume. The greeting uses the existing hour-based policy. Example screenshot times are not hardcoded.
- Prayer data continues through WaktuSolat.app's existing JAKIM-derived provider, month cache, zone selection and Malaysia timezone. There is **no automatic Sabah selection**: Sabah has multiple JAKIM zones. The strip says SABAH only after a Sabah zone is selected, and otherwise names the actual selected state. No zone means SELECT ZONE and `--:--`. Missing today's data also means `--:--`; loading, cache, manual-refresh and error/source details remain available.
- Existing selected wallpapers are preserved. To match the new reference on an upgraded installation, choose **Settings > Wallpaper > 11 · Kinabalu Sunset**. The new asset is the default only when no previous choice exists.
- Fullscreen CENTER_CROP preserves aspect ratio. A subtle full-screen gradient and translucent surfaces support contrast. Normal/focused card text contrast over an all-white background is approximately 5.3:1 / 4.9:1 (rounded; text shadow provides an additional halo). No global accessibility certification is claimed for every possible user wallpaper or font scale.
- Glass is simulated with alpha fills and a layered cyan edge; live backdrop blur is deliberately omitted for performance/SDK 26 compatibility.

## Build and tests
Environment installed separately from source: Amazon Corretto JDK 17, Android SDK platform 34, build-tools 34.0.0. The source archive does not contain local SDK paths, build caches or signing secrets.

From the project root, with JDK 17 and Android SDK configured:

```bash
bash gradlew :app:assembleDebug :app:testDebugUnitTest :app:compileReleaseSources --console=plain
bash gradlew :app:lintDebug --console=plain
```

- **Debug APK assembly: PASS.**
- **Release source compilation: PASS.** This is not a signed or minified release APK build.
- **Full test suite: PASS — 137 tests, 0 failures, 0 errors, 0 skipped.** Includes original speech/wake/cancellation/provider/cache tests and 13 additional redesign tests.
- **APK signature verification: PASS for the debug certificate**, v2 APK signing. Package `dev.farred.fayyadhos.tv.jarvis`; version code 10 / version name 2.8-master; min SDK 26 / target SDK 34, unchanged.
- **Lint: FAIL, inherited project issues.** Both untouched and redesigned projects report 45 errors: 44 MissingTranslation errors in existing resources, plus a ByteOrderMark error in unchanged `Config.java` line 192. Both report 51 warnings. New drawing/allocation/constructor/direction warnings were fixed; the inherited errors were not hidden with a lint baseline or suppressed to claim success. Exact XML/text logs are supplied.

The companion APK is debug-signed, not production-signed. The original release keystore/passwords were not supplied, so no same-certificate release APK is claimed. Do not uninstall an installed production launcher to work around a signature mismatch. Use the original release credentials for production updates; the existing release build script/configuration remains available.

## Visual verification and comparison
Current evidence lives in `verification/redesign/`, separate from the original historical verification files.

- `dashboard-720.png` — 1280×720; Settings stays on one line, seven cards are visible, first focus is clear, no carousel/prayer overlap and all primary content is inside the safe area.
- `dashboard-1080.png` — 1920×1080; wordmark/centered clock/right shortcuts, prominent greeting, consistent glass cards and wide prayer strip.
- `dashboard-4k.png` — 3840×2160; proportional scaling and larger prayer typography.
- `dashboard-unconfigured-1080.png` — no installed test apps / no zone; explicit empty-app message and `--:--`, not fabricated prayer data.
- `reference-comparison.jpg` — reference alongside the implemented native-view render. Composition, header, cinematic wallpaper, app row and prayer-strip placement are aligned. App names/icons, time/greeting and selected prayer state differ intentionally according to runtime data.
- `jarvis-composite.png`, `dashboard-settings.png`, `dashboard-hide.png` — retained integration/menu surfaces. Compact Jarvis remains D-pad-scrollable; scrolling to controls may move its decorative header off-screen.

**These are Robolectric native Android View renders, not physical TV or emulator screenshots.** The screenshot test registry contains seven demo app registrations with generic Android icons and a clearly test-only cached SWK08 prayer fixture. Those registrations/fixtures live only under `src/test`; they are not included as installed apps or live timetables in the APK. The render therefore correctly says SARAWAK, not a falsely assumed Sabah location. The empty/unconfigured render demonstrates the default truth-preserving state.

## Remaining limitations and real-device checklist
- No Android device/emulator was attached (`adb devices` was empty); no physical remote, actual third-party app launch, sound output, HDMI-CEC/standby, firmware Home handling or production update was verified. Input tests exercise real view key dispatch/FocusFinder plus outgoing component intents under Robolectric; they do not prove firmware behavior.
- Live prayer API availability and real location/zone selection were not network-validated; original cache/provider tests pass. Select the correct Sabah zone and verify current date/time before relying on it.
- 720p/1080p/4K default-font-scale renders were inspected. Non-16:9 screens, unusual firmware density/overscan and enlarged accessibility font settings need device checks.
- Keep the system launcher available. On a test TV, verify app scrolling/launch/return focus, move/hide, header/prayer traversal, Back, manual refresh/cache/unavailable states, selected wallpaper, voice/boot behavior, system bars and screen safe area before choosing this as default Home.
