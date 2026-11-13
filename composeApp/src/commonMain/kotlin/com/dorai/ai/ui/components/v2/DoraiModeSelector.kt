package com.dorai.ai.ui.components.v2

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Rule
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dorai.ai.theme.tokens.DoraiSpacing
import com.dorai.ai.theme.tokens.LocalDoraiColors
import dorai.composeapp.generated.resources.Res
import dorai.composeapp.generated.resources.mode_ask
import dorai.composeapp.generated.resources.mode_ask_desc
import dorai.composeapp.generated.resources.mode_build
import dorai.composeapp.generated.resources.mode_build_desc
import dorai.composeapp.generated.resources.mode_plan
import dorai.composeapp.generated.resources.mode_plan_desc
import dorai.composeapp.generated.resources.mode_review
import dorai.composeapp.generated.resources.mode_review_desc
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Data-driven Agent Modes for UI v2:
 * - [Ask]: Q&A only, no tools.
 * - [Plan]: Reads files, writes a plan, changes nothing.
 * - [Build]: Edits files and runs commands, asks before risky steps.
 * - [Review]: Code review / read-only comments.
 */
data class AgentMode(
    val id: String,
    val nameRes: StringResource,
    val descRes: StringResource,
    val icon: ImageVector,
)

val DefaultAgentModes = listOf(
    AgentMode("ask", Res.string.mode_ask, Res.string.mode_ask_desc, Icons.AutoMirrored.Filled.HelpOutline),
    AgentMode("plan", Res.string.mode_plan, Res.string.mode_plan_desc, Icons.AutoMirrored.Filled.Rule),
    AgentMode("build", Res.string.mode_build, Res.string.mode_build_desc, Icons.Default.Build),
    AgentMode("review", Res.string.mode_review, Res.string.mode_review_desc, Icons.Default.CheckCircle),
)

/**
 * UI v2 Mode Selector:
 * Horizontally scrollable mode chips with one-line active mode description below.
 */
@Composable
fun DoraiModeSelector(
    selectedModeId: String,
    onModeSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    modes: List<AgentMode> = DefaultAgentModes,
) {
    val colors = LocalDoraiColors.current
    val scrollState = rememberScrollState()
    val activeMode = modes.firstOrNull { it.id == selectedModeId } ?: modes.first()

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(DoraiSpacing.SM),
        ) {
            modes.forEach { mode ->
                val isSelected = mode.id == activeMode.id
                val label = stringResource(mode.nameRes)

                Box(
                    modifier = Modifier
                        .defaultMinSize(minHeight = 48.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) colors.primary else colors.surface)
                        .border(1.dp, if (isSelected) colors.primary else colors.line, CircleShape)
                        .selectable(selected = isSelected, onClick = { onModeSelected(mode.id) }, role = Role.RadioButton)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = mode.icon,
                            contentDescription = null,
                            tint = if (isSelected) colors.onPrimary else colors.muted,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = label,
                            color = if (isSelected) colors.onPrimary else colors.muted,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(DoraiSpacing.XS))

        // One-line mode description
        Text(
            text = stringResource(activeMode.descRes),
            color = colors.muted,
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = DoraiSpacing.XS),
            maxLines = 1,
        )
    }
}
