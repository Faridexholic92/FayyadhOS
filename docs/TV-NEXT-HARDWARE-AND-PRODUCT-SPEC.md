# FayyadhOS TV Next — Product & Engineering Specification

Status: Planning baseline
Target device: Hako Pro, 4 GB RAM
Development branch: feature/tv-next-hako-pro-4gb
Base: main at 7d1655196268446e8717805905b58292a1138b5a

## Product decisions

- Build a new Android TV launcher architecture; do not continue patching the old sprite-animation implementation.
- PapaZola and BoBoiBoy have equal visual importance.
- Preserve the full existing FayyadhOS feature set through a feature-parity checklist and staged migration.
- Keep the current launcher available as fallback until the new launcher passes device acceptance tests.
- Do not publish a stable APK until build, automated tests, emulator smoke tests, and physical Hako Pro tests pass.

## Hardware profile

Known user-provided target: Hako Pro, 4 GB RAM.
The exact SoC, GPU, Android build, graphics driver, display mode and available storage are NOT yet verified on the user's unit. Marketing-model assumptions must not be treated as confirmed hardware facts.

Initial performance targets (to be measured, not guaranteed):
- Default UI / scene target: 1920x1080 output, 30 FPS sustained.
- Optional 60 FPS profile only after GPU, thermal and frame-pacing validation.
- Lite profile: lower scene resolution and reduced shadows/particles while maintaining a responsive UI.
- Motion Off: stop the 3D render loop completely.
- Budget process memory conservatively; measure PSS, native heap and graphics memory on-device before locking limits.

## Proposed technical architecture

- Kotlin + Android native TV launcher shell.
- Google Filament as the candidate real-time renderer, subject to a device capability probe.
- glTF 2.0 / GLB character assets with proper skeletons, skinning and animation clips.
- Separate controllers for PapaZola and BoBoiBoy.
- Behaviour director for idle, walk, run, sprint, jump, landing, turn, explore and character interaction.
- Time-based movement, animation blending, collision/separation, screen-safe roaming and lifecycle-aware rendering.
- OpenGL ES baseline first; evaluate Vulkan only after confirming driver stability and parity on the actual Hako Pro.
- Existing service/data integrations migrated behind modular repositories and lifecycle-aware components.

## Character asset gate

The project needs properly licensed, character-specific PapaZola and BoBoiBoy rigged 3D assets. Each must provide idle, walk, run, sprint, jump and landing clips (or source animation that can be retargeted). Generic mecha assets and the old six-pose sprite atlas are not acceptable as the primary character animation solution.

No claim of commercial-quality 3D animation can be made until the real assets are integrated and evaluated on the target device.

## Feature parity checklist

The new launcher must preserve:
- App discovery, launch, app ordering, hide/show and app information.
- Clock, date, greeting, wallpapers and theme settings.
- JAKIM prayer schedules, zone selection, caching and refresh.
- Azan scheduling, custom audio, notification and permission handling.
- MET Malaysia forecast and optional hourly weather integration.
- Weather and wallpaper effects, Rain on Glass, lightning and controls.
- Jarvis panel, Groq AI configuration, encrypted key storage and connection test.
- Network information, speed test, Network Focus, snapshot and restore.
- Screen dim overlay and Clock Dream screensaver.
- Android Settings, default Home handling, D-pad navigation and diagnostics.

Each feature requires a regression test or documented device verification before stable release.

## UI / interaction requirements

- 10-foot TV interface, readable at viewing distance.
- Every interactive control reachable by D-pad / remote with visible focus.
- Clear focus restoration after dialogs, app return and settings navigation.
- Central character stage must never obscure clock, prayer/weather HUD or app dock.
- Transparent glass panels; avoid excessive opaque cards and decorative borders.
- App dock must use actual installed application icons and launch intents.

## Performance profiles

1. Enhanced (default): 1080p output, 30 FPS target, two characters, restrained real-time lighting and adaptive shadows.
2. Ultra (experimental): optional 60 FPS only if sustained tests pass; quality may adapt to protect frame pacing.
3. Lite: reduced 3D render resolution, simplified shadows and particles.
4. Motion Off: static background; 3D rendering paused.

Frame-time stability, thermal behaviour and UI input latency matter more than a peak FPS reading.

## Delivery gates

1. Device profiling: collect exact SoC, GPU, Android build, OpenGL ES/Vulkan versions, display mode, RAM/storage and thermal baseline from the actual box.
2. Asset gate: approve both character models, licenses, rigs, materials and animation clips.
3. Renderer prototype: render both real characters and test animation/frame pacing on Hako Pro.
4. Launcher foundation: Home, app discovery, dock, D-pad, Settings and lifecycle.
5. Feature migration: port every existing feature and validate parity.
6. QA: automated tests, emulator smoke tests, physical-device tests, 30-minute sustained run, cold start/resume and remote tests.
7. Release: produce debug preview first; stable APK only after all required gates pass and release signing is configured.

## Stable APK acceptance criteria

- Successful reproducible Gradle release build.
- Automated tests pass; known skips documented.
- Emulator installs, launches and survives navigation smoke tests.
- Physical Hako Pro installs, becomes default Home, survives reboot and returns from apps.
- No recurring animation stalls or UI freezes during a 30-minute sustained run.
- 30 FPS target validated with frame-time data, not assumed.
- All feature parity items pass.
- Migration and fallback verified.
- Release APK signed with the intended release key; signing configuration is not committed to the repository.

## Open items

- Exact Hako Pro hardware report (SoC/GPU/Android/driver/display).
- Character-specific 3D assets and permitted distribution rights.
- User-provided AIDA64 or equivalent device diagnostics.
- Final Home screen visual approval before full implementation.
