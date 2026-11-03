package com.inspiredandroid.kai.screenshots

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.inspiredandroid.kai.TerminalLine
import com.inspiredandroid.kai.workspace.BuildEnvironmentState
import com.inspiredandroid.kai.workspace.BuildStep
import com.inspiredandroid.kai.workspace.BuildTerminalSession
import com.inspiredandroid.kai.workspace.WorkspaceState
import com.inspiredandroid.kai.data.MemoryEntry
import com.inspiredandroid.kai.data.Service
import com.inspiredandroid.kai.getPlatformToolDefinitions
import com.inspiredandroid.kai.linux.LinuxDistro
import com.inspiredandroid.kai.ui.workspace.WorkspaceUiState
import com.inspiredandroid.kai.ui.chat.ChatActions
import com.inspiredandroid.kai.ui.chat.ChatUiState
import com.inspiredandroid.kai.ui.chat.History
import com.inspiredandroid.kai.ui.chat.activities.ActivityDetails
import com.inspiredandroid.kai.ui.chat.activities.ActivityStatus
import com.inspiredandroid.kai.ui.chat.activities.ActivityType
import com.inspiredandroid.kai.ui.chat.activities.AgentActivity
import com.inspiredandroid.kai.ui.settings.ConfiguredServiceEntry
import com.inspiredandroid.kai.ui.settings.ConnectionStatus
import com.inspiredandroid.kai.ui.settings.McpConnectionStatus
import com.inspiredandroid.kai.ui.settings.McpServerUiState
import com.inspiredandroid.kai.ui.settings.SandboxUiState
import com.inspiredandroid.kai.ui.settings.SettingsModel
import com.inspiredandroid.kai.ui.settings.SettingsTab
import com.inspiredandroid.kai.ui.settings.SettingsUiState
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

    private val survivalGameContent =
        "```kai-ui\n" +
            "{\"type\":\"column\",\"spacing\":8,\"children\":[" +
            "{\"type\":\"text\",\"value\":\"\u2694\ufe0f The Goblin Tunnels\",\"style\":\"headline\",\"bold\":true}," +
            "{\"type\":\"text\",\"value\":\"Chapter 1 \u2022 The Beginning\",\"style\":\"caption\",\"color\":\"secondary\"}," +
            "{\"type\":\"row\",\"spacing\":16,\"children\":[" +
            "{\"type\":\"stat\",\"value\":\"20/20\",\"label\":\"HP\"}," +
            "{\"type\":\"stat\",\"value\":\"Lv 1\",\"label\":\"Level\"}," +
            "{\"type\":\"stat\",\"value\":\"50g\",\"label\":\"Gold\"}," +
            "{\"type\":\"stat\",\"value\":\"2\",\"label\":\"DEF\"}" +
            "]}," +
            "{\"type\":\"image\",\"url\":\"resource://orc_survival.png\"}," +
            "{\"type\":\"text\",\"value\":\"The tunnel forks. To the left, faint firelight flickers. To the right, silence \u2014 and a cold draft that makes your torch sputter.\",\"style\":\"body\"}," +
            "{\"type\":\"divider\"}," +
            "{\"type\":\"alert\",\"severity\":\"warning\",\"message\":\"\uD83D\uDC7A Two orcs block the left passage! They haven't noticed you yet.\"}," +
            "{\"type\":\"spacer\",\"height\":4}," +
            "{\"type\":\"button\",\"label\":\"\u2694\ufe0f Attack with sword\",\"variant\":\"filled\",\"action\":{\"type\":\"callback\",\"event\":\"attack\"}}," +
            "{\"type\":\"button\",\"label\":\"\uD83E\uDD2B Sneak past in the shadows\",\"variant\":\"outlined\",\"action\":{\"type\":\"callback\",\"event\":\"sneak\"}}" +
            "]}\n" +
            "```"

    val chatWithMessages = ChatUiState(
        actions = noOpChatActions,
        history = persistentListOf(
            History(
                id = "1",
                role = History.Role.USER,
                content = "lets play a game",
            ),
            History(
                id = "2",
                role = History.Role.ASSISTANT,
                content = survivalGameContent,
            ),
        ),
    )

    private val recipeContent =
        "```kai-ui\n" +
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

    val chatWithDynamicUi = ChatUiState(
        actions = noOpChatActions,
        history = persistentListOf(
            History(
                id = "1",
                role = History.Role.USER,
                content = "suggest Cacio e Pepe recipe for 2 servings",
            ),
            History(
                id = "2",
                role = History.Role.ASSISTANT,
                content = recipeContent,
            ),
        ),
    )

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

    val settingsSandbox = SettingsUiState(
        currentTab = SettingsTab.Sandbox,
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
        arguments = "{\"query\":\"Brox Kai 2027 premium UI design\"}",
        result = null,
        isRunning = true,
        isError = false,
        initiallyExpanded = false,
    )

    val toolExecutionCompletedParams = ToolExecutionCardParams(
        toolName = "file_read",
        arguments = "{\"path\":\"/home/user/README.md\"}",
        result = "# Brox Kai\n\nPremium AI Agent Workspace\n\nBuilt with Compose Multiplatform.",
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

    val settingsDataState = SettingsUiState(
        currentTab = SettingsTab.Data,
    )

    val settingsIntegrationsState = SettingsUiState(
        currentTab = SettingsTab.Integrations,
    )

    // --- Build Workspace states ---

    val buildStateNotInstalled = WorkspaceUiState(
        build = WorkspaceState(
            environment = BuildEnvironmentState.NotInstalled,
            projects = persistentListOf(),
        ),
    )

    val buildStateInstalling = WorkspaceUiState(
        build = WorkspaceState(
            environment = BuildEnvironmentState.Installing(BuildStep.Download, 0.45f),
            projects = persistentListOf(),
        ),
    )

    val buildStateReadyEmpty = WorkspaceUiState(
        build = WorkspaceState(
            environment = BuildEnvironmentState.Ready,
            projects = persistentListOf(),
        ),
    )

    val buildStateReadyWithProjects = WorkspaceUiState(
        build = WorkspaceState(
            environment = BuildEnvironmentState.Ready,
            projects = persistentListOf("kai-project", "research-bot"),
            sessions = persistentListOf(
                BuildTerminalSession(id = "sess-1", project = "kai-project", agentId = null, number = 1),
                BuildTerminalSession(id = "sess-2", project = "kai-project", agentId = "claude-code", number = 2),
            ),
            activeSessionId = "sess-1",
        ),
    )

    val buildStateWithActiveProject = WorkspaceUiState(
        build = WorkspaceState(
            environment = BuildEnvironmentState.Ready,
            projects = persistentListOf("kai-project"),
            sessions = persistentListOf(),
            activeSessionId = null,
        ),
        openProject = "kai-project",
    )

    // --- Agent Activity states ---

    val agentActivitiesMixed = persistentListOf(
        AgentActivity(
            id = "act-1",
            type = ActivityType.AGENT_STARTED,
            title = "Research quantum computing",
            status = ActivityStatus.COMPLETED,
            details = ActivityDetails.Generic(content = "Initialized agent with quantum computing research goal."),
        ),
        AgentActivity(
            id = "act-2",
            type = ActivityType.TOOL_CALL,
            title = "web_search",
            status = ActivityStatus.COMPLETED,
            details = ActivityDetails.ToolCall(
                toolName = "web_search",
                toolDisplayName = "Web Search",
                arguments = "{\"query\":\"quantum computing 2027\"}",
                result = "Quantum computing is advancing rapidly with error-corrected qubits...",
            ),
        ),
        AgentActivity(
            id = "act-3",
            type = ActivityType.SHELL_COMMAND,
            title = "npm install",
            status = ActivityStatus.RUNNING,
            details = ActivityDetails.ShellCommand(
                command = "npm install",
                workingDirectory = "/home/user/project",
            ),
        ),
        AgentActivity(
            id = "act-4",
            type = ActivityType.FILE_WRITE,
            title = "Write README.md",
            status = ActivityStatus.FAILED,
            details = ActivityDetails.FileOperation(
                path = "/home/user/project/README.md",
                operation = "Write",
                changeSummary = "Added project documentation",
            ),
        ),
        AgentActivity(
            id = "act-5",
            type = ActivityType.PERMISSION_REQUEST,
            title = "File system access",
            status = ActivityStatus.PENDING,
            details = ActivityDetails.Permission(
                action = "read",
                target = "/home/user/project",
                scope = "project files",
            ),
        ),
    )

    val agentActivitiesToolCall = persistentListOf(
        AgentActivity(
            id = "act-tool",
            type = ActivityType.TOOL_CALL,
            title = "file_read",
            status = ActivityStatus.COMPLETED,
            details = ActivityDetails.ToolCall(
                toolName = "file_read",
                toolDisplayName = "Read File",
                arguments = "{\"path\":\"/home/user/README.md\"}",
                result = "# Brox Kai\n\nPremium AI Agent Workspace.",
            ),
        ),
    )

    val agentActivitiesShellCommand = persistentListOf(
        AgentActivity(
            id = "act-shell",
            type = ActivityType.SHELL_COMMAND,
            title = "fastfetch",
            status = ActivityStatus.COMPLETED,
            details = ActivityDetails.ShellCommand(
                command = "fastfetch",
                workingDirectory = "~",
                stdout = "OS: Alpine Linux v3.22 aarch64\nKernel: Linux 6.1.145\nPackages: 65 (apk)",
                stderr = null,
                exitCode = 0,
            ),
        ),
    )

    val agentActivitiesFileOperation = persistentListOf(
        AgentActivity(
            id = "act-file",
            type = ActivityType.FILE_EDIT,
            title = "Edit config.yaml",
            status = ActivityStatus.RUNNING,
            details = ActivityDetails.FileOperation(
                path = "/home/user/project/config.yaml",
                operation = "Edit",
                changeSummary = "Updated API endpoint",
                oldContent = "endpoint: https://api.example.com/v1",
                newContent = "endpoint: https://api.example.com/v2",
            ),
        ),
    )

    val agentActivitiesPermission = persistentListOf(
        AgentActivity(
            id = "act-perm",
            type = ActivityType.PERMISSION_REQUEST,
            title = "Access microphone",
            status = ActivityStatus.PENDING,
            details = ActivityDetails.Permission(
                action = "record",
                target = "microphone",
                scope = "voice input",
                allowed = null,
            ),
        ),
    )

    // --- Composer states ---

    val composerEmptyText = TextFieldValue(text = "", selection = TextRange(0, 0))

    val composerDraftText = TextFieldValue(
        text = "Explain quantum computing in simple terms",
        selection = TextRange(43, 43),
    )

    val composerAvailableModels = persistentListOf(
        SettingsModel(id = "kimi-k2.5", subtitle = "Kimi K2.5", displayName = "Kimi K2.5", isSelected = true, isFreeTier = false, arenaScore = 1280, contextWindow = 131072),
        SettingsModel(id = "claude-opus-4-6", subtitle = "Claude Opus 4.6", displayName = "Claude Opus 4.6", isSelected = false, isFreeTier = false, arenaScore = 1250, contextWindow = 200000),
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

    fun localizedChatWithMessages(locale: String): ChatUiState {
        val json = loadJson(locale)
        val chat = json["chatWithMessages"]!!.jsonObject
        return ChatUiState(
            actions = noOpChatActions,
            history = persistentListOf(
                History(
                    id = "1",
                    role = History.Role.USER,
                    content = chat["userMessage"]!!.jsonPrimitive.content,
                ),
                History(
                    id = "2",
                    role = History.Role.ASSISTANT,
                    content = chat["assistantMessage"]!!.jsonPrimitive.content,
                ),
            ),
        )
    }

    fun localizedChatWithDynamicUi(locale: String): ChatUiState {
        val json = loadJson(locale)
        val chat = json["chatWithDynamicUi"]!!.jsonObject
        return ChatUiState(
            actions = noOpChatActions,
            history = persistentListOf(
                History(
                    id = "1",
                    role = History.Role.USER,
                    content = chat["userMessage"]!!.jsonPrimitive.content,
                ),
                History(
                    id = "2",
                    role = History.Role.ASSISTANT,
                    content = chat["assistantMessage"]!!.jsonPrimitive.content,
                ),
            ),
        )
    }
}




