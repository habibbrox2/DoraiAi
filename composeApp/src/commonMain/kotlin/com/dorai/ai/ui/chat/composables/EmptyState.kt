package com.dorai.ai.ui.chat.composables

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
        modifier = modifier,
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
            text = "DORAI AI",
            modifier = Modifier.padding(horizontal = 16.dp),
            style = DoraiTypography.displaySmall,
            textAlign = TextAlign.Center,
            color = LocalDesignColorTokens.current.textPrimary,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
        )

        Spacer(Modifier.height(ChatSpacing.xs))

        Text(
            text = "How can I help you today?",
            modifier = Modifier.padding(horizontal = 16.dp),
            style = DoraiTypography.titleMedium,
            textAlign = TextAlign.Center,
            color = LocalDesignColorTokens.current.textSecondary,
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
            val linkColor = DesignTokens.PrimaryCyan
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

@Composable
private fun QuickStartCards(
    isSandboxAvailable: Boolean,
    onQuickStart: (String) -> Unit,
) {
    val items = listOf(
        QuickStartItem(
            label = "New Chat",
            prompt = "???? ?? ??? ????? ?????? ??? ???",
            icon = Icons.Default.Edit,
            accentColor = DesignTokens.PrimaryCyan,
        ),
        QuickStartItem(
            label = "Resume Project",
            prompt = "Continue with my current project and summarize progress",
            icon = Icons.Default.Code,
            accentColor = DesignTokens.AgentViolet,
        ),
        QuickStartItem(
            label = "Daily Reminders",
            prompt = "Set a reminder for me in 15 minutes",
            icon = Icons.Default.Build,
            accentColor = DesignTokens.Success,
        ),
    ) + if (isSandboxAvailable) {
        listOf(
            QuickStartItem(
                label = "Run Sandbox",
                prompt = "Check sandbox environment and available tools",
                icon = Icons.Default.Terminal,
                accentColor = DesignTokens.BrandCyan,
            ),
        )
    } else {
        emptyList()
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(ChatSpacing.sm),
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
