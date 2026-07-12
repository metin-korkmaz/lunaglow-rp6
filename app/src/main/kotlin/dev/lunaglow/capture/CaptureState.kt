package dev.lunaglow.capture

import dev.lunaglow.color.ScreenColors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface CaptureState {
    data object Idle : CaptureState
    data object Starting : CaptureState
    data class Capturing(val colors: ScreenColors, val processedFrames: Long) : CaptureState
    data class Error(val message: String) : CaptureState
}

object CaptureStateStore {
    private val mutableState = MutableStateFlow<CaptureState>(CaptureState.Idle)
    val state: StateFlow<CaptureState> = mutableState.asStateFlow()

    fun update(state: CaptureState) {
        mutableState.value = state
    }
}

internal object CaptureStopState {
    fun resolve(current: CaptureState, errorMessage: String?): CaptureState = when {
        errorMessage != null -> CaptureState.Error(errorMessage)
        current is CaptureState.Error -> current
        else -> CaptureState.Idle
    }
}
