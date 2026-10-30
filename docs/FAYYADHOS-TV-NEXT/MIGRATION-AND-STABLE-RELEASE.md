# FayyadhOS TV Next — Migration and Stable Release Policy

## 1. Keep the existing launcher safe

- The current FayyadhOS release stays installed and usable throughout development.
- TV Next uses a provisional separate package ID: `dev.farred.fayyadhos.tv.next`.
- Do not uninstall the old launcher, clear its data or disable its Home activity during development.
- Do not merge the old experimental animation PR as the new launcher foundation.
- The old launcher becomes the recovery option until the HAKO Pro acceptance gate passes.

The package ID is provisional and must be locked before the first installable prototype. A package ID change after release is an application migration, not a harmless rename.

## 2. Preference migration

Create a versioned, explicit export/import format for non-secret preferences only.

Potentially migratable values:
- Selected theme and wallpaper identifier.
- App ordering and hidden-app component keys.
- Clock format and greeting preferences.
- JAKIM zone code.
- Rain effect preferences and quality settings.
- Open-Meteo opt-in state and user-entered coordinates only if the user explicitly chooses to export them.
- Dim desired state should default OFF on the new app until separately enabled.
- Screensaver preferences.

Migration must:
- Validate schema version.
- Validate values and package component references.
- Treat unknown values as defaults.
- Never auto-enable sensitive services simply because a preference was imported.
- Never overwrite the old app's preferences.
- Be user-initiated and reversible.

## 3. Data that must not be silently copied

- Groq API keys or other credentials.
- Keystore master keys or encrypted blobs tied to the old app's Android Keystore alias.
- Azan audio unless the user explicitly exports / reselects files and has rights to use them.
- Personal diagnostics, private logs or account identifiers.

The user should re-enter their own AI key in the new app, where it is stored using the new app's Android Keystore identity. Azan audio should be re-imported through the document picker. Never ask the user to post credentials or signing passwords in chat.

## 4. Stable APK definition

A stable APK must satisfy all of the following:
1. Release variant builds successfully with R8 / resource shrinking as configured.
2. All required unit and instrumentation tests pass; intentional skips are listed.
3. Emulator smoke test installs and launches the actual new application package.
4. HAKO Pro 4GB acceptance passes at 30 FPS target for at least 30 minutes with no crash / ANR or unacceptable thermal degradation.
5. D-pad focus, installed app launch, Home recovery, dialog navigation and app resume are verified on the physical box.
6. Both character assets and animations pass visual / rights checks.
7. All feature parity checks are completed or any known difference is explicitly approved.
8. Release APK is signed with the controlled release key and its signing certificate is verified.
9. SHA-256 checksum, version, changelog and test evidence are published alongside the APK.

A debug APK or an emulator-only build is a preview, not Stable. Do not label it Stable.

## 5. Signing and GitHub delivery

The release keystore stays under the user's control. It is never committed to GitHub and never pasted into a chat message.

When the project is ready, configure GitHub Actions secrets using a secure, private workflow:
- Release keystore material (Base64 or a secure file retrieval method).
- Keystore password.
- Key alias.
- Key password.

Prefer a protected release workflow that only runs on a reviewed tag or approved manual dispatch. The workflow should:
- Build the release APK.
- Run tests before signing.
- Verify package name, version and signing certificate.
- Compute SHA-256.
- Attach APK and checksum to a GitHub Release.
- Preserve build logs and test summary.

If the required signing material is not configured, the release workflow must fail closed or produce a clearly labelled unsigned/debug preview. It must never invent or silently substitute a new signing key for an installed stable package.

## 6. Release stages

| Stage | Artifact | Meaning |
|---|---|---|
| Engineering build | Debug APK | Internal developer preview only |
| QA candidate | Release-signed candidate APK | Physical-device testing; not Stable yet |
| Stable | Release-signed APK + checksum + test report | Passed all required gates |
| Hotfix | Versioned patch release | Fixes a confirmed regression with regression test |

## 7. Git workflow

- `main` remains the current stable line.
- `feature/fayyadhos-tv-next-foundation` contains the new product documents and future launcher foundation.
- Existing `feature/papazola-premium-home` stays separate as historical experiment / source reference.
- No merge to main without a reviewed pull request and passing acceptance.
- APK builds are attached to tagged GitHub Releases only when the Stable gates pass.
