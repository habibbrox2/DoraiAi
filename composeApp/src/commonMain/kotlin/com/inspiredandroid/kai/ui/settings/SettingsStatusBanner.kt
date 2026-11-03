package com.inspiredandroid.kai.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.inspiredandroid.kai.ui.AccentPrimary
import com.inspiredandroid.kai.ui.BorderSoft
import com.inspiredandroid.kai.ui.DangerAlert
import com.inspiredandroid.kai.ui.InfoBlue
import com.inspiredandroid.kai.ui.KaiTypography
import com.inspiredandroid.kai.ui.SuccessGreen
import com.inspiredandroid.kai.ui.WarningAmber

/** Severity of an inline settings status banner. */
enum class SettingsBannerSeverity {
    /** Neutral informational message. */
    Info,

    /** Something is progressing (install, sync, download). */
    Progress,

    /** Something needs the user's attention but is not fatal. */
    Warning,

    /** An action failed; the message explains what went wrong. */
    Error,
}

/**
 * Inline status banner shown inside a settings card (Plan 2, Phase 4).
 *
 * Makes progress, installation, sync, and error states explicit where they happen
 * rather than surfacing a transient snackbar, so the state stays attached to the
 * affected card until it resolves.
 */
@Composable
fun SettingsStatusBanner(
    message: String,
    severity: SettingsBannerSeverity,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    val (container, content) = when (severity) {
        SettingsBannerSeverity.Info -> InfoBlue to InfoBlue
        SettingsBannerSeverity.Progress -> AccentPrimary to AccentPrimary
        SettingsBannerSeverity.Warning -> WarningAmber to WarningAmber
        SettingsBannerSeverity.Error -> DangerAlert to DangerAlert
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(SettingsShapes.row)
            .background(container.copy(alpha = 0.14f))
            .border(BorderStroke(1.dp, BorderSoft), SettingsShapes.row),
    ) {
        Row(
            modifier = Modifier.padding(SettingsSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SettingsSpacing.sm),
        ) {
            if (severity == SettingsBannerSeverity.Progress) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = container,
                )
            }
            Text(
                text = message,
                style = KaiTypography.bodyMedium,
                color = content,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
            )
            if (action != null) action()
        }
    }
}
