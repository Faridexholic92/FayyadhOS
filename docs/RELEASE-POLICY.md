# Release policy

A stable commercial release is a verified distribution artifact, not a successful debug build or a filename. The following is the required release process; this documentation alone does **not** implement or certify its automated enforcement.

## Channels

| Channel | Purpose | Distribution |
| --- | --- | --- |
| Development / debug | Feature development and automated checks | Internal testing; never presented as commercial stable |
| Release candidate | Production-signed candidate awaiting acceptance | Controlled device testing; explicitly labelled pre-release |
| Stable | Exact production-signed APK that passed every required gate | Customer/commercial distribution with release notes |

Do not silently change an already-distributed artifact under the same version. Record a new version for a changed APK.

## Signing continuity

- Use one established production signing identity for updates to the same app. Do not substitute a debug key or a newly generated keystore.
- Keep the production keystore and protected backups under the owner's control, outside the repository. Use an approved secure signing environment.
- Retain a trusted SHA-256 certificate fingerprint independently of the build artifact.
- Before distribution, verify the candidate's certificate against that fingerprint and confirm the intended application ID and increasing version code.
- Prove an in-place update from the previously distributed stable APK on a device, without uninstalling or clearing app data.
- Preserve settings, app order, provider configuration and secure credential access across the update where applicable.

**Block the release** if the key/fingerprint is unavailable, does not match, or the upgrade test fails. Do not instruct customers to erase data to bypass a mismatch. No signing-key rotation or package migration is authorized by this policy.

## Verification gates

- [ ] Freeze and record the source commit, dependency/toolchain versions and release configuration.
- [ ] All required automated tests pass. Every skip has a reviewed reason; an unexplained failure or missing test blocks release.
- [ ] Lint has no errors. Review warnings; resolve security, lifecycle and stability risks before acceptance.
- [ ] The release APK passes signature, package/version, archive and DEX/component checks. Verify the release artifact, not only the debug APK.
- [ ] Confirm no embedded credentials, debug-only fixtures, sensitive logging or unintended debug flags in the distributed APK.
- [ ] Complete physical-device testing across the declared supported TV/Android range and representative low-resource hardware.
- [ ] Test D-pad/OK/Back, app launch/return, scrolling, hide/reorder, settings persistence and stock-launcher recovery.
- [ ] Test cold start, restart, standby/resume, offline/error recovery and correct system date/time.
- [ ] Test prayer/azan, weather, wallpaper/effects, network test, optional AI and Dim permissions/stop behavior where included.
- [ ] For glass builds, verify supported blur, unsupported/disabled fallback and low-RAM behavior; measure responsiveness and resource use.
- [ ] Perform the signed in-place upgrade and data-preservation test.
- [ ] Review external-service terms, privacy disclosures, attribution and distribution permissions.
- [ ] Record an owner/release-approver decision for this exact APK.

Automated view fixtures and mock transport tests are useful evidence, but are not physical-TV, audible-audio, live-provider, real-throughput or performance certification.

## Commercial readiness

Code licensing and service access are separate concerns. Preserve upstream notices and review all redistributed components and assets; the existence of this policy does not establish blanket commercial clearance.

In particular:
- The feature-pack documentation identifies Open-Meteo's public free API as non-commercial. Confirm current terms and arrange appropriate commercial access/configuration before customer deployment. No commercial API key is bundled.
- Review Groq account terms, usage costs and credential handling for the intended deployment.
- Review MET/data.gov.my and JAKIM data use, attribution and accuracy disclosures. Do not imply official endorsement.
- Review overlay/foreground-service requirements for the intended distribution channel, including any applicable store review.
- State the supported device range and recovery/support process. Do not claim universal firmware support, measured performance or store approval without evidence.

Use current provider terms and the actual build configuration at release time, not an old report as legal or technical sign-off.

## Release record

Publish these details for every stable artifact:

- App version and version code; source commit and release tag.
- Application ID, APK SHA-256 and certificate SHA-256 fingerprint.
- Changes, known limitations and update instructions.
- Tested device/Android matrix, upgrade result and verification summary.
- Approval and applicable service/configuration requirements.

Distribute the exact approved APK. Retain earlier approved artifacts and a recovery plan; Android may reject version downgrades, so do not promise that installing an older APK is a universal rollback method.
