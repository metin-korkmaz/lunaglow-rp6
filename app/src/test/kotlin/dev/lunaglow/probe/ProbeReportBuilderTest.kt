package dev.lunaglow.probe

import dev.lunaglow.led.LedTopology
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProbeReportBuilderTest {

    @Test
    fun `builds complete report with PServer and LED nodes`() {
        val report = ProbeReportBuilder.build(
            timestamp = 1700000000000L,
            deviceModel = "Retroid Pocket 6",
            buildFingerprint = "retroid/rp6/rp6:13/TP1A.220905.001/12345",
            rawServiceList = "1: [android.os.IServiceManager]\n42: [dev.pulse.PServerBinder]\n",
            rawLedListing = "rgb_left_r\ngreen_left\nblue_left\nrgb_right_r\ngreen_right\nblue_right\n",
            perNodeData = mapOf(
                "rgb_left_r" to NodeRawData("255", "0", "drwxr-xr-- 2 root root 4096 rgb_left_r"),
                "green_left" to NodeRawData("255", "1", "drwxr-xr-- 2 root root 4096 green_left"),
                "blue_left" to NodeRawData("255", "2", "drwxr-xr-- 2 root root 4096 blue_left"),
                "rgb_right_r" to NodeRawData("255", "0", "drwxr-xr-- 2 root root 4096 rgb_right_r"),
                "green_right" to NodeRawData("255", "1", "drwxr-xr-- 2 root root 4096 green_right"),
                "blue_right" to NodeRawData("255", "2", "drwxr-xr-- 2 root root 4096 blue_right"),
            ),
        )

        assertTrue(report.pServerAvailable)
        assertEquals("42: [dev.pulse.PServerBinder]", report.pServerRawLine)
        assertEquals(6, report.ledNodes.size)
        assertTrue(report.probeSuccess)
        assertEquals("Retroid Pocket 6", report.deviceModel)
    }

    @Test
    fun `topology left and right inferred from naming`() {
        val report = ProbeReportBuilder.build(
            timestamp = 0,
            deviceModel = "",
            buildFingerprint = "",
            rawServiceList = "",
            rawLedListing = "rgb_left_r\nrgb_left_g\nrgb_left_b\nrgb_right_r\nrgb_right_g\nrgb_right_b\n",
            perNodeData = emptyMap(),
        )

        assertEquals("rgb_left_r", report.topology.leftNode)
        assertEquals("rgb_right_r", report.topology.rightNode)
        assertEquals(0, report.topology.maxBrightness)
    }

    @Test
    fun `topology max brightness from node data`() {
        val report = ProbeReportBuilder.build(
            timestamp = 0,
            deviceModel = "",
            buildFingerprint = "",
            rawServiceList = "",
            rawLedListing = "led_a\nled_b\n",
            perNodeData = mapOf(
            "led_a" to NodeRawData("255", "red green blue", "drwxr-xr--"),
            "led_b" to NodeRawData("128", "red green blue", "drwxr-xr--"),
            ),
        )

        assertEquals(255, report.topology.maxBrightness)
        assertEquals(listOf("red", "green", "blue"), report.topology.multiIndex)
    }

    @Test
    fun `no LED nodes yields unknown topology`() {
        val report = ProbeReportBuilder.build(
            timestamp = 0,
            deviceModel = "",
            buildFingerprint = "",
            rawServiceList = "",
            rawLedListing = "",
            perNodeData = emptyMap(),
        )

        assertEquals(LedTopology.UNKNOWN, report.topology)
        assertFalse(report.pServerAvailable)
        assertTrue(report.ledNodes.isEmpty())
    }

    @Test
    fun `hostile node names filtered out of report`() {
        val report = ProbeReportBuilder.build(
            timestamp = 0,
            deviceModel = "",
            buildFingerprint = "",
            rawServiceList = "",
            rawLedListing = listOf(
                "valid_node",
                "../../../etc/passwd",
                "rm -rf /",
                "valid_node_2",
            ).joinToString("\n"),
            perNodeData = mapOf(
                "valid_node" to NodeRawData("255", "", ""),
                "valid_node_2" to NodeRawData("255", "", ""),
            ),
        )

        assertEquals(2, report.ledNodes.size)
        assertEquals("valid_node", report.ledNodes[0].name)
        assertEquals("valid_node_2", report.ledNodes[1].name)
    }

    @Test
    fun `malformed max_brightness yields -1 in node info`() {
        val report = ProbeReportBuilder.build(
            timestamp = 0,
            deviceModel = "",
            buildFingerprint = "",
            rawServiceList = "",
            rawLedListing = "led_a\n",
            perNodeData = mapOf("led_a" to NodeRawData("not_a_number", "", "")),
        )

        assertEquals(1, report.ledNodes.size)
        assertEquals(-1, report.ledNodes[0].maxBrightness)
    }

    @Test
    fun `warnings propagated to report`() {
        val warnings = listOf("ls failed", "service list timed out")
        val report = ProbeReportBuilder.build(
            timestamp = 0,
            deviceModel = "",
            buildFingerprint = "",
            rawServiceList = "",
            rawLedListing = "",
            perNodeData = emptyMap(),
            warnings = warnings,
        )

        assertEquals(warnings, report.warnings)
    }

    @Test
    fun `channel order not guessed from names`() {
        val report = ProbeReportBuilder.build(
            timestamp = 0,
            deviceModel = "",
            buildFingerprint = "",
            rawServiceList = "",
            rawLedListing = "rgb_left_r\nrgb_right_b\n",
            perNodeData = emptyMap(),
        )

        // Channel order must NOT be inferred from naming — only confirmed via static test.
        assertTrue(report.topology.channelOrder.isEmpty())
    }

    @Test
    fun `probe failure flag propagated`() {
        val report = ProbeReportBuilder.build(
            timestamp = 0,
            deviceModel = "",
            buildFingerprint = "",
            rawServiceList = "",
            rawLedListing = "",
            perNodeData = emptyMap(),
            probeSuccess = false,
        )

        assertFalse(report.probeSuccess)
    }

    @Test
    fun `probeSuccess true with empty results when caller passes true`() {
        val report = ProbeReportBuilder.build(
            timestamp = 0,
            deviceModel = "",
            buildFingerprint = "",
            rawServiceList = "",
            rawLedListing = "",
            perNodeData = emptyMap(),
            probeSuccess = true,
        )

        assertTrue(report.probeSuccess)
        assertFalse(report.pServerAvailable)
        assertTrue(report.ledNodes.isEmpty())
    }

    @Test
    fun `probeSuccess false does not hide PServer availability`() {
        val report = ProbeReportBuilder.build(
            timestamp = 0,
            deviceModel = "",
            buildFingerprint = "",
            rawServiceList = "42: [dev.pulse.PServerBinder]\n",
            rawLedListing = "rgb_left\n",
            perNodeData = mapOf("rgb_left" to NodeRawData("255", "0", "drwxr-xr--")),
            probeSuccess = false,
        )

        assertFalse(report.probeSuccess)
        assertTrue(report.pServerAvailable)
        assertEquals(1, report.ledNodes.size)
    }
}
