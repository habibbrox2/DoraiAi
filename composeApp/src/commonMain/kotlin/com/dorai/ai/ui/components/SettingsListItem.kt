package com.dorai.ai.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dorai.ai.ui.KaiShapes
import com.dorai.ai.ui.KaiTypography
import com.dorai.ai.ui.handCursor
import com.dorai.ai.ui.kaiAdaptiveCardSurface
import com.dorai.ai.ui.theme.LocalDesignColorTokens

@Composable
fun SettingsListItem(
    title: String,
    subtitle: String,
    onDelete: () -> Unit,
    deleteContentDescription: String?,
    modifier: Modifier = Modifier,
    subtitleMaxLines: Int = 1,
    onClick: (() -> Unit)? = null,
    onRefresh: (() -> Unit)? = null,
    refreshContentDescription: String? = null,
    isRefreshing: Boolean = false,
) {
    val colors = LocalDesignColorTokens.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .kaiAdaptiveCardSurface(KaiShapes.xs)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick).handCursor() else Modifier)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = KaiTypography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = colors.textPrimary,
            )
            Text(
                text = subtitle,
                style = KaiTypography.bodySmall,
                color = colors.textSecondary,
                maxLines = subtitleMaxLines,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (onRefresh != null) {
            RefreshIconButton(
                onClick = onRefresh,
                isRefreshing = isRefreshing,
                contentDescription = refreshContentDescription.orEmpty(),
            )
        }
        IconButton(
            onClick = onDelete,
            modifier = Modifier.handCursor(),
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = deleteContentDescription,
                tint = colors.textSecondary,
            )
        }
    }
}
