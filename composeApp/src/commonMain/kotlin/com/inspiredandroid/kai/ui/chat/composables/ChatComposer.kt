package com.inspiredandroid.kai.ui.chat.composables

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.inspiredandroid.kai.data.ServiceEntry
import com.inspiredandroid.kai.ui.chat.ChatUiState
import kotlinx.collections.immutable.ImmutableList

/**
 * Bottom composer for the chat workspace (Phase 2 of the chat UI upgrade plan).
 *
 * Wraps [QuestionInput] and only renders it while the sandbox is closed, so the
 * draft survives toggling the sandbox/terminal view (the hoisted text state is
 * owned by the caller). No provider/persistence logic lives here.
 */
@Composable
fun ChatComposer(
    uiState: ChatUiState,
    questionInputText: androidx.compose.ui.text.input.TextFieldValue,
    onTextStateChange: (androidx.compose.ui.text.input.TextFieldValue) -> Unit,
    isSandboxOpen: Boolean,
    /** The services the picker offers — narrowed to agentic ones in interactive mode. */
    availableServices: ImmutableList<ServiceEntry> = uiState.availableServices,
    modifier: Modifier = Modifier,
) {
    if (!isSandboxOpen) {
        QuestionInput(
            modifier = modifier,
            files = uiState.files,
            addFile = uiState.actions.addFile,
            removeFile = uiState.actions.removeFile,
            ask = uiState.actions.ask,
            supportedFileExtensions = uiState.supportedFileExtensions,
            textState = questionInputText,
            onTextStateChange = onTextStateChange,
            isLoading = uiState.isLoading,
            cancel = uiState.actions.cancel,
            availableServices = availableServices,
            onSelectService = uiState.actions.selectService,
            availableModels = uiState.availableModels,
            isLoadingModels = uiState.isLoadingModels,
            onSelectModel = uiState.actions.selectModel,
            onLoadModels = uiState.actions.loadModels,
            installedSkills = uiState.installedSkills,
        )
    }
}
