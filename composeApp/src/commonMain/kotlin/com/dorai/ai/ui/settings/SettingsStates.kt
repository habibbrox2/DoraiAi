package com.dorai.ai.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.dorai.ai.theme.tokens.DoraiSpacing
import com.dorai.ai.ui.DoraiTypography
import com.dorai.ai.ui.components.CollectionEmptyState
import com.dorai.ai.ui.handCursor
import com.dorai.ai.ui.theme.DesignTokens
import com.dorai.ai.ui.theme.LocalDesignColorTokens
import dorai.composeapp.generated.resources.Res
import dorai.composeapp.generated.resources.action_retry
import org.jetbrains.compose.resources.stringResource

/**
 * Shared state components for every Settings detail screen. Each wraps an existing
 * building block so states look and behave consistently (DRY):
 *
 * - [SettingsStatusCard]: label + status chip → [SettingsStatusRow].
 * - [SettingsInfoCard]: neutral inline note → [SettingsStatusBanner] Info.
 * - [SettingsEmptyState]: no content → [CollectionEmptyState].
 * - [SettingsErrorState]: failure with polite live region + optional retry.
 * - [SettingsLoadingState]: progress + optional label, never fake rows.
 */

@Composable
internal fun SettingsStatusCard(
    label: String,
    status: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    SettingsStatusRow(label = label, status = status, color = color, modifier = modifier)
}

@Composable
internal fun SettingsInfoCard(
    message: String,
    modifier: Modifier = Modifier,
) {
    SettingsStatusBanner(
        message = message,
        severity = SettingsBannerSeverity.Info,
        modifier = modifier,
    )
}

@Composable
internal fun SettingsEmptyState(
    message: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        CollectionEmptyState(message = message)
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(DoraiSpacing.SM))
            androidx.compose.material3.OutlinedButton(
                onClick = onAction,
                modifier = Modifier
                    .defaultMinSize(minHeight = DesignTokens.MinTouchTarget)
                    .handCursor(),
            ) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
internal fun SettingsErrorState(
    message: String,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    SettingsStatusBanner(
        message = message,
        severity = SettingsBannerSeverity.Error,
        modifier = modifier,
        action = if (onRetry != null) {
            {
                androidx.compose.material3.TextButton(
                    onClick = onRetry,
                    modifier = Modifier.defaultMinSize(minHeight = DesignTokens.MinTouchTarget),
                ) {
                    Text(stringResource(Res.string.action_retry))
                }
            }
        } else {
            null
        },
    )
}

@Composable
internal fun SettingsLoadingState(
    message: String? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(DoraiSpacing.MD),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DoraiSpacing.SM),
    ) {
        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        if (!message.isNullOrBlank()) {
            Text(text = message, style = DoraiTypography.bodyMedium, color = LocalDesignColorTokens.current.textSecondary)
        }
    }
}
