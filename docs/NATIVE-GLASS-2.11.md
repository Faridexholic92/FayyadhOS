# 2.11.0 native glass · version code 17

Stacked on the unmerged 2.10.1 device-fix branch (PR #3). Main and production releases are unchanged. Merge the device fixes first, then this visual-only follow-up; do not merge overlapping old refresh PRs blindly.

## Native design
- GlassyUI-inspired dark tint, gradient sheen, lit rim and distinct blue/cyan remote focus, implemented with Android Views/Canvas. No React, Expo, WebView, Pro assets, bitmap backdrop capture or new looping animation.
- Glass dock, spaced carousel cards and matching Jarvis, AI settings and standard dashboard dialogs. Home wallpaper/prayer layout remains full-screen; the right sidebar never reserves home space.
- Settings → Glass appearance: Auto, Lite and Solid; selection persists independently of prayer zone, artwork, weather, rain and Dim.
- Auto requests cross-window background blur only on supported Android 12+ windows; system-disabled/unsupported blur and low-RAM devices retain dense readable layered glass. Lite never requests blur. Solid uses opaque flat surfaces, without sheen. Dock/cards are drawn approximations, not live blur.
- Secure AI settings keep FLAG_SECURE. Credentials/network features and all existing launcher functions are retained. Existing system reduced-motion handling is unchanged.
- Upstream MIT attribution in NOTICE, root GLASSYUI-LICENSE.txt and packaged assets/licenses/GLASSYUI-MIT.txt.

## Verification
Clean `bash build-verified-debug.sh` passed locally:
- 391 JVM/Robolectric tests: 390 passed, 1 skipped; 0 failures/errors.
- Lint: 0 errors, 89 warnings (not suppressed or claimed fixed).
- Actual APK DEX audit: 414 app class definitions; all 13 manifest app components present.
- Build-input/executable guard and git diff whitespace check pass.
- Added tests for contrast over a white backdrop, rounded/transparent drawables, disabled/focused/pressed states, opaque Solid mode, persistence isolation, Android 8 fallback, low-RAM blur exclusion and secure AI window.
- Native-render test fixtures at 720p, 1080p and 4K: home, Jarvis, speed screen, Settings, appearance and Solid. AI-settings fixture at 1080p. Cards have explicit scaled-gap/clipping assertions. Existing sidebar sizing, focus/actions and no-home-resize tests retained; old opaque-only expectations updated for dense glass, with separate Solid opacity coverage.

Fixtures use sample apps/default Android icons and unavailable test network/weather, and are not physical-device captures or performance evidence. Production system blur is not proven by Robolectric.

## Device acceptance and installation
This is a debug test APK. Local and GitHub Actions debug certificates may differ from the installed app. Do not uninstall a working launcher or clear its data to bypass signature mismatch. A data-preserving update requires the installed signing certificate.

Physical-TV acceptance remains required: D-pad/back/scroll and long-press reorder; live blur versus fallback on Android 12+ and low-RAM TVs; animation/render performance; actual location providers and approximate permission; installed Projectivy artwork; Speedtest throughput/cancel; prayer/azan, rain, Dim and standby. Existing device fixes are carried forward, not newly proven on hardware.
