package dev.lunaglow.led

/**
 * A safe no-operation LED driver used when no hardware driver is available.
 *
 * Every method is a no-op. [isAvailable] is always false so the UI can show
 * the degraded state. This driver never writes to sysfs or invokes binder.
 */
class NoOpLedDriver : LedDriver {
    override val driverName: String = "NoOp"
    override val isAvailable: Boolean = false

    override fun setLeftColor(r: Int, g: Int, b: Int) { /* no-op */ }
    override fun setRightColor(r: Int, g: Int, b: Int) { /* no-op */ }
    override fun setBrightness(brightness: Int) { /* no-op */ }
    override fun turnOff() { /* no-op */ }
    override fun close() { /* no-op */ }
}