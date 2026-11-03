package com.inspiredandroid.kai.tools

import com.inspiredandroid.kai.network.tools.ParameterSchema
import com.inspiredandroid.kai.network.tools.Tool
import com.inspiredandroid.kai.network.tools.ToolInfo
import com.inspiredandroid.kai.network.tools.ToolSchema
import com.inspiredandroid.kai.smartTruncate
import kai.composeapp.generated.resources.Res
import kai.composeapp.generated.resources.tool_execute_shell_command_description
import kai.composeapp.generated.resources.tool_execute_shell_command_name
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import java.io.File
import java.io.InputStream
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private const val MAX_OUTPUT_LENGTH = 30_000
private const val DEFAULT_TIMEOUT_SECONDS = 30L
private const val MAX_TIMEOUT_SECONDS = 120L

// Allowlist of permitted executables (fail-closed: only these are allowed)
private val ALLOWED_EXECUTABLES = setOf(
    // File inspection
    "ls", "cat", "head", "tail", "wc", "grep", "find", "echo", "pwd", "which",
    "whoami", "date", "uname", "file", "stat", "du", "df", "tree",
    // Version control
    "git",
    // Package managers / build tools
    "npm", "yarn", "pnpm", "gradle", "mvn", "cargo", "go", "make", "cmake",
    // Runtimes / compilers
    "python", "python3", "node", "java", "javac", "kotlinc", "rustc", "gcc", "g++",
    // File operations (safe subset)
    "mkdir", "cp", "mv", "touch", "chmod", "rm",
    // Network (read-only)
    "curl", "wget",
    // Misc
    "sort", "uniq", "diff", "sed", "awk", "tr", "cut", "tee", "xargs", "sleep",
)

// Dangerous flags that are never allowed
private val BLOCKED_FLAGS = setOf(
    "-rf",
    "-r",
    "-f",
    "--recursive",
    "--force",
    "-R", // chmod -R
)

// Shell metacharacters that indicate injection attempts
private val SHELL_METACHARACTERS = setOf(';', '|', '&', '$', '`', '(', ')', '{', '}', '<', '>', '!', '\\', '\n', '\r')

// Extended blocked environment variables
private val BLOCKED_ENV_VARS = setOf(
    "PATH", "LD_PRELOAD", "LD_LIBRARY_PATH", "DYLD_INSERT_LIBRARIES",
    "DYLD_LIBRARY_PATH", "DYLD_FRAMEWORK_PATH", "JAVA_TOOL_OPTIONS",
    "PYTHONSTARTUP", "PYTHONPATH", "NODE_OPTIONS", "RUBYOPT", "PERL5OPT",
    "CLASSPATH", "GOPATH", "CARGO_HOME",
)

private fun isAllowedExecutable(executable: String): Boolean {
    val baseName = executable.substringAfterLast('/').substringAfterLast('\\').lowercase()
    return baseName in ALLOWED_EXECUTABLES
}

private fun containsShellMetacharacters(s: String): Boolean = s.any { it in SHELL_METACHARACTERS }

private fun hasDangerousFlags(args: List<String>): Boolean = args.any { it in BLOCKED_FLAGS || (it.startsWith("--") && it in BLOCKED_FLAGS) }

private fun hasPathTraversal(args: List<String>): Boolean = args.any { it.contains("..") }

// Grace for the readers to hit EOF after the shell exits.
private const val STREAM_DRAIN_GRACE_SECONDS = 2L

private val streamReaders = Executors.newCachedThreadPool { runnable ->
    Thread(runnable, "kai-shell-reader").apply { isDaemon = true }
}

private class StreamCollector(stream: InputStream) {
    private val sb = StringBuilder()
    private val done: CompletableFuture<Unit> = CompletableFuture.runAsync({
        val reader = stream.bufferedReader()
        val buf = CharArray(8192)
        var read: Int
        while (reader.read(buf).also { read = it } != -1) {
            synchronized(sb) {
                if (sb.length < MAX_OUTPUT_LENGTH) sb.append(buf, 0, read)
            }
        }
    }, streamReaders).thenApply { }

    fun awaitEof(seconds: Long): Boolean = try {
        done.get(seconds, TimeUnit.SECONDS)
        true
    } catch (_: TimeoutException) {
        false
    } catch (_: Exception) {
        true
    }

    fun text(): String = synchronized(sb) { sb.toString() }.smartTruncate(MAX_OUTPUT_LENGTH)
}

private fun destroyTree(process: Process) {
    process.descendants().forEach { it.destroyForcibly() }
    process.destroyForcibly()
}

private fun buildDescription(): String {
    val osName = System.getProperty("os.name", "").lowercase()
    val platform = when {
        "mac" in osName || "darwin" in osName -> "macOS"
        "win" in osName -> "Windows"
        else -> "Linux"
    }
    return """Execute a shell command on the host machine ($platform) and return stdout, stderr, and exit code.
Commands are executed directly (no shell interpretation). Pipes, redirections, and chaining are not supported.
Output is limited to ${MAX_OUTPUT_LENGTH} characters per stream.
Default timeout: ${DEFAULT_TIMEOUT_SECONDS}s, max: ${MAX_TIMEOUT_SECONDS}s.
Allowed executables: ${ALLOWED_EXECUTABLES.sorted().joinToString(", ")}.
Set background=true to run long-lived processes (servers, builds)."""
}

object ShellCommandTool : Tool {
    override val timeout: Duration = (MAX_TIMEOUT_SECONDS + 10).seconds

    override val schema = ToolSchema(
        name = "execute_shell_command",
        description = buildDescription(),
        parameters = mapOf(
            "command" to ParameterSchema("string", "The command to execute (executable + arguments, space-separated)", true),
            "timeout" to ParameterSchema("integer", "Timeout in seconds (default $DEFAULT_TIMEOUT_SECONDS, max $MAX_TIMEOUT_SECONDS)", false),
            "working_dir" to ParameterSchema("string", "Working directory for the command (must be within project root)", false),
            "env" to ParameterSchema("object", "Environment variables to set (key-value pairs). Cannot override protected vars.", false),
            "background" to ParameterSchema("boolean", "Run in background and return immediately with a session_id.", false),
        ),
    )

    @Suppress("UNCHECKED_CAST")
    override suspend fun execute(args: Map<String, Any>): Any {
        val command = args["command"] as? String
            ?: return mapOf("success" to false, "error" to "Command is required")

        // Parse command into executable + args (no shell interpretation)
        val parts = command.trim().split("\\s+".toRegex())
        if (parts.isEmpty() || parts[0].isEmpty()) {
            return mapOf("success" to false, "error" to "Empty command")
        }

        val executable = parts[0]
        val cmdArgs = parts.drop(1)

        // Fail-closed: check executable against allowlist
        if (!isAllowedExecutable(executable)) {
            return mapOf("success" to false, "error" to "Executable '$executable' is not in the allowed list")
        }

        // Reject shell metacharacters in any argument
        if (parts.any { containsShellMetacharacters(it) }) {
            return mapOf("success" to false, "error" to "Shell metacharacters are not allowed")
        }

        // Reject dangerous flags
        if (hasDangerousFlags(cmdArgs)) {
            return mapOf("success" to false, "error" to "Dangerous flags are not allowed")
        }

        // Reject path traversal
        if (hasPathTraversal(cmdArgs)) {
            return mapOf("success" to false, "error" to "Path traversal (..) is not allowed")
        }

        val timeoutSeconds = ((args["timeout"] as? Number)?.toLong() ?: DEFAULT_TIMEOUT_SECONDS)
            .coerceIn(1, MAX_TIMEOUT_SECONDS)

        val workingDir = (args["working_dir"] as? String)?.let { File(it) }

        // Validate working directory is within project root
        if (workingDir != null) {
            if (!workingDir.isDirectory) {
                return mapOf("success" to false, "error" to "Working directory does not exist or is not a directory")
            }
            val canonicalWorking = workingDir.canonicalPath
            val projectRoot = System.getProperty("user.dir")
            if (!canonicalWorking.startsWith(projectRoot)) {
                return mapOf("success" to false, "error" to "Working directory must be within project root")
            }
        }

        val envMap = (args["env"] as? Map<String, Any>)
            ?.mapValues { it.value.toString() }
            ?.filterKeys { it.uppercase() !in BLOCKED_ENV_VARS }
            ?.filter { (k, v) -> !containsShellMetacharacters(v) }
            ?: emptyMap()

        val background = args["background"] as? Boolean ?: false
        if (background) {
            return ProcessManagerTool.processManager.startBackground(
                parts.joinToString(" "),
                timeoutSeconds,
                workingDir,
                envMap,
            )
        }

        return try {
            runInterruptible(Dispatchers.IO) { runForeground(parts, timeoutSeconds, workingDir, envMap) }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            mapOf(
                "success" to false,
                "error" to (e.message ?: "Failed to execute command"),
            )
        }
    }

    private fun runForeground(parts: List<String>, timeoutSeconds: Long, workingDir: File?, envMap: Map<String, String>): Map<String, Any> {
        val processBuilder = ProcessBuilder(parts)
        processBuilder.redirectErrorStream(false)
        if (workingDir != null && workingDir.isDirectory) {
            processBuilder.directory(workingDir)
        }
        if (envMap.isNotEmpty()) {
            processBuilder.environment().putAll(envMap)
        }

        val process = processBuilder.start()
        try {
            val stdout = StreamCollector(process.inputStream)
            val stderr = StreamCollector(process.errorStream)

            val completed = process.waitFor(timeoutSeconds, TimeUnit.SECONDS)
            if (!completed) destroyTree(process)

            val drained = stdout.awaitEof(STREAM_DRAIN_GRACE_SECONDS) and
                stderr.awaitEof(STREAM_DRAIN_GRACE_SECONDS)
            val stderrText = if (completed && !drained) {
                stderr.text() + "\n[Output may be incomplete: a background process is still holding the shell's output open. " +
                    "Use background=true for long-running processes.]"
            } else {
                stderr.text()
            }

            if (!completed) {
                return mapOf(
                    "success" to false,
                    "stdout" to stdout.text(),
                    "stderr" to stderrText,
                    "exit_code" to -1,
                    "timed_out" to true,
                )
            }

            val exitCode = process.exitValue()
            return mapOf(
                "success" to (exitCode == 0),
                "stdout" to stdout.text(),
                "stderr" to stderrText,
                "exit_code" to exitCode,
                "timed_out" to false,
            )
        } finally {
            if (process.isAlive) destroyTree(process)
        }
    }

    val toolInfo = ToolInfo(
        id = "execute_shell_command",
        name = "Execute Shell Command",
        description = "Execute a shell command on the device",
        nameRes = Res.string.tool_execute_shell_command_name,
        descriptionRes = Res.string.tool_execute_shell_command_description,
        isEnabled = false,
    )
}
