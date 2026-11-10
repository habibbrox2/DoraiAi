package com.dorai.ai

import com.dorai.ai.data.AppSettings
import com.dorai.ai.data.ConversationStorage
import com.dorai.ai.data.DataRepository
import com.dorai.ai.data.EmailStore
import com.dorai.ai.data.HeartbeatManager
import com.dorai.ai.data.MemoryStore
import com.dorai.ai.data.NotificationStore
import com.dorai.ai.data.RemoteDataRepository
import com.dorai.ai.data.SmsDraftStore
import com.dorai.ai.data.SmsStore
import com.dorai.ai.data.TaskScheduler
import com.dorai.ai.data.TaskStore
import com.dorai.ai.data.ToolExecutor
import com.dorai.ai.data.createConversationPersistence
import com.dorai.ai.data.runMigrations
import com.dorai.ai.email.EmailPoller
import com.dorai.ai.filereview.FileReviewController
import com.dorai.ai.filereview.NoOpFileReviewController
import com.dorai.ai.github.GitCredentialBroker
import com.dorai.ai.github.GitHubApiClient
import com.dorai.ai.github.GitHubAppConfig
import com.dorai.ai.github.GitHubDeviceFlow
import com.dorai.ai.github.GitHubIntegrationViewModel
import com.dorai.ai.github.GitHubTokenVault
import com.dorai.ai.github.createGitCredentialBroker
import com.dorai.ai.github.createGitHubTokenVault
import com.dorai.ai.inference.LocalRuntimeRegistry
import com.dorai.ai.inference.createLocalInferenceEngine
import com.dorai.ai.inference.createLocalRuntimeRegistry
import com.dorai.ai.mcp.McpServerManager
import com.dorai.ai.network.Requests
import com.dorai.ai.notifications.NotificationReader
import com.dorai.ai.skills.SkillManager
import com.dorai.ai.sms.SmsPoller
import com.dorai.ai.sms.SmsReader
import com.dorai.ai.sms.SmsSender
import com.dorai.ai.splinterlands.SplinterlandsApi
import com.dorai.ai.splinterlands.SplinterlandsBattleRunner
import com.dorai.ai.splinterlands.SplinterlandsStore
import com.dorai.ai.theme.ThemeViewModel
import com.dorai.ai.theme.data.ThemeSettingsRepository
import com.dorai.ai.tools.AppPermission
import com.dorai.ai.tools.NotificationListenerController
import com.dorai.ai.tools.PermissionController
import com.dorai.ai.ui.chat.ChatViewModel
import com.dorai.ai.ui.sandbox.SandboxFileBrowserViewModel
import com.dorai.ai.ui.sandbox.SandboxPackagesViewModel
import com.dorai.ai.ui.sandbox.SandboxSessionViewModel
import com.dorai.ai.ui.settings.SandboxViewModel
import com.dorai.ai.ui.settings.SettingsViewModel
import com.dorai.ai.ui.settings.SplinterlandsViewModel
import com.dorai.ai.ui.workspace.WorkspaceViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

/** Picks the file browser bound to Workspace's Debian rather than the chat sandbox. */
val WORKSPACE_FILES = named("WorkspaceFiles")

/** Qualifier for the [PermissionController] singleton handling [permission]. */
fun permissionQualifier(permission: AppPermission) = named(permission.name)

val appModule = module {
    single<FileReviewController> { createFileReviewController() }
    AppPermission.entries.forEach { permission ->
        single(permissionQualifier(permission)) { PermissionController(permission) }
    }
    single<SmsReader> { SmsReader() }
    single<SmsSender> { SmsSender() }
    single<NotificationListenerController> { NotificationListenerController() }
    single<NotificationReader> { NotificationReader() }
    single<AppSettings> {
        AppSettings(createSecureSettings()).also {
            it.runMigrations(createLegacySettings())
        }
    }
    // Shares the same Settings instance as AppSettings so engine state is one source of
    // truth across the app root and the Appearance screen.
    single<ThemeSettingsRepository> { ThemeSettingsRepository(get<AppSettings>().settings) }
    single<Requests> {
        Requests()
    }
    single<ConversationStorage> {
        ConversationStorage(get(), createConversationPersistence(get()))
    }
    single<ToolExecutor> {
        ToolExecutor()
    }
    single<MemoryStore> {
        MemoryStore(get())
    }
    single<TaskStore> {
        TaskStore(get())
    }
    single<EmailStore> {
        EmailStore(get())
    }
    single<EmailPoller> {
        EmailPoller(get<EmailStore>())
    }
    single<SmsStore> {
        SmsStore(get())
    }
    single<SmsPoller> {
        SmsPoller(get<SmsStore>(), get<SmsReader>())
    }
    single<SmsDraftStore> {
        SmsDraftStore(get())
    }
    single<NotificationStore> {
        NotificationStore(get())
    }
    single<SplinterlandsStore> {
        SplinterlandsStore(get())
    }
    single<SplinterlandsApi> {
        SplinterlandsApi()
    }
    single<HeartbeatManager> {
        HeartbeatManager(get(), get(), get(), get())
    }
    single<McpServerManager> {
        McpServerManager(get())
    }
    single<SkillManager> {
        SkillManager(get<SandboxController>())
    }
    single<LocalRuntimeRegistry> { createLocalRuntimeRegistry() }
    single<RemoteDataRepository> {
        RemoteDataRepository(
            requests = get(),
            appSettings = get(),
            conversationStorage = get(),
            toolExecutor = get(),
            memoryStore = get(),
            taskStore = get(),
            heartbeatManager = get(),
            emailStore = get(),
            emailPoller = get(),
            smsStore = get(),
            smsPoller = get(),
            smsReader = get(),
            smsPermissionController = get(permissionQualifier(AppPermission.READ_SMS)),
            smsSendPermissionController = get(permissionQualifier(AppPermission.SEND_SMS)),
            smsSender = get(),
            smsDraftStore = get(),
            notificationStore = get(),
            notificationListenerController = get(),
            mcpServerManager = get(),
            skillManager = get(),
            sandboxController = get(),
            localInferenceEngine = createLocalInferenceEngine(),
            localRuntimeRegistry = get(),
        )
    }
    single<DataRepository> { get<RemoteDataRepository>() }
    single<SplinterlandsBattleRunner> {
        SplinterlandsBattleRunner(get(), get(), get<DataRepository>(), get<DaemonController>())
    }
    single<TaskScheduler> {
        TaskScheduler(
            get<DataRepository>(),
            get(),
            get(),
            get(),
            get(),
            get<EmailPoller>(),
            get<SmsStore>(),
            get<SmsPoller>(),
            get<NotificationStore>(),
        )
    }
    single<DaemonController> { createDaemonController() }
    single<SandboxController> { createSandboxController() }
    single<WorkspaceController> { createWorkspaceController() }
    viewModel { SettingsViewModel(get<DataRepository>(), get<DaemonController>(), get(permissionQualifier(AppPermission.POST_NOTIFICATIONS)), get<TaskScheduler>(), localNetworkPermissionController = get(permissionQualifier(AppPermission.LOCAL_NETWORK))) }
    viewModel { SandboxViewModel(get<DataRepository>(), get<SandboxController>()) }
    viewModel { SandboxFileBrowserViewModel(get<SandboxController>()) }
    viewModel { SandboxPackagesViewModel(get<SandboxController>()) }
    viewModel { SandboxSessionViewModel(get<SandboxController>(), get<DataRepository>()) }
    viewModel { WorkspaceViewModel(get<WorkspaceController>(), get<DataRepository>()) }
    // Same browser, second environment: Workspace's Debian instead of the chat sandbox.
    viewModel(WORKSPACE_FILES) { SandboxFileBrowserViewModel(get<WorkspaceController>().files) }
    viewModel { SplinterlandsViewModel(get<DataRepository>(), get(), get(), get<SplinterlandsApi>()) }
    viewModel { ThemeViewModel(get<ThemeSettingsRepository>()) }
    // GitHub integration (read-only by default; clientId is a build-time placeholder until
    // the owner registers a GitHub App, so connect() fails closed with MissingClientId).
    single<GitHubTokenVault> { createGitHubTokenVault() }
    single<GitCredentialBroker> { createGitCredentialBroker() }
    single<GitHubDeviceFlow> { GitHubDeviceFlow(GitHubAppConfig.clientId) }
    single<GitHubApiClient> { GitHubApiClient(get(), httpClient()) }
    viewModel { GitHubIntegrationViewModel(get(), get(), get()) }
    viewModel { ChatViewModel(get<DataRepository>(), get<TaskScheduler>(), localNetworkPermissionController = get(permissionQualifier(AppPermission.LOCAL_NETWORK)), fileReviewController = getOrNull<FileReviewController>() ?: NoOpFileReviewController()) }
}
