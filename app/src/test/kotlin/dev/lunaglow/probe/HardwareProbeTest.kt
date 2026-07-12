package dev.lunaglow.probe

import dev.lunaglow.probe.HardwareProbe.CommandResult
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test

class HardwareProbeTest {

    private fun ok(stdout: String): CommandResult =
        CommandResult(stdout = stdout, stderr = "", exitCode = 0)

    private fun fail(stderr: String, exitCode: Int = 1): CommandResult =
        CommandResult(stdout = "", stderr = stderr, exitCode = exitCode)

    @Test
    fun `probeSuccess true when both essential commands succeed`() {
        val probe = HardwareProbe()
        val runner = HardwareProbe.CommandRunner { command ->
            when {
                command.first() == "service" -> ok("42: [dev.pulse.PServerBinder]\n")
                command.first() == "ls" && command.contains("/sys/class/leds") -> ok("rgb_left_r\nrgb_right_r\n")
                command.first() == "ls" && command.contains("-ld") -> ok("drwxr-xr-- 2 root root 4096 dummy")
                else -> ok("")
            }
        }
        val report = probe.probe(runner = runner, timestamp = 0)

        assertTrue(report.probeSuccess)
        assertTrue(report.pServerAvailable)
        assertEquals(2, report.ledNodes.size)
    }

    @Test
    fun `probeSuccess false when service list exits non-zero`() {
        val probe = HardwareProbe()
        val runner = HardwareProbe.CommandRunner { command ->
            when {
                command.first() == "service" -> fail("Permission denied", exitCode = 1)
                command.first() == "ls" && command.contains("/sys/class/leds") -> ok("rgb_left_r\n")
                command.first() == "ls" && command.contains("-ld") -> ok("drwxr-xr--")
                else -> ok("")
            }
        }
        val report = probe.probe(runner = runner, timestamp = 0)

        assertFalse(report.probeSuccess)
        assertFalse(report.pServerAvailable)
        assertTrue(report.warnings.any { it.contains("service list exit=1") && it.contains("Permission denied") })
    }

    @Test
    fun `probeSuccess false when ls sysfs exits non-zero`() {
        val probe = HardwareProbe()
        val runner = HardwareProbe.CommandRunner { command ->
            when {
                command.first() == "service" -> ok("42: [dev.pulse.PServerBinder]\n")
                command.first() == "ls" && command.contains("/sys/class/leds") -> fail("No such file", exitCode = 2)
                command.first() == "ls" && command.contains("-ld") -> ok("drwxr-xr--")
                else -> ok("")
            }
        }
        val report = probe.probe(runner = runner, timestamp = 0)

        assertFalse(report.probeSuccess)
        assertTrue(report.ledNodes.isEmpty())
        assertTrue(report.warnings.any { it.contains("ls /sys/class/leds exit=2") && it.contains("No such file") })
    }

    @Test
    fun `probeSuccess false when service list throws`() {
        val probe = HardwareProbe()
        val runner = HardwareProbe.CommandRunner { command ->
            when {
                command.first() == "service" -> throw RuntimeException("exec failed")
                command.first() == "ls" && command.contains("/sys/class/leds") -> ok("rgb_left_r\n")
                command.first() == "ls" && command.contains("-ld") -> ok("drwxr-xr--")
                else -> ok("")
            }
        }
        val report = probe.probe(runner = runner, timestamp = 0)

        assertFalse(report.probeSuccess)
        assertTrue(report.warnings.any { it.contains("service list threw") && it.contains("exec failed") })
    }

    @Test
    fun `probeSuccess false when ls sysfs throws`() {
        val probe = HardwareProbe()
        val runner = HardwareProbe.CommandRunner { command ->
            when {
                command.first() == "service" -> ok("42: [dev.pulse.PServerBinder]\n")
                command.first() == "ls" && command.contains("/sys/class/leds") -> throw RuntimeException("no ls")
                command.first() == "ls" && command.contains("-ld") -> ok("drwxr-xr--")
                else -> ok("")
            }
        }
        val report = probe.probe(runner = runner, timestamp = 0)

        assertFalse(report.probeSuccess)
        assertTrue(report.warnings.any { it.contains("ls /sys/class/leds threw") && it.contains("no ls") })
    }

    @Test
    fun `per-node ls -ld failure recorded as warning but does not affect probeSuccess`() {
        val probe = HardwareProbe()
        val runner = HardwareProbe.CommandRunner { command ->
            when {
                command.first() == "service" -> ok("42: [dev.pulse.PServerBinder]\n")
                command.first() == "ls" && command.contains("/sys/class/leds") -> ok("node_a\n")
                command.first() == "ls" && command.contains("-ld") -> fail("Permission denied", exitCode = 1)
                else -> ok("")
            }
        }
        val report = probe.probe(runner = runner, timestamp = 0)

        assertTrue(report.probeSuccess)
        assertEquals(1, report.ledNodes.size)
        assertTrue(report.warnings.any { it.contains("ls -ld node_a exit=1") && it.contains("Permission denied") })
    }

    @Test
    fun `per-node ls -ld thrown exception recorded as warning`() {
        val probe = HardwareProbe()
        val runner = HardwareProbe.CommandRunner { command ->
            when {
                command.first() == "service" -> ok("")
                command.first() == "ls" && command.contains("/sys/class/leds") -> ok("node_a\n")
                command.first() == "ls" && command.contains("-ld") -> throw RuntimeException("boom")
                else -> ok("")
            }
        }
        val report = probe.probe(runner = runner, timestamp = 0)

        assertTrue(report.probeSuccess)
        assertTrue(report.warnings.any { it.contains("ls -ld node_a threw") && it.contains("boom") })
    }

    @Test
    fun `stderr truncated in warning to prevent log flooding`() {
        val probe = HardwareProbe()
        val longStderr = "x".repeat(500)
        val runner = HardwareProbe.CommandRunner { command ->
            when {
                command.first() == "service" -> fail(longStderr, exitCode = 1)
                command.first() == "ls" && command.contains("/sys/class/leds") -> ok("")
                else -> ok("")
            }
        }
        val report = probe.probe(runner = runner, timestamp = 0)

        val serviceWarning = report.warnings.first { it.contains("service list exit=") }
        assertTrue(serviceWarning.length < 300)
    }

    @Test
    fun `non-zero exit with empty stderr still recorded`() {
        val probe = HardwareProbe()
        val runner = HardwareProbe.CommandRunner { command ->
            when {
                command.first() == "service" -> fail("", exitCode = 127)
                command.first() == "ls" && command.contains("/sys/class/leds") -> ok("")
                else -> ok("")
            }
        }
        val report = probe.probe(runner = runner, timestamp = 0)

        assertFalse(report.probeSuccess)
        assertTrue(report.warnings.any { it.contains("exit=127") })
    }

    @Test
    fun `report has timestamp from parameter`() {
        val probe = HardwareProbe()
        val runner = HardwareProbe.CommandRunner { _ -> ok("") }
        val report = probe.probe(runner = runner, timestamp = 1234567890L)

        assertEquals(1234567890L, report.timestamp)
    }

    @Test
    fun `both commands succeed but no nodes and no PServer still success`() {
        val probe = HardwareProbe()
        val runner = HardwareProbe.CommandRunner { _ -> ok("") }
        val report = probe.probe(runner = runner, timestamp = 0)

        assertTrue(report.probeSuccess)
        assertFalse(report.pServerAvailable)
        assertTrue(report.ledNodes.isEmpty())
    }
}