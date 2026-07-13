# LunaGlow: Retroid Pocket 6 Ambient Stick LEDs

## 1. Goal

Build an original Android application for the Retroid Pocket 6 that:

- captures the visible screen with Android's supported `MediaProjection` API;
- derives independent colors from the left and right screen regions;
- updates the two analog-stick RGB rings with low perceived latency;
- works on stock firmware without Magisk when `PServerBinder` is available;
- installs and receives updates through a persistent QR/Obtainium workflow;
- never stores or transmits captured frames.

The working name is **LunaGlow** to avoid copying AmbiLuna's name or branding. No AmbiLuna code, assets, protocol, or proprietary implementation will be used.

## 2. Confirmed Findings and Assumptions

### Confirmed by public sources

- Retroid Pocket 6 uses Android 13 and the QCS8550/Snapdragon 8 Gen 2 platform.
- The shared platform loads the open-source HTR3212 LED driver.
- The open-source PULSE project explicitly lists RP6 joystick RGB support through the firmware-provided `PServerBinder`, without Magisk or user-granted root.
- Direct writes to `/sys/class/leds` are normally unavailable to an unprivileged app.
- `MediaProjection` is the supported Android screen-capture mechanism and requires explicit user consent plus a visible foreground-service notification.
- Android 14+ requires the media-projection foreground-service type, one projection session per consent token, and callback registration before `createVirtualDisplay()`.
- A QR code cannot bypass Android's unknown-source setting, Play Protect, or installation confirmation.

### Must be verified on the actual device

- Presence and behavior of `PServerBinder` on the installed RP6 firmware.
- Presence of the RP6 vendor joystick-color and brightness settings.
- Whether stock firmware offers a unified `multi_intensity` node or individual RGB channels.
- Stability of LED writes at 10–20 updates per second.

No exact node path will be hard-coded before the device probe succeeds.

## 3. Technical Baseline

- Kotlin and Jetpack Compose.
- Single Android application module initially; package boundaries keep capture, color, LED, and distribution concerns independent.
- `minSdk = 33` because RP6 ships Android 13.
- Use the newest compile/target SDK supported by the selected stable Android Gradle Plugin at implementation time. The code must obey Android 14+ MediaProjection rules even when running on Android 13.
- Coroutines/Flow for state propagation; no per-frame coroutine creation.
- Manual dependency injection initially. Add Hilt only if construction/lifecycle complexity demonstrably warrants it.
- Gradle version catalog, Kotlin linting, unit tests, instrumentation tests, and GitHub Actions CI.

## 4. Architecture

### UI layer

- Onboarding: explains screen-capture consent and local-only processing.
- Main control: Start/Stop, capture status, LED-driver status, live left/right color preview.
- Diagnostics: firmware/build data, PServer availability, discovered LED nodes, safe static-color tests, exportable probe report.
- Settings: update rate, brightness cap, smoothing strength, saturation boost, black-frame behavior.

### Capture layer

- `ScreenCaptureService`: foreground service with `mediaProjection` type and persistent notification.
- `MediaProjectionController`: owns one projection session and its lifecycle.
- `FrameReader`: `ImageReader` using RGBA_8888, `maxImages = 2`, and `acquireLatestImage()`.
- Initial capture resolution: 160×90 at approximately 15 Hz.
- Always close acquired images in `finally`; drain skipped frames to avoid producer stalls.
- On orientation/size changes, resize the existing virtual display and replace its surface rather than creating a second display from the same token.

### Color layer

- `ColorExtractor`: pure Kotlin logic over a strided RGBA buffer.
- Independently samples left and right halves, excluding configurable edge/status-bar regions.
- MVP algorithm: saturation-weighted average with luminance guard.
- `ColorSmoother`: exponential moving average, black-frame detection, fade-to-black/hold-last behavior.
- Reuse buffers and avoid allocations in the frame loop.

### LED layer

- `LedDriver` interface: probe, set left/right color, set brightness, turn off, close.
- `RetroidSettingsLedDriver`: primary stock-firmware implementation using the
  vendor's fixed joystick color and brightness settings through PServerBinder.
- `DirectSysfsLedDriver`: enabled only if the probe proves nodes writable.
- `NoOpLedDriver`: safe degraded mode with diagnostics.
- `LedTopology`: device-discovered paths, channel order, maximum brightness, and left/right mapping.
- All shell values and paths must come from validated probe results; no arbitrary user-provided command fragments.

### State and lifecycle

- A single coordinator joins sampled colors to the selected LED driver.
- Driver failures transition to a visible degraded state instead of crashing capture.
- Stop, service destruction, projection revocation, and screen-off all release resources and turn LEDs off.
- Captured pixels never leave memory and are never logged, persisted, or sent over the network.

## 5. Staged Delivery Plan

### Gate H — Hardware capability probe

1. Create the minimal Android project and CI.
2. Build a diagnostics screen that checks for `PServerBinder` and enumerates LED class metadata through a read-only command.
3. Provide explicit user-triggered static tests: left red, right blue, both off.
4. Record exact nodes/channel order in a structured, exportable report.

**Pass:** both stick rings can be independently controlled through the vendor
settings on stock firmware without Magisk.

**Stop:** neither PServerBinder nor a safe writable interface works. Do not build the full capture pipeline until a viable driver exists.

### Gate C — Capture and color proof

1. Implement pure color extraction and smoothing with synthetic-buffer tests first.
2. Add MediaProjection consent and the foreground service.
3. Capture at 160×90 and throttle to about 15 Hz.
4. Display live left/right swatches without activating LEDs.
5. Test projection revocation, screen rotation, app switching, and protected/black content.

**Pass:** at least 10 processed samples/second for 10 minutes, no image backlog, crash, ANR, or leaked projection.

### MVP integration

1. Feed smoothed left/right colors to the proven LED driver.
2. Rate-limit writes and coalesce insignificant color changes.
3. Add brightness, update-rate, smoothing, saturation, and black-frame settings.
4. Add an ongoing notification with a Stop action.
5. Run a 30-minute gameplay soak test and collect latency, CPU, memory, and battery evidence.

**Pass:** median capture-to-write latency below 150 ms, no crashes/ANRs, and independent left/right response.

### Gate D — Distribution and updates

1. Create debug and signed release variants.
2. Generate one long-lived release keystore; keep it out of git and back it up in two encrypted locations.
3. Add GitHub Actions workflows for CI and tagged GitHub Releases.
4. Publish one universal/arm64 APK asset with stable naming and correct APK MIME type.
5. Create an Obtainium import configuration and persistent QR code pointing to the release source.
6. Document the one-time requirement to allow Obtainium to install unknown apps.

**Pass:** scan QR on a clean device, install v0.1.0, publish v0.1.1, and update without uninstalling. Android's normal confirmation remains visible.

### Release hardening

1. Run unit, lint, build, instrumentation, and physical-device tests.
2. Verify binder death, missing nodes, denied capture, revoked projection, rotation, screen-off, and service kill behavior.
3. Confirm no frame data or sensitive paths appear in release logs.
4. Publish source, privacy statement, installation guide, troubleshooting steps, and firmware compatibility report.

## 6. Proposed Project Layout

```text
app/src/main/kotlin/.../
  ui/
  capture/
    ScreenCaptureService.kt
    MediaProjectionController.kt
    FrameReader.kt
  color/
    ColorExtractor.kt
    ColorSmoother.kt
  led/
    LedDriver.kt
    RetroidSettingsLedDriver.kt
    DirectSysfsLedDriver.kt
    NoOpLedDriver.kt
    LedTopology.kt
  probe/
    HardwareProbe.kt
    ProbeReport.kt
  coordinator/
    AmbientLedCoordinator.kt
docs/
  DEVICE_PROBE.md
  PRIVACY.md
  INSTALL.md
  SIGNING.md
.github/workflows/
  ci.yml
  release.yml
```

## 7. Verification Matrix

### Unit tests

- Solid, split, dark, padded-row, and malformed RGBA buffers.
- RGB/channel-order conversion and brightness clamping.
- EMA convergence, black-frame policy, change coalescing, and rate limiting.
- Probe-result-to-driver selection.
- Shell command construction from fixed vendor setting names and generated
  numeric/ARGB values only.

### Instrumentation/emulator tests

- Foreground-service startup and notification.
- MediaProjection callback ordering on API 34+.
- Permission denial/retry and projection revocation.
- Service cleanup and rotation handling.

### Physical RP6 tests

- Firmware fingerprint and PServer presence.
- Independent static left/right LED colors and off state.
- 10-minute capture-only and 30-minute integrated soak tests.
- Games/emulators in portrait and landscape.
- Black/protected surfaces fade safely without strobing.
- v0.1.0 to v0.1.1 Obtainium update using the same signing key.

## 8. Security and Privacy Constraints

- Do not expose an arbitrary command-execution API through PServerBinder.
- Validate discovered paths against `/sys/class/leds` and a strict character allowlist.
- Keep the service and components non-exported unless Android explicitly requires otherwise.
- Do not request Accessibility, overlay, storage, network capture, or root permissions for the MVP.
- Network access, if retained for optional update checks, must never carry captured pixels or LED telemetry.
- The release key and passwords must never be committed, printed in CI logs, or embedded in the application.

## 9. Distribution Recommendation

Use **GitHub Releases + Obtainium**:

1. The QR code adds the GitHub release source to Obtainium once.
2. CI builds and signs each tagged release with the same key.
3. Obtainium detects new releases, downloads the APK, and invokes Android's installer.
4. The user confirms installation; this legitimate Android confirmation cannot be bypassed.

Firebase App Distribution is not preferred because direct binary links expire and tester management adds unnecessary friction. A custom in-app updater is deferred until the core app is stable.

## 10. Principal Risks

- **Firmware removes/restricts PServerBinder:** retain capability detection, fail safely, document compatible firmware; consider Shizuku only as a later opt-in fallback.
- **Vendor setting changes:** self-gate on the joystick color key and degrade
  safely if a firmware update removes or renames it.
- **High-frequency privileged shell overhead:** benchmark static writes early, rate-limit updates, and investigate a persistent vendor-side command only if evidenced and safe.
- **MediaProjection UX:** consent is per capture session on newer Android; design startup around this rather than trying to bypass it.
- **Signing-key loss:** encrypted offline backups are mandatory before the first public APK.
- **Trademark/confusion:** ship under original LunaGlow branding and describe compatibility without implying Retroid or AmbiLuna affiliation.

## 11. MVP Definition

The MVP is complete when one stock RP6 can:

1. install the app through the Obtainium QR flow;
2. pass the on-device LED probe without root;
3. start capture after explicit consent;
4. update both stick LEDs independently from left/right screen colors for 30 minutes;
5. stop cleanly and turn LEDs off;
6. update to the next signed version without uninstalling.

Dominant-color clustering, profiles, quick-settings tiles, per-app rules, boot start, Shizuku support, and a custom updater are post-MVP work.

## 12. Sources Used for Planning

- Android MediaProjection, ImageReader, foreground-service, and app-signing documentation.
- AOSP PackageInstaller behavior and package installation constraints.
- Retroid Pocket 6 public device information and ROCKNIX device documentation.
- LineageOS qcs8550-common module/uevent configuration.
- Public HTR3212 driver and device-tree material.
- PULSE, OdinTools, and ClusterTune public source/documentation for the observable PServerBinder integration pattern.
- Obtainium documentation and GitHub Releases documentation.
