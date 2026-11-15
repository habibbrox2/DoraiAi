package com.dorai.ai.workspace

import com.dorai.ai.NoOpWorkspaceController
import com.dorai.ai.WorkspaceController
import com.dorai.ai.getAppFilesDirectory
import com.dorai.ai.github.GitCommandRunner
import com.dorai.ai.github.GitUrlValidator
import com.dorai.ai.github.createGitCommandRunner
import java.io.File

class DesktopWorkspaceController(
    private val gitRunner: GitCommandRunner = createGitCommandRunner(),
) : WorkspaceController by NoOpWorkspaceController() {

    private val projectsRoot: File
        get() = File(getAppFilesDirectory(), ".dorai/projects").apply { mkdirs() }

    override suspend fun gitStatus(project: String): WorkspaceGitResult {
        if (!GitUrlValidator.isValidProjectName(project)) {
            return WorkspaceGitResult(error = WorkspaceGitError.NotRepository, isRepository = false)
        }
        val projectDir = File(projectsRoot, project)
        if (!projectDir.isDirectory) {
            return WorkspaceGitResult(error = WorkspaceGitError.NotRepository, isRepository = false)
        }

        val res = gitRunner.run(project, listOf("status", "--porcelain=v1", "-z", "-b"))
        if (!res.success) {
            val error = if (res.stderr.contains("not a git repository", ignoreCase = true)) {
                WorkspaceGitError.NotRepository
            } else {
                WorkspaceGitError.Failed
            }
            return WorkspaceGitResult(error = error, isRepository = error != WorkspaceGitError.NotRepository)
        }

        return parseGitStatusPorcelain(res.stdout, projectDir.absolutePath)
    }

    override suspend fun gitClone(project: String, url: String): WorkspaceGitResult {
        if (!GitUrlValidator.isValidProjectName(project)) {
            return WorkspaceGitResult(error = WorkspaceGitError.NotRepository, isRepository = false)
        }
        if (!GitUrlValidator.isValidGitHubUrl(url)) {
            return WorkspaceGitResult(error = WorkspaceGitError.NotRepository, isRepository = false)
        }

        val res = gitRunner.run(project, listOf("clone", "--depth=1", url))
        if (!res.success) {
            return WorkspaceGitResult(error = WorkspaceGitError.Failed, isRepository = false)
        }

        return gitStatus(project)
    }

    override suspend fun gitLog(project: String, maxCount: Int): List<WorkspaceGitLogEntry> {
        if (!GitUrlValidator.isValidProjectName(project)) return emptyList()
        val clampedCount = maxCount.coerceIn(1, 100)
        val res = gitRunner.run(project, listOf("log", "--oneline", "-n", "$clampedCount"))
        if (!res.success) return emptyList()
        return parseGitLogOneline(res.stdout)
    }

    override suspend fun gitFetch(project: String): WorkspaceGitResult {
        if (!GitUrlValidator.isValidProjectName(project)) {
            return WorkspaceGitResult(error = WorkspaceGitError.NotRepository, isRepository = false)
        }
        val res = gitRunner.run(project, listOf("fetch", "--quiet"))
        if (!res.success) {
            return WorkspaceGitResult(error = WorkspaceGitError.Failed, isRepository = true)
        }
        return gitStatus(project)
    }

    override suspend fun gitPullFfOnly(project: String): WorkspaceGitResult {
        if (!GitUrlValidator.isValidProjectName(project)) {
            return WorkspaceGitResult(error = WorkspaceGitError.NotRepository, isRepository = false)
        }
        val res = gitRunner.run(project, listOf("pull", "--ff-only", "--quiet"))
        if (!res.success) {
            return WorkspaceGitResult(error = WorkspaceGitError.Failed, isRepository = true)
        }
        return gitStatus(project)
    }

    override suspend fun gitCommit(project: String, files: List<String>, message: String): WorkspaceGitResult {
        if (!GitUrlValidator.isValidProjectName(project) || files.isEmpty() || message.isBlank()) {
            return WorkspaceGitResult(error = WorkspaceGitError.Failed, isRepository = true)
        }

        val addArgs = listOf("add", "--") + files
        val addRes = gitRunner.run(project, addArgs, writeEnabled = true)
        if (!addRes.success) {
            return WorkspaceGitResult(error = WorkspaceGitError.Failed, isRepository = true)
        }

        val commitRes = gitRunner.run(project, listOf("commit", "-m", message), writeEnabled = true)
        if (!commitRes.success) {
            return WorkspaceGitResult(error = WorkspaceGitError.Failed, isRepository = true)
        }

        return gitStatus(project)
    }

    override suspend fun gitPush(project: String): WorkspaceGitResult {
        if (!GitUrlValidator.isValidProjectName(project)) {
            return WorkspaceGitResult(error = WorkspaceGitError.NotRepository, isRepository = false)
        }
        val res = gitRunner.run(project, listOf("push", "origin", "HEAD"), writeEnabled = true)
        if (!res.success) {
            return WorkspaceGitResult(error = WorkspaceGitError.Failed, isRepository = true)
        }
        return gitStatus(project)
    }
}
