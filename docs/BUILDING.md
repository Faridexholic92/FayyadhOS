# Building and verification

## Toolchain

| Component | Version |
| --- | --- |
| Java | JDK 17 |
| Android platform / compile SDK | 34 |
| Android build-tools | 34.0.0 |
| Minimum Android version | API 26 / Android 8.0 |
| Gradle / Android Gradle Plugin | Use the versions pinned in this branch |

Install the SDK and accept the applicable SDK licenses. Set `JAVA_HOME` to JDK 17 and `ANDROID_HOME` to the SDK directory. Set the SDK location through Android Studio or an untracked `local.properties` containing `sdk.dir=/absolute/path/to/android-sdk`.

Use the checked-in wrapper, not an arbitrary system Gradle version. Do not run multiple Gradle invocations against the same project/output directory concurrently.

## Debug verification

From the repository root:

```sh
bash ./gradlew --no-daemon clean :app:testDebugUnitTest :app:assembleDebug :app:lintDebug --console=plain
python3 tools/verify-apk.py app/build/outputs/apk/debug/app-debug.apk
```

The APK verifier checks that declared app components have actual DEX class definitions. It does not replace runtime/device testing. `build-verified-debug.sh` provides the verification sequence defined on the checked-out branch; older branches do not necessarily include lint in that helper.

| Output | Location |
| --- | --- |
| Debug APK | `app/build/outputs/apk/debug/app-debug.apk` |
| Unit-test report | `app/build/reports/tests/testDebugUnitTest/index.html` |
| Lint report | `app/build/reports/lint-results-debug.html` |

Inspect failures, skips and warnings. A successful compile, simulated screenshot or historical test count is not proof that a different APK is stable.

## Release signing

Use the established production keystore. Do not generate a replacement key as a convenience or fall back to debug signing. The configured environment variables are:

```text
FAYYADHOS_RELEASE_KEYSTORE
FAYYADHOS_RELEASE_STORE_PASSWORD
FAYYADHOS_RELEASE_ALIAS
FAYYADHOS_RELEASE_KEY_PASSWORD
```

Provide them through a secure local environment or approved secret store. Keep keystores, passwords and user API keys out of Git, chat, screenshots and logs. The keystore path should be absolute.

After the checks above, build the configured release variant:

```sh
bash ./gradlew --no-daemon :app:assembleRelease --console=plain
```

Output: `app/build/outputs/apk/release/app-release.apk`. Check the embedded package, version code/name and signing certificate, then complete the physical-device and upgrade checks in the [release policy](RELEASE-POLICY.md).

The legacy `build-fayyadhos-tv.sh` may copy the APK under an older hard-coded versioned filename. Do not infer the embedded version or readiness from that filename; inspect the APK itself. This documentation refresh does not change that script or the signing configuration.

## Inspect the APK

With the Android build-tools on `PATH`:

```sh
apksigner verify --verbose --print-certs app/build/outputs/apk/release/app-release.apk
aapt dump badging app/build/outputs/apk/release/app-release.apk
sha256sum app/build/outputs/apk/release/app-release.apk
```

Match the certificate SHA-256 fingerprint against the independently retained production fingerprint. A checksum identifies a file; it does not establish signing continuity, absence of defects or commercial readiness.

## Keep the source clean

Do not commit `local.properties`, `.gradle/`, build outputs, keystores, credentials, user data or machine-specific paths. Preserve executable modes for the Gradle wrapper and build scripts. Document the exact source commit and toolchain used for every distributed APK.
