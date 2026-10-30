# FayyadhOS TV Next — Implementation Roadmap

Status: Engineering foundation being established
Primary branch: feature/tv-next-hako-pro-4gb
Target hardware: Hako Pro, user-confirmed 4 GB RAM

## What is in this branch

The new Android TV project is isolated under tvnext/. It has its own Gradle root and application ID so it can be installed alongside the existing FayyadhOS launcher during development.

The first engineering checkpoint is intentionally limited to:
- Android TV Home entry point and landscape shell
- Remote / D-pad focus and navigation
- Installed app discovery and app launching
- Clock and date
- Device information panel (model, Android version, memory, display and reported GLES capability)
- Filament Android dependencies for the future 3D scene
- Unit tests and GitHub Actions build/emulator smoke test

This is a development preview, not a feature-complete replacement.

## Product and UX direction

- PapaZola and BoBoiBoy receive equal visual weight.
- Central hero stage remains clear of the clock, prayer/weather HUD and bottom app dock.
- Use transparent glass panels with restrained borders, readable 10-foot TV typography and obvious D-pad focus states.
- No fake character artwork or generic mecha will be presented as the real PapaZola / BoBoiBoy models.
- Character movement must use a real-time 3D scene with proper rigging, skeletal animation, time-based motion and independent behaviour controllers. The old sprite loop is not the implementation path.

## Hardware strategy

- Do not assume the SoC, GPU, Android version or graphics driver from the Hako Pro product name.
- Read actual model/build, available and total RAM, display dimensions and reported GLES capability from the box.
- Capture the live GL vendor / renderer / GL version from an initialized graphics context before enabling the full scene.
- Keep 1080p output and a 30 FPS target as the initial profile. 60 FPS remains disabled until physical profiling validates frame pacing and thermal stability.
- Keep a Lite fallback and ensure Motion Off stops the 3D render loop.

## Migration plan

1. Foundation: separate application, Home shell, app discovery, D-pad, diagnostics, reproducible debug build.
2. Renderer validation: live EGL/Filament probe on emulator and Hako Pro; record renderer, memory and frame-time behaviour.
3. Character pipeline: integrate user-provided or properly licensed PapaZola and BoBoiBoy GLB assets with rigs and animation clips.
4. Feature migration: app management; clock/theme/wallpaper; JAKIM prayer zones and schedules; Azan; MET Malaysia weather; Jarvis/Groq encrypted configuration; network tools; dim overlay and Clock Dream; Android settings and diagnostics.
5. Regression and device QA: feature parity, reboot/default-Home, app-return focus, remote navigation, 30-minute sustained run, thermal and memory checks.
6. Stable release: reproducible signed release APK, documented changelog and GitHub Release asset.

## Non-negotiable release gates

- CI build and unit tests pass.
- Emulator install, launch and navigation smoke test pass.
- Physical Hako Pro installation and Home/default-launcher behaviour pass.
- Character assets, licenses and animation quality are verified.
- All legacy features are migrated and regression-tested.
- No repeated stalls or UI freezes in a 30-minute run.
- Frame pacing and thermal behaviour are measured on the actual box.
- Release signing uses a private key that is never committed to this repository.

## Current limitations

The first preview does not yet contain the real 3D characters, final renderer integration, JAKIM/Azan, live weather, Jarvis/Groq or the complete legacy settings system. The existing launcher remains the fallback until the new app passes the release gates.
