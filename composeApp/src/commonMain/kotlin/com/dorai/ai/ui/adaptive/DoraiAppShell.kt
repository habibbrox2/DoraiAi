package com.dorai.ai.ui.adaptive

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.dorai.ai.PlatformBackHandler

/**
 * State holder for [DoraiAppShell].
 *
 * Preserves the active destination and navigation history across rotation and
 * window resize. Follows the spec rule:
 * "Back always goes one step back. On a top-level destination other than Home it
 * returns to Home first, then exits."
 */
class DoraiAppShellState(
    initialDestination: DoraiDestination = DoraiDestination.Home,
) {
    var currentDestination by mutableStateOf(initialDestination)
        private set

    val backStack = mutableStateListOf(initialDestination)

    fun navigateTo(destination: DoraiDestination) {
        if (currentDestination == destination) return
        currentDestination = destination
        // Don't accumulate identical consecutive destinations
        if (backStack.lastOrNull() != destination) {
            backStack.add(destination)
        }
    }

    /**
     * Attempts to navigate one step back. Returns true if handled, false to allow
     * the system/activity to exit.
     */
    fun handleBack(): Boolean {
        if (backStack.size > 1) {
            backStack.removeAt(backStack.lastIndex)
            currentDestination = backStack.lastOrNull() ?: DoraiDestination.Home
            return true
        } else if (currentDestination != DoraiDestination.Home) {
            currentDestination = DoraiDestination.Home
            backStack.clear()
            backStack.add(DoraiDestination.Home)
            return true
        }
        return false
    }
}

@Composable
fun rememberDoraiAppShellState(
    initialDestination: DoraiDestination = DoraiDestination.Home,
): DoraiAppShellState = rememberSaveable(
    saver = Saver(
        save = { it.currentDestination.id },
        restore = { DoraiAppShellState(DoraiDestination.fromId(it)) },
    ),
) {
    DoraiAppShellState(initialDestination)
}

/**
 * Root adaptive shell for UI v2.
 *
 * Hosts the four primary destinations and handles back navigation, window sizing,
 * and approval badge counts.
 */
@Composable
fun DoraiAppShell(
    waitingApprovalsCount: Int = 0,
    shellState: DoraiAppShellState = rememberDoraiAppShellState(),
    modifier: Modifier = Modifier,
    homeContent: @Composable () -> Unit,
    chatContent: @Composable () -> Unit,
    workspaceContent: @Composable () -> Unit,
    settingsContent: @Composable () -> Unit,
) {
    val canHandleBack = shellState.currentDestination != DoraiDestination.Home || shellState.backStack.size > 1
    PlatformBackHandler(enabled = canHandleBack) {
        shellState.handleBack()
    }

    DoraiAdaptiveScaffold(
        selectedDestination = shellState.currentDestination,
        onDestinationSelected = { destination ->
            shellState.navigateTo(destination)
        },
        waitingApprovalsCount = waitingApprovalsCount,
        modifier = modifier,
    ) {
        Crossfade(
            targetState = shellState.currentDestination,
            label = "DoraiAppDestinationCrossfade",
        ) { destination ->
            Box(modifier = Modifier.fillMaxSize()) {
                when (destination) {
                    DoraiDestination.Home -> homeContent()
                    DoraiDestination.Chat -> chatContent()
                    DoraiDestination.Workspace -> workspaceContent()
                    DoraiDestination.Settings -> settingsContent()
                }
            }
        }
    }
}
