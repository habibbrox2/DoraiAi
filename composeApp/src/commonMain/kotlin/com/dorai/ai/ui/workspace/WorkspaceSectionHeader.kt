package com.dorai.ai.ui.workspace

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import com.dorai.ai.ui.DoraiTypography
import com.dorai.ai.ui.theme.LocalDesignColorTokens
import com.dorai.ai.workspace.WorkspaceEnvironmentState
import com.dorai.ai.workspace.WorkspaceState
import dorai.composeapp.generated.resources.Res
import dorai.composeapp.generated.resources.workspace_env_installing_guidance
import dorai.composeapp.generated.resources.workspace_env_installing_label
import dorai.composeapp.generated.resources.workspace_env_not_installed_guidance
import dorai.composeapp.generated.resources.workspace_env_not_installed_label
import dorai.composeapp.generated.resources.workspace_env_ready_guidance
import dorai.composeapp.generated.resources.workspace_env_ready_label
import org.jetbrains.compose.resources.stringResource

/**
 * Section heading for the Build workspace (Plan 3, Phase 2), marked as a semantic
 * heading so the dashboard reads as an outline to assistive tech.
 */
@Composable
fun WorkspaceSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(WorkspaceSpacing.sm),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(WorkspaceSpacing.xxs),
        ) {
            Text(
                text = title,
                style = DoraiTypography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = LocalDesignColorTokens.current.textPrimary,
                modifier = Modifier.semantics { heading() },
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = DoraiTypography.bodySmall,
                    color = LocalDesignColorTokens.current.textSecondary,
                )
            }
        }
        if (trailing != null) trailing()
    }
}

/**
 * Environment readiness header at the top of the project dashboard (Plan 3, Phase 2).
 * Answers "is Build ready and what should I do next" before any project is opened.
 */
@Composable
fun BuildEnvironmentHeader(
    state: WorkspaceState,
    modifier: Modifier = Modifier,
) {
    val environment = state.environment
    val kind = environment.toStatusKind()
    val (label, guidance) = when (environment) {
        WorkspaceEnvironmentState.NotInstalled -> stringResource(Res.string.workspace_env_not_installed_label) to stringResource(Res.string.workspace_env_not_installed_guidance)
        is WorkspaceEnvironmentState.Installing -> stringResource(Res.string.workspace_env_installing_label) to stringResource(Res.string.workspace_env_installing_guidance)
        WorkspaceEnvironmentState.Ready -> stringResource(Res.string.workspace_env_ready_label) to stringResource(Res.string.workspace_env_ready_guidance)
    }
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(WorkspaceSpacing.sm),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = guidance,
                style = DoraiTypography.bodyMedium,
                color = LocalDesignColorTokens.current.textSecondary,
            )
        }
        WorkspaceStatusPill(label = label, kind = kind)
    }
}
