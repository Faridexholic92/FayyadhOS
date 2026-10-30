# FayyadhOS TV Next — Architecture and 3D Asset Pipeline

## 1. Application shape

Use a new Kotlin-based Android TV launcher application, with clear boundaries between:
- Home / launcher shell.
- TV remote focus navigation.
- App catalog and app actions.
- Glass HUD and widgets.
- Filament scene host.
- PapaZola character controller.
- BoBoiBoy character controller.
- Shared behaviour director.
- Prayer / Azan services.
- MET and Open-Meteo repositories.
- Jarvis / network features.
- Wallpaper, effects and screensaver.
- Preferences, migration and diagnostics.

A single app process is preferred for the first release. Do not create a multi-process renderer. Expensive scene code must not own or block the D-pad input path.

## 2. Proposed stack

| Layer | Choice |
|---|---|
| Language | Kotlin |
| Android | Native Android, minSdk 26 provisional |
| Home UI | Native focusable Views / custom ViewGroups |
| TV navigation | Explicit D-pad focus graph and focus restoration |
| 3D renderer | Google Filament |
| Scene assets | glTF 2.0 / GLB |
| Texture pipeline | KTX2 with compatible compression |
| Animation | glTF skeletal animation clips and blending |
| Data | Repository pattern, local preferences and validated caches |
| Background work | Lifecycle-aware executor / WorkManager only where needed |
| Build | Gradle + GitHub Actions |

Native Views are the initial choice to preserve deterministic TV focus and keep the UI lightweight. Compose or additional UI frameworks can be evaluated later; do not add them solely for novelty.

## 2.1 Isolated build environment

The new launcher is a separate Gradle project at `tvnext/`. It is intentionally not included in the legacy root Gradle build.

- Legacy root project: AGP 8.5.2, Gradle 8.9, compileSdk 34; do not upgrade it for TV Next.
- TV Next project: AGP 9.4.0, Gradle 9.6.0, compileSdk 37, targetSdk 34, minSdk 26.
- AGP 9 built-in Kotlin is used; do not apply `org.jetbrains.kotlin.android` again in the TV Next project.
- The TV Next wrapper lives at `tvnext/gradlew` and `tvnext/gradle/wrapper/`.
- The Home package ID is `dev.farred.fayyadhos.tv.next`, allowing side-by-side installation.
- Android 37 is currently acquired from the preview SDK package `platforms;android-37.0`; the CI records the exact SDK setup. This is a build-time compile SDK only. The app target remains API 34 and the HAKO Pro runtime is expected to be Android 11 until confirmed on-device.

Commands from the repository root:

```sh
cd tvnext
./gradlew :app:assembleDebug :app:testDebugUnitTest
```

This isolation keeps the old launcher build and its package untouched while the new renderer is developed.

## 3. Filament version policy

Pin a single stable Filament release for all runtime and host-side asset tools. The official Filament repository currently documents Android Maven dependencies and warns that asset tools such as `matc` should use the same release as the runtime.

**Implementation baseline: Filament v1.75.1 from Maven Central.** Maven Central currently exposes v1.75.1 for the Android runtime artifact; the public GitHub repository has a newer v1.79.0 release, but the matching Android artifacts were not yet verifiable in Maven Central at the time this baseline was written. Do not reference a dependency version that Gradle cannot resolve.

Baseline runtime artifacts (all pinned to the same version):
- `com.google.android.filament:filament-android:1.75.1`
- `com.google.android.filament:gltfio-android:1.75.1`
- `com.google.android.filament:filament-utils-android:1.75.1` only if required by the selected UI host

The exact artifacts and ABI size must be tested before they are committed to the application. Use runtime libraries and host asset tools from the same v1.75.1 release. Revisit v1.79.0 only after its required Android Maven artifacts are available and a clean build/test confirms them. Start with the OpenGL ES backend. Vulkan remains an opt-in experiment until the actual HAKO Pro driver is validated.

Sources:
- Filament official releases: https://github.com/google/filament/releases
- Maven Central artifact: https://central.sonatype.com/artifact/com.google.android.filament/filament-android

## 4. Renderer integration boundary

The renderer must be hosted behind a small interface such as:

- `SceneHost.start()`
- `SceneHost.pause()`
- `SceneHost.resume()`
- `SceneHost.setQuality(profile)`
- `SceneHost.setMotionEnabled(enabled)`
- `SceneHost.release()`

A renderer exception, missing GLB asset, unsupported extension or device capability failure must not crash the launcher. The scene falls back to a static approved background while Home, remote focus, time, prayer, weather and Settings remain usable.

The rendering surface must be behind all interactive HUD and app views. Validate SurfaceView / TextureView composition and transparency on the HAKO Pro; do not assume emulator composition exactly matches the device.

## 5. Character controller contract

Each character controller owns:
- Asset instance and skeleton.
- Animation state and current clip.
- Transition/blend weights.
- World transform and movement velocity.
- Look direction and interaction target.
- Collision / separation radius.
- Idle cooldown and behaviour cooldowns.
- Deterministic seed for reproducible automated tests.

The shared behaviour director owns only:
- Scene-level event scheduling.
- Character separation and stage boundary rules.
- Interaction invitations and response timing.
- Avoiding simultaneous identical actions.
- Safe stage pause / resume.

Do not bind animation timing to a fixed frame count. Derive movement from monotonic time and drive rendering through display-aware frame scheduling.

## 6. Asset production pipeline

Required assets for each hero:
- Character mesh and UV layout.
- High/low detail model variants if needed.
- Full skeletal rig with named joints.
- Skin weights tested at shoulders, elbows, knees, hips, wrists and neck.
- PBR texture set.
- Idle, walk, run, sprint, jump, landing, turn and reaction clips.
- Consistent scale, forward axis, ground origin and root orientation.
- License / source / attribution record.

### Suggested GLB acceptance rules

- No missing texture or external URI dependencies.
- Correct embedded material references.
- No negative or degenerate scale transforms.
- Character faces the agreed forward axis.
- Feet contact the ground in idle / run / landing clips.
- No skinning collapse or obvious volume loss at joints.
- Clips blend at transition boundaries without snap.
- Model remains within the HAKO Pro polygon / texture budget.
- All model and texture rights are documented before bundling.

## 7. Character licensing gate

PapaZola and BoBoiBoy are existing franchise characters. Before creating a distributable APK containing custom models, ensure the models, textures, voices and other franchise assets are either supplied with permission / licence suitable for the intended distribution or are otherwise cleared for that use.

Do not download random fan models or include them in a public GitHub release without checking rights. For early renderer testing, use clearly labelled neutral placeholder geometry or a user-provided asset whose rights are confirmed. Replace placeholders only after the character-specific assets pass review.

## 8. Repository layout proposal

```text
app/
  src/main/java/.../
    launcher/
    navigation/
    scene/
      SceneHost.kt
      filament/
      behaviour/
      characters/
    prayer/
    azan/
    weather/
    jarvis/
    wallpaper/
    settings/
    migration/
    diagnostics/
  src/main/assets/
    scene/
      environment/
      characters/
      materials/
docs/FAYYADHOS-TV-NEXT/
  PRODUCT-SPEC.md
  HAKO-PRO-4GB-PERFORMANCE.md
  ARCHITECTURE-AND-ASSETS.md
  MIGRATION-AND-STABLE-RELEASE.md
```

The exact package and module names can be refined when the first compileable skeleton is created. Do not bulk-copy the old monolithic activity into the new app; migrate features behind clear interfaces and add regression tests.

## 9. Testability

Provide:
- Headless unit tests for behaviour selection and movement bounds.
- Asset validation tests for GLB load / missing texture / clip list.
- Renderer instrumentation smoke test.
- Focus navigation tests using D-pad key events.
- Lifecycle tests for pause / resume / release.
- HAKO Pro manual acceptance log for actual frame pacing and thermals.

The emulator must run a deterministic scene test; physical hardware remains mandatory for final performance acceptance.
