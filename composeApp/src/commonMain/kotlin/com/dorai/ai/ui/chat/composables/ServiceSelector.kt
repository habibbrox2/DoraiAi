package com.dorai.ai.ui.chat.composables

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.dorai.ai.PlatformBackHandler
import com.dorai.ai.data.ServiceEntry
import com.dorai.ai.ui.AccentPrimary
import com.dorai.ai.ui.BorderSoft
import com.dorai.ai.ui.CardFill
import com.dorai.ai.ui.DoraiShapes
import com.dorai.ai.ui.DoraiTypography
import com.dorai.ai.ui.SurfaceRaised
import com.dorai.ai.ui.components.DoraiSearchField
import com.dorai.ai.ui.handCursor
import com.dorai.ai.ui.settings.SettingsModel
import com.dorai.ai.ui.theme.LocalDesignColorTokens
import dorai.composeapp.generated.resources.Res
import dorai.composeapp.generated.resources.action_close
import dorai.composeapp.generated.resources.action_retry
import dorai.composeapp.generated.resources.service_selector_search_hint
import dorai.composeapp.generated.resources.service_selector_title
import dorai.composeapp.generated.resources.settings_model_label
import dorai.composeapp.generated.resources.settings_model_loading
import dorai.composeapp.generated.resources.settings_tab_services
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource

@Composable
internal fun ServiceSelector(
    services: ImmutableList<ServiceEntry>,
    onSelectService: (String) -> Unit,
    models: ImmutableList<SettingsModel> = persistentListOf(),
    isLoadingModels: Boolean = false,
    onSelectModel: (String) -> Unit = {},
    onLoadModels: () -> Unit = {},
    showLabel: Boolean = false,
) {
    if (services.isEmpty()) return

    val current = services.first()
    var expanded by remember { mutableStateOf(false) }
    // The model the chat will actually use: the effective id of the active instance, shown
    // under the name the provider gives it when the list is loaded.
    val currentModelLabel = remember(current.modelId, models) {
        if (current.modelId == "openrouter/auto" || current.modelId == "auto") {
            "Auto"
        } else {
            models.firstOrNull { it.id == current.modelId }?.let { model ->
                model.displayName?.takeIf { it.isNotBlank() } ?: model.id
            } ?: current.modelId
        }
    }

    // Ask for the provider's model list when the picker opens, so the choices are complete
    // without spending a request on every chat start.
    val currentOnLoadModels by rememberUpdatedState(onLoadModels)
    LaunchedEffect(expanded) {
        if (expanded) currentOnLoadModels()
    }

    // Focus management for popup items
    val popupFocusRequester = remember { FocusRequester() }
    val focusableItemCount = (if (models.isNotEmpty()) models.size else 0) + services.size
    var focusedIndex by remember { mutableStateOf(0) }

    Box {
        if (showLabel) {
            // Pill variant used by the Plan 7 top bar: icon + name + chevron.
            val pillBg = LocalDesignColorTokens.current.surfaceElevated
            val pillBorder = LocalDesignColorTokens.current.borderSubtle

            Row(
                modifier = Modifier
                    .clip(DoraiShapes.pill)
                    .background(pillBg, DoraiShapes.pill)
                    .border(BorderStroke(1.dp, pillBorder), DoraiShapes.pill)
                    .clickable { expanded = true }
                    .handCursor()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = vectorResource(current.icon),
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = LocalDesignColorTokens.current.textSecondary,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = current.serviceName,
                    style = DoraiTypography.labelSmall,
                    color = LocalDesignColorTokens.current.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 100.dp),
                )
                if (currentModelLabel.isNotBlank()) {
                    Text(
                        text = "· $currentModelLabel",
                        style = DoraiTypography.labelSmall,
                        color = LocalDesignColorTokens.current.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 120.dp),
                    )
                }
                Spacer(Modifier.width(2.dp))
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null, // decorative — the service name text is adjacent
                    modifier = Modifier.size(16.dp),
                    tint = LocalDesignColorTokens.current.textSecondary,
                )
            }
        } else {
            val circleBg = SurfaceRaised.copy(alpha = 0.9f)
            val circleBorder = BorderSoft

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(circleBg, CircleShape)
                    .border(BorderStroke(1.dp, circleBorder), CircleShape)
                    .clickable { expanded = true }
                    .handCursor(),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = vectorResource(current.icon),
                    contentDescription = current.serviceName,
                    modifier = Modifier.size(18.dp),
                    tint = LocalDesignColorTokens.current.textSecondary,
                )
            }
        }

        if (expanded) {
            PlatformBackHandler(enabled = true) {
                expanded = false
            }

            var searchQuery by remember { mutableStateOf("") }
            val filteredModels = remember(models, searchQuery) {
                if (searchQuery.isBlank()) {
                    models
                } else {
                    models.filter {
                        (it.displayName ?: it.id).contains(searchQuery, ignoreCase = true) || it.id.contains(searchQuery, ignoreCase = true)
                    }
                }
            }
            val filteredServices = remember(services, searchQuery) {
                if (searchQuery.isBlank()) {
                    services
                } else {
                    services.filter {
                        it.serviceName.contains(searchQuery, ignoreCase = true) || it.modelId.contains(searchQuery, ignoreCase = true)
                    }
                }
            }

            // Bounded Modal Dialog with Solid Opaque Background (#0F172A) & Scrim Backdrop
            Dialog(
                onDismissRequest = { expanded = false },
                properties = DialogProperties(usePlatformDefaultWidth = false),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0x99000000))
                        .imePadding()
                        .clickable { expanded = false },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .widthIn(max = 440.dp)
                            .fillMaxHeight(0.9f)
                            .heightIn(max = 540.dp)
                            .clickable(enabled = false) {}
                            .clip(RoundedCornerShape(16.dp))
                            .background(LocalDesignColorTokens.current.surfaceElevated, RoundedCornerShape(16.dp))
                            .border(BorderStroke(1.dp, LocalDesignColorTokens.current.borderSubtle), RoundedCornerShape(16.dp))
                            .padding(16.dp),
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Modal Header with Title & Close Button
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = stringResource(Res.string.service_selector_title),
                                    style = DoraiTypography.titleMedium,
                                    color = LocalDesignColorTokens.current.textPrimary,
                                )
                                androidx.compose.material3.IconButton(
                                    onClick = { expanded = false },
                                    modifier = Modifier.size(48.dp).handCursor(),
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = stringResource(Res.string.action_close),
                                        tint = LocalDesignColorTokens.current.textSecondary,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            }

                            // Sticky Search / Filter Input
                            DoraiSearchField(
                                query = searchQuery,
                                onQueryChange = { searchQuery = it },
                                placeholder = stringResource(Res.string.service_selector_search_hint),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                            )

                            // Scrollable list of models and services
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f, fill = false)
                                    .verticalScroll(rememberScrollState()),
                            ) {
                                if (filteredModels.isNotEmpty()) {
                                    PickerSectionLabel(stringResource(Res.string.settings_model_label))
                                    filteredModels.forEach { model ->
                                        val isCurrent = model.id == current.modelId
                                        ModelMenuItem(
                                            model = model,
                                            isCurrent = isCurrent,
                                            onClick = {
                                                expanded = false
                                                if (!isCurrent) onSelectModel(model.id)
                                            },
                                        )
                                    }
                                    HorizontalDivider(
                                        color = LocalDesignColorTokens.current.textSecondary.copy(alpha = 0.12f),
                                        modifier = Modifier.padding(vertical = 6.dp),
                                    )
                                } else if (isLoadingModels) {
                                    PickerSectionLabel(stringResource(Res.string.settings_model_label))
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(14.dp),
                                            strokeWidth = 2.dp,
                                            color = AccentPrimary,
                                        )
                                        Text(
                                            text = stringResource(Res.string.settings_model_loading),
                                            style = DoraiTypography.labelSmall,
                                            color = LocalDesignColorTokens.current.textSecondary,
                                        )
                                    }
                                }

                                PickerSectionLabel(stringResource(Res.string.settings_tab_services))
                                filteredServices.forEach { entry ->
                                    key(entry.instanceId) {
                                        val isCurrent = entry.instanceId == current.instanceId
                                        ServiceMenuItem(
                                            entry = entry,
                                            isCurrent = isCurrent,
                                            onClick = {
                                                expanded = false
                                                if (!isCurrent) {
                                                    onSelectService(entry.instanceId)
                                                }
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Section heading inside the picker popup: "Model", "Services".
 */
@Composable
private fun PickerSectionLabel(text: String) {
    Text(
        text = text,
        style = DoraiTypography.labelSmall,
        color = LocalDesignColorTokens.current.textSecondary,
        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

/**
 * A model the active service can run. The loaded one is highlighted.
 */
@Composable
private fun ModelMenuItem(
    model: SettingsModel,
    isCurrent: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rowBackground = if (isCurrent) {
        com.dorai.ai.ui.AccentPrimary.copy(alpha = 0.15f)
    } else {
        Color.Transparent
    }
    val textColor = if (isCurrent) {
        com.dorai.ai.ui.AccentPrimary
    } else {
        LocalDesignColorTokens.current.textPrimary
    }
    val shortName = model.displayName?.takeIf { it.isNotBlank() } ?: model.id
    Row(
        modifier = modifier
            .padding(horizontal = 4.dp)
            .clip(DoraiShapes.sm)
            .background(rowBackground)
            .clickable(onClick = onClick)
            .handCursor()
            .heightIn(min = 48.dp)
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .widthIn(min = 180.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = shortName,
            style = DoraiTypography.bodySmall,
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ServiceMenuItem(
    entry: ServiceEntry,
    isCurrent: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rowBackground = if (isCurrent) {
        com.dorai.ai.ui.AccentPrimary.copy(alpha = 0.15f)
    } else {
        Color.Transparent
    }
    val textColor = if (isCurrent) {
        com.dorai.ai.ui.AccentPrimary
    } else {
        LocalDesignColorTokens.current.textPrimary
    }
    val subTextColor = if (isCurrent) {
        com.dorai.ai.ui.AccentPrimary.copy(alpha = 0.7f)
    } else {
        LocalDesignColorTokens.current.textSecondary
    }
    Row(
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .clip(DoraiShapes.sm)
            .background(rowBackground)
            .clickable(onClick = onClick)
            .handCursor()
            .heightIn(min = 48.dp)
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .widthIn(min = 180.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = vectorResource(entry.icon),
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = textColor,
        )
        Spacer(Modifier.width(8.dp))
        Column {
            Text(
                text = entry.serviceName,
                style = DoraiTypography.bodySmall,
                color = textColor,
            )
            if (entry.modelId.isNotEmpty()) {
                Text(
                    text = entry.modelId,
                    style = DoraiTypography.labelSmall,
                    color = subTextColor,
                )
            }
        }
    }
}

private class AnchorAbovePositionProvider(
    private val verticalSpacing: Int,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val maxX = (windowSize.width - popupContentSize.width).coerceAtLeast(0)
        val x = (anchorBounds.right - popupContentSize.width).coerceIn(0, maxX)
        val above = anchorBounds.top - popupContentSize.height - verticalSpacing
        val y = if (above >= 0) {
            above
        } else {
            val maxY = (windowSize.height - popupContentSize.height).coerceAtLeast(0)
            (anchorBounds.bottom + verticalSpacing).coerceAtMost(maxY)
        }
        return IntOffset(x, y)
    }
}
