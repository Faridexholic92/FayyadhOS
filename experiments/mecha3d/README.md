# Mecha 3D motion study — desktop prototype

Isolated proof of rigged 3D motion for FayyadhOS. **Not Android integration, not a TV recording, not a commercial release.** No app Java, carousel, weather, GO engine, application ID or signing configuration is changed by this experiment.

## Reproduce

Requires Node 20+, Python 3, Chromium and FFmpeg.

```sh
cd experiments/mecha3d
npm ci --ignore-scripts
npm run assets
npm run inspect
npm run audit
python3 -m http.server 8090
```

Open `http://localhost:8090/?play=1` for the 18-second study. Without `play`, the renderer holds a deterministic frame; `window.renderAt(seconds)` selects a pose. HTTP serving is only for local desktop preview, not an application runtime.

To produce the MP4, stop the server if desired and run:

```sh
npm run capture
```

The capture script resolves the installed Chromium executable, or uses `CHROMIUM_PATH`. It starts an isolated **local** headless browser with software WebGL, renders 432 frames at 1280×720, then encodes H.264 at 24 fps. This is an offline export rate, **not measured TV or browser frame rate**. `output/` is intentionally ignored; it contains the MP4, per-frame SHA-256 report and renderer diagnostics. Never upload the ignored GLBs or node_modules through text-only tools.

## What is actually animated

- ATLAS / 09: four skinned primitives sharing an 18-bone rig; 10 supplied clips.
- AETHER / 02: five skinned primitives sharing an 18-bone rig; 11 supplied clips.
- Every supplied clip contains 54 transform tracks. Run, KneelFire and Sentinel drive the bodies; AETHER also demonstrates DodgeRoll.
- This is a **motion study with editorial chapter fades**, not finished continuous combat choreography. The root travel is illustrative and still requires gait-speed/contact tuning. No claim of collision handling, live IK, final combat logic or arbitrary-motion-safe armor is made.
- PBR materials, a local environment, directional lights, shadows and a ground reference are rendered in Three.js. No slide of a flat robot image, sprite atlas, remote animation API or AI-generated video is used.

## Timing

| Time | Study |
| --- | --- |
| 0–2 s | Sentinel: silhouette and materials |
| 2–5 s | Rigged Run, with independent character cycle durations |
| 5–13 s | Complete eight-second KneelFire pose/recoil/recovery study |
| 13–16 s | AETHER DodgeRoll, ATLAS Sentinel |
| 16–18 s | Return to a presentation pose; Android work explicitly pending |

## Assets and licenses

Models are pinned to Ramon Linares's `atlas-09` commit `1b8fb9d54160215c071c5a29a49b1c36dc01f0df`. `assets-manifest.json` records exact source URLs, sizes and reviewed SHA-256s; the fetcher rejects mismatches. `asset-inventory.json` records actual GLB mesh, skin, material and clip structure. Creator CC0 grant, full CC0 text, original animation provenance, MIT software notices and the creator's production-readiness caveats travel under `licenses/`.

CC0 is the creator's grant, not a guarantee of every unrelated third-party right or an endorsement. These are candidate model designs, **not user-approved final FayyadhOS characters**. They are not presented as official Gundam assets.

## Acceptance still open

User acceptance of shape/proportions and motion; purposeful two-character choreography; smoother transitions and root/foot synchronization; renderer/package-size choice; physical-TV CPU/GPU/memory/thermal/lifecycle/input tests; matching signing credentials before any APK update. The original app remains on the previous 2D implementation until a separately reviewed Android replacement is made.
