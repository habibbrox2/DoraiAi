package com.dorai.ai.screenshots

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.dorai.ai.theme.data.ThemePresets
import com.dorai.ai.theme.engine.DoraiTheme
import com.dorai.ai.ui.sandbox.EditorState
import com.dorai.ai.ui.workspace.WorkspaceActions
import com.dorai.ai.ui.workspace.WorkspaceScreenContent
import com.dorai.ai.ui.workspace.WorkspaceTab
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.setResourceReaderAndroidContext
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalResourceApi::class)
class R4MissingScreenScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_9A.copy(softButtons = false),
        showSystemUi = true,
        maxPercentDifference = 0.1,
    )

    @Before
    fun setup() {
        setResourceReaderAndroidContext(paparazzi.context)
    }

    @Test
    fun workspace_editor_dark() = snapshotWorkspace(WorkspaceTab.Editor, editor = sampleEditor)

    @Test
    fun workspace_editor_light() = snapshotWorkspace(WorkspaceTab.Editor, editor = sampleEditor, light = true)

    @Test
    fun workspace_terminal_dark() = snapshotWorkspace(WorkspaceTab.Terminal)

    @Test
    fun workspace_git_dark() = snapshotWorkspace(WorkspaceTab.Git)

    @Test
    fun workspace_git_light() = snapshotWorkspace(WorkspaceTab.Git, light = true)

    private fun snapshotWorkspace(
        tab: WorkspaceTab,
        editor: EditorState? = null,
        light: Boolean = false,
    ) {
        val palette = if (light) ThemePresets.light else ThemePresets.dark
        paparazzi.unsafeUpdateConfig(
            theme = if (light) "android:Theme.Material.Light.NoActionBar" else "android:Theme.Material.NoActionBar",
        )
        paparazzi.snapshot {
            Box(Modifier.fillMaxSize()) {
                CompositionLocalProvider(LocalInspectionMode provides true) {
                    DoraiTheme(repository = screenshotThemeRepository(palette)) {
                        WorkspaceScreenContent(
                            state = ScreenshotTestData.buildStateWithActiveProject,
                            actions = noOpWorkspaceActions,
                            onExit = {},
                            initialTab = tab,
                            initialEditorState = editor,
                        )
                    }
                }
            }
        }
    }
}

private val sampleEditor = EditorState.Loaded(
    path = "/root/projects/dorai-sample/src/Main.kt",
    original = "package sample\n\nfun main() {\n    println(\"Hello DORAI AI\")\n}",
    current = "package sample\n\nfun main() {\n    println(\"Hello DORAI AI\")\n}",
)

private val noOpWorkspaceActions = WorkspaceActions(
    toggleAgent = {},
    install = {},
    installAgent = {},
    cancel = {},
    uninstall = {},
    setLaunchAgent = {},
    openProject = {},
    createProject = {},
    deleteProject = {},
    renameProject = { _, _ -> },
    closeProject = {},
    startSession = {},
    selectSession = {},
    closeSession = {},
    submitLine = {},
    sendKey = { _, _ -> },
    sendText = { _, _ -> },
    sendMouse = {},
    resizeTerminal = { _, _ -> },
    openFile = {},
)
