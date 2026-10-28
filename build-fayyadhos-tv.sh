#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
: "${JAVA_HOME:?Set JAVA_HOME to JDK 17}"
: "${ANDROID_HOME:?Set ANDROID_HOME to Android SDK}"
: "${FAYYADHOS_RELEASE_KEYSTORE:?Set absolute release keystore path}"
: "${FAYYADHOS_RELEASE_STORE_PASSWORD:?Set release store password}"
: "${FAYYADHOS_RELEASE_ALIAS:?Set release alias}"
: "${FAYYADHOS_RELEASE_KEY_PASSWORD:?Set release key password}"
export PATH="$JAVA_HOME/bin:$PATH"
printf 'sdk.dir=%s\n' "$ANDROID_HOME" > local.properties
bash ./gradlew --no-daemon :app:testDebugUnitTest :app:assembleRelease --console=plain
mkdir -p dist
cp app/build/outputs/apk/release/app-release.apk dist/FayyadhOS-TV-Fix-v2.10.1.apk
(cd dist && sha256sum FayyadhOS-TV-Fix-v2.10.1.apk > SHA256SUMS.txt)
