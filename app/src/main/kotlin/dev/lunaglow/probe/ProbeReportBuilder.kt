package dev.lunaglow.probe

import dev.lunaglow.led.LedTopology

/**
 * Builds a [ProbeReport] from raw command outputs collected by [HardwareProbe].
 *
 * This object is pure (no side effects, no I/O) so it can be fully unit-tested
 * without a device. It validates every piece of raw output through
 * [ProbeReportParser] and derives the [LedTopology] conservatively:
 * topology nodes and mapping are only set when names follow an expected
 * naming pattern (left/right keyword); otherwise they stay null/UNKNOWN.
 */
object ProbeReportBuilder {

    /** Keyword patterns used to *guess* left/right mapping from node names only. */
    private val LEFT_PATTERN = Regex("(?i)left|l_stick|stick_l|joystick_l")
    private val RIGHT_PATTERN = Regex("(?i)right|r_stick|stick_r|joystick_r")

    /**
     * Builds a [ProbeReport] from the collected raw outputs.
     */
    fun build(
        timestamp: Long,
        deviceModel: String,
        buildFingerprint: String,
        rawServiceList: String,
        rawLedListing: String,
        perNodeData: Map<String, NodeRawData>,
        warnings: List<String> = emptyList(),
        probeSuccess: Boolean = true,
    ): ProbeReport {
        val pServerLine = ProbeReportParser.extractPServerLine(rawServiceList)
        val pServerAvailable = pServerLine.isNotEmpty()

        val nodeNames = ProbeReportParser.parseLedListing(rawLedListing)
        val ledNodes = nodeNames.map { name ->
            val data = perNodeData[name] ?: NodeRawData("", "", "")
            LedNodeInfo(
                name = name,
                maxBrightness = ProbeReportParser.parseMaxBrightness(data.maxBrightnessRaw),
                multiIndex = ProbeReportParser.parseMultiIndex(data.multiIndexRaw),
                permissions = ProbeReportParser.parsePermissions(data.permissionsRaw),
            )
        }

        val topology = deriveTopology(nodeNames, ledNodes)

        return ProbeReport(
            timestamp = timestamp,
            deviceModel = deviceModel.trim(),
            buildFingerprint = buildFingerprint.trim(),
            pServerAvailable = pServerAvailable,
            pServerRawLine = pServerLine,
            ledNodes = ledNodes,
            topology = topology,
            probeSuccess = probeSuccess,
            warnings = warnings,
        )
    }

    /**
     * Conservatively derives a [LedTopology] from discovered nodes.
     *
     * Channel order is left empty until the device confirms the channel order
     * via a static test (Gate H step 3). Left/right mapping is *guessed* from
     * the node name pattern but stored as a hint, not a confirmed mapping.
     */
    private fun deriveTopology(
        nodeNames: List<String>,
        ledNodes: List<LedNodeInfo>,
    ): LedTopology {
        if (nodeNames.isEmpty()) return LedTopology.UNKNOWN

        val maxBrightness = ledNodes.maxOfOrNull { it.maxBrightness }?.takeIf { it > 0 } ?: 0

        val allMultiIndices = ledNodes.flatMap { it.multiIndex }.distinct()

        val leftNode = nodeNames.firstOrNull { LEFT_PATTERN.containsMatchIn(it) }
        val rightNode = nodeNames.firstOrNull { RIGHT_PATTERN.containsMatchIn(it) }

        return LedTopology(
            nodeNames = nodeNames,
            // Channel order is NOT guessed from naming — confirmed via static test later.
            channelOrder = emptyList(),
            maxBrightness = maxBrightness,
            multiIndex = allMultiIndices,
            leftNode = leftNode,
            rightNode = rightNode,
        )
    }
}

/** Raw text output for a single LED node. */
data class NodeRawData(
    val maxBrightnessRaw: String,
    val multiIndexRaw: String,
    val permissionsRaw: String,
)
