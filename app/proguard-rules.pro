# ProGuard rules for LunaGlow release builds.
# Keep LED driver and probe model classes for serialization/reflection safety.

-keep class dev.lunaglow.led.** { *; }
-keep class dev.lunaglow.probe.** { *; }

# Kotlin metadata
-keep class kotlin.Metadata { *; }