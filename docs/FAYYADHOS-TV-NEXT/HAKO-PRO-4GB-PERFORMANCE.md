# HAKO Pro 4GB — Hardware and Performance Baseline

**Owner-provided target:** HAKO Pro with 4 GB RAM  
**Status:** Hardware family identified; individual firmware / driver still needs device-side verification.

## 1. Published HAKO Pro hardware family

The HAKO Pro S905Y4-B data sheet lists:
- Amlogic S905Y4 SoC.
- Quad-core 64-bit ARM Cortex-A35 CPU.
- DDR4 2 GB / 4 GB RAM configurations.
- ARM Mali-G31 MP2 GPU.
- OpenGL ES 3.2, Vulkan 1.1 and OpenCL 2.0 graphics capabilities.
- Android 11 in the original hardware specification.
- 4K60 video output capability.

The Android TV Guide also lists HAKO Pro with S905Y4, Cortex-A35, Mali-G31 MP2 and 2 GB / 4 GB RAM variants.

Sources:
- Manufacturer specification PDF: https://www.h96tvbox.com/wp-content/uploads/2023/01/HAKOPro-Y4-B-Specification.pdf
- Device reference: https://www.androidtv-guide.com/streaming-gaming/hako-mini-2022/

**Important:** These are published model-family specifications. They do not prove the exact firmware, available RAM, graphics driver, refresh mode or storage size on the user's individual unit. The 4 GB RAM capacity is user-confirmed; chipset and driver should still be checked on the device.

## 2. Baseline rendering profile

### Default profile — Enhanced

| Setting | Initial target |
|---|---|
| UI output | Native TV output; test 1080p and 4K |
| Internal 3D render target | 1280 × 720 initial baseline |
| Scene frame rate | 30 FPS sustained target |
| Characters | Two visible rigged characters |
| Geometry | Start around 15k–25k triangles per character; validate actual model |
| Textures | 512–1024 px; no 4K character textures |
| Texture container | KTX2 where supported by the chosen asset pipeline |
| Compression fallback | ETC2 / compatible compressed texture |
| Lighting | One key directional light plus low-cost environment lighting |
| Shadows | Contact/blob shadow first; dynamic shadow maps only after profiling |
| Effects | Small bounded particle count; no full-screen post-processing requirement |
| Backend | OpenGL ES baseline; Vulkan experimental until verified on the installed driver |

The 4K video decode specification must not be treated as proof of 4K real-time 3D rendering capacity. The initial scene therefore renders at 720p internally and scales to the display. A 1080p internal scene target may be enabled only after profiling. 4K internal scene rendering is not a first-release requirement.

## 3. Quality tiers

### Enhanced (default)
- 720p internal scene render.
- 30 FPS cap/target.
- Two characters.
- Bounded PBR materials and environment light.
- Contact shadows, limited transparent effects.
- Full skeletal animation and movement behaviour.

### Ultra (conditional)
- 30 FPS by default; 60 FPS only when display mode and sustained frame-time measurements support it.
- Higher render scale, better shadow quality or extra particles are enabled independently.
- If 60 FPS cannot be maintained, return to 30 FPS instead of allowing repeated stutter.

### Lite
- Lower internal render resolution and texture detail.
- Simplified lighting and shadows.
- Reduced particle count.
- Character locomotion and animation blending remain active; do not reduce the actual pose animation to a 10–15 FPS flipbook.

### Motion OFF
- Stop the scene render loop.
- Retain static wallpaper / background and all launcher functions.
- Honour Android reduced-motion settings and app lifecycle.

## 4. Profiling targets

The frame-time budget is:
- 30 FPS: 33.3 ms per frame.
- 60 FPS: 16.7 ms per frame.

For 30 FPS acceptance, aim for p95 frame time at or below 33.3 ms and dropped frames below 3% during the sustained test. Track p99 frame time as well; averages alone can hide visible pauses.

The first performance budget is a measured target, not an assumed property:
- Process PSS target: keep typical Home use around or below 350 MB after loading both characters, subject to real measurements.
- Maintain at least 500 MB system memory headroom during sustained Home operation where the OS exposes meaningful available-memory readings.
- Avoid repeated allocation in the render loop.
- Release decoded assets on scene destruction; reuse buffers and material instances.
- No network, file or blocking disk work on the render thread.
- No more than two active hero skeletons in the first release.

If profiling shows that the PSS or frame-time goals are unrealistic, adjust model complexity, render scale or effects before raising memory budgets.

## 5. Device verification checklist

Run this on the actual HAKO Pro before locking the hardware profile:

- [ ] Android version and build number.
- [ ] Device model / board / SoC.
- [ ] RAM reported by Android and memory available after boot.
- [ ] GPU renderer string and OpenGL ES version.
- [ ] Vulkan device and driver version, if exposed.
- [ ] Current display resolution and refresh rate.
- [ ] Storage free space.
- [ ] Thermal status after 10 and 30 minutes under load.
- [ ] D-pad / Bluetooth or IR remote behaviour.
- [ ] Suspend / resume and screen-off behaviour.

### User-side verification

AIDA64 or Device Info HW can show System, CPU, GPU and Display details. The user can send screenshots of:
1. System / Android version.
2. SoC / CPU.
3. Display / GPU renderer.

Do not request passwords, API keys, serial numbers or account identifiers.

## 6. Acceptance test sequence

1. Boot directly into the new launcher without opening the 3D scene.
2. Run a static scene with both model assets loaded.
3. Run the two animation controllers at 30 FPS for 5 minutes.
4. Enable roaming, jumps and interactions; profile for 10 minutes.
5. Run continuous Home for at least 30 minutes.
6. Launch and exit multiple installed apps.
7. Open and close Settings / dialogs while the scene is active.
8. Test screen-off, resume, Motion OFF and renderer fallback.
9. Repeat at 1080p and the user's normal TV output mode.
10. Save frame-time, memory, thermal and crash/ANR evidence.

The HAKO Pro is the primary acceptance device; an emulator is supporting evidence only.
