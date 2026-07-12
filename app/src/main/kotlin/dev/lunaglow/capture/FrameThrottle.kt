package dev.lunaglow.capture

class FrameThrottle(framesPerSecond: Int) {
    private val intervalNanos: Long
    private var lastFrameNanos: Long? = null

    init {
        require(framesPerSecond > 0)
        intervalNanos = 1_000_000_000L / framesPerSecond
    }

    fun shouldProcess(timestampNanos: Long): Boolean {
        val previous = lastFrameNanos
        if (previous == null || timestampNanos < previous || timestampNanos - previous >= intervalNanos) {
            lastFrameNanos = timestampNanos
            return true
        }
        return false
    }

    fun reset() {
        lastFrameNanos = null
    }
}
