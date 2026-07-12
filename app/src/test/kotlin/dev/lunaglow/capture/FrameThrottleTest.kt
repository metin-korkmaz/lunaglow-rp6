package dev.lunaglow.capture

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FrameThrottleTest {
    @Test
    fun `first frame is processed`() {
        assertTrue(FrameThrottle(15).shouldProcess(1L))
    }

    @Test
    fun `frames inside interval are skipped`() {
        val throttle = FrameThrottle(10)
        assertTrue(throttle.shouldProcess(1_000_000_000L))
        assertFalse(throttle.shouldProcess(1_050_000_000L))
        assertTrue(throttle.shouldProcess(1_100_000_000L))
    }

    @Test
    fun `clock reset processes next frame`() {
        val throttle = FrameThrottle(15)
        assertTrue(throttle.shouldProcess(100L))
        assertTrue(throttle.shouldProcess(50L))
    }

    @Test
    fun `reset processes immediately`() {
        val throttle = FrameThrottle(15)
        assertTrue(throttle.shouldProcess(100L))
        throttle.reset()
        assertTrue(throttle.shouldProcess(101L))
    }
}
