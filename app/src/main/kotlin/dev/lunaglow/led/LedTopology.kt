package dev.lunaglow.led

/**
 * Discovered LED topology from the hardware probe.
 *
 * Every path is validated against `/sys/class/leds/` and a strict character
 * allowlist. No path is hard-coded before the probe succeeds.
 */
data class LedTopology(
    /** LED node names discovered under `/sys/class/leds/`, e.g. `["led:rgb_left", "led:rgb_right"]`. */
    val nodeNames: List<String>,
    /** Channel order hint, e.g. `["red", "green", "blue"]` or `["blue", "green", "red"]`. */
    val channelOrder: List<String>,
    /** Maximum brightness value reported by `max_brightness` (commonly 255). */
    val maxBrightness: Int,
    /** `multi_index` values if the node supports multi-channel, empty otherwise. */
    val multiIndex: List<String>,
    /** Maps logical position to node name, populated only when confidently inferred. */
    val leftNode: String?,
    val rightNode: String?,
) {
    companion object {
        /** A safe empty topology used before the probe runs. */
        val UNKNOWN = LedTopology(
            nodeNames = emptyList(),
            channelOrder = emptyList(),
            maxBrightness = 0,
            multiIndex = emptyList(),
            leftNode = null,
            rightNode = null,
        )
    }
}
