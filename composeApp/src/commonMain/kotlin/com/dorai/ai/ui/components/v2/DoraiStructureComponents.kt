package com.dorai.ai.ui.components.v2

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dorai.ai.theme.data.ThemePalette
import com.dorai.ai.theme.tokens.DoraiSpacing
import com.dorai.ai.theme.tokens.LocalDoraiColors
import com.dorai.ai.ui.adaptive.LocalDoraiWindowWidthClass
import dorai.composeapp.generated.resources.Res
import dorai.composeapp.generated.resources.chat_provider_cloud
import dorai.composeapp.generated.resources.chat_provider_local
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

/**
 * Scrollable pill tabs used across Settings, Workspace tabs, etc.
 */
@Composable
fun <T> DoraiPillTabs(
    items: List<T>,
    selectedItem: T,
    onItemSelected: (T) -> Unit,
    labelProvider: @Composable (T) -> String,
    modifier: Modifier = Modifier,
) {
    val colors = LocalDoraiColors.current
    val scrollState = rememberScrollState()
    // Track each chip's offset/width so the selected one is always scrolled into view.
    val itemMeasurements = remember { mutableStateMapOf<Int, Pair<Int, Int>>() }
    var viewportWidth by remember { mutableIntStateOf(0) }
    val allMeasured = itemMeasurements.size == items.size

    LaunchedEffect(selectedItem, allMeasured) {
        if (!allMeasured) return@LaunchedEffect
        val index = items.indexOf(selectedItem)
        if (index < 0) return@LaunchedEffect
        val (x, width) = itemMeasurements[index] ?: return@LaunchedEffect
        val start = scrollState.value
        val end = start + viewportWidth
        val target = when {
            x < start -> x
            x + width > end -> (x + width - viewportWidth)
            else -> null
        }
        if (target != null) scrollState.animateScrollTo(target.coerceAtLeast(0))
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .onSizeChanged { viewportWidth = it.width }
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(DoraiSpacing.SM),
    ) {
        items.forEachIndexed { index, item ->
            val isSelected = item == selectedItem
            val label = labelProvider(item)

            Box(
                modifier = Modifier
                    .onGloballyPositioned { coordinates ->
                        itemMeasurements[index] =
                            (coordinates.positionInParent().x.roundToInt() to coordinates.size.width)
                    }
                    .defaultMinSize(minHeight = 48.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) colors.primary else colors.surface)
                    .border(1.dp, if (isSelected) colors.primary else colors.line, CircleShape)
                    .selectable(selected = isSelected, onClick = { onItemSelected(item) }, role = Role.Tab)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    color = if (isSelected) colors.onPrimary else colors.muted,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * Preset swatch card representing each of the 13 theme palettes.
 */
@Composable
fun DoraiThemePresetTile(
    palette: ThemePalette,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalDoraiColors.current
    val shape = RoundedCornerShape(12.dp)

    Column(
        modifier = modifier
            .clip(shape)
            .background(palette.surfaceColor)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) colors.primary else palette.outlineColor,
                shape = shape,
            )
            .selectable(selected = isSelected, onClick = onSelect, role = Role.RadioButton)
            .padding(DoraiSpacing.SM),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Mockup swatch
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(palette.backgroundColor)
                .border(1.dp, palette.outlineColor, RoundedCornerShape(8.dp))
                .padding(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(palette.primaryColor),
                )
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(colors.primary),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = colors.onPrimary,
                            modifier = Modifier.size(12.dp),
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(DoraiSpacing.XS))

        Text(
            text = palette.displayName,
            color = palette.onSurfaceColor,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Status Badge (Running, Completed, Warning, Stopped, etc.)
 */
@Composable
fun DoraiStatusBadge(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(color.copy(alpha = 0.16f))
            .border(1.dp, color.copy(alpha = 0.40f), CircleShape)
            .padding(horizontal = 8.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/**
 * Provider indicator badge (Local vs Cloud).
 */
@Composable
fun DoraiProviderBadge(
    isLocal: Boolean,
    modifier: Modifier = Modifier,
    iconOnly: Boolean = false,
) {
    val colors = LocalDoraiColors.current
    val badgeColor = if (isLocal) colors.ok else colors.info
    val text = if (isLocal) stringResource(Res.string.chat_provider_local) else stringResource(Res.string.chat_provider_cloud)
    val icon = if (isLocal) Icons.Default.Computer else Icons.Default.Cloud

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(badgeColor.copy(alpha = 0.16f))
            .border(1.dp, badgeColor.copy(alpha = 0.40f), CircleShape)
            .padding(horizontal = if (iconOnly) 6.dp else 8.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                // Non-iconOnly: the label text is adjacent — announcing both reads
                // "Local, Local" on screen readers. iconOnly: the icon carries the
                // meaning alone, so its content description is required.
                contentDescription = if (iconOnly) text else null,
                tint = badgeColor,
                modifier = Modifier.size(14.dp),
            )
            if (!iconOnly) {
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = text,
                    color = badgeColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

/**
 * Adaptive sheet wrapper: BottomSheet on Compact, Dialog on Medium/Expanded/Large.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoraiModalSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val widthClass = LocalDoraiWindowWidthClass.current
    val colors = LocalDoraiColors.current

    if (widthClass.isCompact) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = onDismissRequest,
            sheetState = sheetState,
            containerColor = colors.surface,
            contentColor = colors.text,
            modifier = modifier,
        ) {
            content()
        }
    } else {
        BasicAlertDialog(
            onDismissRequest = onDismissRequest,
            modifier = modifier
                .clip(RoundedCornerShape(20.dp))
                .background(colors.surface)
                .border(1.dp, colors.line, RoundedCornerShape(20.dp))
                .padding(DoraiSpacing.LG),
        ) {
            content()
        }
    }
}
