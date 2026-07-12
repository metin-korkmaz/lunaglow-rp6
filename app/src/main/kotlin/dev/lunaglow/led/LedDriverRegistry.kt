package dev.lunaglow.led

import java.util.concurrent.atomic.AtomicReference

object LedDriverRegistry {
    private val active = AtomicReference<LedDriver>(NoOpLedDriver())

    fun current(): LedDriver = active.get()

    fun install(driver: LedDriver) {
        val previous = active.getAndSet(driver)
        if (previous !== driver) previous.close()
    }
}
