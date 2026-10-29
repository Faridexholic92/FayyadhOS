# Mini mecha rivals — decorative Home animation

Requested: two tiny Gundam-style robots running, chasing and exchanging sci-fi shots.

## Behaviour

- Two AI-generated chibi mecha sprite characters: white/navy and crimson/charcoal. Generated fan-style artwork; not an official Gundam product or licensed asset pack. No assets were extracted from a game.
- Four sprite poses per character, directional mirroring, running bounce, a bounded 20-second chase/duel loop and small alternating cyan/gold beams. Silent; no audio, fullscreen flashes, network calls, permissions or third-party animation dependency.
- One safe design-space lane at x40/y278, 880x96, above the app dock and below quote/weather content. Existing app-card geometry, artwork selection, weather sources and GO measurement engine are unchanged.
- Settings → Mini robot · ON/OFF, appended at index 16; earlier Settings indices stay unchanged. The dedicated preference defaults ON for the requested feature and persists independently of artwork/weather.
- Non-focusable, non-clickable, accessibility-decorative, touch-transparent. Menu overlays, background, screen-off and destruction cancel the frame callback. Nested dialogs cannot resume it until every overlay closes. System reduced-motion disables the decoration.
- 20 fps normally, 10 fps on Android low-RAM devices. One decoded 512x320 atlas (~640 KiB); shared rectangles and paints, no per-frame bitmap creation. Asset is released while inactive. Asset decode failure hides the decoration.
- The 128-colour PNG atlas (45,418 bytes, 512x320) is stored as base64 text (`assets/mini-mecha/rivals.b64`) for text-only repository tooling; decoded locally on activation. No download at runtime.

## Release limits

Native-render tests and demo use simulated installed-app fixtures, not a physical-TV performance certification. The existing GO Preview package can be updated only with its own matching test certificate. Production package/signing remain untouched; never uninstall the original launcher or clear data to bypass a signature conflict.

## Test coverage

Dedicated tests cover input transparency, safe geometry, running playhead, nested-menu gating, screen-off/on, pause/resume/destruction, persistent OFF isolation, reduced motion, low-RAM frame cap, continuous bounded paths, native 720p/1080p states and frame capture.

Two existing menu-count assertions are updated from 16 to 17, with an explicit new-item assertion; old action indices remain checked. The rain pixel-restoration test compares the independent robot animation at the same playhead before/after, without hiding, cropping or masking pixels. Default Android non-clickable View behaviour is retained instead of adding a touch override.

APK preview version code 19 uses `dev.farred.fayyadhos.tv.jarvis.fix.gopreview`, the same package and test certificate as the previous delivered GO Preview. Its signature is not represented as the original production signature.

## Verification result

- Full `:app:testDebugUnitTest :app:lintDebug`: PASS. 412 tests, 411 passed, 1 skipped; 0 failures, 0 errors. One existing local-only video fixture is excluded from the commit.
- Lint: 0 errors, 88 warnings; existing warnings are not claimed fixed.
- Isolated preview: MiniMecha and GO targeted tests PASS; APK built and actual DEX audit passed (418 app class definitions; 13 manifest components). Embedded atlas verified.
- APK v2 signature verified; certificate SHA-256 matches the previously delivered GO Preview: `f36196d18042434142a4cf740e626e6ca5b9d8464a9b909b1dbb9d1c7e403a78`. Original production signing was not obtained or replaced.
- Native 720p/1080p running/duel frames inspected. A 20-second silent video is composed from the Android-rendered animation lane over a static fixture Home; not a physical-TV capture.
- Physical-TV animation performance, long-term standby and in-place preview upgrade remain user acceptance checks.

The full 412-test run preceded the final palette-only atlas optimisation (222,200 to 45,418 encoded PNG bytes). The final palette asset was visually inspected, then all 8 MiniMecha tests and the preview APK build passed again. No Java behaviour changed during that optimisation.
