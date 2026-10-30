# FayyadhOS TV Next — Hako Pro

This is the isolated Android TV launcher project for the Hako Pro 4 GB target.

## Build a debug APK

    cd tvnext
    ./gradlew :app:assembleDebug

APK output:

    app/build/outputs/apk/debug/app-debug.apk

Run unit tests:

    ./gradlew :app:testDebugUnitTest

## Install alongside the current launcher

The new app uses application ID dev.farred.fayyadhos.tv.next, separate from the existing FayyadhOS package. During development, keep the current launcher installed as the fallback.

## Status

This is an engineering preview. The first build validates the Android TV shell, D-pad focus, app discovery/launch, clock and device diagnostics. Renderer integration, licensed PapaZola/BoBoiBoy 3D assets, and migration of the full legacy feature set are separate milestones. Do not treat an engineering APK as the stable release.
