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
