# FayyadhOS TV Next — Product Specification

**Status:** Approved planning baseline  
**Hardware target:** HAKO Pro, user-confirmed 4 GB RAM  
**Product direction:** Balanced BoBoiBoy × PapaZola  
**Repository:** `Faridexholic92/FayyadhOS`  
**Development branch:** `feature/fayyadhos-tv-next-foundation`

## 1. Product goal

Build a new Android TV Home launcher from a clean application architecture. It must feel like a premium animated TV experience while retaining the useful features from the current FayyadhOS launcher.

This is a new product line, not another patch to the existing sprite-based home screen. The old launcher remains intact as a fallback throughout development.

The two hero characters have equal visual importance:
- PapaZola is not a side decoration or a secondary cameo.
- BoBoiBoy is not the permanent lead while PapaZola only follows.
- Both have independent behaviour controllers, animation clips, movement schedules and interaction responses.

## 2. Product principles

1. **TV-first:** every primary action is reachable with a D-pad or standard TV remote.
2. **Animation behind the interface:** the character scene must never block focus, clicks, widgets or app launching.
3. **Function parity:** features in the current FayyadhOS release are carried forward or explicitly documented as intentionally changed; none are silently dropped.
4. **Performance before effects:** stable frame pacing and remote responsiveness take priority over expensive shadows, particles or maximum texture detail.
5. **Safe migration:** the new package is tested side-by-side; the existing launcher is never uninstalled or cleared to make an update work.
6. **Honest release status:** a successful compile is not a stable release. Stable requires automated tests and acceptance testing on the target HAKO Pro.

## 3. Home screen information architecture

The Home screen is a landscape 16:9 composition that scales to 720p, 1080p and 4K output. The UI remains sharp at display resolution; the 3D scene may render at a lower internal resolution and upscale.

### 3.1 Layout zones

| Zone | Position | Content |
|---|---|---|
| A — Top bar | Top edge | FayyadhOS brand, network state, Jarvis shortcut, Settings |
| B — Clock and prayer | Upper-left / left column | Large clock, date, five fardu prayer times, selected JAKIM zone and schedule status |
| C — Hero stage | Center | PapaZola and BoBoiBoy 3D scene; open movement lanes; no text-critical content underneath |
| D — Weather | Upper-right / right column | MET forecast, optional Open-Meteo Now / +3h / +6h, clear provider labels |
| E — App dock | Bottom edge | Real installed app icons, names, focus scaling and looping carousel |

The Home composition must use a safe area around the outer edges to account for television overscan. Widgets should use transparent or low-opacity glass surfaces rather than opaque card grids. Avoid large permanent rectangles around every element.

The hero stage is the dominant visual area but must preserve readable time, prayer and weather information. Characters may roam within the stage; they cannot cross the app dock or cover actionable UI.

### 3.2 Visual identity

- **Foundation:** deep midnight navy, cinematic blue and restrained ice-blue accents.
- **BoBoiBoy identity:** blue/cyan as primary accents, with character-specific color accents only when justified by the approved character design.
- **PapaZola identity:** warm red, gold and orange accents.
- **Balance:** matching visual scale, screen time and prominence; neither character permanently dominates the composition.
- **Glass HUD:** translucent, restrained blur/frost treatment only where the device can afford it. A non-blurred alpha fallback is mandatory.
- **Typography:** high contrast, large enough for viewing from a sofa, with a maximum of two font families.

This is a design system, not permission to alter the characters' defining appearance. Character model and texture fidelity must be reviewed against approved references.

## 4. Character scene and behaviour

### 4.1 Independent character controllers

Implement separate controllers for PapaZola and BoBoiBoy. A shared behaviour director may coordinate scenes, but it must not make both characters follow the same transform or animation timeline.

Required animation states:
- Idle variations: breathing, looking around, weight shifts and short reactions.
- Walk and run cycles with grounded foot contact.
- Sprint / acceleration and deceleration.
- Jump take-off, airborne, landing and recovery.
- Direction changes with blended turn animation.
- Short interaction clips: greeting, playful response, look-at, chase start/stop and coordinated pose.
- Return-to-idle transitions that do not snap or freeze.

### 4.2 Free-roaming rules

The movement director selects varied paths within a bounded 3D stage. Movement must not repeat one fixed left-to-right loop.

Requirements:
- Distinct movement timing and route for each character.
- Smooth path interpolation and bounded acceleration.
- Avoidance / separation so the characters do not overlap accidentally.
- Look direction follows travel or interaction target.
- Position, depth and apparent size remain visually consistent.
- Idle and interaction states have a cooldown to avoid repetitive behaviour.
- The director pauses or safely transitions when the Home screen loses focus.

Randomness must be seeded or recordable in test mode so animation bugs can be reproduced.

### 4.3 Rendering and animation rules

- Skeletal animation from rigged glTF 2.0 / GLB models.
- Blend between clips; do not switch directly between incompatible poses.
- Use root-motion only when the selected asset pipeline supports it reliably; otherwise use in-place locomotion with code-driven movement and foot-phase synchronisation.
- Start with one key light, environment lighting and low-cost contact/blob shadows. Add real-time shadow maps only after profiling.
- No full-screen flash, loud animation audio or input interception.
- The renderer must pause when the activity is not visible, a modal overlay obscures the scene, screen-off occurs, Motion OFF is selected, or the user launches another app.

## 5. Remote control and focus contract

D-pad navigation is a core acceptance requirement.

| Key | Expected action |
|---|---|
| Up / Down | Move between header, widgets and app dock according to the visible focus graph |
| Left / Right | Move within the current row or carousel |
| OK / Enter | Activate the focused item |
| Long OK | Open app actions such as Move, Hide and App info |
| Back | Close the topmost dialog or return to Home without losing the previous focus |
| Home | Return to launcher where the device firmware allows it |

Focus requirements:
- One visible, high-contrast focus indicator at all times.
- No focus trapped behind the 3D rendering surface.
- Focus restores to the last app component after closing Settings or returning from an app.
- App rearrangement uses a bounded list and persists order.
- The normal app carousel may wrap first-to-last and last-to-first.
- If no applications are available, Settings remains reachable.
- Touch support is optional; it must not be required to operate the launcher.

## 6. Functional parity inventory

The following modules are in scope for the new launcher.

| Module | Required retained behaviour |
|---|---|
| Home / App Manager | Discover installed launchable apps; use real package icons and labels; launch app; app info; Move; Hide / unhide; looping carousel |
| Clock | Time, date, 12/24-hour preference and startup greeting controls |
| JAKIM Prayer | Explicit state/zone selection; validated official e-Solat schedule; cache and manual refresh; clear data-source and cache status |
| Azan | Five prayer schedule; user-supplied audio; separate optional Subuh audio; test, stop, enable/disable, exact-alarm and notification permission handling |
| MET Malaysia Weather | Forecast widget and the existing forecast-driven wallpaper / rain classification |
| Open-Meteo | Optional user-configured coordinates; Now / +3h / +6h modelled display; clear provider attribution; separate from MET location and rain logic |
| Wallpaper | Manual and automatic wallpaper; weather-linked transitions; bounded image memory; reduced-motion fallback |
| Rain Effects | Rain-on-glass effects, intensity, density, speed, Moving ON/OFF, preview and optional low-intensity lightning |
| Jarvis | Network facts, Wi-Fi / transport / VPN indicators, speed test and AI explanation |
| Jarvis AI Settings | User's own Groq key, model selection, connection test, encrypted local storage and remove-key action |
| Network Focus | Existing Focus mode, snapshot and restore behaviour with status feedback |
| Screen Dim | Explicit 35% dim overlay, required permission, foreground notification and clear OFF action |
| Screensaver | Clock Dream / idle screen behaviour |
| System | Android Settings, Set Default Home, remote settings and diagnostics |
| Accessibility | Meaningful content descriptions, visible focus, reduced motion and readable contrast |

### 6.1 Data integrity rules

- JAKIM schedule data remains distinct from forecast weather data.
- MET Malaysia remains the source for the existing weather effects and automatic wallpaper logic.
- Open-Meteo remains a separate, optional modelled forecast; it must not silently override MET.
- Prayer zone selection remains explicit; do not infer a zone from IP or GPS.
- Azan remains opt-in and uses audio supplied by the user.
- AI keys are never committed, logged or copied into a plain-text migration file.
- No secret, account credential, private location or user audio is bundled in the repository.

## 7. Settings information architecture

1. Appearance & Theme — wallpaper, character visibility, scene effects, motion profile.
2. Applications — installed app list, order, hidden apps and App info.
3. Prayer & Azan — JAKIM state/zone, schedule, audio and alert settings.
4. Weather — MET, Open-Meteo, manually selected location and weather effects.
5. Jarvis & AI — AI key/model/test, network status and speed test.
6. Display & System — dim overlay, screensaver, Android Settings, default Home.
7. Performance & Diagnostics — renderer backend, frame pacing, memory, thermal status and asset validation.

Settings must be presented as remote-friendly rows or panels, not a dense phone-only form.

## 8. Architecture requirements

Proposed application architecture:
- Kotlin Android application, minSdk 26 provisionally.
- Native Android TV Home activity and focusable view hierarchy.
- A separate scene-rendering layer using Google Filament.
- glTF 2.0 / GLB asset loading and skeletal animation.
- Independent repositories for prayer, weather, wallpaper and preferences.
- Lifecycle-aware jobs and explicit pause/resume handling.
- A feature boundary between launcher UI and the 3D renderer so renderer failures fall back to a static scene rather than taking down Home.

A proposed parallel-install package ID is `dev.farred.fayyadhos.tv.next`. It is provisional until the first runnable prototype and signing strategy are approved. The current application ID must remain unchanged in the old launcher.

## 9. Quality and acceptance criteria

A build is not Stable until all applicable checks pass.

### Functional
- All feature-parity items have a regression test or documented manual test.
- App discovery, launching, carousel wrap, Move, Hide and App info work on the HAKO Pro.
- Prayer zone, schedule, Azan audio and permissions have been tested on device.
- MET and Open-Meteo labels and source boundaries remain correct.
- Jarvis key storage remains encrypted and secrets are absent from logs.

### Visual / animation
- Both characters are recognisable and visually balanced.
- Idle, walk, run, sprint, jump, landing and interaction clips blend correctly.
- No repeated fixed left-right bounce.
- No persistent foot sliding, floating, snapping, character overlap or UI occlusion.
- Scene pause/resume does not restart at an arbitrary pose or continue rendering invisibly.

### Performance / reliability
- 30 FPS is the initial sustained target on the HAKO Pro 4GB.
- 60 FPS is an optional profile and is enabled only after device measurements prove it.
- At least 30 minutes of continuous Home operation without crash, ANR, memory leak or sustained thermal degradation.
- Frame-time distribution, dropped frames, memory, GPU renderer and Android thermal status are recorded.
- D-pad interactions remain responsive while the 3D scene is active.
- Renderer failure, missing model or unsupported graphics API leaves the Home UI usable.

### Release
- Debug and release builds compile.
- Automated tests pass.
- Emulator smoke test passes.
- HAKO Pro physical-device acceptance is signed off.
- Release is signed with the controlled release key.
- GitHub release contains APK, version, SHA-256 checksum and test summary.

## 10. Explicitly out of scope for the first release

- A full 3D game or user-controlled character.
- Network multiplayer or live cloud animation control.
- Unbounded physics simulation.
- Streaming a video as a substitute for character animation.
- 4K internal 3D rendering as a requirement.
- Automatic device overclocking, rooting or system partition modification.

## 11. Planning decisions still open

- Exact Android version/build and actual EGL/Vulkan driver on the user's HAKO Pro.
- Confirmed output mode / refresh rate on the connected TV.
- Actual character-specific rigged GLB assets and the right to distribute them.
- Final package ID and release-signing key process.
- Whether 60 FPS is viable after sustained hardware profiling.

These are explicit gates; they must not be silently guessed.
