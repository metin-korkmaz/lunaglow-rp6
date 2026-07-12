package dev.lunaglow.capture

import org.junit.Assert.assertEquals
import org.junit.Test

class CaptureStopStateTest {
    @Test
    fun `service destruction preserves an existing capture error`() {
        val error = CaptureState.Error("projection failed")

        assertEquals(error, CaptureStopState.resolve(error, null))
    }

    @Test
    fun `new failure message becomes the terminal state`() {
        assertEquals(
            CaptureState.Error("missing consent"),
            CaptureStopState.resolve(CaptureState.Starting, "missing consent"),
        )
    }

    @Test
    fun `normal stop returns idle`() {
        assertEquals(CaptureState.Idle, CaptureStopState.resolve(CaptureState.Starting, null))
    }
}
