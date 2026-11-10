package com.dorai.ai.ui.dashboard

import androidx.compose.runtime.Immutable
import com.dorai.ai.SandboxStatus
import com.dorai.ai.data.Service
import com.dorai.ai.ui.chat.ChatUiState
import com.dorai.ai.ui.chat.ConversationSummary
import com.dorai.ai.ui.components.OperationalStatus
import com.dorai.ai.workspace.WorkspaceState
import dorai.composeapp.generated.resources.Res
import dorai.composeapp.generated.resources.dash_projects_count
import dorai.composeapp.generated.resources.dash_sandbox_fact
import dorai.composeapp.generated.resources.dash_workspace_fact
import dorai.composeapp.generated.resources.status_installing
import dorai.composeapp.generated.resources.status_needs_attention
import dorai.composeapp.generated.resources.status_not_set_up
import dorai.composeapp.generated.resources.status_ready
import dorai.composeapp.generated.resources.status_starting
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import org.jetbrains.compose.resources.StringResource

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
sealed interface FactText {
    /** Localized via Compose Resources — used by the production mapper. */
    @Immutable
    data class Localized(val resource: StringResource) : FactText

    /** [Localized] with a single Int format parameter — e.g. project counts. */
    @Immutable
    data class LocalizedWithParam(val resource: StringResource, val param: Int) : FactText

    /** Raw literal — screenshot fixtures only; never emitted by production code. */
    @Immutable
    data class Raw(val text: String) : FactText
}

/** One operational fact about sandbox or Workspace readiness. */
@Immutable
data class ReadinessFact(
    val label: FactText,
    val value: FactText,
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
    val isCheckingServices: Boolean = false,
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

    val isCheckingServices = chatUiState.isRestoring || (chatUiState.availableServices.isEmpty() && chatUiState.isLoading)

    return DashboardUiState(
        service = service,
        isCheckingServices = isCheckingServices,
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
    error -> ReadinessFact(FactText.Localized(Res.string.dash_sandbox_fact), FactText.Localized(Res.string.status_needs_attention), OperationalStatus.Error)
    working -> ReadinessFact(FactText.Localized(Res.string.dash_sandbox_fact), FactText.Localized(Res.string.status_installing), OperationalStatus.Installing)
    ready -> ReadinessFact(FactText.Localized(Res.string.dash_sandbox_fact), FactText.Localized(Res.string.status_ready), OperationalStatus.Ready)
    installed -> ReadinessFact(FactText.Localized(Res.string.dash_sandbox_fact), FactText.Localized(Res.string.status_starting), OperationalStatus.Connecting)
    else -> ReadinessFact(FactText.Localized(Res.string.dash_sandbox_fact), FactText.Localized(Res.string.status_not_set_up), OperationalStatus.NeedsSetup)
}

private fun WorkspaceState.toReadinessFact(): ReadinessFact = when {
    isReady && projects.isEmpty() -> ReadinessFact(FactText.Localized(Res.string.dash_workspace_fact), FactText.Localized(Res.string.status_ready), OperationalStatus.Ready)
    isReady -> ReadinessFact(FactText.Localized(Res.string.dash_workspace_fact), FactText.LocalizedWithParam(Res.string.dash_projects_count, projects.size), OperationalStatus.Ready)
    isInstalling -> ReadinessFact(FactText.Localized(Res.string.dash_workspace_fact), FactText.Localized(Res.string.status_installing), OperationalStatus.Installing)
    lastError != null -> ReadinessFact(FactText.Localized(Res.string.dash_workspace_fact), FactText.Localized(Res.string.status_needs_attention), OperationalStatus.Error)
    else -> ReadinessFact(FactText.Localized(Res.string.dash_workspace_fact), FactText.Localized(Res.string.status_not_set_up), OperationalStatus.NeedsSetup)
}
