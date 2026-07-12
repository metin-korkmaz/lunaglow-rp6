package dev.lunaglow.probe

import dev.lunaglow.led.LedTopology
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProbeReportFormatterTest {
    private val report = ProbeReport(
        timestamp = 42L,
        deviceModel = "RP6\nmodel=spoof",
        buildFingerprint = "retroid\\build\tfingerprint",
        pServerAvailable = true,
        pServerRawLine = "1: [PServerBinder]",
        ledNodes = listOf(
            LedNodeInfo("left:rgb", 255, listOf("red", "green", "blue"), "lrwxrwxrwx"),
        ),
        topology = LedTopology(
            nodeNames = listOf("left:rgb"),
            channelOrder = listOf("red", "green", "blue"),
            maxBrightness = 255,
            multiIndex = listOf("red", "green", "blue"),
            leftNode = "left:rgb",
            rightNode = null,
        ),
        probeSuccess = true,
        warnings = listOf("warning\nnext=value"),
    )

    @Test
    fun `format is deterministic and includes all report sections`() {
        val first = ProbeReportFormatter.format(report)
        val second = ProbeReportFormatter.format(report)

        assertEquals(first, second)
        assertTrue(first.startsWith("format=lunaglow-probe-v1\n"))
        assertTrue(first.contains("timestamp=42"))
        assertTrue(first.contains("probeSuccess=true"))
        assertTrue(first.contains("pServerAvailable=true"))
        assertTrue(first.contains("ledNodeCount=1"))
        assertTrue(first.contains("ledNode.0.name=left:rgb"))
        assertTrue(first.contains("topology.channelOrder=red,green,blue"))
        assertTrue(first.contains("warningCount=1"))
    }

    @Test
    fun `control characters and field separators cannot inject fields`() {
        val output = ProbeReportFormatter.format(report)

        assertTrue(output.contains("deviceModel=RP6\\nmodel\\=spoof"))
        assertTrue(output.contains("buildFingerprint=retroid\\\\build\\tfingerprint"))
        assertTrue(output.contains("warning.0=warning\\nnext\\=value"))
        assertFalse(output.lines().any { it == "model=spoof" || it == "next=value" })
    }
}
