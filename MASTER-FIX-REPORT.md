# FayyadhOS TV Edition — targeted repair report

## Outcome and limits

Targeted source repairs and a secure, configurable GroqCloud request path have been implemented. **This is not a completed exact-mockup restoration or a live-verified TV deployment.** The supplied source does not contain the reference image's right-hand, six-action Jarvis panel. Rebuilding that missing design would conflict with the instruction not to invent or replace components. The existing native UI was retained rather than silently substituting a new design.

No Groq credentials, deployed backend address, production signing key, or accessible Android TV was supplied. Live AI inference, TV-to-backend HTTPS, physical remote behavior, overscan, and the reported persistent-after-exit symptom on the user's television remain unverified.

## 1. Inspected architecture and original behavior

- Native Android Java / Views / RecyclerView, minimum SDK 26, compile/target SDK 34. Not React, Compose, HTML, or WebView.
- `AndroidManifest.xml` routes HOME and LEANBACK_LAUNCHER to `DashboardActivity`. `MainActivity` is a separate, non-exported legacy screen, not the active home entry point.
- `DashboardActivity.onCreate` builds wallpaper and a separate full-screen content layer; fixed design-space positions are scaled for landscape. `JarvisPanel` uses its own Android Dialog window, not an allocated sidebar in the home layout.
- Inspected home placement/styles, app catalog and launch intents, Back handling, Jarvis TTS/worker/probe lifecycle, shared dialogs, prayer/rain dialogs, weather transport/cache, network snapshot and speed probe, manifest, Gradle configuration, assets, tests, and historical verification outputs.
- No original Git repository, recoverable source history, backup implementation of the reference Jarvis sidebar, or backend was present in the archive. A local Git baseline and `fix/jarvis-layout-groq` branch were created before edits. The uploaded archive remains untouched.

## 2. Blank-space findings — verified versus unverified

**Confirmed source defect:** the original `DashboardActivity.overlayVisible(true)` called:

```java
root.getChildAt(1).setVisibility(View.INVISIBLE);
```

That child is the entire home-content layer, not just Jarvis. It removes the visible branding, clock, weather, greeting, app cards and prayer panel behind overlays. It does not remove a sidebar's layout allocation. The source also used one unowned visibility boolean for multiple dialog windows, and overwrote `popup` without distinguishing older dismissal callbacks from the current dialog.

**Important boundary:** an independently built, untouched-source Robolectric test reproduced the original hidden-home behavior while Jarvis was open and verified that ordinary dismissal restored `View.VISIBLE`. The particular **persistent gap after exit on the physical TV was not reproduced**. Its device-specific cause therefore cannot honestly be claimed as confirmed. No dynamic flex/grid/sidebar reservation was found in this native home implementation.

### Targeted fixes

- Removed overlay-driven changes to home visibility/geometry entirely. No negative margins, replacement wallpaper, global shrinking or arbitrary new dimensions.
- Added per-dialog ownership. Older dismissals cannot clear a newer dialog's state; `popup` is cleared only by its own dismissal.
- Preserved the previously focused home element, with a Settings fallback when it no longer exists.
- Routed prayer dialogs through the same ownership/focus lifecycle **without adding common-dialog restyling** to them.
- Added explicit legacy Android TV Back-key handling; ordinary Android cancel/dismiss also retains cleanup.
- Activity pause dismisses all tracked windows, not just the latest pointer.
- Jarvis close immediately shuts down resources and also remains idempotent when the dismissal listener runs later.

## 3. GroqCloud — before and after

**Before:** local network snapshot, bounded Cloudflare speed sampling and Android TTS only. No Groq request, provider configuration, key environment variable, backend or AI response handler existed. Existing text was native diagnostic text, not evidence of AI inference.

**After:** the existing **Read again** button still reads the native report when TTS is enabled, and additionally follows this path:

```text
Read again → JarvisPanel → JarvisAiClient → HTTPS POST /api/jarvis
→ authenticated Node backend → Groq models validation + chat completion
→ validated text → existing Jarvis Internet/result region
```

- Backend: Node 22+ standard library only. No new Android dependency, SDK, framework, background service, microphone permission or local LLM.
- Default model: `llama-3.1-8b-instant`, configurable with server-side `GROQ_MODEL`. Official Groq model documentation listed it when inspected. Every deployment also checks its configured model against the authenticated live model list before inference (successful validation cached for one hour).
- Provider endpoints: `https://api.groq.com/openai/v1/models` and `https://api.groq.com/openai/v1/chat/completions`.
- App backend endpoint: **POST `/api/jarvis`** on the operator's HTTPS host. No deployed host was supplied or invented.
- `GROQ_API_KEY` exists only in the backend's environment. The APK never contains it. A separate revocable device-access token protects the backend.
- Only the predefined network-explanation action is accepted. The backend independently constructs prompts and strips extra context/free-form text. SSID, IP, serial, location and installed-app lists are not sent.
- Bounded bodies, HTTPS-only Android requests, redirect rejection, server deadline, sanitized failures, one in-flight request and a six-attempts/minute server limit.
- Existing buttons show loading/failure/completion through the existing result region. Speed and AI actions share the request gate. Close cancels network/future work, shuts down the executor/TTS, clears callbacks and ignores late results. Reopening creates fresh state.

**Real Groq request succeeded? Not verified.** The live-check script explicitly refused to run because server credentials were unavailable. Provider-success/error tests use labelled fixtures and must not be described as live inference.

Provider references: https://console.groq.com/docs/models and https://console.groq.com/docs/api-reference.

## 4. Network and existing actions

- Existing `SpeedProbe` already measures a user-triggered download sample of at most 8 MiB, with a roughly eight-second sampling-loop budget and separate connection/read timeouts. The denominator includes setup time, so it is an estimate, not a full benchmark or a guaranteed strict eight-second wall-clock deadline.
- Existing labels already distinguish estimated Internet download from Android's Wi-Fi link rate. Upload is not measured. No hardcoded 57.4, 48.4 or 780 Mbps values were found in application production sources.
- The exact readings described from the TV photographs cannot be traced to a particular request/session/version without those device records. No claim is made that their provenance was established.
- AI receives an optional **earlier** measured estimate, explicitly marked as potentially no longer current, not an invented or automatically repeated speed test.
- Weather remains the existing actual MET/data.gov.my integration and cache. No live weather or specifications were invented.
- Wi-Fi settings remain a native Android settings intent. Installed applications still use `DashboardApps` and explicit Android component launches; no Groq call is made to launch them.
- The reference's six dedicated Jarvis action buttons do **not** exist in the supplied `JarvisPanel`; its controls are Test Internet, Read again, Voice, Options and Close. Weather exists elsewhere on the home screen; applications use the carousel; Wi-Fi settings are under Options. No dedicated Maklumat Sistem handler was found. These missing reference components were not fabricated or silently declared repaired.
- NetworkSnapshot's physical-device behavior, VPN-underlay attribution and firmware permission restrictions still need device verification; no unrelated network rewrite was made.

## 5. Exact file changes

### Modified existing production files

1. `app/src/main/java/com/gathin/tvhome/DashboardActivity.java` — overlay ownership, focus return, Back handling and lifecycle cleanup.
2. `app/src/main/java/com/gathin/tvhome/JarvisPanel.java` — existing-button AI path, shared request gating, response/loading/error handling, cancel/cleanup and Back handling.
3. `app/src/main/java/com/gathin/tvhome/PrayerPanel.java` — shared dialog lifecycle only; original styling retained.
4. `app/src/main/AndroidManifest.xml` — managed-configuration declaration only; no microphone permission.

### Added production/configuration files

5. `app/src/main/java/com/gathin/tvhome/JarvisAiClient.java`.
6. `app/src/main/res/xml/jarvis_restrictions.xml`.
7. `app/src/main/res/values/jarvis_config.xml`.
8. `backend/server.mjs`.

### Tests and documentation

- Modified: `app/src/test/java/com/gathin/tvhome/DashboardTest.java` and `RainFeatureTest.java`. Their obsolete assertions requiring the home to be invisible were changed to require visible home content.
- Added: `app/src/test/java/com/gathin/tvhome/JarvisRepairTest.java`, `backend/server.test.mjs`, `backend/live-check.mjs`, `backend/README.md`, this report, and `verification/master-fix/` evidence. `.gitignore` was added for local build/signing artifacts.

Integrity check against the supplied ZIP: **no original file missing; 348 original files unchanged, including all 13 main asset files**. Only the six original files named above differ. No wallpaper, app-order persistence, card dimensions, home placement, gradients, drawables, font assets or build dependencies were replaced. New strings use existing text regions, not new visual controls.

## 6. Build and test results

See `verification/master-fix/test-summary.json` and accompanying fresh logs/XML for final counts. Historical evidence elsewhere in the archive and failed intermediate iterations are not final results.

- Full Android JVM/Robolectric suite: **216 tests, 215 passed, 1 skipped, 0 failures/errors**. The skipped test is the existing opt-in live MET verification, not a claimed live AI success.
- **15 new repair tests** pass: Close/Back/Cancel, repeated cycles, nested owners, prayer focus, pause cleanup, response display, missing configuration, timeout/failure recovery, duplicates, closing during AI/download work, native Wi-Fi action without AI, and long text in the existing scroll region.
- Backend: **12 tests passed** using actual local HTTP requests and explicitly mocked Groq transport. Covers model validation, success contract, missing/invalid key, missing model, failure, timeout, validation/allowlisting, body/rate limits, duplicates, disconnect cancellation and recovery. Provider mocks are not provider test evidence.
- Debug APK assembly: **passed**. Release Java compilation: **passed**; a production-signed/minified release APK was **not** built.
- Lint: **failed on 45 pre-existing errors** (44 MissingTranslation, 1 ByteOrderMark). Both baseline and final have 45 errors and 57 warnings, with no new lint issues. The comparison is included. No unrelated translation/BOM edits were made to mask this existing gate.
- Fresh native-view renders at 720p, 1080p and 4K, plus dismissal/response evidence, were inspected. These are **Robolectric renders**, using fixture-installed apps/default test icons and selected cached prayer data — **not TV screenshots or live weather/AI proof**.
- ADB device listing: **no connected device**. No emulator or physical TV installation/launch test was performed.

## 7. UI preservation and unfinished reference restoration

The home dimensions, wallpaper assets/positioning code, typography, card order mechanism, weather/prayer content sources, original dialog layouts and existing buttons were preserved. The deliberate behavioral change is that overlays no longer make the whole home invisible. Loading/errors/model text reuse the existing result region.

**Do not interpret this as a claim of pixel-perfect reference restoration.** The supplied source has an italic wordmark, different wallpaper artwork/card treatment, and a centered scrolling diagnostics dialog rather than the reference's narrow right-side six-action panel. That centered modal still overlays home content while open; it was not replaced with a speculative sidebar. Exact reference matching, including its dedicated actions, needs the original matching source/components and clean assets, or explicit approval to reconstruct them from the image. The mockup's microphone/listening imagery was not treated as permission to add speech recognition.

## 8. Remaining deployment and acceptance steps

1. Provide the original source/components and wallpaper/assets that produced the authoritative reference, or approve a separately scoped reconstruction. Confirm the exact TV build/version and capture the reported post-exit gap/reproduction steps/logs.
2. Deploy `backend/server.mjs` through the existing HTTPS infrastructure; configure server-only `GROQ_API_KEY`, `JARVIS_DEVICE_TOKEN` and optional `GROQ_MODEL`. Follow `backend/README.md`, including proxy limits and safe logging.
3. Provision the TV endpoint/device token via managed app restrictions. Debug-only private ADB provisioning is documented. **A normal unmanaged release TV still needs an approved secure provisioning mechanism**; none existed in the archive, and no insecure exported receiver or hardcoded token was added.
4. Run `node backend/live-check.mjs` with server-side credentials, then independently verify real TV → HTTPS backend → Groq → existing result-region display. Exercise genuine credential rejection, timeout/outage, repeat presses and close-during-loading on the deployed path.
5. Test physical remote focus/Back, repeated/nested dialogs, app launches, Wi-Fi settings, overscan, weather/network permissions and the specific reported persistent gap on the actual TV.
6. Resolve existing lint errors as a separate authorized task if lint-clean release is required. Build/sign the final production APK using the **original release signing key**.

**Installation warning:** the supplied test APK uses a new Android debug signer and the same package ID as the existing launcher. It may not update the installed app because signatures differ. Do not uninstall the working launcher to force installation; that can lose settings and Home behavior. Verify signatures and retain a working stock launcher/recovery route. This debug APK is for controlled verification, not a production replacement.
