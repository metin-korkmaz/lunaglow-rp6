package dev.lunaglow.led

import dev.lunaglow.probe.ProbeReport

/**
 * Selects an appropriate [LedDriver] based on the probe results.
 *
 * Selection logic (deterministic, no network or root access):
 *  1. If [ProbeReport.pServerAvailable] is true → [NoOpLedDriver] until
 *     `PServerBinderLedDriver` is implemented in a later gate.
 *  2. If the probe found writable sysfs nodes → [NoOpLedDriver] until
 *     `DirectSysfsLedDriver` is implemented in a later gate.
 *  3. Otherwise → [NoOpLedDriver] (degraded mode).
 *
 * This factory is the single source of truth for driver construction; it must
 * never instantiate a driver that writes LED values before Gate H is passed.
 */
object LedDriverFactory {

    /**
     * Creates a driver for the given [report].
     *
     * Gate H is a read-only probe stage; no writable driver is returned yet.
     * The returned driver is safe to use in all states.
     */
    fun create(report: ProbeReport): LedDriver {
        if (report.pServerAvailable && PServerBinderLedDriver.validateTopology(report.topology)) {
            return PServerBinderLedDriver(report.topology)
        }
        return NoOpLedDriver()
    }
}
