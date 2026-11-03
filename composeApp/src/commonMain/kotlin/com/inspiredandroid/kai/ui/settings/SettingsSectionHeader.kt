package com.inspiredandroid.kai.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import com.inspiredandroid.kai.ui.KaiTypography
import com.inspiredandroid.kai.ui.theme.LocalDesignColorTokens

/**
 * Consistent section heading for settings groups (Phase 1 of Plan 2).
 *
 * Marked as a semantic heading (Phase 5) so screen readers and accessibility
 * tooling can announce the section structure.
 */
@Composable
fun SettingsSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(bottom = SettingsSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SettingsSpacing.sm),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(SettingsSpacing.xxs),
        ) {
            Text(
                text = title,
                style = KaiTypography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = LocalDesignColorTokens.current.textPrimary,
                modifier = Modifier.semantics { heading() },
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = KaiTypography.bodyMedium,
                    color = LocalDesignColorTokens.current.textSecondary,
                )
            }
        }
        if (trailing != null) trailing()
    }
}




