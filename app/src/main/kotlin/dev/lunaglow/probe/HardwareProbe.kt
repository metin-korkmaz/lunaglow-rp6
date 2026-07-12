package dev.lunaglow.probe

import android.os.Build
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Read-only hardware capability probe for Gate H.
 *
 * Executes a fixed set of read-only commands to detect:
 *  - Whether `PServerBinder` is registered as a system service.
 *  - LED node names and safe metadata under `/sys/class/leds/`.
 *
 * Security constraints:
 *  - Only the fixed command `ls -1 /sys/class/leds` is used for enumeration.
 *  - Per-node reads target `max_brightness`, `multi_index`, and `ls -ld` for permissions.
 *  - No arbitrary user-provided command is ever executed.
 *  - No LED values are written. No root, accessibility, or overlay access is requested.
 *  - All paths are validated by [ProbeReportParser] before inclusion in the report.
 */
class HardwareProbe {

    /**
     * Result of a fixed read-only command: stdout, stderr, and exit code.
     * Implementations must NOT accept arbitrary commands from callers.
     */
    data class CommandResult(
        val stdout: String,
        val stderr: String,
        val exitCode: Int,
    ) {
        val succeeded: Boolean get() = exitCode == 0
    }

    fun interface CommandRunner {
        fun run(command: List<String>): CommandResult
    }

    /**
     * Runs the Gate H probe and returns a structured [ProbeReport].
     *
     * Uses [runner] to execute the fixed commands so the probe logic can be
     * unit-tested without a real device. When [runner] is null, a default
     * [RuntimeCommandRunner] is used (only on a real device).
     *
     * The probe is considered successful ([ProbeReport.probeSuccess] = true)
     * only when both essential enumeration commands succeed:
     *  - `service list` (PServerBinder detection)
     *  - `ls -1 /sys/class/leds` (LED node enumeration)
     *
     * Per-node metadata read failures (max_brightness, multi_index, ls -ld) are
     * recorded as non-fatal warnings and do not affect probeSuccess.
     */
    fun probe(
        runner: CommandRunner? = null,
        timestamp: Long = System.currentTimeMillis(),
    ): ProbeReport {
        val cmd = runner ?: RuntimeCommandRunner()
        val warnings = mutableListOf<String>()

        val deviceModel = try { Build.MODEL ?: "" } catch (e: Exception) { warnings.add("model: ${e.message}"); "" }
        val buildFingerprint = try { Build.FINGERPRINT ?: "" } catch (e: Exception) { warnings.add("fingerprint: ${e.message}"); "" }

        val serviceResult = try {
            cmd.run(listOf("service", "list"))
        } catch (e: Exception) {
            warnings.add("service list threw: ${e.message}")
            null
        }
        val rawServiceList = serviceResult?.stdout ?: ""
        if (serviceResult != null && !serviceResult.succeeded) {
            warnings.add("service list exit=${serviceResult.exitCode} stderr=${serviceResult.stderr.trim().take(200)}")
        }

        val ledResult = try {
            cmd.run(listOf("ls", "-1", "/sys/class/leds"))
        } catch (e: Exception) {
            warnings.add("ls /sys/class/leds threw: ${e.message}")
            null
        }
        val rawLedListing = ledResult?.stdout ?: ""
        if (ledResult != null && !ledResult.succeeded) {
            warnings.add("ls /sys/class/leds exit=${ledResult.exitCode} stderr=${ledResult.stderr.trim().take(200)}")
        }

        val nodeNames = ProbeReportParser.parseLedListing(rawLedListing)
        val perNodeData = mutableMapOf<String, NodeRawData>()
        for (name in nodeNames) {
            val validatedPath = ProbeReportParser.LEDS_BASE_PATH + name
            if (!validatedPath.startsWith(ProbeReportParser.LEDS_BASE_PATH)) {
                warnings.add("skipped invalid node path: $name")
                continue
            }

            val maxB = tryReadFile("$validatedPath/max_brightness")
            if (maxB == null) warnings.add("max_brightness read failed for $name")

            val multiIdx = tryReadFile("$validatedPath/multi_index")
            if (multiIdx == null) warnings.add("multi_index read failed for $name")

            val perms = try {
                val r = cmd.run(listOf("ls", "-ld", validatedPath))
                if (!r.succeeded) {
                    warnings.add("ls -ld $name exit=${r.exitCode} stderr=${r.stderr.trim().take(200)}")
                }
                r.stdout
            } catch (e: Exception) {
                warnings.add("ls -ld $name threw: ${e.message}")
                ""
            }
            perNodeData[name] = NodeRawData(maxB ?: "", multiIdx ?: "", perms)
        }

        val essentialSuccess = serviceResult != null && serviceResult.succeeded &&
            ledResult != null && ledResult.succeeded

        return ProbeReportBuilder.build(
            timestamp = timestamp,
            deviceModel = deviceModel,
            buildFingerprint = buildFingerprint,
            rawServiceList = rawServiceList,
            rawLedListing = rawLedListing,
            perNodeData = perNodeData,
            warnings = warnings,
            probeSuccess = essentialSuccess,
        )
    }

    private fun tryReadFile(path: String): String? {
        return try {
            File(path).readText().trim()
        } catch (e: Exception) {
            null
        }
    }

    private class RuntimeCommandRunner : CommandRunner {
        override fun run(command: List<String>): CommandResult {
            val proc = ProcessBuilder(command)
                .redirectErrorStream(false)
                .start()

            var capturedStdout = ""
            var capturedStderr = ""
            val outJob = Thread {
                capturedStdout = try {
                    proc.inputStream.bufferedReader().use { it.readText() }
                } catch (_: Exception) { "" }
            }
            val errJob = Thread {
                capturedStderr = try {
                    proc.errorStream.bufferedReader().use { it.readText() }
                } catch (_: Exception) { "" }
            }

            outJob.start()
            errJob.start()

            var timedOut = false
            try {
                if (!proc.waitFor(COMMAND_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                    timedOut = true
                    proc.destroyForcibly()
                    proc.waitFor()
                }
            } finally {
                if (proc.isAlive) proc.destroyForcibly()
                outJob.join(STREAM_JOIN_TIMEOUT_MILLIS)
                errJob.join(STREAM_JOIN_TIMEOUT_MILLIS)
            }

            return CommandResult(
                stdout = capturedStdout,
                stderr = if (timedOut) {
                    listOf(capturedStderr.trim(), "command timed out")
                        .filter { it.isNotEmpty() }
                        .joinToString(": ")
                } else {
                    capturedStderr
                },
                exitCode = if (timedOut) COMMAND_TIMEOUT_EXIT_CODE else proc.exitValue(),
            )
        }

        private companion object {
            const val COMMAND_TIMEOUT_SECONDS = 5L
            const val STREAM_JOIN_TIMEOUT_MILLIS = 1_000L
            const val COMMAND_TIMEOUT_EXIT_CODE = 124
        }
    }
}
