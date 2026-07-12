package dev.lunaglow.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.lunaglow.R
import dev.lunaglow.capture.CaptureState
import dev.lunaglow.color.RgbColor
import kotlinx.coroutines.flow.StateFlow

@Composable
fun MainScreen(
    captureState: StateFlow<CaptureState>,
    onStartCapture: () -> Unit,
    onStopCapture: () -> Unit,
    onOpenDiagnostics: () -> Unit,
) {
    val state = captureState.collectAsState()
    MainScreenContent(state, onStartCapture, onStopCapture, onOpenDiagnostics)
}

@Composable
private fun MainScreenContent(
    captureState: State<CaptureState>,
    onStartCapture: () -> Unit,
    onStopCapture: () -> Unit,
    onOpenDiagnostics: () -> Unit,
) {
    val state = captureState.value
    val capturing = state is CaptureState.Starting || state is CaptureState.Capturing
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.main_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = stringResource(R.string.main_privacy_notice),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        CaptureStatusCard(state)
        if (capturing) {
            OutlinedButton(onClick = onStopCapture, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.action_stop_capture))
            }
        } else {
            Button(onClick = onStartCapture, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.action_start_capture))
            }
        }
        OutlinedButton(onClick = onOpenDiagnostics, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.action_open_diagnostics))
        }
    }
}

@Composable
private fun CaptureStatusCard(state: CaptureState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = when (state) {
                    CaptureState.Idle -> stringResource(R.string.capture_status_idle)
                    CaptureState.Starting -> stringResource(R.string.capture_status_starting)
                    is CaptureState.Capturing -> stringResource(
                        R.string.capture_status_active,
                        state.processedFrames,
                    )
                    is CaptureState.Error -> stringResource(R.string.capture_status_error, state.message)
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            val colors = (state as? CaptureState.Capturing)?.colors
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ColorPreview(
                    label = stringResource(R.string.label_left_color),
                    color = colors?.left ?: RgbColor.BLACK,
                    modifier = Modifier.weight(1f),
                )
                ColorPreview(
                    label = stringResource(R.string.label_right_color),
                    color = colors?.right ?: RgbColor.BLACK,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun ColorPreview(label: String, color: RgbColor, modifier: Modifier = Modifier) {
    val description = stringResource(
        R.string.color_preview_a11y,
        label,
        color.red,
        color.green,
        color.blue,
    )
    Column(
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = description
        },
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .background(Color(color.red, color.green, color.blue)),
        )
        Text(
            text = "${color.red}, ${color.green}, ${color.blue}",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
