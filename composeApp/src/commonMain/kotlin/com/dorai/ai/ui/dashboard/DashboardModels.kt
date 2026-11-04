package com.dorai.ai.ui.dashboard

import androidx.compose.runtime.Immutable
import com.dorai.ai.SandboxStatus
import com.dorai.ai.data.Service
import com.dorai.ai.ui.chat.ChatUiState
import com.dorai.ai.ui.chat.ConversationSummary
import com.dorai.ai.ui.components.OperationalStatus
import com.dorai.ai.workspace.WorkspaceState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

/**
 * Design destination contract (Plan 6, Phase 1). Design has no dedicated
 * workspace yet, so its dashboard card opens a new chat prefilled with this
 * starter prompt — it must never claim a separate editor exists.
 */
const val DESIGN_STARTER_PROMPT =
    "Help me design a new interface. Ask about the goal, audience, and style first, " +
        "then propose a layout and visual direction step by step."

/** Selected AI service shown in the dashboard readiness card. */
@Immutable
data class ServiceReadiness(
    val serviceName: String,
    val modelId: String,
    val isSharedKey: Boolean,
)

/** One operational fact about sandbox or Workspace readiness. */
@Immutable
data class ReadinessFact(
    val label: String,
    val value: String,
    val status: OperationalStatus,
)

/**
 * Read-only aggregation of existing feature summaries (Plan 6, Phase 1).
 * Display-safe by design: no prompt content, credentials, terminal output, or
 * mutable service configuration lives here.
 */
@Immutable
data class DashboardUiState(
    val service: ServiceReadiness? = null,
    val facts: ImmutableList<ReadinessFact> = persistentListOf(),
    val recentConversations: ImmutableList<ConversationSummary> = persistentListOf(),
    val recentProjects: ImmutableList<String> = persistentListOf(),
    val isBuildAvailable: Boolean = false,
    val isSandboxAvailable: Boolean = false,
    val isDesignAvailable: Boolean = false,
) {
    val hasSavedWork: Boolean
        get() = recentConversations.isNotEmpty() || recentProjects.isNotEmpty()
}

/**
 * Pure mapper from existing feature state into dashboard summaries. No I/O and
 * no business logic — chat history stays in ChatViewModel, project lifecycle in
 * WorkspaceController, and sandbox state in SandboxController.
 */
fun buildDashboardUiState(
    chatUiState: ChatUiState,
    sandboxStatus: SandboxStatus,
    buildState: WorkspaceState,
    isBuildAvailable: Boolean,
    isSandboxAvailable: Boolean,
    isDesignAvailable: Boolean,
): DashboardUiState {
    val primary = chatUiState.availableServices.firstOrNull()
    val service = primary?.let {
        ServiceReadiness(
            serviceName = it.serviceName,
            modelId = it.modelId,
            isSharedKey = it.serviceId == Service.Free.id,
        )
    }

    val facts = buildList {
        if (isSandboxAvailable) add(sandboxStatus.toReadinessFact())
        if (isBuildAvailable) add(buildState.toReadinessFact())
    }.toImmutableList()

    return DashboardUiState(
        service = service,
        facts = facts,
        recentConversations = chatUiState.savedConversations
            .filter { !it.isHeartbeat }
            .sortedByDescending { it.updatedAt }
            .toImmutableList(),
        recentProjects = buildState.projects.toList().toImmutableList(),
        isBuildAvailable = isBuildAvailable,
        isSandboxAvailable = isSandboxAvailable,
        isDesignAvailable = isDesignAvailable,
    )
}

private fun SandboxStatus.toReadinessFact(): ReadinessFact = when {
    error -> ReadinessFact("Sandbox", "Needs attention", OperationalStatus.Error)
    working -> ReadinessFact("Sandbox", "Installing", OperationalStatus.Installing)
    ready -> ReadinessFact("Sandbox", "Ready", OperationalStatus.Ready)
    installed -> ReadinessFact("Sandbox", "Starting", OperationalStatus.Connecting)
    else -> ReadinessFact("Sandbox", "Not set up", OperationalStatus.NeedsSetup)
}

private fun WorkspaceState.toReadinessFact(): ReadinessFact = when {
    isReady && projects.isEmpty() -> ReadinessFact("Workspace", "Ready", OperationalStatus.Ready)
    isReady -> ReadinessFact("Workspace", "${projects.size} project(s)", OperationalStatus.Ready)
    isInstalling -> ReadinessFact("Workspace", "Installing", OperationalStatus.Installing)
    lastError != null -> ReadinessFact("Workspace", "Needs attention", OperationalStatus.Error)
    else -> ReadinessFact("Workspace", "Not set up", OperationalStatus.NeedsSetup)
}
