package dev.lunaglow.ui

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.lunaglow.R
import dev.lunaglow.led.LedDriverFactory
import dev.lunaglow.led.LedDriverRegistry
import dev.lunaglow.probe.HardwareProbe
import dev.lunaglow.probe.LedNodeInfo
import dev.lunaglow.probe.ProbeReport
import dev.lunaglow.probe.ProbeReportFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private sealed interface ProbeStatus {
    data object Idle : ProbeStatus
    data object Running : ProbeStatus
    data class Success(val report: ProbeReport) : ProbeStatus
    data class Failed(val message: String) : ProbeStatus
}

@Composable
fun DiagnosticsScreen(onBack: (() -> Unit)? = null) {
    var probeStatus by remember { mutableStateOf<ProbeStatus>(ProbeStatus.Idle) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.diag_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.semantics { heading() },
        )
        if (onBack != null) {
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.action_back_to_main))
            }
        }
        Text(
            text = stringResource(R.string.diag_readonly_notice),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        val isRunning = probeStatus is ProbeStatus.Running
        val runLabel = stringResource(R.string.action_run_probe)
        val probingLabel = stringResource(R.string.action_probing)
        val runA11y = stringResource(R.string.action_probe_a11y)
        val probingA11y = stringResource(R.string.action_probing_a11y)
        Button(
            onClick = {
                if (probeStatus is ProbeStatus.Running) return@Button
                probeStatus = ProbeStatus.Running
                scope.launch {
                    val result: ProbeStatus = withContext(Dispatchers.IO) {
                        try {
                            val probe = HardwareProbe()
                            ProbeStatus.Success(probe.probe())
                        } catch (e: Exception) {
                            ProbeStatus.Failed(e.message ?: e.javaClass.simpleName)
                        }
                    }
                    probeStatus = result
                }
            },
            enabled = !isRunning,
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = if (isRunning) probingA11y else runA11y
                },
        ) {
            if (isRunning) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 3.dp,
                )
                Spacer(Modifier.width(8.dp))
                Text(probingLabel)
            } else {
                Text(runLabel)
            }
        }

        when (val s = probeStatus) {
            ProbeStatus.Idle -> Text(
                text = stringResource(R.string.diag_idle_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ProbeStatus.Running -> Text(
                text = stringResource(R.string.diag_running_hint),
                style = MaterialTheme.typography.bodyMedium,
            )
            is ProbeStatus.Success -> {
                if (s.report.probeSuccess) {
                    StatusBanner(
                        text = stringResource(R.string.status_success_ok),
                        contentDescriptionText = stringResource(R.string.status_success_a11y),
                        tone = StatusTone.SUCCESS,
                    )
                } else {
                    StatusBanner(
                        text = stringResource(R.string.status_partial),
                        contentDescriptionText = stringResource(R.string.status_partial_a11y),
                        tone = StatusTone.PARTIAL,
                    )
                }
                ProbeReportCard(s.report)
                ShareProbeReportButton(s.report)
            }
            is ProbeStatus.Failed -> {
                StatusBanner(
                    text = stringResource(R.string.status_failed_prefix, s.message),
                    contentDescriptionText = stringResource(R.string.status_failed_a11y, s.message),
                    tone = StatusTone.FAILED,
                )
            }
        }
    }
}

@Composable
private fun ShareProbeReportButton(report: ProbeReport) {
    val context = LocalContext.current
    OutlinedButton(
        onClick = {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.share_probe_subject))
                putExtra(Intent.EXTRA_TEXT, ProbeReportFormatter.format(report))
            }
            context.startActivity(
                Intent.createChooser(shareIntent, context.getString(R.string.share_probe_chooser)),
            )
        },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(stringResource(R.string.action_share_probe))
    }
}

private enum class StatusTone { SUCCESS, PARTIAL, FAILED }

private data class StatusStyle(
    val container: Color,
    val content: Color,
    val border: Color,
    val glyph: String,
)

@Composable
private fun statusStyle(tone: StatusTone): StatusStyle = when (tone) {
    StatusTone.SUCCESS -> StatusStyle(
        container = MaterialTheme.colorScheme.primaryContainer,
        content = MaterialTheme.colorScheme.onPrimaryContainer,
        border = MaterialTheme.colorScheme.primary,
        glyph = "✓",
    )
    StatusTone.PARTIAL -> StatusStyle(
        container = MaterialTheme.colorScheme.surfaceVariant,
        content = MaterialTheme.colorScheme.onSurface,
        border = MaterialTheme.colorScheme.outline,
        glyph = "!",
    )
    StatusTone.FAILED -> StatusStyle(
        container = MaterialTheme.colorScheme.errorContainer,
        content = MaterialTheme.colorScheme.onErrorContainer,
        border = MaterialTheme.colorScheme.error,
        glyph = "✗",
    )
}

/**
 * A status banner that distinguishes success / partial-failure / failed by
 * surface color, a leading glyph, a border, and descriptive text — never by
 * hue alone. Each banner also exposes a TalkBack [contentDescription].
 */
@Composable
private fun StatusBanner(
    text: String,
    contentDescriptionText: String,
    tone: StatusTone,
) {
    val style = statusStyle(tone)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = contentDescriptionText
            },
        colors = CardDefaults.cardColors(containerColor = style.container),
        border = BorderStroke(1.dp, style.border),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = style.glyph,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = style.content,
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = style.content,
            )
        }
    }
}

@Composable
private fun ProbeReportCard(report: ProbeReport) {
    val driver = remember(report) { LedDriverFactory.create(report) }
    LaunchedEffect(driver) { LedDriverRegistry.install(driver) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SectionTitle(stringResource(R.string.section_device))
            InfoRow(stringResource(R.string.label_model), report.deviceModel.ifEmpty { stringResource(R.string.value_dash) })
            InfoRow(stringResource(R.string.label_build), report.buildFingerprint.ifEmpty { stringResource(R.string.value_dash) })

            SectionTitle(stringResource(R.string.section_pserver))
            InfoRow(stringResource(R.string.label_available), if (report.pServerAvailable) stringResource(R.string.value_yes) else stringResource(R.string.value_no))
            if (report.pServerRawLine.isNotEmpty()) {
                Text(
                    text = report.pServerRawLine,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                )
            }

            SectionTitle(stringResource(R.string.section_led_nodes, report.ledNodes.size))
            if (report.ledNodes.isEmpty()) {
                Text(
                    text = stringResource(R.string.value_none_discovered),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                report.ledNodes.forEach { LedNodeCard(it) }
            }

            SectionTitle(stringResource(R.string.section_topology))
            InfoRow(stringResource(R.string.label_left_node), report.topology.leftNode ?: stringResource(R.string.value_unconfirmed))
            InfoRow(stringResource(R.string.label_right_node), report.topology.rightNode ?: stringResource(R.string.value_unconfirmed))
            InfoRow(stringResource(R.string.label_max_brightness), report.topology.maxBrightness.toString())
            InfoRow(stringResource(R.string.label_multi_index), report.topology.multiIndex.joinToString(", ").ifEmpty { stringResource(R.string.value_dash) })

            SectionTitle(stringResource(R.string.section_driver))
            InfoRow(stringResource(R.string.label_selected), driver.driverName)
            InfoRow(stringResource(R.string.label_available), if (driver.isAvailable) stringResource(R.string.value_yes) else stringResource(R.string.value_no_degraded))

            if (report.warnings.isNotEmpty()) {
                SectionTitle(stringResource(R.string.section_warnings, report.warnings.size))
                report.warnings.forEach {
                    Text(
                        text = "- $it",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

/**
 * A single node rendered as a stacked card so that long node names,
 * multi-index lists, and permissions strings wrap instead of forcing
 * a single row that overflows the handheld screen width.
 */
@Composable
private fun LedNodeCard(node: LedNodeInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = node.name,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = stringResource(R.string.label_node_max, node.maxBrightness),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (node.multiIndex.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.label_node_index, node.multiIndex.joinToString(",")),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (node.permissions.isNotEmpty()) {
                Text(
                    text = node.permissions,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Spacer(Modifier.height(4.dp))
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.semantics { heading() },
    )
}

/**
 * Label/value pair laid out so the value wraps below the label on narrow
 * handheld screens instead of being squeezed into a single row.
 */
@Composable
private fun InfoRow(label: String, value: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
    }
}
