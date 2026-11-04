package com.dorai.ai.ui.chat.composables

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.dorai.ai.data.ServiceEntry
import com.dorai.ai.prefersReducedMotion
import com.dorai.ai.ui.AccentPrimary
import com.dorai.ai.ui.chat.ChatActions
import com.dorai.ai.ui.handCursor
import com.dorai.ai.ui.settings.SettingsModel
import com.dorai.ai.ui.theme.LocalDesignColorTokens
import dorai.composeapp.generated.resources.Res
import dorai.composeapp.generated.resources.ic_volume_off
import dorai.composeapp.generated.resources.ic_volume_up
import dorai.composeapp.generated.resources.sandbox_content_description
import dorai.composeapp.generated.resources.toggle_speech_output_content_description
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import nl.marc_apps.tts.TextToSpeechInstance
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource

/**
 * Chat workspace header. Delegates to [AgentHud] (2027 Premium AI Agent Workspace).
 */
@Composable
internal fun TopBar(
    textToSpeech: TextToSpeechInstance? = null,
    isSpeechOutputEnabled: Boolean,
    isSpeaking: Boolean,
    actions: ChatActions,
    onNavigateToSettings: () -> Unit,
    isSandboxAvailable: Boolean,
    isSandboxOpen: Boolean,
    isShellExecuting: Boolean,
    isGenerating: Boolean = false,
    onToggleSandbox: () -> Unit,
    onOpenDrawer: (() -> Unit)? = null,
    services: ImmutableList<ServiceEntry> = persistentListOf(),
    models: ImmutableList<SettingsModel> = persistentListOf(),
    isLoadingModels: Boolean = false,
    /** Interactive UI mode: the model answers with an interactive screen inside this chat. */
    isInteractiveMode: Boolean = false,
    onToggleInteractiveMode: (() -> Unit)? = null,
    /** Removes the last exchange — only offered while interactive mode is on. */
    onGoBack: (() -> Unit)? = null,
) {
    AgentHud(
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
        isInteractiveMode = isInteractiveMode,
        onToggleInteractiveMode = onToggleInteractiveMode,
        onGoBack = onGoBack,
    )
}

@Composable
internal fun SandboxToggleButton(
    isSandboxOpen: Boolean,
    isShellExecuting: Boolean,
    isGenerating: Boolean = false,
    onToggleSandbox: () -> Unit,
) {
    val prefersReducedMotion = prefersReducedMotion()
    val flashAlpha = remember { Animatable(0f) }
    LaunchedEffect(isShellExecuting) {
        if (isShellExecuting && !prefersReducedMotion) {
            flashAlpha.snapTo(0.4f)
            flashAlpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            )
        }
    }
    val primary = AccentPrimary
    val checkedContainer = primary.copy(alpha = 0.2f)
    val flashContainer = primary.copy(alpha = flashAlpha.value)
    IconToggleButton(
        checked = isSandboxOpen,
        onCheckedChange = { onToggleSandbox() },
        modifier = Modifier.handCursor(),
        colors = IconButtonDefaults.iconToggleButtonColors(
            containerColor = flashContainer,
            checkedContainerColor = if (flashAlpha.value > 0f) flashContainer else checkedContainer,
            checkedContentColor = AccentPrimary,
        ),
    ) {
        Icon(
            imageVector = Icons.Filled.Dns,
            contentDescription = stringResource(Res.string.sandbox_content_description),
            tint = if (isSandboxOpen) {
                AccentPrimary
            } else {
                LocalDesignColorTokens.current.textPrimary
            },
        )
    }
}

@Composable
internal fun SpeechToggleButton(
    textToSpeech: TextToSpeechInstance,
    isSpeechOutputEnabled: Boolean,
    isSpeaking: Boolean,
    actions: ChatActions,
) {
    IconButton(
        modifier = Modifier.handCursor(),
        onClick = {
            if (isSpeechOutputEnabled && isSpeaking) {
                actions.setIsSpeaking(false, "")
                textToSpeech.stop()
            }
            actions.toggleSpeechOutput()
        },
    ) {
        Icon(
            imageVector = if (isSpeechOutputEnabled) {
                vectorResource(Res.drawable.ic_volume_up)
            } else {
                vectorResource(Res.drawable.ic_volume_off)
            },
            contentDescription = stringResource(Res.string.toggle_speech_output_content_description),
            tint = LocalDesignColorTokens.current.textPrimary,
        )
    }
}
