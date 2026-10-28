# FayyadhOS

A native Android TV launcher with a glass-inspired home screen, Malaysian prayer times and practical TV controls.

**Android 8.0+ · Java / Android Views · Remote-first**

[Getting started](docs/GETTING-STARTED.md) · [Documentation](docs/README.md) · [Build from source](docs/BUILDING.md) · [Release policy](docs/RELEASE-POLICY.md)

## Built for the living room

- **Your apps, front and centre.** A looping carousel with installed-app artwork, saved order and hide controls.
- **Prayer times for Malaysia.** JAKIM zone selection, cached schedules and prayer options, with existing opt-in azan support.
- **A home screen that adapts.** Local wallpapers, weather-driven scenes and optional forecast effects.
- **Useful controls in one place.** Jarvis brings together network information, Android shortcuts and an explicit download test.
- **AI when you want it.** Optional direct Groq integration using your own key; native TV actions do not require AI.

FayyadhOS is a launcher app, **not firmware or a custom ROM**. It does not replace Android or guarantee control over OEM settings.

## Release status

Development source and test APKs are **not automatically stable commercial releases**. An APK should only be distributed as stable after signing, upgrade and physical-device checks are complete. See the [release policy](docs/RELEASE-POLICY.md).

Ongoing work is tracked separately: [device fixes · #3](https://github.com/Faridexholic92/FayyadhOS/pull/3) and [native glass · #4](https://github.com/Faridexholic92/FayyadhOS/pull/4). An open pull request is not a shipped feature.

## Try it safely

1. Start with the [installation guide](docs/GETTING-STARTED.md).
2. Check the APK's version, package and signing identity before updating.
3. Test navigation and app launching before selecting FayyadhOS as the default Home.

Keep the original launcher enabled as a recovery option. **Never uninstall a working launcher or clear its data to bypass a signature mismatch.**

## Develop

Use JDK 17, Android SDK 34 and build-tools 34.0.0. The repository includes its Gradle wrapper.

```sh
git clone https://github.com/Faridexholic92/FayyadhOS.git
cd FayyadhOS
bash ./gradlew --no-daemon :app:testDebugUnitTest :app:assembleDebug --console=plain
```

Set up the Android SDK first. Debug output is `app/build/outputs/apk/debug/app-debug.apk`; it is not a commercial release. See [building and verification](docs/BUILDING.md) for SDK setup, lint, APK checks and release signing.

## Project map

| Location | Purpose |
| --- | --- |
| `app/` | Android launcher source and tests |
| `docs/` | User guides, developer guides and documentation index |
| `tools/` | Build and APK-verification helpers |
| `verification/` | Historical, version-specific verification records |
| `backend/` | Legacy backend reference; not required by the direct-key Jarvis flow |

Old patch files and `START-HERE-*` documents are retained for traceability, not as the current installation guide. See the [historical index](docs/README.md#historical-reference).

## Contribute

Use a focused pull request, describe the affected version and include verification appropriate to the change. Start with [CONTRIBUTING.md](CONTRIBUTING.md).

## Credits and licensing

FayyadhOS builds on upstream work including TidyTV and Glass TV. Preserve the notices and applicable licenses when redistributing; see [NOTICE](NOTICE), [TidyTV license](TIDYTV-LICENSE.txt), [Glass TV license](GLASS-TV-LICENSE.txt) and [font license](FONT-LICENSE-OFL.txt).

Prayer schedules are sourced from JAKIM e-Solat. Weather providers and Groq have their own terms; review them before commercial deployment. This is not an official JAKIM or MET Malaysia application.
