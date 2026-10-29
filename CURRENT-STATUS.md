# FayyadhOS — current status and next handoff

Updated: 2026-10-29. **Read this before older reports.** This file is the continuation entry point; dated reports are evidence, not automatically the current product contract.

## User direction (latest)

1. Keep the working explicit **GO** download test. User confirmed the delivered preview's Speedtest works on their TV; this is feedback, not broad device certification.
2. Restore the older **wide app-banner carousel**: large landscape artwork, name below, individual thin rounded glass borders, side-edge carousel continuation. Do not remove every card border. The newer large glass dock wrapper and boxier cards are not the requested reference. **Restoration is pending.** Do not publish the user's TV photos.
3. Investigate weather freshness and the always-rainy background. Current-condition card (Open-Meteo) and wallpaper/rain forecast (MET) are independent. Do not claim a screenshot proves a frozen fetch. **No weather repair has been made for this report.**
4. **Replace the rejected 2D robot treatment with a genuinely rigged 3D motion prototype.** User requested a more realistic mini-mecha direction with polished, commercial-value presentation. Prove quality in a short clip before Android integration. Do not call the old sprite decoration premium, 3D, commercial-ready, or user-approved.
5. Keep GitHub progress, status, limitations and concrete next actions current so another AI can continue.

## Runtime baseline / distribution

- Working branch: `feat/2.11.0-native-glass`, PR #4, stacked on the unmerged device-fix branch; do not merge unrelated overlapping work blindly.
- Runtime baseline before this 3D exploration: `199b8c41bbe945ea372af4c581e7e1e4e0c07a69`.
- Latest delivered test APK: `FayyadhOS-2.11.2-GO-Mecha-Preview.apk`, version code 19, package `dev.farred.fayyadhos.tv.jarvis.fix.gopreview`. It contains the **rejected 2D sprite robot**, not the new 3D prototype.
- The original production package `dev.farred.fayyadhos.tv.jarvis.fix` is separate. Its signing key is not available. Do not uninstall the user's original launcher or clear data to bypass a signature conflict.
- GO Preview uses its own established test certificate, SHA-256 `f36196d18042434142a4cf740e626e6ca5b9d8464a9b909b1dbb9d1c7e403a78`. Private keystore material is not in GitHub. Verify certificate continuity before any preview update; stop rather than substitute a key if the matching private key is unavailable.
- No production release, main merge or commercial certification has been performed. GitHub Actions uploads reports, not temporary runner-signed APK updates.

## 3D exploration — IN PROGRESS, not integrated

See [docs/MECHA-3D-HANDOFF.md](docs/MECHA-3D-HANDOFF.md). Isolated work belongs under `experiments/mecha3d/`. Android `MiniMechaView` still uses 2D sprite playback until an explicitly reviewed replacement is implemented.

Current approach: inspect actual skinned GLB characters and baked locomotion/fire clips, using creator-published CC0 assets pinned to an immutable upstream commit. Render a deterministic desktop proof of concept. No unreviewed model is being bundled into the APK.

## Next actions, in order

1. Verify asset licensing/provenance, inspect mesh/rig/clips and render actual 3D movement. Reject bad deformation, skating, detached armor and floaty running.
2. Show a short prototype clip; explicitly separate visual proof from Android runtime performance and user acceptance.
3. Only after visual acceptance, choose Android delivery: native real-time 3D versus an honestly labelled pre-rendered 3D sequence. Profile on the user's TV; preserve lifecycle, D-pad, touch transparency, reduced motion and OFF switch. Do not fake 3D by sliding a flat image.
4. Restore the wide-banner carousel against the supplied visual reference without undoing GO. Keep this as a separate change with 720p/1080p validation.
5. Diagnose weather fetch/cache/provider timestamps, failure visibility, district/coordinate consistency and wallpaper source before proposing a weather fix.
6. Before distributing any next APK: run relevant regressions, verify package/version/certificate/embedded assets and actual DEX, state exactly what is and is not included, and update this file and the handoff.

## Evidence and limits

- GO: [docs/SPEEDTEST-GO-UI.md](docs/SPEEDTEST-GO-UI.md).
- Rejected sprite implementation and its historical checks: [docs/MINI-MECHA.md](docs/MINI-MECHA.md). Historical full local suite was 412 tests (411 passed, 1 skipped, including one local-only video fixture); lint 0 errors / 88 warnings. These counts do **not** certify a future 3D engine.
- Location/signing: [docs/LOCATION-SPEED-RECOVERY-2.11.1.md](docs/LOCATION-SPEED-RECOVERY-2.11.1.md).
- Native glass: [docs/NATIVE-GLASS-2.11.md](docs/NATIVE-GLASS-2.11.md).
- Old 2.9.x/2.10.x headers and historical APK/report claims must not override this status.
