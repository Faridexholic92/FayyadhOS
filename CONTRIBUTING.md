# Contributing

Keep changes focused and easy to verify. App behavior, documentation and release readiness should agree.

## Before a change

1. Read the [project overview](README.md) and [documentation index](docs/README.md).
2. Start from the intended branch. Check open work before copying a newer feature onto `main`.
3. Keep unrelated UI, networking, signing and documentation changes in separate pull requests.

## Pull requests

Describe the problem, the change, the source baseline and how it was checked. Include known limits and physical-device results separately from automated results. For UI changes, label screenshots with their version and whether they are test fixtures or real-device captures.

Do not treat historical reports as current verification. Do not claim speed, stability, commercial readiness or hardware compatibility without evidence.

## Verification

Follow [building and verification](docs/BUILDING.md). Run the tests relevant to the change and full regression checks for app changes. For documentation-only work, check relative links, file paths, commands and claims against the target branch; a new APK is not required.

Preserve existing signing settings, application identity, user preferences, executable script modes and attribution unless a separately reviewed change explicitly requires otherwise. Never commit credentials, keystores or private user configuration.

## Bug reports

Include:

- App version, channel and source commit if known.
- TV/box model, Android version and relevant permission/settings state.
- Reproduction steps and expected versus actual behavior.
- Whether this is a fresh install or update; relevant previous version.
- Redacted logs or screenshots, with sensitive information removed.

Do not post API keys, keystore passwords, precise coordinates or personal network identifiers. For a suspected secret exposure, stop sharing it and use a private maintainer contact you have verified; do not attach the secret to a public issue.

## Releases

Only artifacts meeting the [release policy](docs/RELEASE-POLICY.md) may be labelled stable. Debug builds and experimental pull requests remain clearly identified as development work.
