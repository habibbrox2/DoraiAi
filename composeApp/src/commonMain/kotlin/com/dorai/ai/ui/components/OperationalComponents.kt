package com.dorai.ai.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dorai.ai.ui.AccentPrimary
import com.dorai.ai.ui.BorderSoft
import com.dorai.ai.ui.DangerAlert
import com.dorai.ai.ui.DoraiTypography
import com.dorai.ai.ui.SurfaceRaised
import com.dorai.ai.ui.doraiAdaptiveCardBorder
import com.dorai.ai.ui.doraiAdaptiveCardColors
import com.dorai.ai.ui.settings.SettingsSpacing
import com.dorai.ai.ui.theme.DesignTokens
import com.dorai.ai.ui.theme.LocalDesignColorTokens
import dorai.composeapp.generated.resources.Res
import dorai.composeapp.generated.resources.a11y_status_with_label
import dorai.composeapp.generated.resources.status_active
import dorai.composeapp.generated.resources.status_connected
import dorai.composeapp.generated.resources.status_installing
import dorai.composeapp.generated.resources.status_needs_attention
import dorai.composeapp.generated.resources.status_not_set_up
import dorai.composeapp.generated.resources.status_ready
import dorai.composeapp.generated.resources.status_some_disabled
import dorai.composeapp.generated.resources.status_working
import org.jetbrains.compose.resources.stringResource

/**
 * Shared operational status vocabulary (Plan 4, Phase 1) so MCP, skills,
 * native tools, and sandbox setup all describe the same states the same way.
 */
enum class OperationalStatus {
    /** Live and reachable. */
    Connected,

    /** Handshake or install in flight. */
    Connecting,

    /** Present but switched off. */
    Disabled,

    /** Being installed or updated. */
    Installing,

    /** Usable only after a prerequisite (e.g. sandbox) is set up. */
    NeedsSetup,

    /** Attempted and failed; needs attention. */
    Error,

    /** On and participating right now (tool enabled, session running). */
    Active,

    /** Prerequisite satisfied and idle. */
    Ready,
}

/** One color ramp for every operational badge so status colors never diverge. */
@Immutable
data class OperationalStatusColors(
    val connected: Color,
    val connecting: Color,
    val disabled: Color,
    val installing: Color,
    val needsSetup: Color,
    val error: Color,
    val active: Color,
    val ready: Color,
) {
    fun forStatus(status: OperationalStatus): Color = when (status) {
        OperationalStatus.Connected -> connected
        OperationalStatus.Connecting -> connecting
        OperationalStatus.Disabled -> disabled
        OperationalStatus.Installing -> installing
        OperationalStatus.NeedsSetup -> needsSetup
        OperationalStatus.Error -> error
        OperationalStatus.Active -> active
        OperationalStatus.Ready -> ready
    }
}

@Composable
fun rememberOperationalStatusColors(): OperationalStatusColors {
    val colors = LocalDesignColorTokens.current
    val materialColors = MaterialTheme.colorScheme
    return remember(materialColors) {
        OperationalStatusColors(
            // Positive states use the theme's primary (a11y: distinct from Disabled).
            connected = materialColors.primary,
            connecting = AccentPrimary,
            disabled = colors.textSecondary,
            installing = AccentPrimary,
            needsSetup = colors.textTertiary,
            error = DangerAlert,
            active = materialColors.primary,
            ready = materialColors.primary,
        )
    }
}

/**
 * Status badge pill shared by MCP servers, skills, tools, and sandbox setup
 * (Plan 4, Phase 1). The dot is decorative — the label and the semantic
 * description carry the meaning for screen readers.
 */
@Composable
fun StatusBadge(
    status: OperationalStatus,
    label: String? = null,
    modifier: Modifier = Modifier,
) {
    val resolvedLabel = label ?: when (status) {
        OperationalStatus.Connected -> stringResource(Res.string.status_connected)
        OperationalStatus.Connecting -> stringResource(Res.string.status_working)
        OperationalStatus.Disabled -> stringResource(Res.string.status_some_disabled)
        OperationalStatus.Installing -> stringResource(Res.string.status_installing)
        OperationalStatus.NeedsSetup -> stringResource(Res.string.status_not_set_up)
        OperationalStatus.Error -> stringResource(Res.string.status_needs_attention)
        OperationalStatus.Active -> stringResource(Res.string.status_active)
        OperationalStatus.Ready -> stringResource(Res.string.status_ready)
    }
    val designColors = LocalDesignColorTokens.current
    val colors = rememberOperationalStatusColors()
    val color = colors.forStatus(status)
    // Use neutral text color for status dot
    val dotColor = if (color == designColors.textSecondary) DesignTokens.MutedText else color
    // a11y: announce the badge as one unit with the label in the user's language.
    val statusDescription = stringResource(Res.string.a11y_status_with_label, resolvedLabel)
    Row(
        modifier = modifier.semantics {
            contentDescription = statusDescription
        },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(dotColor),
        )
        Text(
            text = resolvedLabel,
            style = DoraiTypography.labelSmall,
            color = color,
            fontWeight = FontWeight.Medium,
        )
    }
}

/**
 * Capability count card (Plan 4, Phase 1): one number plus a plain-language
 * label, used by the Tools overview to summarize enabled tools, connected MCP
 * servers, and installed skills at a glance.
 */
@Composable
fun CapabilityCountCard(
    label: String,
    count: String,
    modifier: Modifier = Modifier,
    status: OperationalStatus? = null,
) {
    val colors = LocalDesignColorTokens.current
    val cardBg = SurfaceRaised.copy(alpha = 0.5f)
    val cardBorder = BorderStroke(1.dp, BorderSoft)
    val cardShape = com.dorai.ai.ui.settings.SettingsShapes.card

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = cardBorder,
        shape = cardShape,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(SettingsSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = count,
                style = DoraiTypography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary,
            )
            Text(
                text = label,
                style = DoraiTypography.bodySmall,
                color = colors.textSecondary,
            )
            if (status != null) {
                StatusBadge(
                    status = status,
                )
            }
        }
    }
}

/**
 * Dedicated empty state for a collection (Plan 4, Phase 1) instead of a blank
 * list: one line explaining what would appear here.
 */
@Composable
fun CollectionEmptyState(
    message: String,
    modifier: Modifier = Modifier,
) {
    val colors = LocalDesignColorTokens.current
    val cardBg = SurfaceRaised.copy(alpha = 0.4f)
    val cardBorder = BorderStroke(1.dp, BorderSoft.copy(alpha = 0.4f))
    val cardShape = com.dorai.ai.ui.settings.SettingsShapes.card

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(cardBg)
            .border(cardBorder, cardShape)
            .padding(SettingsSpacing.xl),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            style = DoraiTypography.bodyMedium,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
        )
    }
}
