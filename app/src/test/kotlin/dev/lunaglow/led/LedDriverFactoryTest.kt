package dev.lunaglow.led

import dev.lunaglow.probe.LedNodeInfo
import dev.lunaglow.probe.ProbeReport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LedDriverFactoryTest {

    @Test
    fun `returns Retroid settings driver when PServer available`() {
        val report = makeReport(pServerAvailable = true, nodeNames = listOf("rgb_left_r"))
        val driver = LedDriverFactory.create(report)

        assertEquals("RetroidSettings", driver.driverName)
        assertTrue(driver.isAvailable)
    }

    @Test
    fun `returns NoOp when no PServer and no nodes`() {
        val report = makeReport(pServerAvailable = false, nodeNames = emptyList())
        val driver = LedDriverFactory.create(report)

        assertEquals("NoOp", driver.driverName)
        assertFalse(driver.isAvailable)
    }

    @Test
    fun `returns NoOp when nodes present but no PServer`() {
        // Gate H is read-only; no writable driver is returned even with nodes.
        val report = makeReport(pServerAvailable = false, nodeNames = listOf("led_a", "led_b"))
        val driver = LedDriverFactory.create(report)

        assertEquals("NoOp", driver.driverName)
        assertFalse(driver.isAvailable)
    }

    @Test
    fun `NoOp driver is safe to call`() {
        val driver = NoOpLedDriver()
        // These should not throw.
        driver.setLeftColor(255, 0, 0)
        driver.setRightColor(0, 0, 255)
        driver.setBrightness(128)
        driver.turnOff()
        driver.close()
        assertFalse(driver.isAvailable)
    }

    private fun makeReport(pServerAvailable: Boolean, nodeNames: List<String>): ProbeReport {
        return ProbeReport(
            timestamp = 0,
            deviceModel = "Test",
            buildFingerprint = "",
            pServerAvailable = pServerAvailable,
            pServerRawLine = if (pServerAvailable) "42: [dev.pulse.PServerBinder]" else "",
            ledNodes = nodeNames.map { LedNodeInfo(it, 255, emptyList(), "drwxr-xr--") },
            topology = LedTopology.UNKNOWN,
            probeSuccess = true,
            warnings = emptyList(),
        )
    }
}
