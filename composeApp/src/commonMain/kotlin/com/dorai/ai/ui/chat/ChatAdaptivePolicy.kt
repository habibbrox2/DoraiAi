package com.dorai.ai.ui.chat

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Centralized adaptive policy for the chat workspace (Phase 2 of the chat UI
 * upgrade plan). Expanded-layout decisions are derived here so that the shell
 * and the conversation column react to a single source of truth.
 *
 * The expanded layout activates at 840dp (desktop / large tablets); see
 * [ChatAdaptivePolicy.useExpandedLayout].
 */
object ChatAdaptivePolicy {
    /** Width reserved for the persistent conversation rail. */
    val conversationRailWidth: Dp = 280.dp

    /** Width reserved for the expanded sandbox/tool panel. */
    val sandboxPanelWidth: Dp = 340.dp

    /** Minimum usable width retained for chat content beside both panels. */
    val minimumMainContentWidth: Dp = 500.dp

    /** Minimum width for showing the rail and sandbox panel simultaneously. */
    val expandedSidePanelMinWidth: Dp =
        conversationRailWidth + sandboxPanelWidth + minimumMainContentWidth

    /**
     * Keeps the chat workspace expanded only when its fixed side panels leave
     * enough room for the main content. With a side panel on a narrower expanded
     * window, the conversation rail moves back to the drawer and the sandbox uses
     * the main content slot.
     */
    fun useExpandedLayout(width: Dp, hasSidePanel: Boolean): Boolean = width >= 840.dp && (!hasSidePanel || width >= expandedSidePanelMinWidth)

    /** Conversation column is centered and capped on expanded windows. */
    val conversationColumnMax: Dp = 760.dp
}
