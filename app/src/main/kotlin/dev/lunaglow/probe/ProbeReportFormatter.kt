package dev.lunaglow.probe

object ProbeReportFormatter {
    fun format(report: ProbeReport): String = buildString {
        appendLine("format=lunaglow-probe-v1")
        appendLine("timestamp=${report.timestamp}")
        appendLine("probeSuccess=${report.probeSuccess}")
        appendLine("deviceModel=${escape(report.deviceModel)}")
        appendLine("buildFingerprint=${escape(report.buildFingerprint)}")
        appendLine("pServerAvailable=${report.pServerAvailable}")
        appendLine("pServerRawLine=${escape(report.pServerRawLine)}")
        appendLine("topology.nodeNames=${report.topology.nodeNames.joinToString(",") { escape(it) }}")
        appendLine("topology.channelOrder=${report.topology.channelOrder.joinToString(",")}")
        appendLine("topology.maxBrightness=${report.topology.maxBrightness}")
        appendLine("topology.multiIndex=${report.topology.multiIndex.joinToString(",")}")
        appendLine("topology.leftNode=${escape(report.topology.leftNode.orEmpty())}")
        appendLine("topology.rightNode=${escape(report.topology.rightNode.orEmpty())}")
        appendLine("ledNodeCount=${report.ledNodes.size}")
        report.ledNodes.forEachIndexed { index, node ->
            appendLine("ledNode.$index.name=${escape(node.name)}")
            appendLine("ledNode.$index.maxBrightness=${node.maxBrightness}")
            appendLine("ledNode.$index.multiIndex=${node.multiIndex.joinToString(",")}")
            appendLine("ledNode.$index.permissions=${escape(node.permissions)}")
        }
        appendLine("warningCount=${report.warnings.size}")
        report.warnings.forEachIndexed { index, warning ->
            appendLine("warning.$index=${escape(warning)}")
        }
    }

    private fun escape(value: String): String = buildString(value.length) {
        value.forEach { character ->
            when (character) {
                '\\' -> append("\\\\")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                '=' -> append("\\=")
                else -> if (character.isISOControl()) {
                    append("\\u%04x".format(character.code))
                } else {
                    append(character)
                }
            }
        }
    }
}
