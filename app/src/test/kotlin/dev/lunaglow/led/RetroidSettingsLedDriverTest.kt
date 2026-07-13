package dev.lunaglow.led

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class RetroidSettingsLedDriverTest {
    @Test
    fun `writes both stick colors atomically through vendor setting`() {
        val executor = RecordingExecutor(
            colorResponse = "#ff111111,#ff222222",
            brightnessResponse = "0.5",
        )
        val driver = RetroidSettingsLedDriver(executor)

        driver.setColors(1, 2, 3, 4, 5, 6)

        assertEquals(
            listOf(
                "settings get system joystick_led_light_picker_color",
                "settings get system led_light_brightness_percent",
                "settings put system led_light_brightness_percent 1.0",
                "settings put system joystick_led_light_picker_color '#ff010203,#ff040506'",
            ),
            executor.commands,
        )
    }

    @Test
    fun `brightness scales both colors before formatting`() {
        val executor = RecordingExecutor("#ff111111,#ff222222", "1.0")
        val driver = RetroidSettingsLedDriver(executor)
        driver.setBrightness(128)

        driver.setColors(255, 0, 128, 10, 20, 30)

        assertTrue(
            executor.commands.last() ==
                "settings put system joystick_led_light_picker_color '#ff800040,#ff050a0f'",
        )
    }

    @Test
    fun `turn off restores captured vendor color and brightness`() {
        val executor = RecordingExecutor("#ff111111,#ff222222", "0.25")
        val driver = RetroidSettingsLedDriver(executor)
        driver.setColors(1, 2, 3, 4, 5, 6)

        driver.turnOff()

        assertEquals(
            listOf(
                "settings put system joystick_led_light_picker_color '#ff111111,#ff222222'",
                "settings put system led_light_brightness_percent '0.25'",
            ),
            executor.commands.takeLast(2),
        )
    }

    @Test
    fun `missing vendor key rejects writes`() {
        val executor = RecordingExecutor("null", "null")
        val driver = RetroidSettingsLedDriver(executor)

        assertThrows(IllegalStateException::class.java) {
            driver.setColors(1, 2, 3, 4, 5, 6)
        }
        assertTrue(executor.commands.none { it.startsWith("settings put") })
    }

    private class RecordingExecutor(
        private val colorResponse: String,
        private val brightnessResponse: String,
    ) : ShellCommandExecutor {
        val commands = mutableListOf<String>()

        override fun execute(command: String): String {
            commands += command
            return when (command) {
                "settings get system joystick_led_light_picker_color" -> colorResponse
                "settings get system led_light_brightness_percent" -> brightnessResponse
                else -> ""
            }
        }
    }
}
