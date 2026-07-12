package dev.lunaglow.color

import kotlin.math.roundToInt

enum class BlackFramePolicy { HOLD_LAST, FADE_TO_BLACK }

class ColorSmoother(
    private val alpha: Double = 0.35,
    private val blackThreshold: Int = 3,
    private val blackFramePolicy: BlackFramePolicy = BlackFramePolicy.HOLD_LAST,
) {
    init {
        require(alpha in 0.0..1.0)
        require(blackThreshold in 0..255)
    }

    private var previous: ScreenColors? = null

    fun smooth(current: ScreenColors): ScreenColors {
        val prior = previous
        if (prior == null) {
            previous = current
            return current
        }

        val target = if (isBlackFrame(current) && blackFramePolicy == BlackFramePolicy.HOLD_LAST) {
            prior
        } else {
            current
        }
        return ScreenColors(
            left = blend(prior.left, target.left),
            right = blend(prior.right, target.right),
        ).also { previous = it }
    }

    fun reset() {
        previous = null
    }

    private fun isBlackFrame(colors: ScreenColors): Boolean =
        colors.left.luminance <= blackThreshold && colors.right.luminance <= blackThreshold

    private fun blend(previous: RgbColor, current: RgbColor): RgbColor = RgbColor(
        red = interpolate(previous.red, current.red),
        green = interpolate(previous.green, current.green),
        blue = interpolate(previous.blue, current.blue),
    )

    private fun interpolate(previous: Int, current: Int): Int =
        (previous + ((current - previous) * alpha)).roundToInt().coerceIn(0, 255)
}
