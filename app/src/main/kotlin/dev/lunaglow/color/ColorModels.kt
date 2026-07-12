package dev.lunaglow.color

data class RgbColor(val red: Int, val green: Int, val blue: Int) {
    init {
        require(red in 0..255 && green in 0..255 && blue in 0..255)
    }

    val luminance: Int
        get() = ((red * 54) + (green * 183) + (blue * 19)) shr 8

    companion object {
        val BLACK = RgbColor(0, 0, 0)
    }
}

data class ScreenColors(val left: RgbColor, val right: RgbColor)
