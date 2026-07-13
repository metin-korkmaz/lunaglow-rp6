# QA evidence

## Automated and release surfaces

- JVM unit suite: 81 tests, 0 failures, 0 errors.
- Android lint: 0 errors.
- Debug and production-signed release APKs build successfully with API 35.
- GitHub CI and Release APK workflows complete successfully.
- The latest-download URL returns HTTP 200 with
  `application/vnd.android.package-archive`.
- The downloaded APK is `dev.lunaglow`, version `0.1.4` (`versionCode 5`),
  signed by certificate SHA-256
  `6fea11f81031222e65630844bbe3a081573e1d37a7fe3f686f5ca960ab700c5b`.
- The install QR decodes to the same latest-download URL.

## Emulator limitation

The build host has no `/dev/kvm`. API 35 and API 33 x86_64 AVDs were run with
software acceleration, reduced display resolution, four virtual CPUs, and
2 GB guest RAM. Both progressed through first-boot dex optimization, but
`system_server` repeatedly restarted before a stable launcher/package-install
surface was available. An early package install returned a framework-side
`PackageManagerInternal` null error while Android was still starting.

This is partial environment evidence, not proof that LunaGlow's UI or
MediaProjection flow passed. Screenshots, TalkBack traversal, MediaProjection,
PServerBinder, stick LED output, and the 30-minute soak remain physical-RP6
acceptance tests.
