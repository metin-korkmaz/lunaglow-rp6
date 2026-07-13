package dev.lunaglow.led

class RetroidSettingsLedDriver(
    private val executor: ShellCommandExecutor = PServerCommandExecutor(),
) : LedDriver {
    override val driverName = "RetroidSettings"
    override val isAvailable = true

    private var brightness = 255
    private var currentLeft = Rgb(0, 0, 0)
    private var currentRight = Rgb(0, 0, 0)
    private var originalColor: String? = null
    private var originalBrightness: String? = null
    private var captured = false

    override fun setLeftColor(r: Int, g: Int, b: Int) {
        currentLeft = Rgb.clamped(r, g, b)
        writeCurrentColors()
    }

    override fun setRightColor(r: Int, g: Int, b: Int) {
        currentRight = Rgb.clamped(r, g, b)
        writeCurrentColors()
    }

    override fun setColors(
        leftR: Int,
        leftG: Int,
        leftB: Int,
        rightR: Int,
        rightG: Int,
        rightB: Int,
    ) {
        currentLeft = Rgb.clamped(leftR, leftG, leftB)
        currentRight = Rgb.clamped(rightR, rightG, rightB)
        writeCurrentColors()
    }

    override fun setBrightness(brightness: Int) {
        this.brightness = brightness.coerceIn(0, 255)
    }

    override fun turnOff() {
        if (!captured) return
        originalColor?.let {
            executor.execute("settings put system $KEY_COLOR '$it'")
        }
        originalBrightness?.let {
            executor.execute("settings put system $KEY_BRIGHTNESS '$it'")
        }
        captured = false
        originalColor = null
        originalBrightness = null
    }

    override fun close() {
        turnOff()
    }

    private fun writeCurrentColors() {
        captureOriginalSettings()
        val left = currentLeft.scaled(brightness)
        val right = currentRight.scaled(brightness)
        val pair = "${left.toArgbHex()},${right.toArgbHex()}"
        executor.execute("settings put system $KEY_COLOR '$pair'")
    }

    private fun captureOriginalSettings() {
        if (captured) return
        val color = executor.execute("settings get system $KEY_COLOR").trim()
        check(COLOR_PAIR_PATTERN.matches(color)) { "Vendor joystick LED setting is unavailable" }
        val rawBrightness = executor.execute("settings get system $KEY_BRIGHTNESS").trim()
        val parsedBrightness = rawBrightness.toFloatOrNull()
        check(parsedBrightness != null && parsedBrightness in 0f..1f) {
            "Vendor joystick brightness setting is unavailable"
        }
        originalColor = color
        originalBrightness = parsedBrightness.toString()
        executor.execute("settings put system $KEY_BRIGHTNESS 1.0")
        captured = true
    }

    private data class Rgb(val red: Int, val green: Int, val blue: Int) {
        fun scaled(brightness: Int): Rgb = Rgb(
            scale(red, brightness),
            scale(green, brightness),
            scale(blue, brightness),
        )

        fun toArgbHex(): String = "#ff%02x%02x%02x".format(red, green, blue)

        companion object {
            fun clamped(red: Int, green: Int, blue: Int) = Rgb(
                red.coerceIn(0, 255),
                green.coerceIn(0, 255),
                blue.coerceIn(0, 255),
            )

            private fun scale(value: Int, brightness: Int): Int =
                (value * brightness + 127) / 255
        }
    }

    private companion object {
        const val KEY_COLOR = "joystick_led_light_picker_color"
        const val KEY_BRIGHTNESS = "led_light_brightness_percent"
        val COLOR_PAIR_PATTERN = Regex("^#[0-9a-fA-F]{8},#[0-9a-fA-F]{8}$")
    }
}
