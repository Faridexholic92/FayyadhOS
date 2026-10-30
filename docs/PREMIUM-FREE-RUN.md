# Free-run animation pass — FayyadhOS 3.0

This pass changes only character motion on the existing native home screen. Dashboard, time/prayer widgets, weather, Jarvis, app dock, input, and existing preferences remain untouched.

## Implemented in this pass

- Two distinct closed Catmull-Rom movement routes over the center action band.
- Continuous route direction changes, including rightward and leftward travel.
- Ground-level depth scaling, with bounded character size and path containment.
- Independent smooth jump envelopes and subtle run bobbing.
- Position derived from elapsed time, so frame drops do not accumulate movement drift.
- Existing 12-frame artwork retained; sprite pose cadence remains about 15 fps. Full/Lite renderer cadence remains 30/15 fps caps, not a device benchmark.
- Automated tests for path bounds, loop seam continuity, bidirectional movement, jump phases, and OFF/lifecycle behavior.

## Important distinction: 2D motion is not the approved 3D goal

This is an immediate improvement pass using the approved PapaZola/BoBoiBoy atlas; it is not rigged 3D. The separate experiment/mecha3d-motion-study pull request contains two licensed mecha GLB candidates, not models of PapaZola or BoBoiBoy. They must not be silently substituted into the launcher as if they were the requested characters.

Next 3D gate: obtain/create appropriate character-specific rigged GLB assets, then integrate Filament/glTF behind a fallback and verify package size, GPU/CPU load, lifecycle pause/resume, reduced-motion/OFF and physical-TV performance. No extra runtime engine is added to the launcher in this motion pass, and no unrelated home UI is changed.
