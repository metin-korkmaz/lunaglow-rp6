package dev.lunaglow.coordinator

import dev.lunaglow.color.RgbColor
import dev.lunaglow.color.ScreenColors
import dev.lunaglow.led.LedDriver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AmbientLedCoordinatorTest {
    @Test
    fun `available driver receives independent left and right colors`() {
        val driver = RecordingDriver()
        val coordinator = AmbientLedCoordinator(driver, updatesPerSecond = 10)
        val colors = ScreenColors(RgbColor(1, 2, 3), RgbColor(4, 5, 6))

        assertTrue(coordinator.offer(colors, 1_000_000_000L))
        assertEquals(listOf(RgbColor(1, 2, 3)), driver.leftWrites)
        assertEquals(listOf(RgbColor(4, 5, 6)), driver.rightWrites)
    }

    @Test
    fun `writes inside rate limit are skipped`() {
        val driver = RecordingDriver()
        val coordinator = AmbientLedCoordinator(driver, updatesPerSecond = 10)
        assertTrue(coordinator.offer(colors(10), 1_000_000_000L))
        assertFalse(coordinator.offer(colors(30), 1_050_000_000L))
        assertEquals(1, driver.leftWrites.size)
    }

    @Test
    fun `insignificant changes are coalesced`() {
        val driver = RecordingDriver()
        val coordinator = AmbientLedCoordinator(driver, updatesPerSecond = 10, minimumChannelDelta = 3)
        assertTrue(coordinator.offer(colors(10), 1_000_000_000L))
        assertFalse(coordinator.offer(colors(12), 1_200_000_000L))
        assertEquals(1, driver.leftWrites.size)
    }

    @Test
    fun `unavailable driver is never called`() {
        val driver = RecordingDriver(isAvailable = false)
        val coordinator = AmbientLedCoordinator(driver)
        assertFalse(coordinator.offer(colors(10), 1L))
        assertTrue(driver.leftWrites.isEmpty())
    }

    @Test
    fun `stop turns off output and resets rate limit`() {
        val driver = RecordingDriver()
        val coordinator = AmbientLedCoordinator(driver, updatesPerSecond = 1)
        assertTrue(coordinator.offer(colors(10), 100L))
        coordinator.stop()
        assertEquals(1, driver.offCalls)
        assertTrue(coordinator.offer(colors(20), 101L))
    }

    @Test
    fun `driver failure is exposed and prevents repeated privileged writes`() {
        val driver = ThrowingDriver()
        val coordinator = AmbientLedCoordinator(driver)

        assertFalse(coordinator.offer(colors(10), 1L))
        assertEquals("write failed", coordinator.failureMessage)
        assertFalse(coordinator.offer(colors(20), 2_000_000_000L))
        coordinator.stop()
        assertEquals(1, driver.writeAttempts)
    }

    private fun colors(value: Int) = ScreenColors(
        RgbColor(value, value, value),
        RgbColor(value, value, value),
    )

    private class RecordingDriver(
        override val isAvailable: Boolean = true,
    ) : LedDriver {
        override val driverName = "recording"
        val leftWrites = mutableListOf<RgbColor>()
        val rightWrites = mutableListOf<RgbColor>()
        var offCalls = 0

        override fun setLeftColor(r: Int, g: Int, b: Int) {
            leftWrites += RgbColor(r, g, b)
        }

        override fun setRightColor(r: Int, g: Int, b: Int) {
            rightWrites += RgbColor(r, g, b)
        }

        override fun setBrightness(brightness: Int) = Unit

        override fun turnOff() {
            offCalls += 1
        }

        override fun close() = Unit
    }

    private class ThrowingDriver : LedDriver {
        override val driverName = "throwing"
        override val isAvailable = true
        var writeAttempts = 0

        override fun setLeftColor(r: Int, g: Int, b: Int) {
            writeAttempts += 1
            error("write failed")
        }

        override fun setRightColor(r: Int, g: Int, b: Int) = Unit
        override fun setBrightness(brightness: Int) = Unit
        override fun turnOff() {
            writeAttempts += 1
            error("write failed")
        }
        override fun close() = Unit
    }
}
