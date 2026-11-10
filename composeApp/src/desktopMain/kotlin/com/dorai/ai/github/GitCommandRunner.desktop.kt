package com.dorai.ai.github

import com.dorai.ai.getAppFilesDirectory
import java.io.File
import java.io.InputStream
import java.util.concurrent.TimeUnit

private const val PROJECTS_ROOT_NAME = ".dorai/projects"
private val NETWORK_COMMANDS = setOf("clone", "fetch", "pull", "push")

/**
 * Desktop implementation of [GitCommandRunner]. Spawns `git` directly via
 * [ProcessBuilder] (no shell), validates arguments through [GitCommandPolicy],
 * and configures an ephemeral credential helper via [GitCredentialBroker] for
 * network commands so the token never reaches argv or env.
 */
class DesktopGitCommandRunner(
    private val locator: GitExecutableLocator,
    private val broker: GitCredentialBroker,
) : GitCommandRunner {

    private val projectsRoot: File
        get() = File(getAppFilesDirectory(), PROJECTS_ROOT_NAME).apply { mkdirs() }

    override suspend fun run(
        project: String,
        args: List<String>,
        timeoutSeconds: Long,
        writeEnabled: Boolean,
    ): GitCommandResult {
        if (!GitCommandPolicy.isAllowedGitArgs(args, writeEnabled)) {
            return GitCommandResult(false, "", "Rejected by GitCommandPolicy: ${args.joinToString(" ")}", -1)
        }

        val gitPath = locator.locate() ?: return GitCommandResult(false, "", "git not found", -1)
        val projectDir = File(projectsRoot, project)
        if (!projectDir.isDirectory) {
            return GitCommandResult(false, "", "Project directory not found: $project", -1)
        }

        val subcommand = subcommandOf(args)
        val ctx = if (subcommand in NETWORK_COMMANDS) {
            broker.credentialGitConfigArgs(args)
        } else {
            null
        }

        return try {
            val allArgs = (if (ctx != null) ctx.gitArgs else emptyList()) + args
            runGitProcess(gitPath, allArgs, projectDir, timeoutSeconds)
        } finally {
            ctx?.release()
        }
    }

    override suspend fun isGitAvailable(): Boolean = locator.locate() != null

    private fun runGitProcess(
        gitPath: String,
        args: List<String>,
        workDir: File,
        timeoutSeconds: Long,
    ): GitCommandResult {
        val processBuilder = ProcessBuilder(listOf(gitPath) + args)
            .directory(workDir)
        restrictEnvironment(processBuilder.environment())

        val process = processBuilder.start()
        val stdout = readFully(process.inputStream)
        val stderr = readFully(process.errorStream)

        val completed = process.waitFor(timeoutSeconds, TimeUnit.SECONDS)
        val timedOut = !completed
        if (timedOut) {
            process.destroyForcibly()
        }
        val exitCode = if (timedOut) -1 else process.exitValue()

        return GitCommandResult(
            success = !timedOut && exitCode == 0,
            stdout = stdout,
            stderr = stderr,
            exitCode = exitCode,
            timedOut = timedOut,
        )
    }

    /**
     * Restricts the environment to a minimal safe set so no credential or
     * secret leaks into the git process.
     */
    private fun restrictEnvironment(env: MutableMap<String, String>) {
        env.keys.toList().forEach { env.remove(it) }
        env["PATH"] = System.getenv("PATH") ?: ""
        env["HOME"] = System.getProperty("user.home") ?: ""
        env["LANG"] = "C.UTF-8"
        if (System.getProperty("os.name").lowercase().contains("win")) {
            env["SystemRoot"] = System.getenv("SystemRoot") ?: ""
        }
    }

    private fun readFully(stream: InputStream): String = stream.bufferedReader().use { it.readText() }
}

/**
 * Extracts the git subcommand from args (after stripping a leading `git` token
 * and global options like `-C <dir>`).
 */
private fun subcommandOf(args: List<String>): String {
    val tokens = if (args.isNotEmpty() && GitCommandPolicy.isGitExecutable(args.first())) args.drop(1) else args
    var index = 0
    while (index < tokens.size) {
        when (val token = tokens[index]) {
            "-C" -> index += 2

            "--no-pager" -> index += 1

            else -> {
                if (!token.startsWith("-")) return token
                return ""
            }
        }
    }
    return ""
}

actual fun createGitCommandRunner(): GitCommandRunner = DesktopGitCommandRunner(GitExecutableLocator(), createGitCredentialBroker())
