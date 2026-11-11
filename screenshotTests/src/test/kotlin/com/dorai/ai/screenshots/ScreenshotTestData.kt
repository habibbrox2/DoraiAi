package com.dorai.ai.screenshots

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.dorai.ai.TerminalLine
import com.dorai.ai.data.MemoryEntry
import com.dorai.ai.data.Service
import com.dorai.ai.getPlatformToolDefinitions
import com.dorai.ai.linux.LinuxDistro
import com.dorai.ai.ui.chat.ChatActions
import com.dorai.ai.ui.chat.ChatConversationSummary
import com.dorai.ai.ui.chat.ChatMessageItem
import com.dorai.ai.ui.chat.ChatUiState
import com.dorai.ai.ui.components.v2.DynamicUiModel
import com.dorai.ai.ui.components.v2.DynamicUiOption
import com.dorai.ai.ui.components.v2.ToolExecutionModel
import com.dorai.ai.ui.components.v2.ToolExecutionState
import com.dorai.ai.ui.settings.ConfiguredServiceEntry
import com.dorai.ai.ui.settings.ConnectionStatus
import com.dorai.ai.ui.settings.McpConnectionStatus
import com.dorai.ai.ui.settings.McpServerUiState
import com.dorai.ai.ui.settings.SandboxUiState
import com.dorai.ai.ui.settings.SettingsModel
import com.dorai.ai.ui.settings.SettingsTab
import com.dorai.ai.ui.settings.SettingsUiState
import com.dorai.ai.ui.workspace.WorkspaceProjectItem
import com.dorai.ai.ui.workspace.WorkspaceUiState
import com.dorai.ai.workspace.WorkspaceEnvironmentState
import com.dorai.ai.workspace.WorkspaceState
import com.dorai.ai.workspace.WorkspaceStep
import com.dorai.ai.workspace.WorkspaceTerminalSession
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object ScreenshotTestData {

    val noOpChatActions = ChatActions(
        ask = {},
        toggleSpeechOutput = {},
        retry = {},
        clearHistory = {},
        setIsSpeaking = { _, _ -> },
        addFile = { _ -> },
        removeFile = { _ -> },
        startNewChat = { },
        regenerate = {},
        cancel = {},
        selectService = {},
        selectModel = {},
        loadModels = {},
        loadConversation = {},
        deleteConversation = {},
        renameConversation = { _, _ -> },
        togglePinConversation = {},
        clearSnackbar = {},
        clearUnreadHeartbeat = {},
        undoDeleteConversation = { },
        submitUiCallback = { _, _ -> },
        resubmit = { _, _, _ -> },
        setInteractiveMode = { },
        goBackInteractiveMode = { },
        sendSmsDraft = {},
        discardSmsDraft = {},
        consumeComposerPrefill = {},
    )

    val chatEmptyState = ChatUiState(
        actions = noOpChatActions,
        history = persistentListOf(),
        showPrivacyInfo = false,
    )

    private val recipeContent =
        "```dorai-ui\n" +
            "{\"type\":\"column\",\"spacing\":8,\"children\":[" +
            "{\"type\":\"text\",\"value\":\"Cacio e Pepe\",\"style\":\"headline\",\"bold\":true}," +
            "{\"type\":\"row\",\"spacing\":8,\"children\":[{\"type\":\"badge\",\"value\":\"\u23f1 20 min\",\"color\":\"secondary\"},{\"type\":\"badge\",\"value\":\"\uD83C\uDF7D 2 servings\",\"color\":\"secondary\"},{\"type\":\"badge\",\"value\":\"\u2b50 4.9/5\",\"color\":\"primary\"}]}," +
            "{\"type\":\"image\",\"url\":\"resource://cacio_e_pepe.png\",\"aspectRatio\":1.5}," +
            "{\"type\":\"text\",\"value\":\"Ingredients\",\"style\":\"title\"}," +
            "{\"type\":\"list\",\"ordered\":false,\"items\":[" +
            "{\"type\":\"text\",\"value\":\"200g tonnarelli or spaghetti\"}," +
            "{\"type\":\"text\",\"value\":\"150g Pecorino Romano, finely grated\"}," +
            "{\"type\":\"text\",\"value\":\"2 tsp black peppercorns\"}," +
            "{\"type\":\"text\",\"value\":\"Salt for pasta water\"}" +
            "]}," +
            "{\"type\":\"divider\"}," +
            "{\"type\":\"text\",\"value\":\"Instructions\",\"style\":\"title\"}," +
            "{\"type\":\"accordion\",\"title\":\"Step 1: Toast pepper & cook pasta\",\"children\":[{\"type\":\"text\",\"value\":\"Toast peppercorns in a dry pan until fragrant, crush coarsely. Boil pasta until al dente, reserve pasta water.\",\"style\":\"body\"}]}," +
            "{\"type\":\"accordion\",\"title\":\"Step 2: Make the sauce\",\"children\":[{\"type\":\"text\",\"value\":\"Mix grated Pecorino with warm pasta water to form a smooth cream.\",\"style\":\"body\"}]}," +
            "{\"type\":\"accordion\",\"title\":\"Step 3: Combine\",\"children\":[{\"type\":\"text\",\"value\":\"Toss hot pasta with pepper off heat. Add Pecorino cream and toss until silky.\",\"style\":\"body\"}]}" +
            "]}\n" +
            "```"

    val freeConnected = SettingsUiState(
        currentTab = SettingsTab.Services,
        configuredServices = persistentListOf(
            ConfiguredServiceEntry(
                instanceId = "moonshot",
                service = Service.Moonshot,
                connectionStatus = ConnectionStatus.Connected,
                apiKey = "sk-••••••••••••••••••••••••••••••••••••",
                selectedModel = SettingsModel(id = "kimi-k2.5", subtitle = "Kimi K2.5", isSelected = true, displayName = "Kimi K2.5"),
            ),
            ConfiguredServiceEntry(
                instanceId = "anthropic",
                service = Service.Anthropic,
                connectionStatus = ConnectionStatus.Connected,
                apiKey = "sk-ant-••••••••••••••••••••••••••••••••",
                selectedModel = SettingsModel(id = "claude-opus-4-6", subtitle = "Claude Opus 4.6", isSelected = true, displayName = "Claude Opus 4.6"),
            ),
        ),
        availableServicesToAdd = persistentListOf(Service.OpenAI, Service.DeepSeek, Service.Mistral),
    )

    val settingsAgent = SettingsUiState(
        currentTab = SettingsTab.Agent,
        soulText = "",
        isMemoryEnabled = true,
        memories = persistentListOf(
            MemoryEntry(
                key = "user_name",
                content = "The user's name is Simon",
                createdAt = 1709300000000,
                updatedAt = 1709300000000,
            ),
            MemoryEntry(
                key = "preferred_language",
                content = "Prefers Kotlin for app development",
                createdAt = 1709310000000,
                updatedAt = 1709310000000,
            ),
        ),
    )

    val sandboxState = SandboxUiState(
        showSandbox = true,
        sandboxInstalled = true,
        sandboxReady = true,
        sandboxDiskUsageMB = 578,
        sandboxPackagesInstalled = true,
        isSandboxEnabled = true,
        installedDistros = setOf(LinuxDistro.DEBIAN),
    )

    private val fastfetchOutput =
        "       .hddddddddddddddddddddddh.\n" +
            "      :dddddddddddddddddddddddddd:\n" +
            "     /dddddddddddddddddddddddddddd/\n" +
            "    +dddddddddddddddddddddddddddddd+\n" +
            "  \u0060sdddddddddddddddddddddddddddddddds\u0060\n" +
            " \u0060ydddddddddddd++hdddddddddddddddddddy\u0060\n" +
            ".hddddddddddd+\u0060  \u0060+ddddh:-sdddddddddddh.\n" +
            "hdddddddddd+\u0060      \u0060+y:    .sddddddddddh\n" +
            "ddddddddh+\u0060   \u0060//\u0060   \u0060.\u0060     -sddddddddd\n" +
            "ddddddh+\u0060   \u0060/hddh/\u0060   \u0060:s-    -sddddddd\n" +
            "ddddh+\u0060   \u0060/+/dddddh/\u0060   \u0060+s-    -sddddd\n" +
            "ddd+\u0060   \u0060/o\u0060 :dddddddh/\u0060   \u0060oy-    .yddd\n" +
            "hdddyo+ohddyosdddddddddho+oydddy++ohdddh\n" +
            ".hddddddddddddddddddddddddddddddddddddh.\n" +
            " \u0060yddddddddddddddddddddddddddddddddddy\u0060\n" +
            "  \u0060sdddddddddddddddddddddddddddddddds\u0060\n" +
            "    +dddddddddddddddddddddddddddddd+\n" +
            "     /dddddddddddddddddddddddddddd/\n" +
            "      :dddddddddddddddddddddddddd:\n" +
            "       .hddddddddddddddddddddddh.root@localhost\n" +
            "--------------\n" +
            "OS: Alpine Linux v3.22 aarch64\n" +
            "Kernel: Linux 6.1.145-android14-11-gfa1d6308d1fe-ab14691759\n" +
            "Uptime: 3 days, 12 hours, 51 mins\n" +
            "Packages: 65 (apk)\n" +
            "Shell: libproot.so\n" +
            "Terminal: iredandroid.kai\n" +
            "CPU: Cortex-A520*4 + Cortex-A720*3 + Cortex-X4 (8) @ 3.10 GHz\n" +
            "Memory: 6.75 GiB / 7.39 GiB (91%)\n" +
            "\u001B[40m   \u001B[41m   \u001B[42m   \u001B[43m   \u001B[44m   " +
            "\u001B[45m   \u001B[46m   \u001B[47m   \u001B[0m\n" +
            "\u001B[100m   \u001B[101m   \u001B[102m   \u001B[103m   \u001B[104m   " +
            "\u001B[105m   \u001B[106m   \u001B[107m   \u001B[0m"

    val sandboxTerminalLines = persistentListOf(
        TerminalLine.Command("fastfetch"),
        TerminalLine.Output(fastfetchOutput),
    )

    val settingsTools = SettingsUiState(
        currentTab = SettingsTab.Tools,
        tools = getPlatformToolDefinitions().toImmutableList(),
        mcpServers = persistentListOf(
            McpServerUiState(
                id = "context7",
                name = "Context7",
                url = "https://context7.liam.sh/mcp",
                isEnabled = true,
                connectionStatus = McpConnectionStatus.Connected,
                tools = persistentListOf(),
            ),
            McpServerUiState(
                id = "manifold_markets",
                name = "Manifold Markets",
                url = "https://api.manifold.markets/v0/mcp",
                isEnabled = true,
                connectionStatus = McpConnectionStatus.Connected,
                tools = persistentListOf(),
            ),
        ),
    )

    // --- Tool Execution Card parameters ---

    data class ToolExecutionCardParams(
        val toolName: String,
        val arguments: String?,
        val result: String?,
        val isRunning: Boolean,
        val isError: Boolean,
        val initiallyExpanded: Boolean,
    )

    val toolExecutionRunningParams = ToolExecutionCardParams(
        toolName = "web_search",
        arguments = "{\"query\":\"DORAI AI premium UI design\"}",
        result = null,
        isRunning = true,
        isError = false,
        initiallyExpanded = false,
    )

    val toolExecutionCompletedParams = ToolExecutionCardParams(
        toolName = "file_read",
        arguments = "{\"path\":\"/home/user/README.md\"}",
        result = "# DORAI AI\n\nPremium AI Agent Workspace\n\nBuilt with Compose Multiplatform.",
        isRunning = false,
        isError = false,
        initiallyExpanded = false,
    )

    val toolExecutionFailedParams = ToolExecutionCardParams(
        toolName = "execute_shell_command",
        arguments = "{\"command\":\"rm -rf /important\"}",
        result = null,
        isRunning = false,
        isError = true,
        initiallyExpanded = false,
    )

    val toolExecutionExpandedParams = ToolExecutionCardParams(
        toolName = "web_search",
        arguments = "{\"query\":\"long running research query\"}",
        result = "Research results...",
        isRunning = false,
        isError = false,
        initiallyExpanded = true,
    )

    val toolExecutionLongOutputParams = ToolExecutionCardParams(
        toolName = "execute_shell_command",
        arguments = "{\"command\":\"cat /var/log/syslog\"}",
        result = (1..50).joinToString("\n") { "line $it: system log entry with some content here" },
        isRunning = false,
        isError = false,
        initiallyExpanded = true,
    )

    // --- Tool Approval Card parameters ---

    data class ToolConfirmationCardParams(
        val toolName: String,
        val toolCallId: String,
        val arguments: String,
    )

    val toolApprovalParams = ToolConfirmationCardParams(
        toolName = "fetch_url",
        toolCallId = "call_123",
        arguments = "{\"url\":\"https://api.example.com/data\"}",
    )

    // --- Shell Execution Card parameters ---

    data class ShellExecutionCardParams(
        val command: String,
        val workingDirectory: String?,
        val stdout: String?,
        val stderr: String?,
        val exitCode: Int?,
        val isRunning: Boolean,
    )

    val shellExecutionRunningParams = ShellExecutionCardParams(
        command = "npm install",
        workingDirectory = "/home/user/project",
        stdout = null,
        stderr = null,
        exitCode = null,
        isRunning = true,
    )

    val shellExecutionCompletedParams = ShellExecutionCardParams(
        command = "fastfetch",
        workingDirectory = "~",
        stdout = "OS: Alpine Linux v3.22 aarch64\nKernel: Linux 6.1.145\nPackages: 65 (apk)",
        stderr = null,
        exitCode = 0,
        isRunning = false,
    )

    val shellExecutionFailedParams = ShellExecutionCardParams(
        command = "npm install",
        workingDirectory = "/home/user/project",
        stdout = null,
        stderr = "ERR! code ENOENT\nERR! syscall open\nERR! path /home/user/project/package.json\nERR! errno -2\nERR! syscall open",
        exitCode = 1,
        isRunning = false,
    )

    // --- Settings tab states ---

    val settingsGeneralState = SettingsUiState(
        currentTab = SettingsTab.General,
    )

    val settingsAppearanceState = SettingsUiState(
        currentTab = SettingsTab.Appearance,
    )

    val settingsAccountState = SettingsUiState(
        currentTab = SettingsTab.Account,
    )

    // --- Build Workspace states ---

    val buildStateNotInstalled = WorkspaceUiState(
        build = WorkspaceState(
            environment = WorkspaceEnvironmentState.NotInstalled,
            projects = persistentListOf(),
        ),
    )

    val buildStateInstalling = WorkspaceUiState(
        build = WorkspaceState(
            environment = WorkspaceEnvironmentState.Installing(WorkspaceStep.Download, 0.45f),
            projects = persistentListOf(),
        ),
    )

    val buildStateReadyEmpty = WorkspaceUiState(
        build = WorkspaceState(
            environment = WorkspaceEnvironmentState.Ready,
            projects = persistentListOf(),
        ),
    )

    val buildStateReadyWithProjects = WorkspaceUiState(
        build = WorkspaceState(
            environment = WorkspaceEnvironmentState.Ready,
            projects = persistentListOf("kai-project", "research-bot"),
            sessions = persistentListOf(
                WorkspaceTerminalSession(id = "sess-1", project = "kai-project", agentId = null, number = 1),
                WorkspaceTerminalSession(id = "sess-2", project = "kai-project", agentId = "claude-code", number = 2),
            ),
            activeSessionId = "sess-1",
        ),
    )

    val buildStateWithActiveProject = WorkspaceUiState(
        build = WorkspaceState(
            environment = WorkspaceEnvironmentState.Ready,
            projects = persistentListOf("kai-project"),
            sessions = persistentListOf(),
            activeSessionId = null,
        ),
        openProject = "kai-project",
    )

    // --- UI v2 chat fixtures ---

    val v2Conversations = listOf(
        ChatConversationSummary(
            id = "1",
            title = "ডার্ক-মোড সুইচ",
            preview = "সেটিংস স্ক্রিনে ডার্ক-মোড সুইচ যোগ করো।",
            isToday = true,
        ),
        ChatConversationSummary(
            id = "2",
            title = "কোন ডেটাবেস নেব",
            preview = "লোকাল স্টোরেজে কোন ডেটাবেস ব্যবহার করব?",
            isToday = true,
        ),
        ChatConversationSummary(
            id = "3",
            title = "ব্যর্থ টেস্ট ঠিক করা",
            preview = "টেস্ট ফেইল করছে, দেখো তো।",
            isToday = false,
        ),
    )

    val v2Messages = listOf(
        ChatMessageItem(
            id = "1",
            isUser = true,
            text = "সেটিংস স্ক্রিনে ডার্ক-মোড সুইচ যোগ করো।",
        ),
        ChatMessageItem(
            id = "2",
            isUser = false,
            text = "আগে থিমের কোড পড়ছি, তারপর সুইচটি যোগ করব।",
            modelName = "Google Gemini",
            isModelLocal = false,
            toolExecutions = listOf(
                ToolExecutionModel(
                    toolName = "read_file",
                    argsSummary = "ThemeState.kt",
                    state = ToolExecutionState.Completed,
                    outputText = "42 lines",
                    durationText = "0.4s",
                ),
                ToolExecutionModel(
                    toolName = "search",
                    argsSummary = "\"isDarkTheme\"",
                    state = ToolExecutionState.Completed,
                    outputText = "3 matches",
                    durationText = "0.9s",
                ),
                ToolExecutionModel(
                    toolName = "edit_file",
                    argsSummary = "SettingsScreen.kt",
                    state = ToolExecutionState.PendingApproval,
                    diffText = "@@ SettingsScreen.kt\n- SwitchRow(\"Dark mode\", false) {}\n+ SwitchRow(\"Dark mode\", state.dark, state::toggleDark)",
                ),
            ),
            dynamicUi = DynamicUiModel(
                title = "Kotlin Multiplatform-এ কাজ করে এমন তিনটি অপশন নিচে। একটি বেছে নিন:",
                options = listOf(
                    DynamicUiOption("sq", "SQLDelight", "SQL আগে লিখুন, টাইপ-সেফ কোড তৈরি হবে"),
                    DynamicUiOption("room", "Room", "Android-এ পরিচিত, KMP সমর্থন বাড়ছে"),
                    DynamicUiOption("kv", "Key-value store", "সহজ সেটিংস ও ক্যাশের জন্য"),
                ),
                selectedOptionId = "sq",
                isLocked = false,
            ),
        ),
    )

    val v2DynamicUiMessages = listOf(
        ChatMessageItem(
            id = "1",
            isUser = true,
            text = "suggest Cacio e Pepe recipe for 2 servings",
        ),
        ChatMessageItem(
            id = "2",
            isUser = false,
            text = recipeContent,
            modelName = "Google Gemini",
            isModelLocal = false,
        ),
    )

    // --- Model selector models ---

    val modelSelectorModels = persistentListOf(
        SettingsModel(id = "kimi-k2.5", subtitle = "Kimi K2.5", displayName = "Kimi K2.5", isSelected = true, isFreeTier = false, arenaScore = 1280, contextWindow = 131072),
        SettingsModel(id = "claude-opus-4-6", subtitle = "Claude Opus 4.6", displayName = "Claude Opus 4.6", isSelected = false, isFreeTier = false, arenaScore = 1250, contextWindow = 200000),
        SettingsModel(id = "gemini-2.5-pro", subtitle = "Gemini 2.5 Pro", displayName = "Gemini 2.5 Pro", isSelected = false, isFreeTier = true, arenaScore = 1220, contextWindow = 1048576),
    )

    val modelSelectorEmptyModels = persistentListOf<SettingsModel>()

    val modelSelectorScoreBandModels = persistentListOf(
        SettingsModel(id = "top-tier-model", subtitle = "Top Tier", displayName = "Top Tier Model", isFreeTier = false, arenaScore = 1400, contextWindow = 128000),
        SettingsModel(id = "mid-tier-model", subtitle = "Mid Tier", displayName = "Mid Tier Model", isFreeTier = false, arenaScore = 1100, contextWindow = 64000),
        SettingsModel(id = "entry-model", subtitle = "Entry Level", displayName = "Entry Model", isFreeTier = true, arenaScore = 800, contextWindow = 32000),
    )

    // --- Workspace v2 preview fixtures (projects list + agents + sandbox section) ---

    val workspaceV2Projects = listOf(
        WorkspaceProjectItem(
            name = "dorai-notes",
            path = "/root/projects/dorai-notes",
            branch = "main",
            lastUpdatedText = "2026-11-01",
            changesCount = 3,
        ),
        WorkspaceProjectItem(
            name = "kai-project",
            path = "/root/projects/kai-project",
            branch = "main",
            changesCount = 1,
        ),
        WorkspaceProjectItem(
            name = "research-bot",
            path = "/root/projects/research-bot",
        ),
    )

    // --- Sandbox states ---

    val sandboxEmptyState = SandboxUiState(
        showSandbox = true,
        sandboxInstalled = false,
        sandboxReady = false,
        sandboxDiskUsageMB = 0,
        sandboxPackagesInstalled = false,
        isSandboxEnabled = false,
        installedDistros = emptySet(),
    )

    val sandboxLoadingState = SandboxUiState(
        showSandbox = true,
        sandboxInstalled = false,
        sandboxReady = false,
        sandboxDiskUsageMB = 0,
        sandboxPackagesInstalled = false,
        isSandboxEnabled = false,
        installedDistros = emptySet(),
    )

    // --- Localized data loading for StoreScreenshotTest ---

    private fun loadJson(locale: String): JsonObject {
        val stream = ScreenshotTestData::class.java.getResourceAsStream("/screenshot-data/$locale.json")
            ?: error("Missing screenshot data for locale: $locale")
        val text = stream.bufferedReader().use { it.readText() }
        return Json.parseToJsonElement(text).jsonObject
    }

    private fun localizedV2MessagesFrom(locale: String, key: String): List<ChatMessageItem> {
        val chat = loadJson(locale)[key]!!.jsonObject
        return listOf(
            ChatMessageItem(
                id = "1",
                isUser = true,
                text = chat["userMessage"]!!.jsonPrimitive.content,
            ),
            ChatMessageItem(
                id = "2",
                isUser = false,
                text = chat["assistantMessage"]!!.jsonPrimitive.content,
                modelName = "Google Gemini",
                isModelLocal = false,
            ),
        )
    }

    fun localizedV2Messages(locale: String): List<ChatMessageItem> = localizedV2MessagesFrom(locale, "chatWithMessages")

    fun localizedV2DynamicUiMessages(locale: String): List<ChatMessageItem> = localizedV2MessagesFrom(locale, "chatWithDynamicUi")
}
