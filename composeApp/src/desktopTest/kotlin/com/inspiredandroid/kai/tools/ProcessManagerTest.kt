package com.inspiredandroid.kai.tools

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProcessManagerTest {

    private val isWindows = System.getProperty("os.name").lowercase().contains("win")

    /**
     * Twenty numbered lines, one per line. The desktop runs commands through `cmd /c` on
     * Windows and `sh -c` everywhere else, so the command has to be the one that shell knows.
     */
    private fun sequenceCommand(): String = if (isWindows) "(for /L %i in (1,1,20) do @echo %i)" else "seq 1 20"

    /**
     * A command that keeps running for [seconds]. `ping` against the loopback address is the
     * Windows stand-in for `sleep` — it needs no extra binary and is silenced.
     */
    private fun sleepCommand(seconds: Int): String = if (isWindows) "ping -n ${seconds + 1} 127.0.0.1 > nul" else "sleep $seconds"

    /** Prints the value of an environment variable — `%VAR%` on Windows, `$VAR` on POSIX. */
    private fun echoEnvCommand(name: String): String = if (isWindows) "echo %$name%" else "echo \$$name"

    /**
     * Waits until the session has finished.
     *
     * A fixed sleep would race the process: on a loaded machine the shell can take longer than
     * half a second to start, and the log would be read before the command wrote anything.
     */
    private fun awaitFinished(pm: ProcessManager, sessionId: String, timeoutMs: Long = 15_000) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (pm.log(sessionId, 0, 1)["status"] == "finished") return
            Thread.sleep(25)
        }
        throw AssertionError("session $sessionId did not finish within ${timeoutMs}ms")
    }

    @Test
    fun startBackgroundReturnsSessionId() {
        val pm = ProcessManager()
        val result = pm.startBackground("echo hello", 10, null, emptyMap())
        assertEquals(true, result["success"])
        assertTrue((result["session_id"] as String).startsWith("bg-"))
        assertEquals("running", result["status"])
    }

    @Test
    fun listShowsRunningAndFinished() {
        val pm = ProcessManager()
        val result = pm.startBackground("echo fast", 10, null, emptyMap())
        awaitFinished(pm, result["session_id"] as String)
        val list = pm.list()
        assertEquals(1, list["total"])
    }

    @Test
    fun logReturnsOutput() {
        val pm = ProcessManager()
        val result = pm.startBackground("echo hello_world", 10, null, emptyMap())
        val sessionId = result["session_id"] as String
        awaitFinished(pm, sessionId)
        val log = pm.log(sessionId, 0, 200)
        assertEquals(true, log["success"])
        assertTrue((log["stdout"] as String).contains("hello_world"))
        assertEquals("finished", log["status"])
    }

    @Test
    fun logWithOffsetAndLimit() {
        val pm = ProcessManager()
        val result = pm.startBackground(sequenceCommand(), 10, null, emptyMap())
        val sessionId = result["session_id"] as String
        awaitFinished(pm, sessionId)
        val log = pm.log(sessionId, 5, 3)
        assertEquals(true, log["success"])
        assertEquals(5, log["offset"])
        val lines = (log["stdout"] as String).trim().lines()
        assertEquals(3, lines.size)
        assertEquals("6", lines[0]) // 20 numbered lines, offset 5 means line index 5 = "6"
    }

    @Test
    fun killTerminatesRunningProcess() {
        val pm = ProcessManager()
        val result = pm.startBackground(sleepCommand(5), 120, null, emptyMap())
        val sessionId = result["session_id"] as String
        Thread.sleep(200)
        val killResult = pm.kill(sessionId)
        assertEquals(true, killResult["success"])
    }

    @Test
    fun killAlreadyFinishedProcess() {
        val pm = ProcessManager()
        val result = pm.startBackground("echo done", 10, null, emptyMap())
        val sessionId = result["session_id"] as String
        awaitFinished(pm, sessionId)
        val killResult = pm.kill(sessionId)
        assertEquals(true, killResult["success"])
        assertTrue((killResult["message"] as String).contains("already finished"))
    }

    @Test
    fun removeSession() {
        val pm = ProcessManager()
        val result = pm.startBackground("echo bye", 10, null, emptyMap())
        val sessionId = result["session_id"] as String
        awaitFinished(pm, sessionId)
        val removeResult = pm.remove(sessionId)
        assertEquals(true, removeResult["success"])
        // After removal, list should be empty
        val list = pm.list()
        assertEquals(0, list["total"])
    }

    @Test
    fun unknownSessionReturnsError() {
        val pm = ProcessManager()
        val log = pm.log("nonexistent", 0, 200)
        assertEquals(false, log["success"])
        val kill = pm.kill("nonexistent")
        assertEquals(false, kill["success"])
        val remove = pm.remove("nonexistent")
        assertEquals(false, remove["success"])
    }

    @Test
    fun envVariablesArePassedThrough() {
        val pm = ProcessManager()
        val result = pm.startBackground(echoEnvCommand("MY_TEST_VAR"), 10, null, mapOf("MY_TEST_VAR" to "test_value_123"))
        val sessionId = result["session_id"] as String
        awaitFinished(pm, sessionId)
        val log = pm.log(sessionId, 0, 200)
        assertTrue((log["stdout"] as String).contains("test_value_123"))
    }

    @Test
    fun backgroundProcessTimesOut() {
        val pm = ProcessManager()
        // Runs for three seconds, so the one-second timeout has to cut it short.
        val result = pm.startBackground(sleepCommand(3), 1, null, emptyMap())
        val sessionId = result["session_id"] as String
        awaitFinished(pm, sessionId)
        val log = pm.log(sessionId, 0, 200)
        assertEquals(true, log["timed_out"])
        assertEquals("finished", log["status"])
    }
}
