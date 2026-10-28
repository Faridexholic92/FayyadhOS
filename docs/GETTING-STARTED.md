# Getting started

## Requirements

- An Android TV or TV box running Android 8.0 / API 26 or later.
- A working remote with directional navigation, OK and Back.
- Internet access for provider-backed features; cached data does not make every feature available offline.
- An existing working launcher kept enabled as a recovery path.

FayyadhOS is an app, not a ROM. Default-Home selection, overlay permissions, location providers, speech and standby events depend on the device firmware.

## Choose the right APK

Use the exact APK identified by its release notes. Check its version, package, channel, checksum and signing fingerprint. Do not assume a filename containing “release” means that device acceptance or signing checks have passed.

Debug APKs and CI artifacts are for testing. Their signing certificates may differ from each other and from a commercial installation. For production distribution, follow the [release policy](RELEASE-POLICY.md).

The source baseline for this documentation uses `dev.farred.fayyadhos.tv.jarvis.fix`. Earlier non-Fix packages are not automatically migrated. Verify the package on the actual APK you intend to install.

## Install or update safely

1. Keep the stock launcher enabled. For an unverified build, use a spare test device.
2. For an update, confirm the same application ID and signing certificate as the installed app, and an appropriate increasing version code.
3. Install through the device's normal package-installer process. Approve installation permissions only for the installer you deliberately use.
4. Open FayyadhOS manually. Test Left/Right, OK, Back, app launch and return to Home.
5. Check your saved settings after an update. Only then select FayyadhOS as default Home if the device offers it.

**If Android reports a signature mismatch, stop.** Do not uninstall the working app, erase its data, disable the stock launcher or reset the TV to force the update. The maintainer needs the original signing process.

## First setup

| Feature | What to configure |
| --- | --- |
| Apps | Use Settings for hide/unhide; hold OK on an app for Move where available |
| Prayer | Select the correct state and JAKIM zone; check the TV clock and today's schedule |
| Wallpaper | Choose a local wallpaper or the available automatic-weather option |
| Weather | Configure the provider-specific location; MET and Open-Meteo settings are separate |
| Jarvis AI | Optional: open Jarvis AI Settings, save your own Groq key, choose a returned model and test access |
| Dim overlay | Optional: enable deliberately and grant the required Android permissions; use its OFF action to stop it |

Menu wording and available options vary by version. Android-location detection and native-glass appearance controls belong to development work unless included in the installed build's release notes.

## Privacy and service limits

- Direct-key Jarvis calls Groq over HTTPS using your account. Calls can incur usage. Never share the key in a bug report, screenshot or repository.
- The current direct-key flow does not require deploying the historical `backend/` service.
- Open-Meteo receives configured coordinates when its feature is enabled. Check the implementation and permissions for your particular version; do not assume every version uses Android-location detection.
- Forecasts are not observed weather or an emergency-warning service. A Wi-Fi link rate is not Internet download throughput.
- Prayer times depend on the selected zone and correct clock. FayyadhOS is not an official JAKIM application.

Commercial use of external services needs a terms and access review. See [commercial readiness](RELEASE-POLICY.md#commercial-readiness).

## Report a problem

Include the app version/channel, Android version, TV model, exact steps and expected/actual result. Mention whether it occurs after an update or fresh install. Redact keys, precise coordinates, network identifiers and other private data. See [contributing](../CONTRIBUTING.md).
