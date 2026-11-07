package com.dorai.ai.ui.adaptive

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * UI v2 window width classes based on section 3.1:
 * - [Compact]: < 600dp (Bottom bar with 4 items, 1 pane, modal drawers)
 * - [Medium]: 600–839dp (78dp navigation rail, list pane 250dp + detail)
 * - [Expanded]: 840–1199dp (78dp navigation rail, list pane 270dp + detail)
 * - [Large]: >= 1200dp (224dp permanent drawer with labels, list pane 300dp + detail, 3-pane workspace)
 */
enum class DoraiWindowWidthClass {
    Compact,
    Medium,
    Expanded,
    Large,
    ;

    val isCompact: Boolean get() = this == Compact
    val isMedium: Boolean get() = this == Medium
    val isExpanded: Boolean get() = this == Expanded
    val isLarge: Boolean get() = this == Large

    /** Whether the screen shows a permanent list pane in Chat and Settings. */
    val showsPermanentListPane: Boolean get() = this != Compact

    /** List pane width per width class (0dp on Compact). */
    val listPaneWidth: Dp
        get() = when (this) {
            Compact -> 0.dp
            Medium -> DoraiAdaptiveDefaults.ListPaneWidthMedium
            Expanded -> DoraiAdaptiveDefaults.ListPaneWidthExpanded
            Large -> DoraiAdaptiveDefaults.ListPaneWidthLarge
        }

    /** Navigation width (0dp on Compact where bottom bar is used). */
    val navigationWidth: Dp
        get() = when (this) {
            Compact -> 0.dp
            Medium, Expanded -> DoraiAdaptiveDefaults.NavRailWidth
            Large -> DoraiAdaptiveDefaults.LargeDrawerWidth
        }

    /** Workspace agent panel width on the right. */
    val workspaceAgentPanelWidth: Dp
        get() = when (this) {
            Large -> DoraiAdaptiveDefaults.AgentPanelWidthLarge
            else -> DoraiAdaptiveDefaults.AgentPanelWidthMedium
        }
}

object DoraiAdaptiveDefaults {
    val NavRailWidth: Dp = 78.dp
    val LargeDrawerWidth: Dp = 224.dp

    val ListPaneWidthMedium: Dp = 250.dp
    val ListPaneWidthExpanded: Dp = 270.dp
    val ListPaneWidthLarge: Dp = 300.dp

    val AgentPanelWidthMedium: Dp = 320.dp
    val AgentPanelWidthLarge: Dp = 380.dp

    val FileTreeWidthLarge: Dp = 230.dp
}

/**
 * Maps a window or container width in [Dp] to the corresponding [DoraiWindowWidthClass].
 */
fun calculateDoraiWindowWidthClass(width: Dp): DoraiWindowWidthClass = when {
    width < 600.dp -> DoraiWindowWidthClass.Compact
    width < 840.dp -> DoraiWindowWidthClass.Medium
    width < 1200.dp -> DoraiWindowWidthClass.Expanded
    else -> DoraiWindowWidthClass.Large
}

/**
 * CompositionLocal carrying the active [DoraiWindowWidthClass].
 */
val LocalDoraiWindowWidthClass = compositionLocalOf { DoraiWindowWidthClass.Compact }
