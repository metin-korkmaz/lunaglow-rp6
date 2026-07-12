# LunaGlow

LunaGlow is an original Android application for deriving independent left and
right colors from the Retroid Pocket 6 screen and driving its two stick-light
rings. It does not use AmbiLuna code, assets, or branding.

## Current status

- Android 13+ Kotlin/Jetpack Compose application.
- Read-only RP6 hardware probe with exportable reports.
- MediaProjection foreground service using a 160×90 `ImageReader` at 15 Hz.
- Left/right saturation-weighted color extraction and EMA smoothing.
- Rate-limited LED coordinator with a safe `NoOp` fallback.
- PServerBinder driver stays disabled until a physical RP6 report confirms
  exact left/right nodes and RGB channel order.
- GitHub Actions CI/release workflows and a prepared latest-release QR code.

Gate H is still open: a physical RP6 must run the diagnostics and return the
shared probe report before hardware writes are enabled for its topology.

## Build

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export ANDROID_HOME=/opt/android-sdk
./gradlew testDebugUnitTest lintDebug assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

Release builds require the environment variables documented in
`docs/SIGNING.md`. Never distribute a build signed with a temporary test key.

## Installation

See `docs/INSTALL.md` for the planned GitHub Releases + Obtainium flow and
`docs/lunaglow-latest-qr.png` for the persistent latest-release QR target.
The QR becomes active only after the dedicated GitHub repository and first
release are published.

## Privacy

Frames are downscaled and processed only in memory. They are not stored or
transmitted. See `docs/PRIVACY.md`.

## Project plan

The reviewed implementation plan is in
`.omo/plans/retroid-pocket-6-ambient-led.md`.
