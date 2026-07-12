package dev.lunaglow.color

import java.nio.ByteBuffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ColorExtractorTest {
    @Test
    fun `solid red frame produces red on both sides`() {
        val frame = frame(8, 4) { _, _ -> RgbColor(255, 0, 0) }

        val result = ColorExtractor(sampleStep = 1).extract(frame, 8, 4, 8 * 4)

        assertEquals(ScreenColors(RgbColor(255, 0, 0), RgbColor(255, 0, 0)), result)
    }

    @Test
    fun `left and right halves are sampled independently`() {
        val frame = frame(8, 4) { x, _ ->
            if (x < 4) RgbColor(255, 0, 0) else RgbColor(0, 0, 255)
        }

        val result = ColorExtractor(sampleStep = 1).extract(frame, 8, 4, 8 * 4)

        assertEquals(RgbColor(255, 0, 0), result.left)
        assertEquals(RgbColor(0, 0, 255), result.right)
    }

    @Test
    fun `row padding is ignored`() {
        val width = 4
        val height = 2
        val rowStride = 20
        val bytes = ByteArray(rowStride * height) { 99 }
        for (y in 0 until height) {
            for (x in 0 until width) {
                val index = (y * rowStride) + (x * 4)
                bytes[index] = if (x < 2) 10 else 40
                bytes[index + 1] = if (x < 2) 20 else 50
                bytes[index + 2] = if (x < 2) 30 else 60
                bytes[index + 3] = 0xFF.toByte()
            }
        }

        val result = ColorExtractor(sampleStep = 1).extract(
            ByteBuffer.wrap(bytes), width, height, rowStride,
        )

        assertEquals(RgbColor(10, 20, 30), result.left)
        assertEquals(RgbColor(40, 50, 60), result.right)
    }

    @Test
    fun `native-sized extraction remains inexpensive`() {
        val frame = frame(160, 90) { x, y -> RgbColor(x, y * 2, 128) }
        val extractor = ColorExtractor(sampleStep = 4)

        val started = System.nanoTime()
        repeat(100) { extractor.extract(frame, 160, 90, 160 * 4) }
        val elapsedMillis = (System.nanoTime() - started) / 1_000_000

        assertTrue("100 extractions took ${elapsedMillis}ms", elapsedMillis < 1_000)
    }

    private fun frame(
        width: Int,
        height: Int,
        colorAt: (x: Int, y: Int) -> RgbColor,
    ): ByteBuffer {
        val bytes = ByteArray(width * height * 4)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val color = colorAt(x, y)
                val index = ((y * width) + x) * 4
                bytes[index] = color.red.toByte()
                bytes[index + 1] = color.green.toByte()
                bytes[index + 2] = color.blue.toByte()
                bytes[index + 3] = 0xFF.toByte()
            }
        }
        return ByteBuffer.wrap(bytes)
    }
}
