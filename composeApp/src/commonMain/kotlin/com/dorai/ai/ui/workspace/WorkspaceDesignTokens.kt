package com.dorai.ai.ui.workspace

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dorai.ai.prefersReducedMotion
import com.dorai.ai.ui.AccentPrimary
import com.dorai.ai.ui.DangerAlert
import com.dorai.ai.ui.SuccessGreen
import com.dorai.ai.ui.WarningAmber
import com.dorai.ai.ui.theme.DesignTokens
import com.dorai.ai.ui.theme.LocalDesignColorTokens
import com.dorai.ai.workspace.WorkspaceEnvironmentState
import com.dorai.ai.workspace.WorkspaceStep

/**
 * Build-specific design tokens for the Workspace workspace (Plan 3, Phase 1).
 *
 * Terminal-centric colors that work with the DORAI AI palette and stay legible on
 * both the dark terminal surface and the app's adaptive backgrounds.
 */
object WorkspaceSpacing {
    val xxs: Dp = 2.dp
    val xs: Dp = 4.dp
    val sm: Dp = 8.dp
    val md: Dp = 12.dp
    val lg: Dp = 16.dp
    val xl: Dp = 24.dp
    val xxl: Dp = 32.dp

    val minTouchTarget: Dp = 48.dp
}

object WorkspaceShapes {
    val card = RoundedCornerShape(16.dp)
    val pane = RoundedCornerShape(12.dp)
    val pill = RoundedCornerShape(999.dp)
    val terminal = RoundedCornerShape(10.dp)
}

/** Status semantics shared by the workspace header, session tabs, and setup flow. */
@Immutable
enum class WorkspaceStatusKind {
    /** Environment/session is ready and idle. */
    Idle,

    /** Something is actively running (install, build, command). */
    Running,

    /** Completed successfully. */
    Success,

    /** Needs attention but did not fail outright (missing agent, unsaved work). */
    Warning,

    /** A step or command failed. */
    Failed,
}

/**
 * Resolves the environment state to a status kind so headers, pills, and setup
 * blocks all agree on what "installing", "ready", and "not installed" look like.
 */
fun WorkspaceEnvironmentState.toStatusKind(): WorkspaceStatusKind = when (this) {
    WorkspaceEnvironmentState.NotInstalled -> WorkspaceStatusKind.Idle
    is WorkspaceEnvironmentState.Installing -> WorkspaceStatusKind.Running
    WorkspaceEnvironmentState.Ready -> WorkspaceStatusKind.Success
}

/** Color ramp for the four terminal/workflow statuses plus neutral idle. */
@Composable
fun rememberWorkspaceStatusColors(): WorkspaceStatusColors {
    val textSecondary = LocalDesignColorTokens.current.textSecondary
    return remember {
        WorkspaceStatusColors(
            idle = textSecondary,
            running = DesignTokens.AgentViolet,
            success = DesignTokens.Success,
            warning = DesignTokens.Warning,
            failed = DesignTokens.Error,
        )
    }
}

@Immutable
data class WorkspaceStatusColors(
    val idle: Color,
    val running: Color,
    val success: Color,
    val warning: Color,
    val failed: Color,
) {
    fun forKind(kind: WorkspaceStatusKind): Color = when (kind) {
        WorkspaceStatusKind.Idle -> idle
        WorkspaceStatusKind.Running -> running
        WorkspaceStatusKind.Success -> success
        WorkspaceStatusKind.Warning -> warning
        WorkspaceStatusKind.Failed -> failed
    }
}

/** Fixed surfaces used by the workspace, kept here so the palette stays in one place. */
object WorkspaceSurfaces {
    /** The terminal canvas — always dark for readable ANSI colors. */
    val terminal: Color = DesignTokens.TerminalBackground
    val terminalRaised: Color = DesignTokens.SurfaceElevated
    val terminalBorder: Color = DesignTokens.BorderSubtle

    /** Progress bar fill for the current install step. */
    val progressTrack: Color = DesignTokens.BrandNavy
}

/**
 * Centralized width policy for the Build workspace (Plan 3, Phase 6).
 *
 * One breakpoint table shared by the hub (projects/setup) and workspace
 * surfaces so compact, medium, and expanded layouts never disagree.
 */
enum class WorkspaceTier {
    /** Phone/narrow window: everything stacked, full-bleed. */
    Compact,

    /** Medium window: full-bleed but roomier rows. */
    Medium,

    /** Wide desktop: hub content centers with a readable max width. */
    Expanded,
}

/** Breakpoints in width — mirrors the chat/settings adaptive policy (600/840 style). */
fun WorkspaceTier(width: Dp): WorkspaceTier = when {
    width < 600.dp -> WorkspaceTier.Compact
    width < 1000.dp -> WorkspaceTier.Medium
    else -> WorkspaceTier.Expanded
}

/**
 * Max width for the hub surfaces (projects, setup) on each tier. Compact and
 * Medium stay full-bleed; Expanded centers so project rows don't stretch into
 * unreadable line lengths on desktop.
 */
val WorkspaceTier.contentMaxWidth: Dp
    get() = when (this) {
        // Dp.Unbounded does not exist on every target; the platform-agnostic
        // infinity value keeps this common code compiling on wasm too.
        WorkspaceTier.Compact -> Dp(Float.POSITIVE_INFINITY)

        WorkspaceTier.Medium -> Dp(Float.POSITIVE_INFINITY)

        WorkspaceTier.Expanded -> 960.dp
    }

/**
 * Human label for an install step, used by the setup surface and progress blocks.
 */
fun WorkspaceStep.label(): String = when (this) {
    WorkspaceStep.Download -> "Download"
    WorkspaceStep.Extract -> "Extract"
    WorkspaceStep.Configure -> "Configure"
    WorkspaceStep.BasePackages -> "Base packages"
    WorkspaceStep.Agent -> "Agent"
}
