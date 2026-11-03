package com.inspiredandroid.kai.tools

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assume.assumeTrue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlin.time.measureTime

/**
 * Foreground runs must return (or cancel) promptly even when the command leaves something holding
 * its output pipes open, and a cancelled run must not leave the command behind.
 */
class ShellCommandToolTest {

    private val isWindows = System.getProperty("os.name").lowercase().contains("win")

    @Test
    fun `plain command returns output and exit code`() = runBlocking {
        assumeTrue("Windows has no POSIX shell", !isWindows)
        val result = ShellCommandTool.execute(mapOf("command" to "echo out")) as Map<*, *>

        assertEquals(0, result["exit_code"])
        assertEquals("out", result["stdout"].toString().trim())
        assertEquals(false, result["timed_out"])
    }

    @Test
    fun `cancelling a running command kills it`() = runBlocking {
        assumeTrue("Windows has no POSIX shell", !isWindows)
        // Odd duration so the process scan below cannot collide with an
        // unrelated `sleep` running elsewhere on the machine.
        val elapsed = measureTime {
            assertFailsWith<CancellationException> {
                withTimeout(1.seconds) {
                    ShellCommandTool.execute(mapOf("command" to "sleep 27.314"))
                }
            }
        }
        assertTrue(elapsed < 3.seconds, "cancellation took $elapsed")

        // The call returned promptly; now prove the command itself did not
        // survive the cancellation (destroyTree in the finally block kills it).
        Thread.sleep(1_000)
        val survived = ProcessHandle.allProcesses().anyMatch { handle ->
            handle.isAlive && handle.info().commandLine().orElse("").contains("sleep 27.314")
        }
        assertFalse(survived, "command kept running after cancellation")
    }
}
