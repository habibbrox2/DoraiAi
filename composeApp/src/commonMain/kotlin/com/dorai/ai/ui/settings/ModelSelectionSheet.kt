package com.dorai.ai.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dorai.ai.formatContextWindow
import com.dorai.ai.formatReleaseDate
import com.dorai.ai.ui.KaiOutlinedTextField
import com.dorai.ai.ui.KaiShapes
import com.dorai.ai.ui.KaiTypography
import com.dorai.ai.ui.components.KaiSearchField
import com.dorai.ai.ui.components.VerticalScrollbarForGrid
import com.dorai.ai.ui.handCursor
import com.dorai.ai.ui.theme.DesignTokens
import com.dorai.ai.ui.theme.LocalDesignColorTokens
import dorai.composeapp.generated.resources.Res
import dorai.composeapp.generated.resources.ic_arrow_drop_down
import dorai.composeapp.generated.resources.model_filter_free
import dorai.composeapp.generated.resources.model_free_badge
import dorai.composeapp.generated.resources.model_free_empty
import dorai.composeapp.generated.resources.model_sort_alpha
import dorai.composeapp.generated.resources.model_sort_context
import dorai.composeapp.generated.resources.model_sort_date
import dorai.composeapp.generated.resources.model_sort_score
import dorai.composeapp.generated.resources.settings_model_label
import dorai.composeapp.generated.resources.settings_model_search
import kotlinx.collections.immutable.ImmutableList
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ModelSelection(
    currentSelectedModel: SettingsModel?,
    models: ImmutableList<SettingsModel>,
    onClick: (String) -> Unit,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onRetry: () -> Unit = {},
    canRetry: Boolean = true,
) {
    var expanded by remember { mutableStateOf(false) }
    // Provider list only — synthetic manual entries stay out of the picker grid.
    val pickerModels = remember(models) { models.filter { !it.isManualEntry } }
    if (pickerModels.isNotEmpty()) {
        Box(
            modifier = Modifier.fillMaxWidth(),
        ) {
            KaiOutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = currentSelectedModel?.let { it.displayName ?: it.id } ?: "",
                onValueChange = {},
                readOnly = true,
                label = {
                    Text(
                        stringResource(Res.string.settings_model_label),
                        color = LocalDesignColorTokens.current.textPrimary,
                    )
                },
                trailingIcon = {
                    Icon(
                        modifier = Modifier.handCursor(),
                        imageVector = vectorResource(Res.drawable.ic_arrow_drop_down),
                        contentDescription = null,
                        tint = LocalDesignColorTokens.current.textPrimary,
                    )
                },
            )
            // Transparent overlay to capture clicks reliably on all platforms
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .handCursor()
                    .clickable { expanded = true },
            )
        }
        if (expanded) {
            val sheetFocusRequester = remember { FocusRequester() }
            ModalBottomSheet(
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                onDismissRequest = {
                    expanded = false
                },
            ) {
                Column(
                    modifier = Modifier
                        .focusRequester(sheetFocusRequester)
                        .onKeyEvent { event ->
                            if (event.key == Key.Escape) {
                                expanded = false
                                true
                            } else {
                                false
                            }
                        }
                        .onFocusChanged { focused ->
                            if (focused.isFocused) {
                                sheetFocusRequester.requestFocus()
                            }
                        },
                ) {
                    // Request focus when sheet opens
                    LaunchedEffect(Unit) {
                        sheetFocusRequester.requestFocus()
                    }
                    var searchQuery by remember { mutableStateOf("") }
                    val hasFreeModels = remember(pickerModels) { pickerModels.any { it.isFreeTier } }
                    var freeFilterOnly by remember { mutableStateOf(false) }
                    // Reset free filter when the service has no free models.
                    LaunchedEffect(hasFreeModels) {
                        if (!hasFreeModels) freeFilterOnly = false
                    }
                    val filteredModels = remember(pickerModels, searchQuery, freeFilterOnly) {
                        pickerModels.filter { model ->
                            val matchesFree = !freeFilterOnly || model.isFreeTier
                            val matchesSearch = searchQuery.isBlank() ||
                                model.id.contains(searchQuery, ignoreCase = true) ||
                                model.subtitle.contains(searchQuery, ignoreCase = true) ||
                                model.displayName?.contains(searchQuery, ignoreCase = true) == true
                            matchesFree && matchesSearch
                        }
                    }
                    if (pickerModels.size > 6) {
                        KaiSearchField(
                            query = searchQuery,
                            onQueryChange = { searchQuery = it },
                            placeholder = stringResource(Res.string.settings_model_search),
                        )
                    }
                    var sortOption by remember { mutableStateOf(ModelSortOption.Alpha) }
                    val sortedModels = remember(filteredModels, sortOption, searchQuery) {
                        val base = filteredModels.sortedWith(sortOption.comparator)
                        if (searchQuery.isBlank()) {
                            base
                        } else {
                            base.sortedBy { model ->
                                val name = model.displayName ?: model.id
                                when {
                                    name.contains(searchQuery, ignoreCase = true) -> 0
                                    model.subtitle.contains(searchQuery, ignoreCase = true) -> 1
                                    else -> 2
                                }
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ModelSortOption.entries.forEach { option ->
                            FilterChip(
                                selected = sortOption == option,
                                onClick = { sortOption = option },
                                label = { Text(stringResource(option.labelRes)) },
                                modifier = Modifier.handCursor(),
                            )
                        }
                        if (hasFreeModels) {
                            FilterChip(
                                selected = freeFilterOnly,
                                onClick = { freeFilterOnly = !freeFilterOnly },
                                label = { Text(stringResource(Res.string.model_filter_free)) },
                                modifier = Modifier.handCursor(),
                            )
                        }
                    }
                    val gridState = rememberLazyGridState()
                    LaunchedEffect(sortOption, freeFilterOnly) {
                        gridState.requestScrollToItem(0)
                    }
                    if (sortedModels.isEmpty() && freeFilterOnly) {
                        Text(
                            text = stringResource(Res.string.model_free_empty),
                            style = KaiTypography.bodyMedium,
                            color = LocalDesignColorTokens.current.textSecondary,
                            modifier = Modifier.padding(24.dp),
                        )
                    } else {
                        Box {
                            LazyVerticalGrid(
                                GridCells.Adaptive(300.dp),
                                state = gridState,
                                contentPadding = PaddingValues(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                items(sortedModels, key = { it.id }) { model ->
                                    ModelCard(
                                        model = model,
                                        isSelected = currentSelectedModel?.id == model.id,
                                        onClick = {
                                            onClick(model.id)
                                            expanded = false
                                        },
                                    )
                                }
                            }
                            VerticalScrollbarForGrid(
                                gridState = gridState,
                                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                            )
                        }
                    }
                }
            }
        }
    } else {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(Res.string.settings_model_label),
                style = KaiTypography.labelLarge,
                color = LocalDesignColorTokens.current.textPrimary,
            )
            if (isLoading) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CircularProgressIndicator(modifier = Modifier.width(18.dp), strokeWidth = 2.dp)
                    Text("Fetching models from provider…", style = KaiTypography.bodySmall, color = LocalDesignColorTokens.current.textSecondary)
                }
            } else {
                Text(
                    text = errorMessage ?: "No models have been loaded yet. Retry model discovery.",
                    style = KaiTypography.bodySmall,
                    color = if (errorMessage != null) DesignTokens.Error else LocalDesignColorTokens.current.textSecondary,
                )
                OutlinedButton(onClick = onRetry, enabled = canRetry) {
                    Text("Retry model discovery")
                }
            }
        }
    }
}

internal enum class ModelSortOption(
    val labelRes: StringResource,
    val comparator: Comparator<SettingsModel>,
) {
    Alpha(
        Res.string.model_sort_alpha,
        compareByDescending<SettingsModel> { it.isFreeTier }.thenBy { it.id }.thenBy { it.displayName ?: "" },
    ),
    Date(Res.string.model_sort_date, compareByDescending<SettingsModel> { it.releaseDate }.thenBy { it.id }),
    Score(Res.string.model_sort_score, compareByDescending<SettingsModel> { it.arenaScore }.thenBy { it.id }),
    Ctx(Res.string.model_sort_context, compareByDescending<SettingsModel> { it.contextWindow }.thenBy { it.id }),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModelCard(model: SettingsModel, isSelected: Boolean, onClick: () -> Unit) {
    val displayName = model.displayName?.takeIf { it.isNotBlank() && it != model.id }
    val title = displayName ?: model.id
    val secondary = if (displayName == null && model.subtitle.isNotBlank()) model.subtitle else null
    val contextText = model.contextWindow?.let { formatContextWindow(it) }
    val releaseText = model.releaseDate?.let { formatReleaseDate(it) }
    val detailText = listOfNotNull(releaseText, model.parameterCount, contextText)
        .joinToString("  ·  ").ifEmpty { null }

    val primaryColor = if (isSelected) {
        LocalDesignColorTokens.current.textPrimary
    } else {
        LocalDesignColorTokens.current.textSecondary
    }
    val secondaryColor = if (isSelected) {
        LocalDesignColorTokens.current.textPrimary.copy(alpha = 0.7f)
    } else {
        LocalDesignColorTokens.current.textSecondary
    }
    Box(
        modifier = Modifier
            .handCursor()
            .clip(KaiShapes.card)
            .clickable { onClick() }
            .background(if (isSelected) DesignTokens.SurfaceRaised else DesignTokens.SurfaceElevated, KaiShapes.card)
            .border(BorderStroke(1.dp, DesignTokens.BorderSubtle), KaiShapes.card),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TooltipBox(
                    modifier = Modifier.weight(1f),
                    positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                    tooltip = { Text(text = title, style = KaiTypography.bodyMedium) },
                    state = rememberTooltipState(),
                ) {
                    Text(
                        text = title,
                        style = KaiTypography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = primaryColor,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (model.isFreeTier) {
                    Spacer(Modifier.width(8.dp))
                    FreeTierBadge(isSelected = isSelected)
                }
                model.arenaScore?.let { score ->
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "$score",
                        style = KaiTypography.labelSmall,
                        color = arenaScoreColor(score),
                    )
                }
            }
            secondary?.let {
                TooltipBox(
                    positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                    tooltip = { Text(text = it, style = KaiTypography.bodyMedium) },
                    state = rememberTooltipState(),
                ) {
                    Text(
                        text = it,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = KaiTypography.bodySmall,
                        color = secondaryColor,
                    )
                }
            }
            detailText?.let {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = it,
                    style = KaiTypography.labelSmall,
                    color = secondaryColor,
                )
            }
        }
    }
}

private fun arenaScoreColor(score: Int): Color = when {
    score >= 1400 -> DesignTokens.Success
    score >= 1350 -> DesignTokens.BrandCyan
    score >= 1300 -> DesignTokens.PrimaryCyan
    score >= 1250 -> DesignTokens.Warning
    else -> DesignTokens.Error
}

@Composable
private fun FreeTierBadge(isSelected: Boolean) {
    val background = if (isSelected) {
        LocalDesignColorTokens.current.textPrimary
    } else {
        DesignTokens.Success
    }
    val content = if (isSelected) {
        DesignTokens.SurfaceRaised
    } else {
        LocalDesignColorTokens.current.textPrimary
    }
    Box(
        modifier = Modifier
            .clip(KaiShapes.xs)
            .background(background, KaiShapes.xs),
    ) {
        Text(
            text = stringResource(Res.string.model_free_badge),
            style = KaiTypography.labelMedium,
            color = content,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}
