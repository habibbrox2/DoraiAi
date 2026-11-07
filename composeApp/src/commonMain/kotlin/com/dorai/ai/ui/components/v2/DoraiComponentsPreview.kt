package com.dorai.ai.ui.components.v2

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dorai.ai.theme.data.ThemePresets
import com.dorai.ai.theme.data.ThemeStyle
import com.dorai.ai.theme.engine.LocalDoraiPalette
import com.dorai.ai.theme.engine.LocalDoraiThemeStyle
import com.dorai.ai.theme.tokens.DoraiSemanticColors
import com.dorai.ai.theme.tokens.DoraiSpacing
import com.dorai.ai.theme.tokens.LocalDoraiColors

/**
 * Preview showcase containing all Phase 4 shared components across
 * different states and themes.
 */
@Composable
fun DoraiComponentsPreviewContent(
    modifier: Modifier = Modifier,
) {
    val palette = LocalDoraiPalette.current
    val colors = LocalDoraiColors.current
    val scrollState = rememberScrollState()

    var composerText by remember { mutableStateOf("") }
    var selectedMode by remember { mutableStateOf("ask") }
    var selectedTab by remember { mutableStateOf("General") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(DoraiSpacing.MD)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(DoraiSpacing.MD),
    ) {
        Text(
            text = "Dorai UI v2 Shared Components Showcase",
            color = colors.text,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )

        // 4a. ToolCard states
        Text(text = "1. Tool Cards (4 States)", color = colors.primary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)

        DoraiToolCard(
            model = ToolExecutionModel(
                toolName = "git_checkout",
                argsSummary = "feature/ui-v2-migration",
                state = ToolExecutionState.Running,
                commandText = "git checkout -b feature/ui-v2-migration",
                outputText = "Switched to a new branch 'feature/ui-v2-migration'",
            ),
        )

        DoraiToolCard(
            model = ToolExecutionModel(
                toolName = "list_files",
                argsSummary = "composeApp/src/commonMain",
                state = ToolExecutionState.Completed,
                durationText = "0.4s",
                outputText = "Found 12 matching files.",
            ),
        )

        DoraiToolCard(
            model = ToolExecutionModel(
                toolName = "run_gradle_task",
                argsSummary = "./gradlew assembleRelease",
                state = ToolExecutionState.Failed,
                errorText = "Execution failed for task ':composeApp:assembleRelease'. Missing signing config.",
            ),
            onRetry = {},
            onAskFix = {},
        )

        DoraiToolCard(
            model = ToolExecutionModel(
                toolName = "edit_file",
                argsSummary = "AppSettings.kt",
                state = ToolExecutionState.PendingApproval,
                diffText = "--- a/AppSettings.kt\n+++ b/AppSettings.kt\n@@ -10,3 +10,4 @@\n+const val KEY_UI_V2 = \"ui_v2_enabled\"",
            ),
            onAllowOnce = {},
            onAllowSession = {},
            onDeny = {},
        )

        // 4b. Timeline
        Spacer(modifier = Modifier.height(DoraiSpacing.SM))
        Text(text = "2. Agent Activity Timeline", color = colors.primary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)

        DoraiAgentTimeline(
            items = listOf(
                TimelineItemModel("1", TimelineNodeType.Task, "Initialize task workspace", state = TimelineNodeState.Completed),
                TimelineItemModel("2", TimelineNodeType.Thinking, "Analyzing adaptive layout specs", state = TimelineNodeState.Completed),
                TimelineItemModel("3", TimelineNodeType.Tool, "generate_components", "Writing v2 components", state = TimelineNodeState.Running),
                TimelineItemModel("4", TimelineNodeType.Note, "Review pending", state = TimelineNodeState.Pending),
            ),
        )

        // 4c. Mode Selector & Composer
        Spacer(modifier = Modifier.height(DoraiSpacing.SM))
        Text(text = "3. Mode Selector & Composer", color = colors.primary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)

        DoraiModeSelector(
            selectedModeId = selectedMode,
            onModeSelected = { selectedMode = it },
        )

        DoraiComposer(
            value = composerText,
            onValueChange = { composerText = it },
            onSend = {},
        )

        // 4d. Structure Components
        Spacer(modifier = Modifier.height(DoraiSpacing.SM))
        Text(text = "4. Structure Components", color = colors.primary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)

        DoraiPillTabs(
            items = listOf("General", "Appearance", "Agent", "Providers", "Tools"),
            selectedItem = selectedTab,
            onItemSelected = { selectedTab = it },
            labelProvider = { it },
        )

        Row(horizontalArrangement = Arrangement.spacedBy(DoraiSpacing.SM)) {
            DoraiStatusBadge(text = "Running", color = colors.info)
            DoraiStatusBadge(text = "Completed", color = colors.ok)
            DoraiStatusBadge(text = "Warning", color = colors.warn)
            DoraiStatusBadge(text = "Failed", color = colors.err)
            DoraiProviderBadge(isLocal = true)
            DoraiProviderBadge(isLocal = false)
        }

        // 4e. Dynamic UI block
        Spacer(modifier = Modifier.height(DoraiSpacing.SM))
        Text(text = "5. Dynamic UI Interactive Block", color = colors.primary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)

        DoraiDynamicUiBlock(
            model = DynamicUiModel(
                title = "Select deployment target",
                description = "Choose which runtime environment to deploy your service to:",
                options = listOf(
                    DynamicUiOption("1", "Cloudflare Workers", "Fast, globally distributed serverless execution."),
                    DynamicUiOption("2", "Docker Container", "Standardized Linux container deployment."),
                ),
            ),
            onOptionSelected = {},
            onConfirm = {},
        )
    }
}
