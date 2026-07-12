package dev.lunaglow.led

import dev.lunaglow.probe.ProbeReportParser

class PServerBinderLedDriver(
    private val topology: LedTopology,
    private val executor: ShellCommandExecutor = PServerCommandExecutor(),
) : LedDriver {
    override val driverName = "PServerBinder"
    override val isAvailable: Boolean = validateTopology(topology)
    private var brightness = 255

    override fun setLeftColor(r: Int, g: Int, b: Int) {
        writeColor(requireNotNull(topology.leftNode), r, g, b)
    }

    override fun setRightColor(r: Int, g: Int, b: Int) {
        writeColor(requireNotNull(topology.rightNode), r, g, b)
    }

    override fun setBrightness(brightness: Int) {
        this.brightness = brightness.coerceIn(0, 255)
    }

    override fun turnOff() {
        if (!isAvailable) return
        setLeftColor(0, 0, 0)
        setRightColor(0, 0, 0)
    }

    override fun close() = Unit

    private fun writeColor(nodeName: String, red: Int, green: Int, blue: Int) {
        check(isAvailable) { "LED topology has not been confirmed" }
        val values = mapOf(
            "red" to scale(red),
            "green" to scale(green),
            "blue" to scale(blue),
        )
        val ordered = topology.channelOrder.map { values.getValue(it) }
        val nodePath = ProbeReportParser.LEDS_BASE_PATH + nodeName
        val command = buildString {
            append("printf '%s' '")
            append(ordered.joinToString(" "))
            append("' > '")
            append(nodePath)
            append("/multi_intensity'")
            append(" && printf '%s' '")
            append(topology.maxBrightness.coerceAtLeast(1))
            append("' > '")
            append(nodePath)
            append("/brightness'")
        }
        executor.execute(command)
    }

    private fun scale(value: Int): Int =
        (value.coerceIn(0, 255) * brightness + 127) / 255

    companion object {
        fun validateTopology(topology: LedTopology): Boolean {
            val left = topology.leftNode ?: return false
            val right = topology.rightNode ?: return false
            if (ProbeReportParser.validateNodeName(left) == null) return false
            if (ProbeReportParser.validateNodeName(right) == null) return false
            if (topology.maxBrightness <= 0) return false
            return topology.channelOrder.size == 3 &&
                topology.channelOrder.toSet() == setOf("red", "green", "blue")
        }
    }
}
