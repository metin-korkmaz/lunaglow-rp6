package dev.lunaglow.led

import dev.lunaglow.probe.ProbeReport

/**
 * Contract for all LED driver implementations.
 *
 * No implementation may accept arbitrary shell commands or paths from the caller.
 * All paths and channel mappings must originate from a validated [ProbeReport].
 */
interface LedDriver {
    val driverName: String
    val isAvailable: Boolean

    /**
     * Sets the left analog-stick RGB ring to the given [r], [g], [b] values (0–255).
     */
    fun setLeftColor(r: Int, g: Int, b: Int)

    /**
     * Sets the right analog-stick RGB ring to the given [r], [g], [b] values (0–255).
     */
    fun setRightColor(r: Int, g: Int, b: Int)

    fun setColors(
        leftR: Int,
        leftG: Int,
        leftB: Int,
        rightR: Int,
        rightG: Int,
        rightB: Int,
    ) {
        setLeftColor(leftR, leftG, leftB)
        setRightColor(rightR, rightG, rightB)
    }

    /**
     * Sets a global brightness multiplier (0–255) that scales all subsequent writes.
     */
    fun setBrightness(brightness: Int)

    /** Turns both stick rings off immediately. */
    fun turnOff()

    /** Releases any hardware handles. Safe to call multiple times. */
    fun close()
}
