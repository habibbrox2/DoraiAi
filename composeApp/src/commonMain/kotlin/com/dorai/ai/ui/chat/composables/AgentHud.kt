package com.dorai.ai.ui.chat.composables

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dorai.ai.BackIcon
import com.dorai.ai.data.ServiceEntry
import com.dorai.ai.prefersReducedMotion
import com.dorai.ai.ui.DoraiTypography
import com.dorai.ai.ui.chat.ChatActions
import com.dorai.ai.ui.chat.ChatSpacing
import com.dorai.ai.ui.handCursor
import com.dorai.ai.ui.settings.SettingsModel
import com.dorai.ai.ui.theme.DesignTokens
import com.dorai.ai.ui.theme.LocalDesignColorTokens
import dorai.composeapp.generated.resources.Res
import dorai.composeapp.generated.resources.ic_settings
import dorai.composeapp.generated.resources.interactive_back_content_description
import dorai.composeapp.generated.resources.interactive_mode_content_description
import dorai.composeapp.generated.resources.logo
import dorai.composeapp.generated.resources.settings_content_description
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import nl.marc_apps.tts.TextToSpeechInstance
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource

/**
 * DORAI AI � 2027 Premium AI Agent Workspace HUD (Heads-Up Display).
 *
 * Implements the floating top bar specification from docs/ui-upgrade-plan-7-premium-chat.md.
 * Provides left cluster (drawer + BK monogram + brand), center cluster (pulsing agent orb + model picker),
 * and right cluster (terminal/sandbox toggle + TTS speech toggle + settings).
 */
@Composable
internal fun AgentHud(
    textToSpeech: TextToSpeechInstance? = null,
    isSpeechOutputEnabled: Boolean = false,
    isSpeaking: Boolean = false,
    actions: ChatActions,
    onNavigateToSettings: () -> Unit,
    isSandboxAvailable: Boolean = false,
    isSandboxOpen: Boolean = false,
    isShellExecuting: Boolean = false,
    isGenerating: Boolean = false,
    onToggleSandbox: () -> Unit = {},
    onOpenDrawer: (() -> Unit)? = null,
    services: ImmutableList<ServiceEntry> = persistentListOf(),
    models: ImmutableList<SettingsModel> = persistentListOf(),
    isLoadingModels: Boolean = false,
    isInteractiveMode: Boolean = false,
    onToggleInteractiveMode: (() -> Unit)? = null,
    onGoBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = 12.dp, bottom = 4.dp),
    ) {
        val compact = isCompactAgentHud(maxWidth)
        val hudBackground = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
        val hudBorder = BorderStroke(1.dp, DesignTokens.PrimaryCyan.copy(alpha = 0.38f))
        val hudShape = RoundedCornerShape(DesignTokens.RadiusHud)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 56.dp)
                .padding(horizontal = ChatSpacing.md)
                .clip(hudShape)
                .background(hudBackground, hudShape)
                .border(hudBorder, hudShape),
        ) {
            if (compact) {
                CompactAgentHudContent(
                    textToSpeech = textToSpeech,
                    isSpeechOutputEnabled = isSpeechOutputEnabled,
                    isSpeaking = isSpeaking,
                    actions = actions,
                    onNavigateToSettings = onNavigateToSettings,
                    isSandboxAvailable = isSandboxAvailable,
                    isSandboxOpen = isSandboxOpen,
                    isShellExecuting = isShellExecuting,
                    isGenerating = isGenerating,
                    onToggleSandbox = onToggleSandbox,
                    onOpenDrawer = onOpenDrawer,
                    services = services,
                    models = models,
                    isLoadingModels = isLoadingModels,
                    onGoBack = onGoBack,
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = ChatSpacing.sm, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        if (onOpenDrawer != null) {
                            IconButton(
                                modifier = Modifier.size(48.dp).handCursor(),
                                onClick = onOpenDrawer,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = "Open navigation",
                                    tint = LocalDesignColorTokens.current.textPrimary,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                        Icon(
                            painter = painterResource(Res.drawable.logo),
                            contentDescription = "DORAI AI Logo",
                            tint = Color.Unspecified,
                            modifier = Modifier.size(28.dp),
                        )
                        Text(
                            text = "DORAI AI",
                            style = DoraiTypography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = LocalDesignColorTokens.current.textPrimary,
                            maxLines = 1,
                        )
                        onToggleInteractiveMode?.let { toggle ->
                            IconToggleButton(
                                checked = isInteractiveMode,
                                onCheckedChange = { toggle() },
                                modifier = Modifier.size(48.dp).handCursor(),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Widgets,
                                    contentDescription = stringResource(Res.string.interactive_mode_content_description),
                                    tint = if (isInteractiveMode) DesignTokens.PrimaryCyan else LocalDesignColorTokens.current.textTertiary,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.weight(1f).padding(horizontal = 10.dp),
                    ) {
                        AgentStatusOrb(
                            isGenerating = isGenerating,
                            isShellExecuting = isShellExecuting,
                            isSpeaking = isSpeaking,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = agentStatusLabel(
                                isGenerating = isGenerating,
                                isShellExecuting = isShellExecuting,
                                isSpeaking = isSpeaking,
                            ),
                            style = DoraiTypography.titleSmall,
                            color = LocalDesignColorTokens.current.textPrimary,
                            maxLines = 1,
                        )
                        Spacer(Modifier.width(6.dp))
                        if (services.isNotEmpty()) {
                            ServiceSelector(
                                services = services,
                                onSelectService = actions.selectService,
                                models = models,
                                isLoadingModels = isLoadingModels,
                                onSelectModel = actions.selectModel,
                                onLoadModels = actions.loadModels,
                                showLabel = false,
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        onGoBack?.let { goBack ->
                            IconButton(
                                modifier = Modifier.size(48.dp).handCursor(),
                                onClick = goBack,
                            ) {
                                Icon(
                                    imageVector = BackIcon,
                                    contentDescription = stringResource(Res.string.interactive_back_content_description),
                                    tint = LocalDesignColorTokens.current.textPrimary,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                        if (isSandboxAvailable) {
                            SandboxToggleButton(
                                isSandboxOpen = isSandboxOpen,
                                isShellExecuting = isShellExecuting,
                                onToggleSandbox = onToggleSandbox,
                            )
                        }
                        if (textToSpeech != null) {
                            SpeechToggleButton(textToSpeech, isSpeechOutputEnabled, isSpeaking, actions)
                        }
                        IconButton(
                            modifier = Modifier.size(48.dp).handCursor(),
                            onClick = onNavigateToSettings,
                        ) {
                            Icon(
                                imageVector = vectorResource(Res.drawable.ic_settings),
                                contentDescription = stringResource(Res.string.settings_content_description),
                                tint = LocalDesignColorTokens.current.textPrimary,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactAgentHudContent(
    textToSpeech: TextToSpeechInstance?,
    isSpeechOutputEnabled: Boolean,
    isSpeaking: Boolean,
    actions: ChatActions,
    onNavigateToSettings: () -> Unit,
    isSandboxAvailable: Boolean,
    isSandboxOpen: Boolean,
    isShellExecuting: Boolean,
    isGenerating: Boolean,
    onToggleSandbox: () -> Unit,
    onOpenDrawer: (() -> Unit)?,
    services: ImmutableList<ServiceEntry>,
    models: ImmutableList<SettingsModel>,
    isLoadingModels: Boolean,
    onGoBack: (() -> Unit)?,
) {
    val statusLabel = agentStatusLabel(isGenerating, isShellExecuting, isSpeaking)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        onGoBack?.let { goBack ->
            IconButton(
                modifier = Modifier.size(48.dp).handCursor(),
                onClick = goBack,
            ) {
                Icon(
                    imageVector = BackIcon,
                    contentDescription = stringResource(Res.string.interactive_back_content_description),
                    tint = LocalDesignColorTokens.current.textPrimary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        if (onOpenDrawer != null) {
            IconButton(
                modifier = Modifier.size(48.dp).handCursor(),
                onClick = onOpenDrawer,
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Open navigation",
                    tint = LocalDesignColorTokens.current.textPrimary,
                )
            }
        }
        Icon(
            painter = painterResource(Res.drawable.logo),
            contentDescription = "DORAI AI Logo",
            tint = Color.Unspecified,
            modifier = Modifier.size(26.dp),
        )
        Spacer(Modifier.weight(1f))
        if (services.isNotEmpty()) {
            ServiceSelector(
                services = services,
                onSelectService = actions.selectService,
                models = models,
                isLoadingModels = isLoadingModels,
                onSelectModel = actions.selectModel,
                onLoadModels = actions.loadModels,
                showLabel = false,
            )
        }
        AgentStatusOrb(
            isGenerating = isGenerating,
            isShellExecuting = isShellExecuting,
            isSpeaking = isSpeaking,
            modifier = Modifier.semantics { contentDescription = "Agent status: $statusLabel" },
        )
        if (textToSpeech != null) {
            SpeechToggleButton(textToSpeech, isSpeechOutputEnabled, isSpeaking, actions)
        }
        IconButton(
            modifier = Modifier.size(48.dp).handCursor(),
            onClick = onNavigateToSettings,
        ) {
            Icon(
                imageVector = vectorResource(Res.drawable.ic_settings),
                contentDescription = stringResource(Res.string.settings_content_description),
                tint = LocalDesignColorTokens.current.textPrimary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

internal fun isCompactAgentHud(width: Dp): Boolean = width < 720.dp

/**
 * Pulsing Agent Status Orb indicating runtime execution states:
 * - Idle: calm cyan breathing glow
 * - Generating / Thinking: active agent violet pulse
 * - Shell / Tool running: amber energy glow
 * - Speaking: cyan voice-wave pulse
 */
@Composable
fun AgentStatusOrb(
    isGenerating: Boolean,
    isShellExecuting: Boolean,
    isSpeaking: Boolean,
    modifier: Modifier = Modifier,
) {
    val pulseScale = if (prefersReducedMotion()) {
        0.82f
    } else {
        val infiniteTransition = rememberInfiniteTransition(label = "hudStatus")
        val animatedPulseScale by infiniteTransition.animateFloat(
            initialValue = 0.82f,
            targetValue = 1.25f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = if (isGenerating || isShellExecuting) 650 else 1800,
                    easing = FastOutSlowInEasing,
                ),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "hudStatusPulse",
        )
        animatedPulseScale
    }

    val orbColor = when {
        isShellExecuting -> DesignTokens.Warning
        isGenerating -> DesignTokens.AgentViolet
        isSpeaking -> DesignTokens.PrimaryCyan
        else -> DesignTokens.BrandCyan
    }

    Box(
        modifier = modifier.size(18.dp),
        contentAlignment = Alignment.Center,
    ) {
        // Outer pulsing aura
        Box(
            modifier = Modifier
                .size(16.dp * pulseScale)
                .clip(CircleShape)
                .background(orbColor.copy(alpha = 0.28f)),
        )
        // Core status bead
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(orbColor),
        )
    }
}

private fun agentStatusLabel(
    isGenerating: Boolean,
    isShellExecuting: Boolean,
    isSpeaking: Boolean,
): String = when {
    isShellExecuting -> "Running"
    isGenerating -> "Thinking"
    isSpeaking -> "Speaking"
    else -> "Idle"
}
