package com.dorai.ai.ui.chat.composables

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dorai.ai.ui.DoraiTypography
import com.dorai.ai.ui.chat.ChatSpacing
import com.dorai.ai.ui.handCursor
import com.dorai.ai.ui.theme.DesignTokens
import com.dorai.ai.ui.theme.LocalDesignColorTokens
import dorai.composeapp.generated.resources.Res
import dorai.composeapp.generated.resources.dashboard_title
import dorai.composeapp.generated.resources.empty_state_subtitle
import dorai.composeapp.generated.resources.home_quick_action_daily_reminders
import dorai.composeapp.generated.resources.home_quick_action_new_chat
import dorai.composeapp.generated.resources.home_quick_action_resume_project
import dorai.composeapp.generated.resources.home_quick_action_run_sandbox
import dorai.composeapp.generated.resources.privacy_agree_prefix
import dorai.composeapp.generated.resources.privacy_policy
import org.jetbrains.compose.resources.stringResource

/**
 * 2027 Premium AI Agent Workspace Home / Welcome State.
 *
 * Implements Screen 1 from docs/ui-upgrade-plan-7-premium-chat.md.
 * Features central BK Monogram branding with ambient violet/cyan radial aura,
 * title, and responsive quick-start action chips.
 */
@Composable
internal fun EmptyState(
    modifier: Modifier,
    isUsingSharedKey: Boolean,
    isGenerating: Boolean,
    isShellExecuting: Boolean,
    isSpeaking: Boolean,
    isSandboxAvailable: Boolean,
    onQuickStart: ((String) -> Unit)? = null,
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AgentOrb(
            state = when {
                isShellExecuting -> AgentPresentationState.Executing
                isGenerating -> AgentPresentationState.Generating
                isSpeaking -> AgentPresentationState.Speaking
                else -> AgentPresentationState.Idle
            },
        )

        Spacer(Modifier.height(ChatSpacing.lg))

        Text(
            text = stringResource(Res.string.dashboard_title),
            modifier = Modifier.padding(horizontal = 16.dp),
            style = DoraiTypography.displaySmall,
            textAlign = TextAlign.Center,
            // The premium chat backdrop is always dark, so the welcome copy uses fixed
            // light tokens instead of theme-aware text colors.
            color = DesignTokens.PrimaryText,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
        )

        Spacer(Modifier.height(ChatSpacing.xs))

        Text(
            text = stringResource(Res.string.empty_state_subtitle),
            modifier = Modifier.padding(horizontal = 16.dp),
            style = DoraiTypography.titleMedium,
            textAlign = TextAlign.Center,
            color = DesignTokens.SecondaryText,
        )

        if (onQuickStart != null) {
            Spacer(Modifier.height(ChatSpacing.xl))
            QuickStartCards(
                isSandboxAvailable = isSandboxAvailable,
                onQuickStart = onQuickStart,
            )
        }

        if (isUsingSharedKey) {
            Spacer(Modifier.height(ChatSpacing.xl))
            val linkColor = LocalDesignColorTokens.current.primary
            val prefixText = stringResource(Res.string.privacy_agree_prefix)
            val policyText = stringResource(Res.string.privacy_policy)
            val annotatedString = remember(prefixText, policyText, linkColor) {
                buildAnnotatedString {
                    append(prefixText)
                    withLink(
                        LinkAnnotation.Url(
                            url = "https://schubert-simon.de/privacy/kai.txt",
                        ),
                    ) {
                        withStyle(style = SpanStyle(color = linkColor)) {
                            append(policyText)
                        }
                    }
                }
            }
            Text(
                annotatedString,
                modifier = Modifier.padding(horizontal = 16.dp),
                style = DoraiTypography.bodySmall,
                textAlign = TextAlign.Center,
                color = LocalDesignColorTokens.current.textTertiary,
            )
        }
    }
}

private data class QuickStartItem(
    val label: String,
    val prompt: String,
    val icon: ImageVector,
    val accentColor: Color,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuickStartCards(
    isSandboxAvailable: Boolean,
    onQuickStart: (String) -> Unit,
) {
    val items = listOf(
        QuickStartItem(
            label = stringResource(Res.string.home_quick_action_new_chat),
            prompt = "How can Dorai AI help me today?",
            icon = Icons.Default.Edit,
            accentColor = LocalDesignColorTokens.current.primary,
        ),
        QuickStartItem(
            label = stringResource(Res.string.home_quick_action_resume_project),
            prompt = "Continue with my current project and summarize progress",
            icon = Icons.Default.Code,
            accentColor = LocalDesignColorTokens.current.accent,
        ),
        QuickStartItem(
            label = stringResource(Res.string.home_quick_action_daily_reminders),
            prompt = "Set a reminder for me in 15 minutes",
            icon = Icons.Default.Build,
            accentColor = LocalDesignColorTokens.current.success,
        ),
    ) + if (isSandboxAvailable) {
        listOf(
            QuickStartItem(
                label = stringResource(Res.string.home_quick_action_run_sandbox),
                prompt = "Check sandbox environment and available tools",
                icon = Icons.Default.Terminal,
                accentColor = LocalDesignColorTokens.current.info,
            ),
        )
    } else {
        emptyList()
    }
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(ChatSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(ChatSpacing.sm),
    ) {
        items.forEach { item ->
            QuickStartCard(
                item = item,
                onClick = { onQuickStart(item.prompt) },
                modifier = Modifier.widthIn(min = 156.dp, max = 220.dp),
            )
        }
    }
}

@Composable
private fun QuickStartCard(
    item: QuickStartItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val cardAccent = item.accentColor
    val cardBg = LocalDesignColorTokens.current.surfaceRaised
    val cardShape = RoundedCornerShape(DesignTokens.RadiusPill)

    val cardBorder = if (isHovered) {
        BorderStroke(1.dp, cardAccent)
    } else {
        BorderStroke(1.dp, LocalDesignColorTokens.current.borderSoft)
    }

    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 56.dp)
            .handCursor()
            .hoverable(interactionSource)
            .clickable(role = Role.Button, onClick = onClick)
            .clip(cardShape)
            .background(cardBg, cardShape)
            .border(cardBorder, cardShape),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ChatSpacing.sm),
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.label,
                tint = if (isHovered) cardAccent else cardAccent.copy(alpha = 0.85f),
                modifier = Modifier.size(24.dp),
            )
            Text(
                text = item.label,
                style = DoraiTypography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = LocalDesignColorTokens.current.textPrimary,
            )
        }
    }
}
