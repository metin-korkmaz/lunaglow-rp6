package dev.lunaglow.led

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PServerBinderLedDriverTest {
    @Test
    fun `confirmed topology writes channels in reported order`() {
        val commands = mutableListOf<String>()
        val driver = PServerBinderLedDriver(
            topology = topology(channelOrder = listOf("green", "red", "blue")),
            executor = ShellCommandExecutor { command -> commands += command; "" },
        )

        driver.setLeftColor(10, 20, 30)

        assertEquals(
            "printf '%s' '20 10 30' > '/sys/class/leds/left:rgb/multi_intensity' && " +
                "printf '%s' '255' > '/sys/class/leds/left:rgb/brightness'",
            commands.single(),
        )
    }

    @Test
    fun `brightness scales clamped channel values`() {
        val commands = mutableListOf<String>()
        val driver = PServerBinderLedDriver(
            topology = topology(),
            executor = ShellCommandExecutor { command -> commands += command; "" },
        )
        driver.setBrightness(128)

        driver.setRightColor(255, -10, 300)

        assertEquals(
            "printf '%s' '128 0 128' > '/sys/class/leds/right:rgb/multi_intensity' && " +
                "printf '%s' '255' > '/sys/class/leds/right:rgb/brightness'",
            commands.single(),
        )
    }

    @Test
    fun `unconfirmed channel order disables driver`() {
        val driver = PServerBinderLedDriver(
            topology = topology(channelOrder = emptyList()),
            executor = ShellCommandExecutor { "" },
        )
        assertFalse(driver.isAvailable)
    }

    @Test
    fun `invalid node name disables driver`() {
        assertFalse(
            PServerBinderLedDriver.validateTopology(
                topology(leftNode = "../left"),
            ),
        )
    }

    @Test
    fun `valid topology is available`() {
        assertTrue(PServerBinderLedDriver(topology(), ShellCommandExecutor { "" }).isAvailable)
    }

    private fun topology(
        channelOrder: List<String> = listOf("red", "green", "blue"),
        leftNode: String = "left:rgb",
    ) = LedTopology(
        nodeNames = listOf(leftNode, "right:rgb"),
        channelOrder = channelOrder,
        maxBrightness = 255,
        multiIndex = listOf("red", "green", "blue"),
        leftNode = leftNode,
        rightNode = "right:rgb",
    )
}
