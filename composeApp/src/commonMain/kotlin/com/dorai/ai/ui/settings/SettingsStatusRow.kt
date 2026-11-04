package com.dorai.ai.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dorai.ai.ui.KaiTypography
import com.dorai.ai.ui.theme.LocalDesignColorTokens

/**
 * A key/value row used by settings cards and the overview surface (Plan 2, Phase 3).
 */
@Composable
fun SettingsKeyValueRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SettingsSpacing.sm),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = KaiTypography.bodyMedium,
                color = LocalDesignColorTokens.current.textPrimary,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = value,
                style = KaiTypography.bodySmall,
                color = LocalDesignColorTokens.current.textSecondary,
            )
        }
        if (trailing != null) trailing()
    }
}

/**
 * A row showing a label with a status chip (connected/checking/error/unknown).
 */
@Composable
fun SettingsStatusRow(
    label: String,
    status: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SettingsSpacing.sm),
    ) {
        Text(
            text = label,
            style = KaiTypography.bodyMedium,
            color = LocalDesignColorTokens.current.textPrimary,
            modifier = Modifier.weight(1f),
        )
        Box(
            modifier = Modifier
                .clip(SettingsShapes.pill)
                .background(color.copy(alpha = 0.14f), SettingsShapes.pill),
        ) {
            Text(
                text = status,
                style = KaiTypography.labelSmall,
                color = color,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = SettingsSpacing.sm, vertical = SettingsSpacing.xxs),
            )
        }
    }
}
