#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
# IMPORTANT: never run a second Gradle build against this project concurrently.
"${GRADLE:-./gradlew}" --no-daemon clean :app:assembleDebug :app:testDebugUnitTest
python3 tools/verify-apk.py app/build/outputs/apk/debug/app-debug.apk
