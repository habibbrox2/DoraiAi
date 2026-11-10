package com.dorai.ai

import android.content.Context
import com.dorai.ai.linux.LinuxDistro
import com.dorai.ai.linux.LinuxInstalls
import com.dorai.ai.linux.LinuxPaths
import com.dorai.ai.sandbox.LinuxSandboxManager
import com.dorai.ai.workspace.WorkspaceGitDiff
import com.dorai.ai.workspace.WorkspaceGitResult
import com.dorai.ai.workspace.WorkspaceState
import com.dorai.ai.workspace.runtime.WorkspaceEnvironmentManager
import com.dorai.ai.workspace.runtime.WorkspaceFileBrowser
import kotlinx.coroutines.flow.StateFlow
import org.koin.java.KoinJavaComponent.inject

actual fun createWorkspaceController(): WorkspaceController = AndroidWorkspaceController()

class AndroidWorkspaceController : WorkspaceController {

    private val context: Context by inject(Context::class.java)
    private val sandboxManager: LinuxSandboxManager by inject(LinuxSandboxManager::class.java)

    private val installs by lazy { LinuxInstalls(context) }

    /**
     * Workspace is always Debian: its coding agents are vendor scripts that
     * expect glibc and apt. Debian has one directory on the device, so this is
     * the same install the chat sandbox uses whenever the shell integration is
     * pointed at Debian — a Linux set up from either surface is immediately
     * there for the other. Pointing the shell integration at Alpine instead
     * leaves this install alone; the two simply coexist.
     */
    private val paths: LinuxPaths by lazy { installs.pathsFor(LinuxDistro.DEBIAN) }

    private val manager: WorkspaceEnvironmentManager by lazy {
        WorkspaceEnvironmentManager(paths).also { built ->
            // Both directions are checked at fire time, not wired once: the chat
            // sandbox can be pointed at this install and away from it while the
            // app runs.
            sandboxManager.onBeforeReset = {
                // A sandbox reset deletes the rootfs; sessions holding file
                // descriptors into it have to go first.
                if (sandboxManager.rootDir == paths.root) built.onEnvironmentRemoved()
            }
            // And the other direction: installing Debian from Workspace's setup
            // screen is what gives a Debian-pointed chat sandbox its Linux.
            built.onEnvironmentChanged = { sandboxManager.refreshInstallState() }
        }
    }

    override val state: StateFlow<WorkspaceState> get() = manager.state
    override val files: FileBrowserSource by lazy { WorkspaceFileBrowser(context, paths) }

    override fun install(agentIds: Set<String>) = manager.install(agentIds)
    override fun cancel() = manager.cancel()
    override fun uninstall() = manager.uninstall()
    override fun refresh() = manager.refresh()
    override suspend fun gitStatus(project: String): WorkspaceGitResult = manager.gitStatus(project)
    override suspend fun gitClone(project: String, url: String): WorkspaceGitResult = manager.gitClone(project, url)
    override suspend fun gitLog(project: String, maxCount: Int): List<com.dorai.ai.workspace.WorkspaceGitLogEntry> = manager.gitLog(project, maxCount)
    override suspend fun gitFetch(project: String): WorkspaceGitResult = manager.gitFetch(project)
    override suspend fun gitPullFfOnly(project: String): WorkspaceGitResult = manager.gitPullFfOnly(project)
    override suspend fun gitDiff(project: String, path: String): WorkspaceGitDiff = manager.gitDiff(project, path)
    override suspend fun gitStage(project: String, path: String): Boolean = manager.gitStage(project, path)
    override suspend fun gitUnstage(project: String, path: String): Boolean = manager.gitUnstage(project, path)
    override suspend fun gitDiscard(project: String, path: String): Boolean = manager.gitDiscard(project, path)
    override fun createProject(name: String): String? = manager.createProject(name)
    override fun deleteProject(name: String) = manager.deleteProject(name)
    override fun renameProject(name: String, newName: String): String? = manager.renameProject(name, newName)
    override fun startSession(project: String, agentId: String?) = manager.startSession(project, agentId)
    override fun selectSession(id: String) = manager.selectSession(id)
    override fun closeSession(id: String) = manager.closeSession(id)
    override fun resumeProject(project: String): Boolean = manager.resumeProject(project)
    override fun leaveProject(project: String) = manager.leaveProject(project)
    override fun writeToTerminal(text: String) = manager.writeToTerminal(text)
    override fun resizeTerminal(columns: Int, rows: Int) = manager.resizeTerminal(columns, rows)
    override fun close() = manager.close()
}
