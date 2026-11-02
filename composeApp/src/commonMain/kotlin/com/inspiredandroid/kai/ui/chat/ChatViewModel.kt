package com.inspiredandroid.kai.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inspiredandroid.kai.data.Conversation
import com.inspiredandroid.kai.data.DataRepository
import com.inspiredandroid.kai.data.FreeMode
import com.inspiredandroid.kai.data.Service
import com.inspiredandroid.kai.data.ServiceEntry
import com.inspiredandroid.kai.data.TaskScheduler
import com.inspiredandroid.kai.data.UiSubmission
import com.inspiredandroid.kai.filereview.FileReviewActionResult
import com.inspiredandroid.kai.filereview.FileReviewController
import com.inspiredandroid.kai.filereview.FileReviewState
import com.inspiredandroid.kai.filereview.NoOpFileReviewController
import com.inspiredandroid.kai.getBackgroundDispatcher
import com.inspiredandroid.kai.network.UiError
import com.inspiredandroid.kai.network.shouldShowFreeProviderSuggestions
import com.inspiredandroid.kai.network.toUiError
import com.inspiredandroid.kai.tools.AppPermission
import com.inspiredandroid.kai.tools.PermissionController
import com.inspiredandroid.kai.tools.isLocalNetworkUrl
import com.inspiredandroid.kai.ui.markdown.KaiUiBlock
import com.inspiredandroid.kai.ui.markdown.KaiUiError
import com.inspiredandroid.kai.ui.markdown.parseMarkdown
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.extension
import kai.composeapp.generated.resources.Res
import kai.composeapp.generated.resources.conversation_untitled
import kai.composeapp.generated.resources.error_local_network_permission
import kai.composeapp.generated.resources.error_unsupported_file_type
import kai.composeapp.generated.resources.litert_no_model_warning
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.seconds

class ChatViewModel(
    private val dataRepository: DataRepository,
    private val taskScheduler: TaskScheduler,
    private val backgroundDispatcher: CoroutineContext = getBackgroundDispatcher(),
    private val localNetworkPermissionController: PermissionController = PermissionController(AppPermission.LOCAL_NETWORK),
    private val fileReviewController: FileReviewController = NoOpFileReviewController(),
) : ViewModel() {

    private val actions = ChatActions(
        ask = ::ask,
        retry = ::retry,
        toggleSpeechOutput = ::toggleSpeechOutput,
        clearHistory = ::clearHistory,
        setIsSpeaking = ::setIsSpeaking,
        addFile = ::addFile,
        removeFile = ::removeFile,
        startNewChat = { startNewChat() },
        regenerate = ::regenerate,
        cancel = ::cancel,
        selectService = ::selectService,
        selectModel = ::selectModel,
        loadModels = ::loadModels,
        loadConversation = ::loadConversation,
        deleteConversation = ::deleteConversation,
        renameConversation = ::renameConversation,
        togglePinConversation = ::togglePinConversation,
        clearUnreadHeartbeat = ::clearUnreadHeartbeat,
        clearSnackbar = ::clearSnackbar,
        undoDeleteConversation = ::undoDeleteConversation,
        submitUiCallback = ::submitUiCallback,
        resubmit = ::resubmit,
        setInteractiveMode = ::setInteractiveMode,
        goBackInteractiveMode = ::goBackInteractiveMode,
        sendSmsDraft = ::sendSmsDraft,
        discardSmsDraft = ::discardSmsDraft,
        consumeComposerPrefill = ::consumeComposerPrefill,
        confirmToolExecution = { toolCallId, allowOnce, alwaysAllow, allowSession ->
            dataRepository.confirmToolExecution(toolCallId, allowOnce, alwaysAllow, allowSession)
        },
        denyToolExecution = { toolCallId ->
            dataRepository.denyToolExecution(toolCallId)
        },
        acceptFileChange = ::acceptFileChange,
        rejectFileChange = ::rejectFileChange,
        rollbackFileChange = ::rollbackFileChange,
    )
    private val freeModeNames: Map<FreeMode, String> = FreeMode.entries.associateWith { "Free ${it.modelId.replaceFirstChar { c -> c.uppercase() }}" }
    private var currentJob: Job? = null
    private var pendingConversationDeleteJob: Job? = null

    /** The instance whose provider model list was already fetched in this session. */
    private val _state = MutableStateFlow(
        ChatUiState(
            actions = actions,
            showPrivacyInfo = dataRepository.isUsingSharedKey(),
        ),
    )

    init {
        updateAvailableServices()

        viewModelScope.launch(backgroundDispatcher) {
            runCatching { fileReviewController.initialize() }
            fileReviewController.proposals.collect { proposals ->
                val visible = proposals.filter { it.state != FileReviewState.REJECTED }.takeLast(5)
                val previews = LinkedHashMap<String, com.inspiredandroid.kai.filereview.FileReviewPreview>()
                visible.forEach { proposal -> fileReviewController.preview(proposal.id)?.let { previews[proposal.id] = it } }
                _state.update { it.copy(fileReviewProposals = visible.toImmutableList(), fileReviewPreviews = previews) }
            }
        }

        // Keep restoreCurrentConversation off the main thread; see issue #197 (large persisted
        // tool outputs caused ANRs when JSON-decoded synchronously during VM construction).
        // ChatScreen gates the interactive-mode branch on !isRestoring to avoid a flash.
        viewModelScope.launch(backgroundDispatcher) {
            dataRepository.loadConversations()
            // Share and assist both want a fresh chat. Skip restore so a slow
            // load cannot clobber startNewChat() when Kai is cold-started from
            // ACTION_SEND or ACTION_ASSIST.
            val skipRestore = dataRepository.pendingShareText.value != null ||
                dataRepository.openAssistRequested.value
            if (!skipRestore) {
                dataRepository.restoreCurrentConversation()
                presetInteractiveModeForCurrentConversation()
            }
            _state.update { it.copy(isRestoring = false) }
        }

        viewModelScope.launch(backgroundDispatcher) {
            dataRepository.connectEnabledMcpServers()
        }
        viewModelScope.launch {
            dataRepository.fallbackStatus.collect { status ->
                _state.update { it.copy(fallbackStatus = status) }
            }
        }
        taskScheduler.isLoadingCheck = { _state.value.isLoading }
        taskScheduler.start()

        viewModelScope.launch {
            dataRepository.smsDrafts.collect { drafts ->
                _state.update { it.copy(smsDrafts = drafts.toImmutableList()) }
            }
        }

        viewModelScope.launch {
            dataRepository.openHeartbeatRequested
                .filter { it }
                .collect {
                    val heartbeatId = dataRepository.savedConversations.value
                        .firstOrNull { it.type == Conversation.TYPE_HEARTBEAT }?.id
                    if (heartbeatId != null) {
                        loadConversation(heartbeatId)
                        clearUnreadHeartbeat()
                    }
                    dataRepository.consumeOpenHeartbeatRequest()
                }
        }

        viewModelScope.launch {
            dataRepository.openAssistRequested
                .filter { it }
                .collect {
                    startNewChat()
                    dataRepository.consumeOpenAssistRequest()
                }
        }

        viewModelScope.launch {
            dataRepository.pendingShareText
                .filterNotNull()
                .collect { text ->
                    startNewChat(composerPrefill = text)
                    dataRepository.consumeOpenShareRequest()
                }
        }
    }

    val state = combine(
        _state,
        dataRepository.chatHistory,
        dataRepository.savedConversations,
        dataRepository.currentConversationId,
        dataRepository.hasUnreadHeartbeat,
    ) { state, history, conversations, conversationId, hasUnreadHeartbeat ->
        val summaries = conversations
            .sortedByDescending { it.updatedAt }
            .map {
                val isHeartbeat = it.type == Conversation.TYPE_HEARTBEAT
                val isInteractive = it.type == Conversation.TYPE_INTERACTIVE
                ConversationSummary(
                    id = it.id,
                    title = if (isHeartbeat) "" else it.title.ifEmpty { getString(Res.string.conversation_untitled) },
                    updatedAt = it.updatedAt,
                    isHeartbeat = isHeartbeat,
                    isInteractive = isInteractive,
                    isPinned = it.isPinned,
                )
            }
        state.copy(
            history = history.toImmutableList(),
            supportedFileExtensions = dataRepository.supportedFileExtensions().toImmutableList(),
            savedConversations = summaries.toImmutableList(),
            currentConversationId = conversationId,
            hasUnreadHeartbeat = hasUnreadHeartbeat,
            installedSkills = dataRepository.getInstalledSkills().toImmutableList(),
        )
    }.distinctUntilChanged().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = _state.value,
    )

    private fun submitUiCallback(event: String, data: Map<String, String>) {
        val message = if (data.isNotEmpty()) {
            val formattedData = data.entries.joinToString(", ") { "${it.key}: ${it.value}" }
            "Responded with: $formattedData"
        } else {
            "Pressed: $event"
        }
        val lastAssistant = dataRepository.chatHistory.value.lastRenderedAssistant()
        val submission = lastAssistant?.let {
            UiSubmission(sourceContent = it.content, values = data, pressedEvent = event)
        }
        askInternal(message, submission)
    }

    private fun ask(question: String?) {
        askInternal(question, null)
    }

    private fun askInternal(question: String?, uiSubmission: UiSubmission?) {
        // Atomic check-and-set to prevent concurrent requests (race on double-send)
        val before = _state.value
        val files = before.files
        val started = _state.updateAndGet { current ->
            if (current.isLoading) {
                current
            } else {
                current.copy(
                    isLoading = true,
                    error = null,
                    showFreeProviderSuggestions = false,
                    files = persistentListOf(),
                    followUpSuggestions = persistentListOf(),
                )
            }
        }
        if (started === before) return // another call already won

        val (strippedQuestion, activeSkillId) = parseSkillInvocation(question)

        currentJob = viewModelScope.launch(backgroundDispatcher) {
            // Android 17+ blocks LAN traffic without the local network permission — without
            // asking first, requests to self-hosted servers silently never leave the device.
            if (!ensureLocalNetworkPermission()) {
                _state.update {
                    it.copy(
                        error = UiError.Resource(Res.string.error_local_network_permission),
                        showFreeProviderSuggestions = false,
                        isLoading = false,
                    )
                }
                return@launch
            }
            try {
                dataRepository.ask(strippedQuestion, files, uiSubmission, activeSkillId)

                // Auto-retry in interactive mode if the response has no valid kai-ui
                if (_state.value.isInteractiveMode) {
                    retryIfNoValidKaiUi()
                }

                _state.update {
                    it.copy(isLoading = false)
                }

                if (!_state.value.isInteractiveMode) {
                    generateFollowUpSuggestions()
                }
            } catch (exception: Exception) {
                // CancellationException must be re-thrown to properly propagate coroutine cancellation
                if (exception is CancellationException) throw exception

                val showUpsell = shouldShowFreeProviderSuggestions(
                    noConfiguredServices = dataRepository.getConfiguredServiceInstances().isEmpty(),
                    exception = exception,
                )
                _state.update {
                    it.copy(
                        error = exception.toUiError(),
                        showFreeProviderSuggestions = showUpsell,
                        isLoading = false,
                        followUpSuggestions = persistentListOf(),
                    )
                }
            }
        }
    }

    /**
     * True unless the active service points at a local network host and the user
     * denied the local network permission. Cheap no-op on non-Android platforms.
     */
    private suspend fun generateFollowUpSuggestions() {
        val history = dataRepository.chatHistory.value
        val lastAssistant = history.lastRenderedAssistant() ?: return
        val lastUser = history.lastOrNull { it.role == History.Role.USER } ?: return
        val prompt = buildString {
            appendLine("Given this conversation:")
            appendLine("User: ${lastUser.content.take(400)}")
            appendLine("Assistant: ${lastAssistant.content.take(800)}")
            appendLine()
            appendLine("Suggest exactly 3 short follow-up questions the user might ask next.")
            append("Return only the questions, one per line, no numbering, no extra text.")
        }
        val result = try {
            dataRepository.askSilently(prompt)
        } catch (_: Exception) {
            return
        }
        val suggestions = result.lines()
            .map { line ->
                line.trim()
                    .replace(Regex("^[0-9]+[.)]\\s*"), "")
                    .replace(Regex("^[-*]\\s*"), "")
                    .trim()
            }
            .filter { it.isNotBlank() && it.length in 3..120 }
            .distinct()
            .take(3)
        if (suggestions.isNotEmpty()) {
            _state.update { it.copy(followUpSuggestions = suggestions.toImmutableList()) }
        }
    }

    private suspend fun ensureLocalNetworkPermission(): Boolean {
        val instance = dataRepository.getConfiguredServiceInstances().firstOrNull() ?: return true
        val baseUrl = dataRepository.getInstanceBaseUrl(instance.instanceId, Service.fromId(instance.serviceId))
        if (!isLocalNetworkUrl(baseUrl)) return true
        return localNetworkPermissionController.requestPermission()
    }

    private suspend fun retryIfNoValidKaiUi(maxRetries: Int = 2) {
        repeat(maxRetries) {
            currentCoroutineContext().ensureActive()
            val lastAssistant = dataRepository.chatHistory.value.lastRenderedAssistant() ?: return

            val blocks = parseMarkdown(lastAssistant.content).blocks
            val hasValidUi = blocks.any { it is KaiUiBlock }
            if (hasValidUi) return

            // Build error feedback for the AI
            val errorBlock = blocks.filterIsInstance<KaiUiError>().firstOrNull()
            val errorDetail = if (errorBlock != null) {
                "JSON parse error in: ${errorBlock.rawJson.take(200)}"
            } else {
                "No kai-ui code fence found in your response."
            }
            val retryMessage = "[SYSTEM] Your previous response failed to render as interactive UI. $errorDetail " +
                "Remember: respond with ONLY a single ```kai-ui code fence containing valid JSON. No text outside the fence."

            dataRepository.ask(retryMessage, emptyList())
        }
    }

    private fun clearHistory() {
        dataRepository.clearHistory()
        _state.update {
            it.copy(error = null, showFreeProviderSuggestions = false, followUpSuggestions = persistentListOf())
        }
    }

    /**
     * If [text] begins with `/<skill-id>`, look up the skill among the currently-
     * installed-and-enabled skills and return its id alongside the verbatim user
     * text. The text is sent unchanged so the conversation visibly reflects what
     * the user typed; the skill's instructions in the system prompt tell the model
     * how to parse the args after the slash command. Falls through with null skill
     * id when no match — slash commands are opt-in.
     */
    private fun parseSkillInvocation(text: String?): Pair<String?, String?> {
        if (text == null) return null to null
        val trimmed = text.trimStart()
        if (!trimmed.startsWith('/')) return text to null
        val firstSpace = trimmed.indexOfFirst { it.isWhitespace() }
        val rawId = if (firstSpace < 0) trimmed.substring(1) else trimmed.substring(1, firstSpace)
        if (rawId.isEmpty()) return text to null
        val skill = dataRepository.getInstalledSkills().firstOrNull { it.id.equals(rawId, ignoreCase = true) }
            ?: return text to null
        return text to skill.id
    }

    private fun setIsSpeaking(isSpeaking: Boolean, contentId: String) {
        _state.update {
            it.copy(
                isSpeaking = isSpeaking,
                isSpeakingContentId = if (isSpeaking) {
                    contentId
                } else {
                    it.isSpeakingContentId
                },
            )
        }
    }

    private fun addFile(file: PlatformFile) {
        val ext = file.extension.lowercase()
        val supported = dataRepository.supportedFileExtensions()
        if (ext.isEmpty() || ext !in supported) {
            _state.update {
                it.copy(snackbarMessage = Res.string.error_unsupported_file_type)
            }
            return
        }
        _state.update {
            it.copy(files = (it.files + file).toImmutableList())
        }
    }

    private fun removeFile(file: PlatformFile) {
        _state.update {
            it.copy(files = it.files.filterNot { f -> f == file }.toImmutableList())
        }
    }

    private fun clearSnackbar() {
        _state.update {
            it.copy(snackbarMessage = null)
        }
    }

    private fun retry() {
        ask(null)
    }

    private fun toggleSpeechOutput() {
        _state.update {
            it.copy(
                isSpeechOutputEnabled = !it.isSpeechOutputEnabled,
            )
        }
    }

    private fun cancel() {
        currentJob?.cancel()
        currentJob = null
        _state.update {
            it.copy(isLoading = false)
        }
    }

    private fun selectService(instanceId: String) {
        val freeMode = FREE_MODE_INSTANCE_IDS[instanceId]
        if (freeMode != null) {
            dataRepository.setFreeMode(freeMode)
            dataRepository.setFreeServicePrimary(true)
            updateAvailableServices()
            return
        }

        dataRepository.setFreeServicePrimary(false)
        val instances = dataRepository.getConfiguredServiceInstances()
        val currentIds = instances.map { it.instanceId }
        if (instanceId !in currentIds) return
        val reordered = listOf(instanceId) + currentIds.filter { it != instanceId }
        dataRepository.reorderConfiguredServices(reordered)
        updateAvailableServices()
        _state.update { it.copy(modelsLoadedInstanceId = null) }
        loadModels()
    }

    private fun updateAvailableServices() {
        val configuredEntries = dataRepository.getServiceEntries()
        val currentFreeMode = dataRepository.getFreeMode()
        val freeIsPrimary = dataRepository.isFreeServicePrimary() || configuredEntries.isEmpty()

        val freeModes = (listOf(currentFreeMode) + FreeMode.entries.filter { it != currentFreeMode }).map { mode ->
            ServiceEntry(
                instanceId = mode.instanceId,
                serviceId = Service.Free.id,
                serviceName = freeModeNames.getValue(mode),
                modelId = "",
                icon = mode.icon,
            )
        }

        val entries = if (freeIsPrimary) {
            freeModes + configuredEntries
        } else {
            configuredEntries + freeModes
        }.toImmutableList()

        val primaryService = entries.firstOrNull()?.let { Service.fromId(it.serviceId) }
        val warning = if (primaryService?.isOnDevice == true && dataRepository.getLocalDownloadedModels().isEmpty()) {
            Res.string.litert_no_model_warning
        } else {
            null
        }
        _state.update { it.copy(availableServices = entries, warning = warning, showPrivacyInfo = dataRepository.isUsingSharedKey()) }
        refreshModelsForPrimary()
    }

    /**
     * The service the chat currently talks to. Free modes are pseudo-services without a
     * model list, so they never carry models.
     */
    private fun primaryServiceInstance(): Pair<ServiceEntry, Service>? {
        val primary = _state.value.availableServices.firstOrNull() ?: return null
        val service = Service.fromId(primary.serviceId)
        if (service == Service.Free) return null
        return primary to service
    }

    /**
     * Keeps [ChatUiState.availableModels] in sync with the active instance, so the chat
     * shows the model that is loaded and can offer the alternatives.
     */
    private fun refreshModelsForPrimary() {
        val primaryWithService = primaryServiceInstance()
        if (primaryWithService == null) {
            _state.update { it.copy(availableModels = persistentListOf()) }
            return
        }
        val (primary, service) = primaryWithService
        val models = dataRepository.getInstanceModels(primary.instanceId, service).value
        _state.update { it.copy(availableModels = models.toImmutableList()) }
    }

    /**
     * Picks another model for the active service instance. The choice is stored per instance,
     * so it applies to the next message and survives a restart.
     */
    private fun selectModel(modelId: String) {
        val (primary, service) = primaryServiceInstance() ?: return
        if (modelId.isBlank()) return
        // A custom model id overrides the list selection, so picking from the list has to
        // switch that off — otherwise the chat would keep running the custom model.
        if (dataRepository.getInstanceUseCustomModel(primary.instanceId)) {
            dataRepository.updateInstanceUseCustomModel(primary.instanceId, false)
        }
        dataRepository.updateInstanceSelectedModel(primary.instanceId, service, modelId)
        // The service entry carries the effective model id, so it has to be rebuilt for the
        // header to show the new model.
        updateAvailableServices()
    }

    /**
     * Loads the provider's model list for the active instance. The chat asks for it when the
     * model picker opens, so the list is only fetched when it is actually needed — and only
     * once per instance, so reopening the picker does not spend a request every time.
     */
    private fun loadModels() {
        val (primary, service) = primaryServiceInstance() ?: return
        if (_state.value.modelsLoadedInstanceId == primary.instanceId) {
            refreshModelsForPrimary()
            return
        }
        viewModelScope.launch(backgroundDispatcher) {
            _state.update { it.copy(isLoadingModels = true) }
            try {
                dataRepository.validateConnection(service, primary.instanceId)
                _state.update { it.copy(modelsLoadedInstanceId = primary.instanceId) }
            } catch (_: Exception) {
                // Keep the list we already have: the curated defaults are still selectable.
            }
            _state.update { it.copy(isLoadingModels = false) }
            refreshModelsForPrimary()
        }
    }

    companion object {
        private val FREE_MODE_INSTANCE_IDS = FreeMode.entries.associateBy { it.instanceId }
    }

    private fun regenerate() {
        dataRepository.regenerate()
        ask(null)
    }

    fun loadConversation(id: String) {
        currentJob?.cancel()
        currentJob = null
        val conversation = dataRepository.savedConversations.value.find { it.id == id }
        val isInteractive = conversation?.type == Conversation.TYPE_INTERACTIVE
        dataRepository.setInteractiveMode(isInteractive)
        dataRepository.loadConversation(id)
        _state.update {
            it.copy(
                error = null,
                showFreeProviderSuggestions = false,
                isInteractiveMode = isInteractive,
                isLoading = false,
                composerPrefill = null,
                followUpSuggestions = persistentListOf(),
            )
        }
    }

    private fun deleteConversation(id: String) {
        // If there's already a pending deletion, commit it first (don't silently discard).
        // This avoids the race where deleting A then B within 4s leaves A undeleted but
        // only B's undo works.
        if (_state.value.pendingConversationDeletion != null) {
            commitPendingConversationDeletion()
        }
        _state.update { it.copy(pendingConversationDeletion = id) }
        pendingConversationDeleteJob = viewModelScope.launch(backgroundDispatcher) {
            delay(4.seconds)
            dataRepository.deleteConversation(id)
            _state.update {
                if (it.pendingConversationDeletion == id) it.copy(pendingConversationDeletion = null) else it
            }
        }
    }

    private fun renameConversation(id: String, title: String) {
        viewModelScope.launch(backgroundDispatcher) {
            dataRepository.renameConversation(id, title)
        }
    }

    private fun togglePinConversation(id: String) {
        viewModelScope.launch(backgroundDispatcher) {
            dataRepository.togglePinConversation(id)
        }
    }

    private fun undoDeleteConversation() {
        pendingConversationDeleteJob?.cancel()
        pendingConversationDeleteJob = null
        _state.update { it.copy(pendingConversationDeletion = null) }
    }

    private fun commitPendingConversationDeletion() {
        pendingConversationDeleteJob?.cancel()
        pendingConversationDeleteJob = null
        val pendingId = _state.value.pendingConversationDeletion ?: return
        _state.update { it.copy(pendingConversationDeletion = null) }
        viewModelScope.launch(backgroundDispatcher) {
            dataRepository.deleteConversation(pendingId)
        }
    }

    override fun onCleared() {
        // viewModelScope is cancelled before onCleared runs; use a background coroutine
        // with its own scope so the pending deletion actually completes.
        if (_state.value.pendingConversationDeletion != null) {
            val pendingId = _state.value.pendingConversationDeletion!!
            CoroutineScope(backgroundDispatcher).launch {
                dataRepository.deleteConversation(pendingId)
            }
        }
        // The scheduler lives longer than this ViewModel (it's a singleton driving the
        // Android foreground service). Reset the predicate so the daemon path keeps
        // running without a stale reference to a dead state flow. The foreground-visible
        // signal (`appInForeground`) is tracked separately via `ProcessLifecycleOwner`
        // on Android — ViewModel lifecycle is too narrow (survives backgrounding).
        taskScheduler.isLoadingCheck = { false }
        super.onCleared()
    }

    private fun clearUnreadHeartbeat() {
        dataRepository.clearUnreadHeartbeat()
    }

    private fun sendSmsDraft(draftId: String) {
        viewModelScope.launch(backgroundDispatcher) {
            dataRepository.sendSmsDraft(draftId)
        }
    }

    private fun discardSmsDraft(draftId: String) {
        viewModelScope.launch(backgroundDispatcher) {
            dataRepository.discardSmsDraft(draftId)
        }
    }

    /**
     * Dashboard Design card (Plan 6): a fresh chat whose composer opens with a
     * design-starter prompt. The composer is the whole Design destination
     * contract — there is no separate design workspace yet.
     */
    fun startDesignChat(prompt: String) {
        startNewChat(composerPrefill = prompt)
    }

    private fun startNewChat(composerPrefill: String? = null) {
        currentJob?.cancel()
        currentJob = null
        dataRepository.startNewChat()
        _state.update {
            it.copy(
                error = null,
                showFreeProviderSuggestions = false,
                isLoading = false,
                composerPrefill = composerPrefill,
                followUpSuggestions = persistentListOf(),
            )
        }
    }

    private fun consumeComposerPrefill() {
        _state.update { it.copy(composerPrefill = null) }
    }

    /**
     * Turns the interactive-UI mode on or off for the current chat.
     *
     * The mode is a property of the chat, not a separate screen: the conversation keeps its
     * messages and an interactive answer renders inline with the rest of the timeline. The
     * stored flag decides which system prompt the next request uses.
     */
    private fun setInteractiveMode(enabled: Boolean) {
        if (_state.value.isInteractiveMode == enabled) return
        dataRepository.setInteractiveMode(enabled)
        _state.update {
            it.copy(
                isInteractiveMode = enabled,
                error = null,
                showFreeProviderSuggestions = false,
            )
        }
    }

    private fun resubmit(messageId: String, event: String, data: Map<String, String>) {
        if (_state.value.isLoading) return
        dataRepository.truncateFrom(messageId)
        submitUiCallback(event, data)
    }

    private fun goBackInteractiveMode() {
        val userCount = dataRepository.chatHistory.value.count { it.role == History.Role.USER }
        if (userCount <= 1) {
            // Go back to initial prompt — clear history but stay in interactive mode
            dataRepository.clearHistory()
        } else {
            dataRepository.popLastExchange()
        }
    }

    fun refreshSettings() {
        updateAvailableServices()
        viewModelScope.launch(backgroundDispatcher) {
            dataRepository.restoreCurrentConversation()
            presetInteractiveModeForCurrentConversation()
        }
    }

    /**
     * Resolves the interactive mode flag from the currently-loaded conversation, or — when
     * there is no loaded conversation (new empty chat) — falls back to the persisted flag.
     */
    private fun presetInteractiveModeForCurrentConversation() {
        val currentId = dataRepository.currentConversationId.value
        val conversation = dataRepository.savedConversations.value.find { it.id == currentId }
        val isInteractive = if (conversation != null) {
            conversation.type == Conversation.TYPE_INTERACTIVE
        } else {
            dataRepository.isInteractiveModeActive()
        }
        dataRepository.setInteractiveMode(isInteractive)
        _state.update { it.copy(isInteractiveMode = isInteractive) }
    }
    private fun acceptFileChange(proposalId: String) = performFileReviewAction { fileReviewController.accept(proposalId) }
    private fun rejectFileChange(proposalId: String) = performFileReviewAction { fileReviewController.reject(proposalId) }
    private fun rollbackFileChange(proposalId: String) = performFileReviewAction { fileReviewController.rollback(proposalId) }

    private fun performFileReviewAction(action: suspend () -> FileReviewActionResult) {
        viewModelScope.launch(backgroundDispatcher) {
            val result = runCatching { action() }.getOrElse {
                FileReviewActionResult.Failure(it.message ?: "File Review action failed")
            }
            val message = when (result) {
                is FileReviewActionResult.Success -> "${result.proposal.path}: ${result.proposal.state.name.lowercase().replace('_', ' ')}"
                is FileReviewActionResult.Conflict -> "${result.proposal.path}: conflict — ${result.reason}"
                is FileReviewActionResult.Failure -> result.reason
                FileReviewActionResult.NotFound -> "File Review proposal was not found"
            }
            _state.update { it.copy(fileReviewActionMessage = message) }
        }
    }

}
