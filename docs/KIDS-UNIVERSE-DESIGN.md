# FayyadhOS Kids Universe — visual direction

Status: design specification only. This document does not claim that the Android UI or APK has been changed.

## Target device
- Hako Pro Android TV, 4 GB RAM, Android TV OS 14
- 16:9 landscape; responsive 960 × 540 design coordinate system already used by DashboardActivity
- Remote-first focus navigation; keep animation cost low and provide a reduced-motion fallback

## Visual direction
- Edition name: **FayyadhOS Kids Universe — BoBoiBoy × PapaZola**
- Bright deep-blue / cobalt sky, warm gold highlights, luminous cyan accents and an adventurous floating-island landscape
- Keep characters in the lower-middle / foreground; preserve readable negative space behind live information
- Wordmark remains exactly “FayyadhOS”; edition label is secondary

## Live dashboard composition
1. Top-left: FayyadhOS wordmark and small Kids Universe edition label.
2. Left: large live clock and Gregorian date.
3. Upper-middle: next prayer countdown and five prayer times, populated by existing prayer data.
4. Upper-right: existing Jarvis and Settings actions; weather/location/forecast stays live and transparent.
5. Lower-left: greeting text, not baked into the wallpaper.
6. Bottom: real installed-app dock, using the existing app catalog and RecyclerView focus system.

## Functional invariants
- Do not bake clock, date, greeting, weather, prayer times, countdown, app icons, or labels into the wallpaper.
- Preserve the current prayer calculation/API, MetMalaysia weather repository, Jarvis panel, settings flows, package identity, app discovery, and remote D-pad navigation.
- Weather and prayer information must remain transparent overlays: no opaque rectangular cards.
- Use the generated concept image only as visual direction until an appropriately licensed/approved production asset is supplied.
- Avoid shipping copyrighted character artwork without appropriate rights. Use user-provided/licensed artwork or a generic original adventure theme for distributable builds.
- No network-loaded animation loops. Prefer lightweight local assets and short, bounded animation; pause when screen is off and respect reduced-motion settings.

## Implementation plan
- Add a selectable Kids Universe wallpaper/theme option through the existing wallpaper/config mechanism.
- Keep all dashboard information as native Android views above the wallpaper.
- Tune dock spacing, focus glow, typography and safe areas at 720p/1080p/4K.
- Verify on the actual Hako Pro: cold start, D-pad traversal, app launch, weather/prayer refresh, Jarvis/settings, screen-off pause, and sustained memory/animation performance.
- Build and test a debug APK before producing any release APK.

## Current limitation
The image generated during the design discussion is a concept mockup, not a production-ready asset package. It contains illustrative UI text/icons and must not be used as the actual dashboard background.
