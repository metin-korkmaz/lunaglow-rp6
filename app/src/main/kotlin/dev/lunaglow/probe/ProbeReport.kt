package dev.lunaglow.probe

import dev.lunaglow.led.LedTopology

/**
 * Structured result of the Gate H hardware capability probe.
 *
 * Every field is derived from validated, read-only command output.
 * No arbitrary user-provided command fragments are stored here.
 */
data class ProbeReport(
    /** Timestamp (epoch millis) when the probe completed. */
    val timestamp: Long,
    /** Device model reported by `getprop ro.product.model` (or empty). */
    val deviceModel: String,
    /** Android `BUILD.DISPLAY` fingerprint (or empty). */
    val buildFingerprint: String,
    /** True if a service named `PServerBinder` is registered with `service list`. */
    val pServerAvailable: Boolean,
    /** Raw service-list line(s) mentioning PServer, for audit/debug only. */
    val pServerRawLine: String,
    /** All LED node names discovered under `/sys/class/leds/`. */
    val ledNodes: List<LedNodeInfo>,
    /** Derived topology, or [LedTopology.UNKNOWN] if nothing usable was found. */
    val topology: LedTopology,
    /** Whether the probe itself completed without internal errors. */
    val probeSuccess: Boolean,
    /** Non-fatal error/warning messages collected during the probe. */
    val warnings: List<String>,
)

/**
 * Metadata for a single LED node discovered under `/sys/class/leds/`.
 */
data class LedNodeInfo(
    /** Bare node name, e.g. `rgb_left_r` — validated against the allowlist. */
    val name: String,
    /** `max_brightness` value read from the node, or -1 if unreadable. */
    val maxBrightness: Int,
    /** `multi_index` values if the node exposes them, empty otherwise. */
    val multiIndex: List<String>,
    /** Permissions string from `ls -l` (e.g. `rw-r--r--`), or empty if unavailable. */
    val permissions: String,
)
