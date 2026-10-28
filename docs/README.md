# Documentation

Start with the guide for your task. Historical delivery reports are separated below so old instructions are not mistaken for current behavior.

## Use FayyadhOS

- [Getting started](GETTING-STARTED.md) — requirements, safe installation, setup and recovery.
- [Release policy](RELEASE-POLICY.md) — signing continuity and the requirements for a stable commercial APK.

## Develop FayyadhOS

- [Building and verification](BUILDING.md) — toolchain, debug checks and release signing.
- [Contributing](../CONTRIBUTING.md) — focused changes, bug reports and review expectations.
- [Project overview](../README.md) — features and repository map.

## Historical reference

These records describe particular deliveries. They may contain superseded versions, package names, providers, menu labels and test counts. They do not certify a newer APK or establish current support.

| Record | Scope |
| --- | --- |
| [Archived project README](archive/legacy-readme.md) | Earlier accumulated instructions; preserved rather than overwritten |
| [2.9 feature-pack guide](../START-HERE-FEATURE-PACK.md) | Feature contract and limits for that delivery |
| [Feature-pack verification](../verification/feature-pack/REPORT.md) | Build-specific evidence, not a rolling quality badge |
| [Azan continuation](../START-HERE-AZAN.md) | Earlier azan setup |
| [Startup continuation](../START-HERE-STARTUP-FIX.md) | Earlier startup behavior |
| [Weather continuation](../START-HERE-WEATHER.md) | Earlier weather setup |
| [Redesign report](REDESIGN-REPORT.md) | Earlier UI delivery and limitations |
| [Rain-on-Glass report](RAIN-ON-GLASS-REPORT.md) | Earlier forecast-effects delivery |

Additional root `*-REPORT.*`, `DELIVERY-READ-ME.md` and `.patch` files remain in their original locations to preserve existing links and patch workflows. Do not apply an old patch to a different baseline without checking it first.

## Reading development work

[PR #3](https://github.com/Faridexholic92/FayyadhOS/pull/3) tracks device fixes; [PR #4](https://github.com/Faridexholic92/FayyadhOS/pull/4) tracks the native-glass follow-up. Check their actual merge and validation status before assuming either is included in your build. Screenshots generated from test fixtures are not physical-TV acceptance evidence.
