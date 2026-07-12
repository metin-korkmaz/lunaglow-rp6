package dev.lunaglow.color

import java.nio.ByteBuffer
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class ColorExtractor(
    private val sampleStep: Int = 4,
    private val saturationBoost: Double = 1.0,
) {
    init {
        require(sampleStep > 0)
        require(saturationBoost >= 0.0)
    }

    fun extract(
        buffer: ByteBuffer,
        width: Int,
        height: Int,
        rowStride: Int,
        pixelStride: Int = 4,
    ): ScreenColors {
        require(width >= 2 && height > 0)
        require(rowStride > 0 && pixelStride >= 3)

        val readBuffer = buffer.duplicate()
        val midpoint = width / 2
        val left = Accumulator()
        val right = Accumulator()

        var y = 0
        while (y < height) {
            var x = 0
            while (x < width) {
                val index = (y * rowStride) + (x * pixelStride)
                if (index >= 0 && index + 2 < readBuffer.limit()) {
                    val red = readBuffer.get(index).toInt() and 0xFF
                    val green = readBuffer.get(index + 1).toInt() and 0xFF
                    val blue = readBuffer.get(index + 2).toInt() and 0xFF
                    val target = if (x < midpoint) left else right
                    target.add(red, green, blue, weight(red, green, blue))
                }
                x += sampleStep
            }
            y += sampleStep
        }

        return ScreenColors(left.toColor(), right.toColor())
    }

    private fun weight(red: Int, green: Int, blue: Int): Double {
        val maximum = max(red, max(green, blue))
        if (maximum == 0) return 1.0
        val minimum = min(red, min(green, blue))
        val saturation = (maximum - minimum).toDouble() / maximum
        return 1.0 + (saturation * saturationBoost)
    }

    private class Accumulator {
        private var redTotal = 0.0
        private var greenTotal = 0.0
        private var blueTotal = 0.0
        private var totalWeight = 0.0

        fun add(red: Int, green: Int, blue: Int, weight: Double) {
            redTotal += red * weight
            greenTotal += green * weight
            blueTotal += blue * weight
            totalWeight += weight
        }

        fun toColor(): RgbColor {
            if (totalWeight == 0.0) return RgbColor.BLACK
            return RgbColor(
                red = (redTotal / totalWeight).roundToInt().coerceIn(0, 255),
                green = (greenTotal / totalWeight).roundToInt().coerceIn(0, 255),
                blue = (blueTotal / totalWeight).roundToInt().coerceIn(0, 255),
            )
        }
    }
}
