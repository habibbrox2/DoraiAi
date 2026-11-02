package com.inspiredandroid.kai.tools

import com.inspiredandroid.kai.data.AppSettings
import com.inspiredandroid.kai.data.EmailStore
import com.inspiredandroid.kai.data.MemoryStore
import com.inspiredandroid.kai.data.TaskStore
import com.inspiredandroid.kai.filereview.FileReviewController
import com.inspiredandroid.kai.mcp.McpServerManager
import com.inspiredandroid.kai.network.tools.Tool

/**
 * The tool set every platform builds, in the order the agent receives it.
 *
 * Each platform's `getAvailableTools()` used to repeat this gating block verbatim, which is how
 * they drifted — Android in particular had re-implemented the common-tool checks inline instead
 * of calling [CommonTools.getCommonTools]. Platform-specific tools are contributed through
 * [platformExtras]; MCP tools stay last everywhere.
 */
fun buildAgentToolSet(
    appSettings: AppSettings,
    memoryStore: MemoryStore,
    taskStore: TaskStore,
    mcpServerManager: McpServerManager,
    // Web has no email support and injects no store.
    emailStore: EmailStore? = null,
    fileReviewController: FileReviewController? = null,
    platformExtras: MutableList<Tool>.() -> Unit = {},
): List<Tool> = buildList {
    addAll(CommonTools.getCommonTools(appSettings))

    if (appSettings.isMemoryEnabled()) {
        addAll(CommonTools.getMemoryTools(memoryStore))
    }

    // Heartbeat tools ride the scheduling switch — they are only reachable from a scheduled run.
    if (appSettings.isSchedulingEnabled()) {
        addAll(SchedulingTools.getSchedulingTools(taskStore))
        addAll(HeartbeatTools.getHeartbeatTools(memoryStore, appSettings))
    }

    if (emailStore != null && appSettings.isEmailEnabled()) {
        addAll(EmailTools.getEmailTools(emailStore))
    }

    // File Review tools are exposed only when a platform supplies a real, configured controller.
    fileReviewController?.let { addAll(FileReviewTools.getTools(it)) }

    platformExtras()

    // MCP server management tools let the chat agent add/remove/toggle/connect servers
    // on behalf of the user, in addition to the per-server MCP tools below.
    addAll(McpManagementTools.getMcpManagementTools(mcpServerManager))

    addAll(mcpServerManager.getEnabledMcpTools(reservedNames = mapTo(HashSet()) { it.schema.name }))
}
