# 2.10.0 — Home refresh

Based on the 2.9.1 stability branch (PR #1). No merge to main, automatic release, production signing or device install is performed.

## Requested changes

- **Full artwork:** wider 16:9 artwork area, installed icon-pack mapping first, then the installed app's activity TV banner, application banner and icon fallback. FIT_CENTER preserves the whole logo instead of cropping it. The carousel remains infinite, with saved order/hide choices and focus restoration.
- **Community pack:** Projectivy Icon Pack by SicMundus86, referenced in r/Projectivy_Launcher. Install the optional pack yourself, then Settings → Artwork. Auto prefers an installed pack whose label contains Projectivy. This app reads the standard compiled `xml/appfilter` (or asset fallback) and the installed pack's drawable resources. No third-party artwork/APK is copied into this repository and no APK is silently downloaded or installed. Pack updates are picked up when returning Home. Missing mappings fall back to native artwork; no promise that every regional/sideloaded app has an icon.
- **Current weather:** remove +3/+6 columns and the home-screen “Cuaca jam: tetapkan lokasi” prompt. Show temperature, condition, optional user-entered location label, provider timestamp and attribution. Production requests only Open-Meteo's `current` fields. Refresh every 15 minutes while Home is active; pause on screen-off/background; failed requests back off and cached readings are labelled. After 2 hours cached data is unavailable, not passed off as live.
- **No invented location:** existing confirmed coordinates migrate automatically because the preference keys are unchanged. A new installation still needs Settings → Cuaca → coordinates; no GPS or silent coordinate guessing. Open-Meteo coordinates/label are independent of the MET district and are never falsely labelled as Kinarut. Coordinates are transmitted only when current weather is enabled. Free Open-Meteo hosted API is for non-commercial use; commercial distribution needs its appropriate plan/self-hosting.
- **Brand/greeting:** FayyadhOS alone uses bold italic. Greeting is 20 design-dp lower; Good Morning / Good Afternoon / Good Evening / Good Night retain the existing time boundaries and use Title Case.
- **Daily life quote:** replace entertainment tagline with a short quote (max 110 characters), author and source credit. Free keyless ZenQuotes batch endpoint; cache one successful batch per Malaysian calendar day, select one short entry per day locally, and back off 6 hours after failure. No geolocation/identifier is sent. Cached quotes work offline; original, unattributed FayyadhOS reflections provide a fallback. Settings → Daily quotes offers the source link and online ON/OFF. Disabling online access retains offline reflections/cache.
- **Control center:** calmer opaque navy surface, wider remote-friendly action grid, clear section labels, concise network summary with details on demand, optional AI setup and readable focus. Speedtest's existing limits, cancel/back behavior, native actions and encrypted AI-key handling are retained. Full diagnostic text stays available but does not occupy the initial home view. A failed speed test is still a failure, not displayed as a successful measurement.

## Sources and attribution

- Community reference: https://www.reddit.com/r/Projectivy_Launcher/comments/1mtmall/introducing_the_projectivy_icon_pack/
- Pack and install instructions: https://github.com/SicMundus86/ProjectivyIconPack
- Optional pack 1.1.9 inspected for compatibility only: package `dev.projectivy.blueprint.app`; compiled resource `xml/appfilter`, standard ComponentInfo mappings and ADW/Anddoes theme discovery. Its asset appfilter is a sample; the compiled resource takes precedence. No reference APK or extracted artwork is committed.
- Inspirational quotes provided by ZenQuotes API: https://zenquotes.io/ ; usage, caching and attribution: https://docs.zenquotes.io/zenquotes-documentation/
- Current weather: https://open-meteo.com/en/docs ; based on 15-minute model data, not an on-site observation or second-by-second realtime feed.

## Validation / limits

The full serial build/test/lint/DEX audit is `bash build-verified-debug.sh`. Dedicated HomeRefreshTest covers current-only responses, stale/invalid data, quote parsing/selection/cache/backoff/pause, icon-pack mapping/fallback, typography and preview geometry. Old assertions for +3/+6 and lowercase greeting are updated to the explicitly requested behavior; other regression coverage remains.

Robolectric previews at 720p, 1080p and 4K use clearly marked synthetic app banners and weather/prayer fixtures; they are not TV captures or a claim that third-party pack artwork was tested on a physical device. Existing MET-driven weather wallpapers and soft dissolve, prayer/azan, Jarvis, Dim and manual controls remain. Hardware-specific overscan, remote navigation, external icon-pack resource access, standby/reboot and audio/overlay behavior still need physical-TV acceptance.

CI APKs remain ephemeral debug-signed test builds. Do not uninstall a working launcher or clear data to bypass signature mismatch. Use the original installed signing key for a data-preserving update.
