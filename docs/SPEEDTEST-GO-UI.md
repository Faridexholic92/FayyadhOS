# Speedtest GO UI — phase 1

The user chose an explicit **GO** action, not automatic testing on open.

## Delivered in source

- Clear GO action with a larger Mbps reading, native glass styling and D-pad focus.
- Opening the panel does not create a probe or start download traffic.
- Cancel, Back, retry, stale callback rejection and route-change checks remain.
- The ring tracks the bounded sample (time target plus minimum received bytes, or data cap), not an invented percentage of a mandatory 100 MiB transfer.
- Cancelled, invalid and failed samples clear the ring rather than retaining a success state.
- Actual bytes/time and existing sample/data/time bounds remain.
- Explicitly labelled download-only; upload, ping and jitter are not implemented or fabricated. This is not an Ookla, Netflix or Fireprobe integration.

## Remaining work

A complete multi-metric test needs a separately verified measurement engine and appropriate endpoint access: upload, latency/jitter definitions, multi-stream behaviour, provider terms and physical-TV acceptance. These are not delivered by this UI change. No new signing key, APK distribution or release certification is part of this work.

## Local verification

- Full `:app:testDebugUnitTest :app:lintDebug`: PASS. 404 tests, 403 passed, 1 skipped; 0 failures, 0 errors. Includes one local-only video fixture test not committed.
- Lint: 0 errors, 88 warnings. Existing warnings are not claimed fixed.
- Full-suite setup initially failed fetching Android 8 test dependency; exact Maven Central artifact was cached with published SHA-512 verification, then the full suite reran successfully. No TLS bypass or SDK/test removal.
- Native-render ready, running, result, failed and cancelled states inspected at 1080p; ready state also inspected at 720p. Simulated UI/data only, not physical-TV or live-throughput proof.
- Signing configuration and version remain unchanged; no APK was distributed.
