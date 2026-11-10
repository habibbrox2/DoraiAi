package com.dorai.ai.tools

import com.dorai.ai.github.GitCommandPolicy
import java.io.File
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

private const val MAX_OUTPUT_LENGTH = 30_000

/** Shell metacharacters that must never reach a git subprocess (defense in depth). */
private val SHELL_METACHARS = setOf('|', '&', ';', '<', '>', '(', ')', '$', '`', '"', '\'', '\\')

class ProcessManager {

    class Session(
        val id: String,
        val command: String,
        val startTime: Long,
        val process: Process,
        val stdoutBuffer: StringBuilder = StringBuilder(),
        val stderrBuffer: StringBuilder = StringBuilder(),
        @Volatile var finished: Boolean = false,
        @Volatile var exitCode: Int? = null,
        @Volatile var timedOut: Boolean = false,
    )

    private val sessions = ConcurrentHashMap<String, Session>()
    private val nextId = AtomicInteger(1)

    fun startBackground(
        command: String,
        timeoutSeconds: Long,
        workingDir: File?,
        envMap: Map<String, String>,
    ): Map<String, Any> {
        val tokens = command.trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }

        // Git allowlist gate: route git invocations through GitCommandPolicy and spawn
        // directly via ProcessBuilder (no shell), so shell metacharacters cannot bypass
        // the read-only policy. Non-git commands keep their existing shell path.
        if (tokens.isNotEmpty() && GitCommandPolicy.isGitExecutable(tokens.first())) {
            return startGitProcess(tokens, timeoutSeconds, workingDir, envMap)
        }

        return startShellProcess(command, timeoutSeconds, workingDir, envMap)
    }

    /**
     * Spawns a validated `git` invocation directly (no shell). Returns a rejected
     * result when [GitCommandPolicy] denies the arguments.
     */
    private fun startGitProcess(
        tokens: List<String>,
        timeoutSeconds: Long,
        workingDir: File?,
        envMap: Map<String, String>,
    ): Map<String, Any> {
        val gitArgs = tokens.drop(1)

        if (!GitCommandPolicy.isAllowedGitArgs(gitArgs)) {
            return mapOf(
                "success" to false,
                "error" to "Rejected by GitCommandPolicy: ${tokens.joinToString(" ")}",
            )
        }

        // Defense in depth: even though we bypass the shell, refuse any shell
        // metacharacters in git arguments so a future shell-based caller cannot
        // inject through this path.
        if (gitArgs.any { it.any { it in SHELL_METACHARS } }) {
            return mapOf(
                "success" to false,
                "error" to "Rejected: shell metacharacters in git arguments",
            )
        }

        val processBuilder = ProcessBuilder(*tokens.toTypedArray())
        return registerSession(processBuilder, tokens.joinToString(" "), timeoutSeconds, workingDir, envMap)
    }

    private fun startShellProcess(
        command: String,
        timeoutSeconds: Long,
        workingDir: File?,
        envMap: Map<String, String>,
    ): Map<String, Any> {
        val isWindows = System.getProperty("os.name").lowercase().contains("win")
        val processBuilder = if (isWindows) {
            ProcessBuilder("cmd", "/c", command)
        } else {
            ProcessBuilder("sh", "-c", command)
        }
        return registerSession(processBuilder, command, timeoutSeconds, workingDir, envMap)
    }

    /**
     * Starts [processBuilder], registers a [Session], drains stdout/stderr, and monitors
     * completion/timeout. Shared by the git (direct) and shell code paths.
     */
    private fun registerSession(
        processBuilder: ProcessBuilder,
        command: String,
        timeoutSeconds: Long,
        workingDir: File?,
        envMap: Map<String, String>,
    ): Map<String, Any> {
        processBuilder.redirectErrorStream(false)
        if (workingDir != null && workingDir.isDirectory) {
            processBuilder.directory(workingDir)
        }
        if (envMap.isNotEmpty()) {
            processBuilder.environment().putAll(envMap)
        }

        val sessionId = "bg-${nextId.getAndIncrement()}"
        val process = processBuilder.start()
        val session = Session(
            id = sessionId,
            command = command,
            startTime = System.currentTimeMillis(),
            process = process,
        )
        sessions[sessionId] = session

        // Drain stdout/stderr in background threads with bounded buffers
        val stdoutDrain = CompletableFuture.runAsync {
            process.inputStream.bufferedReader().forEachLine { line ->
                synchronized(session.stdoutBuffer) {
                    if (session.stdoutBuffer.length < MAX_OUTPUT_LENGTH) {
                        session.stdoutBuffer.appendLine(line)
                    }
                }
            }
        }
        val stderrDrain = CompletableFuture.runAsync {
            process.errorStream.bufferedReader().forEachLine { line ->
                synchronized(session.stderrBuffer) {
                    if (session.stderrBuffer.length < MAX_OUTPUT_LENGTH) {
                        session.stderrBuffer.appendLine(line)
                    }
                }
            }
        }

        // Monitor for completion/timeout
        CompletableFuture.runAsync {
            val completed = process.waitFor(timeoutSeconds, TimeUnit.SECONDS)
            if (!completed) {
                process.destroyForcibly()
                session.timedOut = true
                session.exitCode = -1
            } else {
                session.exitCode = process.exitValue()
            }
            try {
                stdoutDrain.get(5, TimeUnit.SECONDS)
            } catch (_: Exception) {
            }
            try {
                stderrDrain.get(5, TimeUnit.SECONDS)
            } catch (_: Exception) {
            }
            session.finished = true
        }

        return mapOf(
            "success" to true,
            "session_id" to sessionId,
            "status" to "running",
            "message" to "Process started in background. Use manage_process tool to check status.",
        )
    }

    fun list(): Map<String, Any> {
        val running = sessions.values.filter { !it.finished }.map { it.toInfo() }
        val finished = sessions.values.filter { it.finished }.map { it.toInfo() }
        return mapOf(
            "running" to running,
            "finished" to finished,
            "total" to sessions.size,
        )
    }

    fun log(sessionId: String, offset: Int, limit: Int): Map<String, Any> {
        val session = sessions[sessionId]
            ?: return mapOf("success" to false, "error" to "Unknown session: $sessionId")

        val stdout = synchronized(session.stdoutBuffer) { session.stdoutBuffer.toString() }
        val stderr = synchronized(session.stderrBuffer) { session.stderrBuffer.toString() }

        val stdoutLines = stdout.lines()
        val sliced = stdoutLines.drop(offset).take(limit).joinToString("\n")

        return mapOf(
            "success" to true,
            "session_id" to sessionId,
            "status" to if (session.finished) "finished" else "running",
            "exit_code" to (session.exitCode ?: -1),
            "stdout" to sliced,
            "stderr" to stderr.takeLast(2000),
            "total_stdout_lines" to stdoutLines.size,
            "offset" to offset,
            "timed_out" to session.timedOut,
        )
    }

    fun kill(sessionId: String): Map<String, Any> {
        val session = sessions[sessionId]
            ?: return mapOf("success" to false, "error" to "Unknown session: $sessionId")

        if (session.finished) {
            return mapOf("success" to true, "message" to "Process already finished", "exit_code" to (session.exitCode ?: -1))
        }

        session.process.destroyForcibly()
        session.finished = true
        session.exitCode = -1
        return mapOf("success" to true, "message" to "Process killed")
    }

    fun remove(sessionId: String): Map<String, Any> {
        val session = sessions.remove(sessionId)
            ?: return mapOf("success" to false, "error" to "Unknown session: $sessionId")

        if (!session.finished) {
            session.process.destroyForcibly()
        }
        return mapOf("success" to true, "message" to "Session removed")
    }

    private fun Session.toInfo(): Map<String, Any> = mapOf(
        "session_id" to id,
        "command" to command,
        "status" to if (finished) "finished" else "running",
        "exit_code" to (exitCode ?: -1),
        "duration_seconds" to ((System.currentTimeMillis() - startTime) / 1000),
        "timed_out" to timedOut,
        "stdout_length" to stdoutBuffer.length,
    )
}

/** Desktop spawns host processes directly, so no collaborators are needed. */
internal fun createProcessManager(): ProcessManager = ProcessManager()
