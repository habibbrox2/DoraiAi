package com.dorai.ai.github

import java.io.File

/**
 * Locates a usable `git` executable on the desktop host.
 *
 * Searches PATH, then common Windows install locations, and verifies the
 * binary reports `--version` with major ≥ 2.30. The result is cached per
 * process.
 */
class GitExecutableLocator {

    private companion object {
        const val MIN_MAJOR = 2
        const val MIN_MINOR = 30
    }

    /** Common Windows install locations for git.exe. */
    private val windowsInstallPaths = listOf(
        "C:/Program Files/Git/cmd/git.exe",
        "C:/Program Files (x86)/Git/cmd/git.exe",
        "C:/Program Files/Git/bin/git.exe",
        "C:/Program Files (x86)/Git/bin/git.exe",
    )

    @Volatile
    private var cachedPath: String? = null

    /**
     * Returns the path to a usable `git` executable, or null when none is found.
     * Result is cached after the first successful resolution.
     */
    fun locate(): String? = cachedPath ?: runCaching {
        val path = findGit()
        if (path != null && isVersionAtLeast(path)) path else null
    }

    /** Clears the cache, forcing re-resolution on the next [locate] call. */
    fun resetCache() {
        cachedPath = null
    }

    /** For testing: expose version check on a specific path. */
    fun versionOf(gitPath: String): GitVersion? = parseGitVersion(gitPath)

    private fun runCaching(block: () -> String?): String? {
        val result = block()
        if (result != null) cachedPath = result
        return result
    }

    private fun findGit(): String? {
        // 1. PATH lookup.
        val pathEnv = System.getenv("PATH") ?: return null
        val pathSep = if (isWindows()) ";" else ":"
        for (entry in pathEnv.split(pathSep)) {
            val candidate = File(entry.trim(), if (isWindows()) "git.exe" else "git")
            if (candidate.isFile && candidate.canExecute()) return candidate.absolutePath
        }

        // 2. Common Windows install locations.
        if (isWindows()) {
            for (candidate in windowsInstallPaths) {
                val file = File(candidate)
                if (file.isFile && file.canExecute()) return candidate
            }
        }

        return null
    }

    private fun isVersionAtLeast(gitPath: String): Boolean {
        val version = parseGitVersion(gitPath) ?: return false
        return if (version.major > MIN_MAJOR) {
            true
        } else if (version.major == MIN_MAJOR) {
            version.minor >= MIN_MINOR
        } else {
            false
        }
    }

    private fun parseGitVersion(gitPath: String): GitVersion? = runCatching {
        val process = ProcessBuilder(listOf(gitPath, "--version"))
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader(Charsets.UTF_8).readText()
        process.waitFor()
        output
    }.getOrNull()?.let { parseVersionString(it) }

    private fun parseVersionString(output: String): GitVersion? {
        // "git version 2.47.1" or "git version 2.30.1.windows.1"
        val match = Regex("""git version (\d+)\.(\d+)""").find(output)
            ?: return null
        return GitVersion(
            major = match.groupValues[1].toInt(),
            minor = match.groupValues[2].toInt(),
        )
    }

    private fun isWindows(): Boolean = System.getProperty("os.name").lowercase().contains("win")
}

/** Parsed git version. */
data class GitVersion(val major: Int, val minor: Int)
