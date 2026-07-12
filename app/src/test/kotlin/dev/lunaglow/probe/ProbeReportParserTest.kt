package dev.lunaglow.probe

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProbeReportParserTest {

    // --- validateNodeName ---

    @Test
    fun `valid simple names pass`() {
        assertEquals("rgb_left", ProbeReportParser.validateNodeName("rgb_left"))
        assertEquals("led-1", ProbeReportParser.validateNodeName("led-1"))
        assertEquals("stick:rgb", ProbeReportParser.validateNodeName("stick:rgb"))
    }

    @Test
    fun `path separators rejected`() {
        assertNull(ProbeReportParser.validateNodeName("foo/bar"))
        assertNull(ProbeReportParser.validateNodeName("foo\\bar"))
    }

    @Test
    fun `directory traversal rejected`() {
        assertNull(ProbeReportParser.validateNodeName(".."))
        assertNull(ProbeReportParser.validateNodeName("."))
        assertNull(ProbeReportParser.validateNodeName("../.."))
    }

    @Test
    fun `special characters rejected`() {
        assertNull(ProbeReportParser.validateNodeName("foo;rm -rf /"))
        assertNull(ProbeReportParser.validateNodeName("foo\$BAR"))
        assertNull(ProbeReportParser.validateNodeName("foo|cat"))
        assertNull(ProbeReportParser.validateNodeName("foo $(id)"))
        assertNull(ProbeReportParser.validateNodeName("foo`id`"))
        assertNull(ProbeReportParser.validateNodeName("foo\nbar"))
        assertNull(ProbeReportParser.validateNodeName("foo bar"))
        assertNull(ProbeReportParser.validateNodeName(""))
        assertNull(ProbeReportParser.validateNodeName("   "))
    }

    // --- parseLedListing ---

    @Test
    fun `clean listing parsed correctly`() {
        val raw = "rgb_left_r\ngreen_left\nblue_right\n"
        assertEquals(
            listOf("rgb_left_r", "green_left", "blue_right"),
            ProbeReportParser.parseLedListing(raw),
        )
    }

    @Test
    fun `hostile listing filtered`() {
        val raw = listOf(
            "valid_node",
            "../../../etc/passwd",
            "rm -rf /",
            "foo\$BAR",
            "good_node_2",
        ).joinToString("\n")
        assertEquals(
            listOf("valid_node", "good_node_2"),
            ProbeReportParser.parseLedListing(raw),
        )
    }

    @Test
    fun `empty listing returns empty list`() {
        assertEquals(emptyList<String>(), ProbeReportParser.parseLedListing(""))
        assertEquals(emptyList<String>(), ProbeReportParser.parseLedListing("\n\n\n"))
    }

    // --- parseMaxBrightness ---

    @Test
    fun `valid brightness parsed`() {
        assertEquals(255, ProbeReportParser.parseMaxBrightness("255"))
        assertEquals(0, ProbeReportParser.parseMaxBrightness("0"))
        assertEquals(1, ProbeReportParser.parseMaxBrightness("  1  "))
    }

    @Test
    fun `invalid brightness returns -1`() {
        assertEquals(-1, ProbeReportParser.parseMaxBrightness(""))
        assertEquals(-1, ProbeReportParser.parseMaxBrightness("abc"))
        assertEquals(-1, ProbeReportParser.parseMaxBrightness("-1"))
        assertEquals(-1, ProbeReportParser.parseMaxBrightness("255abc"))
        assertEquals(-1, ProbeReportParser.parseMaxBrightness("3.14"))
    }

    // --- parseMultiIndex ---

    @Test
    fun `space-separated multi_index parsed`() {
        assertEquals(listOf("red", "green", "blue"), ProbeReportParser.parseMultiIndex("red green blue"))
    }

    @Test
    fun `newline-separated multi_index parsed`() {
        assertEquals(listOf("red", "green", "blue"), ProbeReportParser.parseMultiIndex("red\ngreen\nblue\n"))
    }

    @Test
    fun `comma-separated multi_index parsed`() {
        assertEquals(listOf("red", "green", "blue"), ProbeReportParser.parseMultiIndex("red,green,blue"))
    }

    @Test
    fun `mixed separators parsed`() {
        assertEquals(listOf("red", "green", "blue"), ProbeReportParser.parseMultiIndex("red green, blue"))
    }

    @Test
    fun `invalid tokens filtered from multi_index`() {
        assertEquals(listOf("red", "blue"), ProbeReportParser.parseMultiIndex("red bad;token blue"))
    }

    @Test
    fun `empty multi_index returns empty list`() {
        assertEquals(emptyList<String>(), ProbeReportParser.parseMultiIndex(""))
        assertEquals(emptyList<String>(), ProbeReportParser.parseMultiIndex("   "))
    }

    @Test
    fun `unsafe values filtered from multi_index`() {
        assertEquals(emptyList<String>(), ProbeReportParser.parseMultiIndex("../red green/blue"))
    }

    // --- parsePermissions ---

    @Test
    fun `standard ls output parsed`() {
        val raw = "drwxr-xr-- 2 root root 4096 Jan 1 00:00 /sys/class/leds/rgb_left"
        assertEquals("drwxr-xr--", ProbeReportParser.parsePermissions(raw))
    }

    @Test
    fun `missing permission field returns empty`() {
        val raw = "no permission field here"
        assertEquals("", ProbeReportParser.parsePermissions(raw))
    }

    @Test
    fun `empty ls output returns empty`() {
        assertEquals("", ProbeReportParser.parsePermissions(""))
    }

    // --- isPServerLine ---

    @Test
    fun `genuine PServerBinder line detected`() {
        val line = "42: [dev.pulse.PServerBinder]"
        assertTrue(ProbeReportParser.isPServerLine(line))
    }

    @Test
    fun `bare PServerBinder line detected`() {
        val line = "PServerBinder: [active]"
        assertTrue(ProbeReportParser.isPServerLine(line))
    }

    @Test
    fun `substring inside longer name rejected`() {
        assertFalse(ProbeReportParser.isPServerLine("MyPServerBinderExtended: [active]"))
        assertFalse(ProbeReportParser.isPServerLine("NotPServerBinder"))
    }

    @Test
    fun `unrelated line rejected`() {
        assertFalse(ProbeReportParser.isPServerLine("surfaceflinger: [android.ui.ISurfaceComposer]"))
        assertFalse(ProbeReportParser.isPServerLine(""))
    }

    // --- extractPServerLine ---

    @Test
    fun `extracts first matching line from full service list`() {
        val full = listOf(
            "1: [android.os.IServiceManager]",
            "42: [dev.pulse.PServerBinder]",
            "43: [dev.pulse.OtherService]",
        ).joinToString("\n")
        assertEquals("42: [dev.pulse.PServerBinder]", ProbeReportParser.extractPServerLine(full))
    }

    @Test
    fun `returns empty when no PServer line`() {
        assertEquals("", ProbeReportParser.extractPServerLine("no match here\nanother line"))
    }
}
