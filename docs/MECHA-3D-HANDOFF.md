# 3D mecha continuation — prototype workstream

Updated: 2026-10-29. Status: **IN PROGRESS / DESKTOP PROTOTYPE ONLY**.

## Why this exists

The user rejected the mini-robot delivered in GO Preview 2.11.2: four 2D poses per rival, translation/bounce, and small beam effects do not meet the requested 3D animation quality. Reusing or recolouring that sheet is not a 3D upgrade. The newer direction is realistic-proportioned miniature mecha, genuinely articulated running, braking, aiming, recoil and evasion, with coherent materials/light and restrained effects. Commercial-value presentation is a target, not an achieved certification.

## Scope boundary

- Build an isolated visual proof of concept before changing Android rendering.
- Do not change the app carousel, weather or GO measurement engine in this experiment.
- Do not silently add a WebView, heavyweight engine, new permissions or remote model downloads to the TV application.
- No Gundam branding, copied licensed game model or assertion of franchise rights. Use original/licensed assets and retain provenance. CC0 is the creator's grant, not a warranty about every unrelated third-party right.

## Candidate selected for inspection

Ramon Linares's `atlas-09` asset repository, pinned upstream commit `1b8fb9d54160215c071c5a29a49b1c36dc01f0df`:

- `public/models/atlas-09.glb`: heavy armored character.
- `public/models/aether-02.glb`: athletic light armored character.
- Creator's [asset grant](https://github.com/RamonLinares/atlas-09/blob/1b8fb9d54160215c071c5a29a49b1c36dc01f0df/ASSET-LICENSE.md): models, textures, rigs, baked animations and concepts under CC0 1.0; project code separately MIT.
- Original animation provenance: [Quaternius CC0 library notes](https://github.com/RamonLinares/atlas-09/blob/1b8fb9d54160215c071c5a29a49b1c36dc01f0df/assets/animations/quaternius/README.md).
- Creator's [production-readiness assessment](https://github.com/RamonLinares/atlas-09/blob/1b8fb9d54160215c071c5a29a49b1c36dc01f0df/PRODUCTION-READINESS.md) limits the supplied characters to controlled prototypes/presentation, warns about open cut boundaries, approximate pivots, missing arbitrary-motion/collision support and close-up limitations. Do not erase those caveats or turn desktop FPS into a TV guarantee.

No model has been approved by the user yet. Suitability must be checked from actual renders, not only a licence or a pleasing static image.

## Prototype plan

1. Download only immutable model/licence files and verify pinned SHA-256s; keep the GLBs out of Git text-tool payloads.
2. Inspect actual GLB JSON for skinned meshes, bone chains, clips, channels, texture sizes and geometry. Save machine-readable inventory.
3. Render a studio-scale two-character test with a real WebGL skinning/animation mixer. Use supplied locomotion/fire clips, not hand-translated still images.
4. Capture deterministic run, aim/fire and recovery states; check clipping, foot contact, frame continuity, direction, light/material readability and the full scene.
5. Show the user a short MP4 marked **desktop 3D prototype, not TV capture**. Keep source, package lock, asset fetcher, licence files, manifests and capture recipe available for the next AI.
6. Update this document with actual outputs, what was tested, what failed, and the next unresolved choices. Do not promise a new 3D APK yet.

## Release gates still open

- User acceptance of model design, proportions and animation quality.
- Bespoke two-character choreography, robust action transitions and collision/ground contact.
- Android engine/asset delivery choice, APK size budget and TV performance/thermal/memory profiling.
- Lifecycle/back/dialog/screen-off/reduced-motion/OFF switch regressions on the final renderer.
- Asset and dependency review plus source notices retained in any distribution.
- Wide-banner carousel restoration and weather diagnosis are independent pending workstreams.


## Desktop prototype result — 2026-10-29

An isolated, reproducible motion study is now available under [`experiments/mecha3d/`](../experiments/mecha3d/README.md). This is **desktop visual proof only**, not Android integration, final combat choreography, user approval or a TV benchmark.

- Exact upstream GLBs inspected: ATLAS has 18 bones, four skinned primitives and 10 clips; AETHER has 18 bones, five skinned primitives and 11 clips. Every clip has 54 transform tracks. Original asset bytes, licenses and manifests are pinned; the fetcher verifies SHA-256 before accepting bytes. No binary model is added to Android.
- Three.js renders the actual skinned meshes with PBR materials, lights, shadows and an environment. Run, KneelFire, Sentinel and AETHER DodgeRoll are driven by animation mixers. Root travel remains illustrative; chapter fades are editorial cuts, not a claim of smooth combat transitions.
- Silent MP4 generated: 18 seconds, 1280×720, H.264, 24 fps, 432 distinct captured frame hashes. Full decode check passed. Those are export settings, **not real-time browser/TV FPS**. Output is local/session-delivered; it is not committed as a large binary. Reproduce with `npm run capture`.
- Audit: 72 samples at 0.25-second intervals; both rigs have 18 bones, skinned primitives and changing shin rotations. Sampled model bounds stay within the desktop camera; lowest sampled vertex is approximately −0.0565 scene units, below the audit's 0.1-unit tolerance. This is not collision handling or a comprehensive ground-contact guarantee. No page errors in capture/audit.
- Inspected silhouette, two running poses, entry to aim, kneeling/recovery, roll entry/midpoint/recovery and final pose. Desktop 1280×720 and narrow 390×844 layouts inspected; phone framing was corrected before handoff. `verification-summary.json` records exactly which states were visually reviewed.
- `node --check` on renderer/capture/audit scripts and Python compilation passed. No app Java, weather, GO engine, carousel, manifest, Gradle signing or APK distribution was changed for this experiment. The 412-test local Android baseline reported earlier is not a validation of this desktop renderer.

### Next unresolved step

Show the clip and obtain the user's judgment on model design/proportions and movement. If accepted, refine gait/root synchronization and continuous two-character choreography before choosing Android delivery. If rejected, replace the candidate models or motion style instead of relabelling this prototype as premium or commercial-ready. Native Android still contains the older 2D decoration. Keep banner restoration and weather diagnosis separate.
