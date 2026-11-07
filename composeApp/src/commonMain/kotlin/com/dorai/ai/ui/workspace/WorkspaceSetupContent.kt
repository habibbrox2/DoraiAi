package com.dorai.ai.ui.workspace

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dorai.ai.ui.DoraiTypography
import com.dorai.ai.ui.handCursor
import com.dorai.ai.ui.sandbox.SandboxProgressRow
import com.dorai.ai.ui.settings.SettingsCard
import com.dorai.ai.ui.theme.LocalDesignColorTokens
import com.dorai.ai.workspace.WorkspaceAgents
import com.dorai.ai.workspace.WorkspaceEnvironmentState
import com.dorai.ai.workspace.WorkspaceState
import com.dorai.ai.workspace.WorkspaceStep
import dorai.composeapp.generated.resources.Res
import dorai.composeapp.generated.resources.workspace_explorer_retry
import dorai.composeapp.generated.resources.workspace_setup_agents_description
import dorai.composeapp.generated.resources.workspace_setup_agents_title
import dorai.composeapp.generated.resources.workspace_setup_failed_title
import dorai.composeapp.generated.resources.workspace_setup_install
import dorai.composeapp.generated.resources.workspace_setup_verify
import dorai.composeapp.generated.resources.workspace_step_base_packages
import dorai.composeapp.generated.resources.workspace_step_configure
import dorai.composeapp.generated.resources.workspace_step_download
import dorai.composeapp.generated.resources.workspace_step_extract
import dorai.composeapp.generated.resources.workspace_step_install_agent
import kotlinx.collections.immutable.ImmutableSet
import org.jetbrains.compose.resources.stringResource

/**
 * First screen a user lands on: install Debian, optionally with coding agents.
 * Doubles as the progress surface for later agent installs.
 *
 * Phase 4 (Plan 3) turns this into a step-based setup surface: an environment
 * status header, a numbered step guide, install progress with cancellation, and
 * an actionable retry path when setup or an agent install fails.
 */
@Composable
internal fun WorkspaceSetupContent(
    state: WorkspaceState,
    selectedAgents: ImmutableSet<String>,
    onToggleAgent: (String) -> Unit,
    onInstall: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val installing = state.environment as? WorkspaceEnvironmentState.Installing
    val hasError = state.lastError != null
    // Derive the active step purely from existing UI state: never invent progress.
    val activeStep = when {
        installing != null -> SetupStep.Install
        hasError -> SetupStep.Recover
        else -> SetupStep.Options
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val tier = WorkspaceTier(maxWidth)
        val centered = if (tier == WorkspaceTier.Expanded) {
            Modifier.widthIn(tier.contentMaxWidth).align(Alignment.TopCenter)
        } else {
            Modifier
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(centered)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BuildEnvironmentHeader(state = state)

            WorkspaceStepGuide(activeStep = activeStep)

            SettingsCard(modifier = Modifier.fillMaxWidth()) {
                WorkspaceSectionHeader(
                    title = stringResource(Res.string.workspace_setup_agents_title),
                    subtitle = stringResource(Res.string.workspace_setup_agents_description),
                )
                WorkspaceAgents.all.forEach { agent ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = installing == null && agent.id !in state.installedAgents) { onToggleAgent(agent.id) }
                            .handCursor(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = agent.id in selectedAgents || agent.id in state.installedAgents,
                            onCheckedChange = { if (agent.id !in state.installedAgents) onToggleAgent(agent.id) },
                            enabled = installing == null && agent.id !in state.installedAgents,
                        )
                        Text(
                            text = agent.title,
                            style = DoraiTypography.bodyLarge,
                            color = LocalDesignColorTokens.current.textPrimary,
                        )
                    }
                }
            }

            if (installing != null) {
                SandboxProgressRow(
                    progress = installing.progress,
                    statusText = stepLabel(installing),
                    onCancel = onCancel,
                )
            } else {
                Button(onClick = onInstall, modifier = Modifier.handCursor()) {
                    Text(stringResource(Res.string.workspace_setup_install))
                }
            }

            // Actionable recovery: name the failure and offer a retry that reuses the
            // existing install action rather than leaving a dead-end error string.
            state.lastError?.let { error ->
                SetupErrorCard(
                    message = error,
                    canRetry = installing == null,
                    onRetry = onInstall,
                )
            }
        }
    }
}

/** The setup flow as explicit steps so a new user always knows where they are. */
private enum class SetupStep { Options, Install, Recover }

/**
 * Numbered guide for the setup flow (Plan 3, Phase 4). Marks the current step
 * semantically so screen readers announce progress through setup.
 */
@Composable
private fun WorkspaceStepGuide(activeStep: SetupStep) {
    val steps = listOf(
        SetupStep.Options to stringResource(Res.string.workspace_setup_agents_title),
        SetupStep.Install to stringResource(Res.string.workspace_setup_install),
        SetupStep.Recover to stringResource(Res.string.workspace_setup_verify),
    )
    val activeIndex = steps.indexOfFirst { it.first == activeStep }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(WorkspaceSpacing.sm),
    ) {
        steps.forEachIndexed { index, (_, label) ->
            val state = when {
                index == activeIndex -> WorkspaceStatusKind.Running
                index < activeIndex -> WorkspaceStatusKind.Success
                else -> WorkspaceStatusKind.Idle
            }
            WorkspaceStatusPill(
                label = "${index + 1}. $label",
                kind = state,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * Failure surface for setup or agent install with an explicit retry action
 * (Plan 3, Phase 4, item 3).
 */
@Composable
private fun SetupErrorCard(
    message: String,
    canRetry: Boolean,
    onRetry: () -> Unit,
) {
    SettingsCard(modifier = Modifier.fillMaxWidth()) {
        WorkspaceSectionHeader(
            title = stringResource(Res.string.workspace_setup_failed_title),
            subtitle = message,
            trailing = {
                if (canRetry) {
                    Button(onClick = onRetry, modifier = Modifier.handCursor()) {
                        Text(stringResource(Res.string.workspace_explorer_retry))
                    }
                }
            },
        )
    }
}

/**
 * The Compose resource formatter substitutes `%1$d` but passes the `%%` escape through verbatim,
 * so `"%1$d%%"` would render as `42%%`. No-op once the formatter unescapes it itself.
 */
internal fun String.collapsePercentEscape(): String = replace("%%", "%")

@Composable
private fun stepLabel(state: WorkspaceEnvironmentState.Installing): String = when (state.step) {
    WorkspaceStep.Download -> stringResource(
        Res.string.workspace_step_download,
        ((state.progress ?: 0f) * 100).toInt(),
    ).collapsePercentEscape()

    WorkspaceStep.Extract -> stringResource(Res.string.workspace_step_extract)

    WorkspaceStep.Configure -> stringResource(Res.string.workspace_step_configure)

    WorkspaceStep.BasePackages -> stringResource(Res.string.workspace_step_base_packages)

    WorkspaceStep.Agent -> stringResource(
        Res.string.workspace_step_install_agent,
        WorkspaceAgents.get(state.agentId)?.title.orEmpty(),
    )
}
