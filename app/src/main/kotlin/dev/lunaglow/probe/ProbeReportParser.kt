package dev.lunaglow.probe

/**
 * Pure parser for the raw output of the fixed read-only probe command.
 *
 * The command is always:
 * ```
 * ls -1 /sys/class/leds
 * ```
 * followed by per-node reads of `max_brightness`, `multi_index`, and
 * `ls -ld` for permissions. This parser does NOT execute any command;
 * it only validates and structures text output.
 *
 * Security constraints enforced by this parser:
 *  - Node names must match `^[a-zA-Z0-9_:-]+$` (strict allowlist).
 *  - Paths must be under `/sys/class/leds/` with no traversal (`..` or `/` in the name).
 *  - Numeric values must be non-negative integers.
 *  - No arbitrary command fragments are accepted or echoed back.
 */
object ProbeReportParser {

    private val NODE_NAME_PATTERN = Regex("^[a-zA-Z0-9_:-]+$")

    /** Base path that every discovered node must belong to. */
    const val LEDS_BASE_PATH = "/sys/class/leds/"

    /**
     * Validates a single LED node name from `ls -1 /sys/class/leds` output.
     * Returns null if the name is malformed, contains path separators,
     * or attempts directory traversal.
     */
    fun validateNodeName(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null
        if (trimmed.contains('/') || trimmed.contains('\\')) return null
        if (trimmed == "." || trimmed == "..") return null
        if (!NODE_NAME_PATTERN.matches(trimmed)) return null
        return trimmed
    }

    /**
     * Parses the output of `ls -1 /sys/class/leds` into a list of validated
     * node names. Blank lines and malformed entries are skipped silently.
     */
    fun parseLedListing(rawOutput: String): List<String> {
        return rawOutput.lineSequence()
            .mapNotNull { validateNodeName(it) }
            .toList()
    }

    /**
     * Parses a `max_brightness` file content. Returns -1 if the value is
     * not a valid non-negative integer.
     */
    fun parseMaxBrightness(raw: String): Int {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return -1
        return trimmed.toIntOrNull()?.let { if (it >= 0) it else -1 } ?: -1
    }

    /**
     * Parses the ordered color names exposed by a multicolor LED node.
     */
    fun parseMultiIndex(raw: String): List<String> {
        val tokens = raw.trim().split(Regex("[\\s,]+"))
            .filter { it.isNotEmpty() }
        return tokens.filter { MULTI_INDEX_TOKEN_PATTERN.matches(it) }
    }

    /**
     * Parses a permission string from `ls -ld /sys/class/leds/<node>`.
     * Extracts the drwxr-xr-- style permission field, or returns empty.
     */
    fun parsePermissions(rawLsOutput: String): String {
        val parts = rawLsOutput.trim().split(Regex("\\s+"))
        for (part in parts) {
            if (UNIX_MODE_PATTERN.matches(part)) {
                return part
            }
        }
        return ""
    }

    /**
     * Checks whether a raw `service list` line indicates PServerBinder availability.
     * The line must contain the token `PServerBinder` as a service name, not just
     * as a substring inside another word.
     */
    fun isPServerLine(rawLine: String): Boolean {
        val trimmed = rawLine.trim()
        if (!trimmed.contains("PServerBinder")) return false
        // Must appear as a standalone service name, e.g. "42: [dev.lunaglow.PServerBinder]"
        // or "PServerBinder:" — reject if it's only inside a longer identifier.
        val token = Regex(
            "(^|[^a-zA-Z0-9_])(?:[a-zA-Z0-9_]+\\.)*PServerBinder([^a-zA-Z0-9_]|$)",
        )
        return token.containsMatchIn(trimmed)
    }

    /**
     * Extracts the raw PServer line from full `service list` output.
     * Returns the first matching line, or empty string.
     */
    fun extractPServerLine(fullServiceListOutput: String): String {
        return fullServiceListOutput.lineSequence()
            .firstOrNull { isPServerLine(it) }
            ?: ""
    }

    private val UNIX_MODE_PATTERN = Regex("^[dlcbps-][rwxStTs-]{9}$")
    private val MULTI_INDEX_TOKEN_PATTERN = Regex("^[a-zA-Z0-9_-]+$")
}
