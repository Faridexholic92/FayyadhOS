# 2.9.1 — Build and stability

Maintenance continuation of the uploaded 2.9.0 feature pack. No new launcher feature or layout redesign.
Application ID remains `dev.farred.fayyadhos.tv.jarvis.fix`; versionCode is 14, versionName is `2.9.1-stability`.

## Changes

- Build scripts and Gradle wrapper must have executable mode **100755 in Git**. The verified build script also invokes the wrapper through Bash, so extracted ZIPs/web uploads that strip the filesystem mode can still build. On a local Git checkout, restore with `git update-index --chmod=+x gradlew build-verified-debug.sh build-fayyadhos-tv.sh` and commit.
- Complete generic `values-en/strings.xml` covers the existing base English strings. Missing Catalan, German, French, Spanish, Thai, Simplified Chinese and Serbian Latin entries are completed without changing existing translations. Placeholders and the regional `values-en-rUS` date override are preserved. New translations still benefit from native-speaker wording review. No translation-warning suppression, broad `translatable=false` change, or lint baseline is used to hide the 44 MissingTranslation errors.
- `Config.read` identifies an initial U+FEFF BOM by character value instead of a literal invisible BOM byte sequence in Java source. BOM-prefixed JSON still parses; tests cover normal/BOM/empty config.
- Dissolve keeps its 800 ms timing, MET-driven trigger and reduced-motion/foreground rules. Returning to the already incoming scene clears a superseded pending scene without a second decode/fade. A failed decoder retains the displayed key and can be retried. Reuse a RectF and motion preferences; do not allocate a RectF/RainOptions per frame. This is an allocation/logic improvement, NOT a measured FPS, CPU, memory or power claim.
- The Gradle 8.9 distribution is pinned to its official SHA-256, and the GitHub workflow validates the wrapper JAR using the pinned official Gradle action.
- Release script output is named for 2.9.1; signing configuration and original release-key requirements are preserved. No signing key, credential, user coordinate or audio is added.

## GitHub Actions

`.github/workflows/android-validation.yml` runs on push, pull request and manual dispatch. It uses pinned action commit SHAs, JDK 17, Android SDK 34/build-tools 34.0.0, a 30-minute job timeout, and concurrency cancellation per ref. The lasting validation workflow has **contents: read**, does not retain checkout credentials and does not request signing secrets or write repository contents.

It uses an explicit Android SDK setup action on Ubuntu 24.04 rather than relying on the moving ubuntu-latest image. It checks all locale resource coverage/placeholders, source BOM bytes, the Gradle checksum and executable Git modes; then performs one serial clean build, all JVM/Robolectric tests, lint and the actual DEX class-definition audit. A lint error fails the job. Warnings remain visible and are not claimed as resolved.

Successful runs upload a debug test APK, SHA256SUMS and an installation warning. Test/lint reports upload even when the build fails. Artifacts are retained for seven days. There is no automatic release publishing, merge, deployment or production signing.

A single-purpose temporary bootstrap workflow may be used while constructing this branch because GitHub's file-content tools cannot set executable bits. It is restricted to this repair branch and commits only the three named script modes. It must be removed after verifying the mode commit, before handing off the PR. It is NOT part of the lasting CI configuration.

## Local checks

JDK 17, Android SDK 34/build-tools 34.0.0 and Internet for first dependency resolution:

```sh
python3 tools/check-build-inputs.py --require-executable  # Git checkout
bash build-verified-debug.sh
```

For an extracted ZIP without `.git`, omit `--require-executable` on the first command. The build script uses Bash for the wrapper even when mode metadata was lost. Optional `GRADLE=/absolute/path/to/gradle` is still supported. Never run concurrent Gradle invocations on this project output tree.

## Acceptance and signing

Existing features stay: infinite carousel, hourly Open-Meteo display, MET-driven rain/wallpaper, manual wallpapers, Dim 35%, five-prayer glow, opt-in azan, Jarvis, Speedtest and welcome speech. No iCal feature was introduced.

**CI APKs are ephemeral debug-signed test builds**, not updates guaranteed to match your installed certificate. Do not uninstall/clear a working launcher or disable stock Home to bypass a signing mismatch. A data-preserving update requires the original installed signing key. Keep production keys out of GitHub and out of this workflow.

JVM/Robolectric and DEX checks do not prove physical-TV behavior. Real-device D-pad/overscan, overlay permission and cross-app Dim OFF, azan/audio coexistence and standby/reboot still need acceptance testing. No TV/emulator install or production-signed release is implied by a green workflow. Historical verification files remain historical; fresh results are recorded separately for this maintenance work.
