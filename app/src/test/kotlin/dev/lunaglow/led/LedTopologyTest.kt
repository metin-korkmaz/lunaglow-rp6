package dev.lunaglow.led

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LedTopologyTest {

    @Test
    fun `UNKNOWN topology has safe defaults`() {
        val t = LedTopology.UNKNOWN
        assertEquals(0, t.nodeNames.size)
        assertEquals(0, t.channelOrder.size)
        assertEquals(0, t.maxBrightness)
        assertEquals(0, t.multiIndex.size)
        assertNull(t.leftNode)
        assertNull(t.rightNode)
    }

    @Test
    fun `data class equality holds`() {
        val a = LedTopology(
            nodeNames = listOf("left", "right"),
            channelOrder = listOf("r", "g", "b"),
            maxBrightness = 255,
            multiIndex = listOf("red", "green", "blue"),
            leftNode = "left",
            rightNode = "right",
        )
        val b = a.copy()
        assertEquals(a, b)
    }
}
