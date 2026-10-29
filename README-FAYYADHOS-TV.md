> **Latest continuation (2026-10-29):** read [CURRENT-STATUS.md](CURRENT-STATUS.md) and [AGENTS.md](AGENTS.md) first. The 3D robot replacement is a prototype workstream, not a shipped Android feature. Headers and delivery notes below describe older stages.

> **Current maintenance: 2.9.1-stability / code 14.** Read [docs/STABILITY-2.9.1.md](docs/STABILITY-2.9.1.md). The dated validation folders and older feature descriptions below remain historical.

> **Current: 2.9.0 feature pack / code 13.** Read [START-HERE-FEATURE-PACK.md](START-HERE-FEATURE-PACK.md) and `verification/feature-pack/REPORT.md` first. Older rotating-prayer/TTS/speed-test instructions below describe earlier stages and are not the current feature contract.

## Current continuation — single prayer tile and forecast effects

- Native dashboard now has one glass tile **below the clock**. It cycles Subuh, Zohor,
  Asar, Maghrib, Isyak every 8 seconds. Syuruk stays as supplemental information.
  A separate frosted `SEMASA` badge remains visible regardless of the rotated slot.
  Focus or an open dialog pauses rotation. Left/right cycles manually; OK opens options
  including the full six-time schedule. Existing legacy screen retains its six-column strip.
- Home focus path: header Down → prayer tile Down → apps; apps Up → tile Up → header.
  Existing app discovery/icons/launch intents, ordering, wallpaper and Jarvis are retained.
- Prayer transport is now **direct official JAKIM e-Solat**, not api.waktusolat.app.
  POST `https://www.e-solat.gov.my/index.php?r=esolatApi/takwimsolat&period=duration&zone=CODE`
  with form `datestart=YYYY-MM-01&dateend=YYYY-MM-last`. No key/GPS/server needed.
  Select your own state and JAKIM zone (cities are grouped within zones).
- Official, complete validated monthly schedules use the separate `jakim-esolat-v1`
  private cache; old third-party cache is retained but not mislabelled or used. First official
  cache population therefore needs Internet. Only today's matching zone/date is rendered.
  Cache timestamp/origin are displayed. Invalid/offline responses cannot replace good cache.
- Revalidate at Malaysian midnight while Home is foreground, and on resume if overdue.
  Offline failures retry after 15 minutes, no continuous requests. No background service,
  alarm permission, wake lock or claim to wake a sleeping TV. Network Focus intentionally
  disables automatic prayer refresh: restore it or use Refresh manually.
- `SEMASA` is a schedule-interval marker, not a religious ruling. Subuh loses the marker
  at Syuruk; between Syuruk and Zohor, and before today's Subuh, no active badge is claimed.
- Full-screen drizzle is bounded at 48 light streaks plus the existing maximum 96 glass
  droplets, all input-transparent. New/unset settings default to forecast effects ON;
  existing explicit OFF settings are respected. Forecast is MET Malaysia/data.gov.my,
  **not observed real-time rain**. Clear/unknown/expired data does not trigger rain.
- Soft local lightning is **OFF until opted in** in Rain-on-Glass · Weather settings.
  Only a fresh supported thunderstorm-period forecast enables it; no daily-summary inference.
  One low-contrast local bolt every 18–30 seconds, no full-screen white flash, no sound.
  Moving OFF/system animator scale 0 disables motion/lightning; menus/background pause effects.
  Disable lightning if sensitive to light. Existing adaptive fallback and resource limits remain.
- Preview PNGs are native-view fixture renders, explicitly marked simulation, not TV screenshots
  or evidence of current weather. Generic Android app icons are test package-manager fixtures,
  never replacements for real discovered application icons in the APK.

Historical instructions below describe earlier versions. See this delivery's report for exact
build/test results, changed files and remaining physical-device limitations.

---

# FayyadhOS TV · 2.8 Master — Reference UI Redesign

## Current approved continuation: direct-key Jarvis and right sidebar

This source now uses native HTTPS directly to Groq (`api.groq.com`), NOT the legacy
backend/provisioning path. `backend/` and older verification reports are retained history;
do not start or deploy them for this version.

Setup on the TV: Jarvis > Jarvis AI Settings > enter your own key > Save / replace key >
Choose model from Groq > select a chat-capable model returned for your key > Test connection.
Model listing alone is not proof of chat access. No shared key/model is bundled. Test calls
use the user's account and may incur usage. Never put keys in chat, source, build scripts,
or diagnostic logs. Keys are encrypted with AES-GCM using an Android Keystore key. If secure
storage fails, the app fails closed; it does not fall back to plaintext. A rooted/compromised
running device can still expose credentials. Remove deletes this installation's saved key,
not the Groq account key; revoke it with Groq if exposed.

The right sidebar uses a separate opaque dialog window; it does not shrink or hide the home
layout, and therefore temporarily covers the rightmost underlying home items. Closing/Back
restores focus and exposes the unchanged full home. This is NOT a claim of pixel-perfect
mockup parity. App icons, prayer zone/data and network facts are real installation values,
not the example mockup's values. More actions/content can be reached by D-pad scrolling.
Spoken output and startup TTS are preserved; there is no microphone/recognition input.
Weather now uses the band between header and greeting so it remains outside the sidebar.
"Tiada hujan" is not assumed to prove a clear sky; explicit "Cerah" still uses the sun icon.

New actions use native Android APIs: discovered installed apps/YouTube, Wi-Fi settings,
weather details and readable system information. Wi-Fi link rate is not Internet speed.
The explicit Cloudflare download sample has a total deadline, is not a full speed test, and
is not sent to AI after a network change or once older than five minutes. No upload, ping,
packet loss or fictional hardware information is generated.

Build a DEBUG test APK using JDK 17, Android SDK 34/build-tools 34.0.0:
`bash ./gradlew --no-daemon :app:testDebugUnitTest :app:assembleDebug --console=plain`.
Output: `app/build/outputs/apk/debug/app-debug.apk`. For a production-signed release, keep
the original signing setup. Never uninstall a working launcher to resolve a signature mismatch.
See the delivery report for actual test/build results; tests use mocked Groq transport and
an injected AES key for storage tests, NOT a live Groq key or hardware Keystore certification.
Physical TV/IME, firmware Keystore, live Groq/weather and signed-release checks remain required.
No runtime dependency was added. No claimed performance improvement was measured.

---

### Earlier project documentation (historical context)


Android TV / box launcher for Android 8+. Not firmware or a ROM.
Package: dev.farred.fayyadhos.tv.jarvis. VersionCode 10; versionName 2.8-master.

## Redesign delivery and safe installation
This archive modifies the existing native dashboard, not a replacement application. See `docs/REDESIGN-REPORT.md` for changed files, checks, screenshots and limitations. The package, version code/name, SDK levels, build variants and release signing configuration are unchanged.

The accompanying `FayyadhOS-TV-Redesign-debug.apk` is a **debug-signed test build**, NOT an update signed with your production certificate. It cannot update an installed differently signed release with the same package name. Do not uninstall your working launcher just to install this test APK. Test on a spare device or rebuild with your original release keystore and credentials. No production signing key was supplied or generated as a substitute.

With your original release certificate, normal Android update behavior retains saved preferences. TV Plus 2.6 uses a separate package and is not automatically migrated. Keep the original system launcher as a recovery path. Test the remote before choosing default Home; firmware determines whether that option is available. No Accessibility/ADB bypass is required by this dashboard.

## English UI and speech
The dashboard branding, menus, prayer options and Jarvis report remain English. Dashboard clock and date formatting follow the device locale and local time. Third-party app labels, JAKIM place/zone names, provider names and Android system dialogs remain as provided by their source or system.
Prayer labels: Fajr, Sunrise, Dhuhr, Asr, Maghrib, Isha. Sunrise is not an obligatory prayer.
English speech uses an installed Android text-to-speech engine, preferring an offline English voice. The app tries US, UK then generic English language support. It is not a film voice clone and does not use a microphone.
If English speech is unavailable, the UI remains usable. Open Jarvis > Options > English voice settings to install/enable a supported English voice through Android. Firmware may not provide that settings page. Network-dependent voices may require Internet. Media volume and mute are respected; the launcher does not force volume.

## Hello, Master
Startup greeting is ON by default. It says exactly: "Hello, Master."
- Plays after FayyadhOS comes to the foreground for the first time after a detected boot, subject to speech engine readiness.
- Does not repeat for every normal return from another app or activity recreation after a successful greeting.
- An observed Android SCREEN_OFF event arms one more greeting for the next SCREEN_ON/foreground return.
- A pending greeting is cancelled when Home is left. Speech stops when Home is paused or destroyed. Late engine callbacks cannot start speech after cancellation.
- Jarvis > Options > Startup greeting: ON/OFF toggles it independently of Jarvis report voice.
- Jarvis > Options > Play greeting provides a manual check/replay while startup greeting is enabled.

Important: turning on only the TV panel does not necessarily restart or wake the Android box. HDMI-CEC and firmware may not send Android screen events. The app cannot promise a greeting on every press of the TV power button. It cannot speak before Android starts and Home opens. Default Home behavior, standby events, audio routing and installed voice availability require a real-device check.
Boot identification uses Android BOOT_COUNT where available; firmware withholding it falls back to an approximate boot epoch. Large system-clock corrections may affect that fallback.

## Cinematic glass dashboard
The home screen follows the supplied sunset reference: left FayyadhOS / TV EDITION wordmark, centered live clock/date, right Jarvis and Settings, time-sensitive greeting, a horizontal app carousel, and a six-entry prayer strip. Dark translucent cards and prayer panel, a subtle wallpaper gradient and cyan focus edges improve readability. No animated blur, new runtime libraries or remote wallpaper URL is required.

11 bundled wallpapers. `11-Kinabalu-Sunset.jpg` is the new default for users without a saved choice. Existing saved wallpapers remain unchanged; choose Settings > Wallpaper > 11 · Kinabalu Sunset to apply it manually. It is local AI-edited artwork based on the supplied reference, with all interface elements removed.

The carousel shows genuinely installed apps with their actual Android icons. No example streaming apps or fake shortcuts are bundled. Initial focus is the first available app; returning Home restores the previous app. More apps scroll horizontally. Up goes to Jarvis; Down goes to prayer options. The header and prayer panel return to the remembered app. Back retains the existing Settings-focus behavior.

Hold OK on an app > Move, use Left/Right, then OK/Back to finish. Hide/unhide via Settings. Hiding is not uninstalling or a security lock. Jarvis/dashboard menus continue hiding the foreground while open.

## Network and prayer data
Jarvis shows the actual network information Android exposes. SSID/band/link speed can be unavailable because of permissions or firmware. Grant Wi-Fi name permission explicitly from Options if wanted; GPS coordinates are not read.
Wi-Fi link speed is not Internet throughput. Test Internet downloads a sample of up to 8 MiB through Cloudflare, with no upload or full subscription-speed validation. Test only runs when selected.
Light boost pauses the launcher's automatic prayer/Places refresh only. It does not increase bandwidth, stop other apps or change router/DNS/VPN. Restore reinstates the saved launcher refresh options.
Prayer source: https://api.waktusolat.app/ . The provider states that it uses JAKIM data; this is not an official JAKIM app. Select your correct state and zone manually. Cache/status/manual refresh remain available. No valid schedule for today means --:--, not stale/demo times.
Prayer calculations use Asia/Kuala_Lumpur. Keep the TV clock correct. The main dashboard clock follows the device locale’s short-time format (for example 12-hour AM/PM or 24-hour); prayer times remain 24-hour for clarity.

## Verification and real-device checklist
Redesign verification: 137 JVM/Robolectric tests passed, zero failures/errors/skips. Debug APK assembly and release source compilation succeeded. Android lint reports 45 errors and 51 warnings also present in the untouched source; see the report for exact details. The historical files directly under `verification/` describe the supplied original build; **current results are under `verification/redesign/`**.

The 720p, 1080p and 4K dashboard images are native Android view renders in Robolectric, not physical TV/emulator screenshots. Test-only app registrations have generic Android icons; cached prayer fixtures are not live prayer data. No demo apps, fixtures or sample prayer times are included in the production launcher. The unconfigured screenshot shows `--:--` and an explicit zone-selection prompt.

No physical device or Android emulator was attached. No audible speaker, boot/standby, HDMI-CEC, firmware Home recovery, production certificate update, live API connection or real installed-app launch was verified here. View-level remote/focus dispatch and outgoing launch component intents were tested in Robolectric, including minimum SDK 26.

On your box, check: original-certificate update without preference loss; remote Left/Right through more than seven apps; OK/Enter launch and return focus; Up/Down header/prayer navigation; move/hide; Back; actual icon/label rendering; 720p/1080p and overscan settings; correct Sabah zone/manual refresh/cache/error behavior; TTS and cold boot/standby; original-Home recovery. Do not treat a rendered fixture schedule as today's timetable.

## Source / signing / build
Private key and passwords are excluded from delivery. Future APK updates require the same signing key. Do not assume a fresh self-generated key can replace this installed APK.
JDK 17, Android SDK/platform 34 and build-tools 34.0.0; Gradle 8.9, AGP 8.5.2, Robolectric 4.17. Internet is needed for dependencies.
Set JAVA_HOME, ANDROID_HOME and FAYYADHOS_RELEASE_KEYSTORE (absolute path), FAYYADHOS_RELEASE_STORE_PASSWORD, FAYYADHOS_RELEASE_ALIAS, FAYYADHOS_RELEASE_KEY_PASSWORD. Run bash build-fayyadhos-tv.sh. Generated build outputs/local machine paths are excluded from source.

## Attribution
TidyTV https://github.com/jolan94/tidytv (MIT), Glass TV https://github.com/cocombee/glass-tv (MIT). See NOTICE and license files. No proprietary font file is bundled.
