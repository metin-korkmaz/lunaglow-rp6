package dev.lunaglow.coordinator

import dev.lunaglow.color.RgbColor
import dev.lunaglow.color.ScreenColors
import dev.lunaglow.led.LedDriver
import kotlin.math.abs

class AmbientLedCoordinator(
    private val driver: LedDriver,
    updatesPerSecond: Int = 15,
    private val minimumChannelDelta: Int = 2,
) {
    private val minimumIntervalNanos: Long
    private var lastWriteNanos: Long? = null
    private var lastColors: ScreenColors? = null
    var failureMessage: String? = null
        private set

    init {
        require(updatesPerSecond > 0)
        require(minimumChannelDelta >= 0)
        minimumIntervalNanos = 1_000_000_000L / updatesPerSecond
    }

    fun offer(colors: ScreenColors, timestampNanos: Long): Boolean {
        if (!driver.isAvailable || failureMessage != null) return false
        val previousTime = lastWriteNanos
        if (previousTime != null && timestampNanos >= previousTime &&
            timestampNanos - previousTime < minimumIntervalNanos
        ) return false
        if (lastColors?.let { isInsignificant(it, colors) } == true) return false

        try {
            driver.setLeftColor(colors.left.red, colors.left.green, colors.left.blue)
            driver.setRightColor(colors.right.red, colors.right.green, colors.right.blue)
        } catch (error: Exception) {
            failureMessage = error.message ?: error.javaClass.simpleName
            return false
        }
        lastWriteNanos = timestampNanos
        lastColors = colors
        return true
    }

    fun stop() {
        try {
            driver.turnOff()
        } catch (error: Exception) {
            failureMessage = error.message ?: error.javaClass.simpleName
        }
        lastWriteNanos = null
        lastColors = null
    }

    private fun isInsignificant(previous: ScreenColors, current: ScreenColors): Boolean =
        colorDelta(previous.left, current.left) < minimumChannelDelta &&
            colorDelta(previous.right, current.right) < minimumChannelDelta

    private fun colorDelta(previous: RgbColor, current: RgbColor): Int = maxOf(
        abs(previous.red - current.red),
        abs(previous.green - current.green),
        abs(previous.blue - current.blue),
    )
}
