package dev.lunaglow.led

import dev.lunaglow.probe.ProbeReport

object LedDriverFactory {
    fun create(report: ProbeReport): LedDriver {
        if (report.pServerAvailable) {
            return RetroidSettingsLedDriver()
        }
        return NoOpLedDriver()
    }
}
