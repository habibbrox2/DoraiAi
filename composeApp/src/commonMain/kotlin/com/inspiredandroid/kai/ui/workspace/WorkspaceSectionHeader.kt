package com.inspiredandroid.kai.ui.workspace

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
import com.inspiredandroid.kai.workspace.WorkspaceEnvironmentState
import com.inspiredandroid.kai.workspace.WorkspaceState
import com.inspiredandroid.kai.ui.KaiTypography
import com.inspiredandroid.kai.ui.theme.LocalDesignColorTokens

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
                style = KaiTypography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = LocalDesignColorTokens.current.textPrimary,
                modifier = Modifier.semantics { heading() },
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = KaiTypography.bodySmall,
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
        WorkspaceEnvironmentState.NotInstalled -> "Not installed" to "Set up the Linux environment to create and open projects."
        is WorkspaceEnvironmentState.Installing -> "Installing" to "Finishing setup — you can open projects once it is ready."
        WorkspaceEnvironmentState.Ready -> "Ready" to "Create a project or open one to start building."
    }
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(WorkspaceSpacing.sm),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = guidance,
                style = KaiTypography.bodyMedium,
                color = LocalDesignColorTokens.current.textSecondary,
            )
        }
        WorkspaceStatusPill(label = label, kind = kind)
    }
}





