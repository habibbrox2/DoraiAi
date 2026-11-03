package com.inspiredandroid.kai.ui.chat

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Responsive shell for the chat workspace (Phase 2 of the chat UI upgrade plan).
 *
 * Centralizes the window-width adaptive policy: on expanded windows the main
 * content is flanked by an optional conversation rail and an optional side panel;
 * on compact/medium windows the main content takes the full width and history is
 * presented as a bottom sheet by the caller.
 *
 * Business state (history, services, sandbox) is still owned by [ChatUiState] and
 * the [ChatActions] contract; this composable only arranges presentation slots.
 */
@Composable
fun ChatShell(
    header: @Composable () -> Unit,
    mainContent: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
    conversationRail: @Composable (() -> Unit)? = null,
    sidePanel: @Composable (() -> Unit)? = null,
    overlays: @Composable BoxScope.() -> Unit = {},
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val sizeClass = chatWindowSizeClass(maxWidth)
        Column(Modifier.fillMaxSize()) {
            header()
            when (sizeClass) {
                ChatWindowSizeClass.Expanded -> Row(Modifier.weight(1f).fillMaxSize()) {
                    if (conversationRail != null) {
                        Box(Modifier.width(ChatAdaptivePolicy.conversationRailWidth).fillMaxHeight()) {
                            conversationRail()
                        }
                    }
                    Box(Modifier.weight(1f).fillMaxHeight()) {
                        Column(Modifier.fillMaxSize()) {
                            mainContent()
                        }
                    }
                    if (sidePanel != null) {
                        Box(Modifier.width(ChatAdaptivePolicy.sandboxPanelWidth).fillMaxHeight()) {
                            sidePanel()
                        }
                    }
                }

                else -> Box(Modifier.weight(1f).fillMaxSize()) {
                    Column(Modifier.fillMaxSize()) {
                        mainContent()
                    }
                }
            }
        }
        overlays()
    }
}
