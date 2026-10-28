#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
# Never run a second Gradle invocation against this project concurrently.
# ZIP/web uploads can lose executable bits; bash also supports that download path.
if [[ -n "${GRADLE:-}" ]]; then
    runner=("$GRADLE")
else
    runner=(bash ./gradlew)
fi
"${runner[@]}" --no-daemon clean :app:assembleDebug :app:testDebugUnitTest :app:lintDebug --console=plain
python3 tools/verify-apk.py app/build/outputs/apk/debug/app-debug.apk
