package dev.lunaglow.color

import org.junit.Assert.assertEquals
import org.junit.Test

class ColorSmootherTest {
    @Test
    fun `first frame passes through unchanged`() {
        val colors = ScreenColors(RgbColor(10, 20, 30), RgbColor(40, 50, 60))
        assertEquals(colors, ColorSmoother(alpha = 0.5).smooth(colors))
    }

    @Test
    fun `ema moves halfway toward next frame`() {
        val smoother = ColorSmoother(alpha = 0.5)
        smoother.smooth(ScreenColors(RgbColor.BLACK, RgbColor.BLACK))

        val result = smoother.smooth(
            ScreenColors(RgbColor(100, 200, 50), RgbColor(200, 100, 250)),
        )

        assertEquals(ScreenColors(RgbColor(50, 100, 25), RgbColor(100, 50, 125)), result)
    }

    @Test
    fun `hold policy ignores a black frame`() {
        val smoother = ColorSmoother(alpha = 1.0, blackFramePolicy = BlackFramePolicy.HOLD_LAST)
        val visible = ScreenColors(RgbColor(200, 50, 20), RgbColor(20, 50, 200))
        smoother.smooth(visible)

        assertEquals(visible, smoother.smooth(ScreenColors(RgbColor.BLACK, RgbColor.BLACK)))
    }

    @Test
    fun `fade policy blends toward black`() {
        val smoother = ColorSmoother(alpha = 0.5, blackFramePolicy = BlackFramePolicy.FADE_TO_BLACK)
        smoother.smooth(ScreenColors(RgbColor(100, 100, 100), RgbColor(200, 200, 200)))

        val result = smoother.smooth(ScreenColors(RgbColor.BLACK, RgbColor.BLACK))

        assertEquals(ScreenColors(RgbColor(50, 50, 50), RgbColor(100, 100, 100)), result)
    }

    @Test
    fun `reset makes next frame pass through`() {
        val smoother = ColorSmoother(alpha = 0.1)
        smoother.smooth(ScreenColors(RgbColor.BLACK, RgbColor.BLACK))
        smoother.reset()
        val next = ScreenColors(RgbColor(1, 2, 3), RgbColor(4, 5, 6))
        assertEquals(next, smoother.smooth(next))
    }
}
