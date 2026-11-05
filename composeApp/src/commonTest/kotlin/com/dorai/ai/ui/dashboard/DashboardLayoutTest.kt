package com.dorai.ai.ui.dashboard

import androidx.compose.ui.unit.dp
import com.dorai.ai.SandboxStatus
import com.dorai.ai.ui.chat.ChatActions
import com.dorai.ai.ui.chat.ChatUiState
import com.dorai.ai.workspace.WorkspaceState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DashboardLayoutTest {

    private fun dummyActions() = ChatActions(
        ask = {},
        toggleSpeechOutput = {},
        retry = {},
        clearHistory = {},
        setIsSpeaking = { _, _ -> },
        addFile = {},
        removeFile = {},
        startNewChat = {},
        regenerate = {},
        cancel = {},
        selectService = {},
        selectModel = {},
        loadModels = {},
        loadConversation = {},
        deleteConversation = {},
        renameConversation = { _, _ -> },
        togglePinConversation = {},
        clearUnreadHeartbeat = {},
        clearSnackbar = {},
        undoDeleteConversation = {},
        submitUiCallback = { _, _ -> },
        resubmit = { _, _, _ -> },
        setInteractiveMode = {},
        goBackInteractiveMode = {},
        sendSmsDraft = {},
        discardSmsDraft = {},
        consumeComposerPrefill = {},
    )

    @Test
    fun compactWidthUsesSingleColumn() {
        assertEquals(1, dashboardColumnsFor(599.dp))
    }

    @Test
    fun tabletWidthUsesTwoColumns() {
        assertEquals(2, dashboardColumnsFor(600.dp))
        assertEquals(2, dashboardColumnsFor(999.dp))
    }

    @Test
    fun expandedWidthUsesFourColumns() {
        assertEquals(4, dashboardColumnsFor(1000.dp))
    }

    @Test
    fun buildDashboardUiState_isCheckingServices_whenRestoring() {
        val chatState = ChatUiState(
            actions = dummyActions(),
            isRestoring = true,
            isLoading = false,
        )
        val state = buildDashboardUiState(
            chatUiState = chatState,
            sandboxStatus = SandboxStatus(),
            buildState = WorkspaceState(),
            isBuildAvailable = false,
            isSandboxAvailable = false,
            isDesignAvailable = false,
        )
        assertTrue(state.isCheckingServices)
        assertEquals(null, state.service)
    }

    @Test
    fun buildDashboardUiState_isCheckingServices_whenLoadingAndEmpty() {
        val chatState = ChatUiState(
            actions = dummyActions(),
            isRestoring = false,
            isLoading = true,
        )
        val state = buildDashboardUiState(
            chatUiState = chatState,
            sandboxStatus = SandboxStatus(),
            buildState = WorkspaceState(),
            isBuildAvailable = false,
            isSandboxAvailable = false,
            isDesignAvailable = false,
        )
        assertTrue(state.isCheckingServices)
        assertEquals(null, state.service)
    }

    @Test
    fun buildDashboardUiState_notCheckingServices_whenNotRestoringAndNotLoading() {
        val chatState = ChatUiState(
            actions = dummyActions(),
            isRestoring = false,
            isLoading = false,
        )
        val state = buildDashboardUiState(
            chatUiState = chatState,
            sandboxStatus = SandboxStatus(),
            buildState = WorkspaceState(),
            isBuildAvailable = false,
            isSandboxAvailable = false,
            isDesignAvailable = false,
        )
        assertFalse(state.isCheckingServices)
        assertEquals(null, state.service)
    }
}
