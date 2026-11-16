@file:OptIn(ExperimentalMaterial3Api::class)

package com.dorai.ai.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dorai.ai.BackIcon
import com.dorai.ai.Version
import com.dorai.ai.data.EmailAccount
import com.dorai.ai.data.Service
import com.dorai.ai.prefersReducedMotion
import com.dorai.ai.ui.AccentPrimary
import com.dorai.ai.ui.BorderSoft
import com.dorai.ai.ui.CardFill
import com.dorai.ai.ui.DoraiTypography
import com.dorai.ai.ui.SurfaceRaised
import com.dorai.ai.ui.components.VerticalScrollbarForScroll
import com.dorai.ai.ui.doraiAdaptiveCardBorder
import com.dorai.ai.ui.doraiAdaptiveCardColors
import com.dorai.ai.ui.handCursor
import com.dorai.ai.ui.theme.LocalDesignColorTokens
import dorai.composeapp.generated.resources.Res
import dorai.composeapp.generated.resources.a11y_navigate_back
import dorai.composeapp.generated.resources.settings_ai_mistakes_warning
import dorai.composeapp.generated.resources.settings_licenses
import dorai.composeapp.generated.resources.settings_licenses_close
import dorai.composeapp.generated.resources.settings_screen_title
import dorai.composeapp.generated.resources.settings_tab_account
import dorai.composeapp.generated.resources.settings_tab_agent
import dorai.composeapp.generated.resources.settings_tab_appearance
import dorai.composeapp.generated.resources.settings_tab_data
import dorai.composeapp.generated.resources.settings_tab_general
import dorai.composeapp.generated.resources.settings_tab_github
import dorai.composeapp.generated.resources.settings_tab_services
import dorai.composeapp.generated.resources.settings_tab_tools
import dorai.composeapp.generated.resources.snackbar_email_removed
import dorai.composeapp.generated.resources.snackbar_mcp_server_removed
import dorai.composeapp.generated.resources.snackbar_memory_deleted
import dorai.composeapp.generated.resources.snackbar_service_removed
import dorai.composeapp.generated.resources.snackbar_skill_removed
import dorai.composeapp.generated.resources.snackbar_task_cancelled
import dorai.composeapp.generated.resources.snackbar_undo
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun BottomInfo() {
    Text(
        text = stringResource(Res.string.settings_ai_mistakes_warning),
        style = DoraiTypography.bodySmall,
        textAlign = TextAlign.Center,
        color = LocalDesignColorTokens.current.textSecondary,
        modifier = Modifier.padding(vertical = 4.dp),
    )
}

@Composable
internal fun SettingsCard(
    modifier: Modifier = Modifier,
    innerPadding: Boolean = true,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val cardBg = LocalDesignColorTokens.current.surfaceElevated
    val cardBorder = BorderStroke(1.dp, LocalDesignColorTokens.current.borderSubtle)
    val cardShape = SettingsShapes.card

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = cardShape,
                ambientColor = com.dorai.ai.ui.AccentPrimary.copy(alpha = 0.06f),
                spotColor = com.dorai.ai.ui.AccentPrimary.copy(alpha = 0.1f),
            )
            .clip(cardShape)
            .background(cardBg)
            .border(cardBorder, cardShape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick, role = Role.Button).handCursor() else Modifier),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (innerPadding) Modifier.padding(SettingsSpacing.xl) else Modifier),
        ) {
            content()
        }
    }
}

/**
 * The two-column-on-wide, one-column-on-narrow arrangement the settings tabs share.
 *
 * Each section is declared exactly once. [start] and [end] are the two columns a window of at
 * least 600.dp gets; below that they run into a single column, [start] first. Writing a tab as
 * two independent branches instead is what let the same section drift into two different
 * argument lists, so the branch lives here and the tabs only say what goes in which column.
 */
@Composable
internal fun StaggeredSettingsColumns(
    start: @Composable ColumnScope.() -> Unit,
    end: @Composable ColumnScope.() -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        if (maxWidth >= 600.dp) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(SettingsSpacing.sectionGap),
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(SettingsSpacing.cardGap),
                    content = start,
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(SettingsSpacing.cardGap),
                    content = end,
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(SettingsSpacing.cardGap)) {
                start()
                end()
            }
        }
    }
}

@Composable
internal fun ToggleableHeadline(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val switchInteractionSource = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = switchInteractionSource,
                indication = LocalIndication.current,
                role = Role.Switch,
            ) { onCheckedChange(!checked) }
            .handCursor(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = DoraiTypography.titleMedium,
                color = LocalDesignColorTokens.current.textPrimary,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.size(4.dp))
            Text(
                text = description,
                style = DoraiTypography.bodySmall,
                color = LocalDesignColorTokens.current.textSecondary,
            )
        }
        actions()
        Spacer(Modifier.width(12.dp))
        androidx.compose.material3.Switch(
            checked = checked,
            onCheckedChange = null,
            interactionSource = switchInteractionSource,
            colors = androidx.compose.material3.SwitchDefaults.colors(
                checkedThumbColor = com.dorai.ai.ui.AccentPrimary,
                checkedTrackColor = com.dorai.ai.ui.AccentPrimary.copy(alpha = 0.32f),
                uncheckedThumbColor = LocalDesignColorTokens.current.textSecondary,
                uncheckedTrackColor = BorderSoft,
            ),
        )
        Spacer(Modifier.width(4.dp))
    }
}
