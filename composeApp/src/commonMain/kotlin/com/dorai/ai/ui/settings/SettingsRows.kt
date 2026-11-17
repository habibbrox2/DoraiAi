package com.dorai.ai.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.dorai.ai.theme.tokens.DoraiSpacing
import com.dorai.ai.ui.DoraiTypography
import com.dorai.ai.ui.components.v2.ActionButton
import com.dorai.ai.ui.components.v2.ActionButtonVariant
import com.dorai.ai.ui.handCursor
import com.dorai.ai.ui.theme.DesignTokens
import com.dorai.ai.ui.theme.LocalDesignColorTokens

/**
 * Shared row primitives for every Settings detail screen. Each wraps an existing
 * component so there is one visual/a11y language across the app (DRY).
 *
 * - [SettingsPreferenceRow] wraps a clickable row (navigates to a sub-screen).
 * - [SettingsSwitchRow] wraps [SettingsSwitchCard]'s toggle row behavior.
 * - [SettingsActionRow] pairs copy with an [ActionButton].
 *
 * All rows keep a >= 48dp touch target and expose the correct semantics role.
 */

@Composable
internal fun SettingsPreferenceRow(
    title: String,
    subtitle: String? = null,
    leadingIcon: ImageVector? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalDesignColorTokens.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = DesignTokens.MinTouchTarget)
            .clickable(role = Role.Button, onClick = onClick)
            .handCursor()
            .padding(horizontal = DoraiSpacing.MD, vertical = DoraiSpacing.SM),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.width(DoraiSpacing.MD))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = DoraiTypography.titleSmall,
                color = colors.textPrimary,
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = DoraiTypography.bodySmall,
                    color = colors.textSecondary,
                )
            }
        }
    }
}

@Composable
internal fun SettingsSwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    SettingsSwitchCard(
        modifier = modifier,
        title = title,
        description = description,
        checked = checked,
        onCheckedChange = onCheckedChange,
    )
}

@Composable
internal fun SettingsActionRow(
    title: String,
    subtitle: String? = null,
    actionLabel: String,
    onAction: () -> Unit,
    variant: ActionButtonVariant = ActionButtonVariant.SECONDARY,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = DesignTokens.MinTouchTarget)
            .padding(vertical = DoraiSpacing.SM),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = DoraiTypography.titleSmall,
                color = LocalDesignColorTokens.current.textPrimary,
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = DoraiTypography.bodySmall,
                    color = LocalDesignColorTokens.current.textSecondary,
                )
            }
        }
        Spacer(Modifier.width(DoraiSpacing.SM))
        ActionButton(text = actionLabel, onClick = onAction, variant = variant)
    }
}
