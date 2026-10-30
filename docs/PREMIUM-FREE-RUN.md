# Free-run animation pass — FayyadhOS 3.0

This pass changes only character motion on the existing native home screen. Dashboard, time/prayer widgets, weather, Jarvis, app dock, input, and existing preferences remain untouched.

## Implemented in this pass

- Two separate closed Catmull-Rom routes, sampled by arc length for steadier travel speed.
- Routes reserve the birthday greeting column and dock, and keep the two character sprites in separate lanes.
- Continuous direction changes in both axes, with a small direction-aware body lean.
- Ground-level depth scaling, bounded character size, and independent smooth jump envelopes.
- Desynchronised run-cycle timing and subtle, character-specific body bobbing.
- Position derived from elapsed time, so frame drops do not accumulate movement drift.
- Existing 12-frame artwork retained; sprite pose cadence remains about 15 fps. Full/Lite renderer cadence remains 30/15 fps caps, not a device benchmark.
- Automated tests for greeting/dock clearance, character separation, loop seam continuity, two-axis direction changes, independent jump phases, desynchronised sprite cycles and OFF/lifecycle behavior.

## Important distinction: 2D motion is not the approved 3D goal

This is an immediate improvement pass using the approved PapaZola/BoBoiBoy atlas; it is not rigged 3D. The separate experiment/mecha3d-motion-study pull request contains two licensed mecha GLB candidates, not models of PapaZola or BoBoiBoy. They must not be silently substituted into the launcher as if they were the requested characters.

Next 3D gate: obtain/create PapaZola- and BoBoiBoy-specific rigged GLB assets. The existing separate mecha GLBs are not substitutes. Only then integrate Filament/glTF behind the current sprite fallback and verify package size, GPU/CPU load, lifecycle pause/resume, reduced-motion/OFF and physical-TV performance. No extra runtime engine is added to the launcher in this motion pass, and no unrelated home UI is changed.


## Review of the 7.27-second user recording

The submitted capture showed PapaZola entering the left greeting area, both characters moving on a simple visual plane, and their running poses cycling in sync. This pass responds by moving both routes out of the greeting column, keeping them in separate curved lanes, pacing motion by sampled route length, and de-synchronising the two flipbook cycles. It remains 2D artwork with 2D motion; the 3D asset and Filament integration gate is still open.
