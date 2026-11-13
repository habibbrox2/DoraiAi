package com.dorai.ai.ui.chat.composables

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.dorai.ai.ui.AccentPrimary
import com.dorai.ai.ui.chat.ChatSpacing
import com.dorai.ai.ui.handCursor
import com.dorai.ai.ui.theme.LocalDesignColorTokens
import dorai.composeapp.generated.resources.*
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource

/**
 * Consistent agent-style message action row (Copy / Retry / Speech / Thumbs / Flag).
 * Features:
 * - Instant "Copied!" checkmark feedback on copy
 * - Hover, press and selected feedback on every action, so the row reads as a
 *   control strip rather than a row of floating glyphs
 * - Selected state tinted with the accent colour (copied, thumbs up/down)
 * - 48dp accessible touch targets
 */
@Composable
fun MessageActionRow(
    onCopy: () -> Unit,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
    onShare: (() -> Unit)? = null,
    onSpeak: (() -> Unit)? = null,
    onThumbsUp: (() -> Unit)? = null,
    onThumbsDown: (() -> Unit)? = null,
    thumbsUpSelected: Boolean = false,
    thumbsDownSelected: Boolean = false,
    onFlag: (() -> Unit)? = null,
) {
    var isCopied by remember { mutableStateOf(false) }

    LaunchedEffect(isCopied) {
        if (isCopied) {
            delay(2000)
            isCopied = false
        }
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(ChatSpacing.xxs),
    ) {
        ActionButton(
            icon = if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy,
            contentDescription = stringResource(if (isCopied) Res.string.chat_action_copied else Res.string.chat_action_copy_message),
            selected = isCopied,
            onClick = {
                onCopy()
                isCopied = true
            },
        )
        if (onRetry != null) {
            ActionButton(
                icon = Icons.Default.Refresh,
                contentDescription = stringResource(Res.string.chat_action_regenerate_retry),
                onClick = onRetry,
            )
        }
        if (onSpeak != null) {
            ActionButton(
                icon = Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = stringResource(Res.string.chat_read_aloud),
                onClick = onSpeak,
            )
        }
        if (onThumbsUp != null) {
            ActionButton(
                icon = if (thumbsUpSelected) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                contentDescription = stringResource(Res.string.chat_action_thumbs_up),
                selected = thumbsUpSelected,
                onClick = onThumbsUp,
            )
        }
        if (onThumbsDown != null) {
            ActionButton(
                icon = if (thumbsDownSelected) Icons.Filled.ThumbDown else Icons.Outlined.ThumbDown,
                contentDescription = stringResource(Res.string.chat_action_thumbs_down),
                selected = thumbsDownSelected,
                onClick = onThumbsDown,
            )
        }
        if (onFlag != null) {
            ActionButton(
                icon = Icons.Default.Flag,
                contentDescription = stringResource(Res.string.chat_action_report),
                onClick = onFlag,
            )
        }
        if (onShare != null) {
            ActionButton(
                icon = Icons.Default.Share,
                contentDescription = stringResource(Res.string.chat_action_share),
                onClick = onShare,
            )
        }
    }
}

/**
 * Icon-only action with an agent-style hover/press surface. The visible chip is
 * kept compact while the hit area stays the 48dp accessibility target.
 */
@Composable
private fun ActionButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val hovered by interactionSource.collectIsHoveredAsState()

    val tint = when {
        selected -> AccentPrimary
        hovered || pressed -> LocalDesignColorTokens.current.textPrimary
        else -> LocalDesignColorTokens.current.textSecondary
    }
    val chipColor = when {
        selected -> AccentPrimary.copy(alpha = 0.16f)
        pressed -> AccentPrimary.copy(alpha = 0.10f)
        hovered -> LocalDesignColorTokens.current.surfaceRaised
        else -> Color.Transparent
    }

    Box(
        modifier = modifier
            .size(ChatSpacing.minTouchTarget)
            .clip(RoundedCornerShape(14.dp))
            .handCursor()
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                role = Role.Button,
                onClick = onClick,
            )
            .hoverable(interactionSource)
            .semantics { if (selected) this.selected = true },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(chipColor),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(17.dp),
                tint = tint,
            )
        }
    }
}
