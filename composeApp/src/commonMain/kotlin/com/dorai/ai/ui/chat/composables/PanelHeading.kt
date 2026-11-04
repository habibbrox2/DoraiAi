package com.dorai.ai.ui.chat.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.dorai.ai.ui.DoraiTypography
import com.dorai.ai.ui.chat.ChatSpacing
import com.dorai.ai.ui.theme.LocalDesignColorTokens

/**
 * Consistent heading used by panels, sheets and sections in the chat workspace
 * (history sheet, sandbox panel, tool panel). Keeps typography and spacing
 * uniform across the responsive shell.
 */
@Composable
fun PanelHeading(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ChatSpacing.sm),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(ChatSpacing.xxs),
        ) {
            Text(
                text = title,
                style = DoraiTypography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = LocalDesignColorTokens.current.textPrimary,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = DoraiTypography.bodySmall,
                    color = LocalDesignColorTokens.current.textSecondary,
                )
            }
        }
        if (trailing != null) {
            trailing()
        }
    }
}
