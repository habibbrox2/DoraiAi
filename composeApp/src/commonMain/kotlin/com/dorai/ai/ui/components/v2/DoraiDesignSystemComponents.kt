package com.dorai.ai.ui.components.v2

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dorai.ai.theme.tokens.DoraiSpacing
import com.dorai.ai.theme.tokens.DoraiType
import com.dorai.ai.theme.tokens.LocalDoraiColors
import com.dorai.ai.ui.DoraiTypography
import com.dorai.ai.ui.handCursor
import com.dorai.ai.ui.theme.DesignTokens
import dorai.composeapp.generated.resources.Res
import dorai.composeapp.generated.resources.a11y_copy_details
import dorai.composeapp.generated.resources.action_retry
import dorai.composeapp.generated.resources.agent_activity_details
import dorai.composeapp.generated.resources.agent_activity_hide_details
import dorai.composeapp.generated.resources.chat_action_copied
import dorai.composeapp.generated.resources.status_error
import org.jetbrains.compose.resources.stringResource

/**
 * 1. SettingsCard:
 * Consistent card surface with 3-level surface support, 16dp rounded corners,
 * subtle 1dp border, and >=16dp padding.
 */
@Composable
fun SettingsCard(
    modifier: Modifier = Modifier,
    elevated: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = LocalDoraiColors.current
    val surfaceColor = if (elevated) colors.surface2 else colors.surface
    val shape = RoundedCornerShape(DesignTokens.RadiusCard)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(surfaceColor)
            .border(1.dp, colors.line, shape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick, role = Role.Button).handCursor()
                } else {
                    Modifier
                },
            )
            .padding(DoraiSpacing.LG),
        content = content,
    )
}

/**
 * 2. SettingsRow:
 * Minimum 48dp touch target row with optional leading icon, title, secondary subtitle
 * (with WCAG AA contrast >= 4.5:1), and trailing control slot.
 */
@Composable
fun SettingsRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val colors = LocalDoraiColors.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = DesignTokens.MinTouchTarget)
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick, role = Role.Button).handCursor()
                } else {
                    Modifier
                },
            )
            .padding(vertical = DoraiSpacing.SM),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingIcon != null) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(colors.surface2),
                contentAlignment = Alignment.Center,
            ) {
                leadingIcon()
            }
            Spacer(modifier = Modifier.width(DoraiSpacing.MD))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = DoraiTypography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = colors.text,
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = DoraiTypography.bodySmall,
                    color = colors.muted,
                )
            }
        }

        if (trailingContent != null) {
            Spacer(modifier = Modifier.width(DoraiSpacing.SM))
            trailingContent()
        }
    }
}

/**
 * 3. SectionHeader:
 * M3 typography section header: larger, bold, brighter, with optional count badge/pill
 * and subtitle helper text.
 */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    badgeText: String? = null,
    subtitle: String? = null,
) {
    val colors = LocalDoraiColors.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics { heading() }
            .padding(horizontal = DoraiSpacing.XS, vertical = DoraiSpacing.SM),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DoraiSpacing.SM),
        ) {
            Text(
                text = title,
                style = DoraiTypography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                ),
                color = colors.text,
            )
            if (!badgeText.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(colors.primary.copy(alpha = 0.16f))
                        .border(1.dp, colors.primary.copy(alpha = 0.40f), CircleShape)
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = badgeText,
                        color = colors.primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        if (!subtitle.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = DoraiTypography.bodySmall,
                color = colors.muted,
            )
        }
    }
}

/**
 * 4. AppSwitch:
 * Compact M3 switch using Primary Magenta for active state, subtle surface for inactive.
 * Never uses cyan. Minimum 48dp touch target, with proper right padding so it is never
 * glued to the right edge of cards.
 */
@Composable
fun AppSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = LocalDoraiColors.current

    Box(
        modifier = modifier
            .defaultMinSize(minHeight = DesignTokens.MinTouchTarget, minWidth = DesignTokens.MinTouchTarget)
            .padding(end = 4.dp),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = colors.onPrimary,
                checkedTrackColor = colors.primary,
                checkedBorderColor = colors.primary,
                uncheckedThumbColor = colors.muted,
                uncheckedTrackColor = colors.surface2,
                uncheckedBorderColor = colors.line,
                disabledCheckedThumbColor = colors.onPrimary.copy(alpha = 0.5f),
                disabledCheckedTrackColor = colors.primary.copy(alpha = 0.5f),
                disabledUncheckedThumbColor = colors.muted.copy(alpha = 0.5f),
                disabledUncheckedTrackColor = colors.surface.copy(alpha = 0.5f),
            ),
        )
    }
}

/**
 * 5. AppSlider:
 * Thin track (4dp), larger thumb (>=20dp) with elevation, value label, and valid min..max range.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    valueLabel: String? = null,
    minRangeLabel: String? = null,
    maxRangeLabel: String? = null,
    onValueChangeFinished: (() -> Unit)? = null,
) {
    val colors = LocalDoraiColors.current

    Column(modifier = modifier.fillMaxWidth()) {
        if (valueLabel != null || minRangeLabel != null || maxRangeLabel != null) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = DoraiSpacing.XS),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (minRangeLabel != null) {
                    Text(
                        text = minRangeLabel,
                        style = DoraiTypography.labelSmall,
                        color = colors.muted,
                    )
                }
                if (valueLabel != null) {
                    Text(
                        text = valueLabel,
                        style = DoraiTypography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = colors.primary,
                    )
                }
                if (maxRangeLabel != null) {
                    Text(
                        text = maxRangeLabel,
                        style = DoraiTypography.labelSmall,
                        color = colors.muted,
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
        }

        Slider(
            value = value,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier.fillMaxWidth().handCursor(),
            colors = SliderDefaults.colors(
                thumbColor = colors.primary,
                activeTrackColor = colors.primary,
                inactiveTrackColor = colors.surface2,
            ),
            thumb = {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .shadow(elevation = 3.dp, shape = CircleShape)
                        .background(colors.primary, CircleShape)
                        .border(2.dp, colors.surface2, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(colors.onPrimary, CircleShape),
                    )
                }
            },
            track = { sliderState ->
                SliderDefaults.Track(
                    sliderState = sliderState,
                    colors = SliderDefaults.colors(
                        activeTrackColor = colors.primary,
                        inactiveTrackColor = colors.surface2,
                    ),
                    modifier = Modifier.height(4.dp),
                    drawStopIndicator = null,
                    drawTick = { _, _ -> },
                )
            },
        )
    }
}

/**
 * 6. SegmentedPillSelector:
 * Container-matching segmented control with pill options. Selected option highlighted
 * with primary magenta, unselected with subtle surface.
 */
@Composable
fun <T> SegmentedPillSelector(
    items: List<T>,
    selectedItem: T,
    onItemSelected: (T) -> Unit,
    labelProvider: @Composable (T) -> String,
    modifier: Modifier = Modifier,
    iconProvider: (@Composable (T) -> ImageVector)? = null,
) {
    val colors = LocalDoraiColors.current
    val containerShape = RoundedCornerShape(12.dp)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(containerShape)
            .background(colors.surface2)
            .border(1.dp, colors.line, containerShape)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items.forEach { item ->
            val isSelected = item == selectedItem
            val pillShape = RoundedCornerShape(8.dp)

            Box(
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 40.dp)
                    .clip(pillShape)
                    .background(if (isSelected) colors.primary else Color.Transparent)
                    .selectable(
                        selected = isSelected,
                        onClick = { onItemSelected(item) },
                        role = Role.RadioButton,
                    )
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    if (iconProvider != null) {
                        val icon = iconProvider(item)
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isSelected) colors.onPrimary else colors.muted,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = labelProvider(item),
                        color = if (isSelected) colors.onPrimary else colors.muted,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/**
 * 7. StatusChip:
 * Semantic status indicator pairing semantic color WITH an icon (check / warning / error / info).
 * Never relies on color alone.
 */
enum class StatusType {
    SUCCESS,
    WARNING,
    ERROR,
    INFO,
}

@Composable
fun StatusChip(
    text: String,
    type: StatusType,
    modifier: Modifier = Modifier,
) {
    val colors = LocalDoraiColors.current
    val (color, icon) = when (type) {
        StatusType.SUCCESS -> colors.ok to Icons.Default.Check
        StatusType.WARNING -> colors.warn to Icons.Default.WarningAmber
        StatusType.ERROR -> colors.err to Icons.Default.Close
        StatusType.INFO -> colors.info to Icons.Default.Info
    }

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(color.copy(alpha = 0.16f))
            .border(1.dp, color.copy(alpha = 0.40f), CircleShape)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(13.dp),
            )
            Text(
                text = text,
                color = color,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

/**
 * 8. ActionButton:
 * Primary (filled magenta), Secondary (tonal surface), Destructive (soft red outline/tonal),
 * and Outlined variants. Min 48dp touch target.
 */
enum class ActionButtonVariant {
    PRIMARY,
    SECONDARY,
    DESTRUCTIVE,
    OUTLINED,
}

@Composable
fun ActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ActionButtonVariant = ActionButtonVariant.PRIMARY,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val colors = LocalDoraiColors.current
    val shape = RoundedCornerShape(10.dp)

    when (variant) {
        ActionButtonVariant.PRIMARY -> {
            Button(
                onClick = onClick,
                enabled = enabled,
                shape = shape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primary,
                    contentColor = colors.onPrimary,
                    disabledContainerColor = colors.primary.copy(alpha = 0.38f),
                    disabledContentColor = colors.onPrimary.copy(alpha = 0.38f),
                ),
                modifier = modifier.defaultMinSize(minHeight = DesignTokens.MinTouchTarget).handCursor(),
            ) {
                if (icon != null) {
                    Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(text = text, fontWeight = FontWeight.SemiBold)
            }
        }

        ActionButtonVariant.SECONDARY -> {
            Button(
                onClick = onClick,
                enabled = enabled,
                shape = shape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.surface2,
                    contentColor = colors.text,
                    disabledContainerColor = colors.surface2.copy(alpha = 0.38f),
                    disabledContentColor = colors.muted.copy(alpha = 0.38f),
                ),
                modifier = modifier
                    .defaultMinSize(minHeight = DesignTokens.MinTouchTarget)
                    .border(1.dp, colors.line, shape)
                    .handCursor(),
            ) {
                if (icon != null) {
                    Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(text = text, fontWeight = FontWeight.SemiBold)
            }
        }

        ActionButtonVariant.DESTRUCTIVE -> {
            Button(
                onClick = onClick,
                enabled = enabled,
                shape = shape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.err,
                    contentColor = Color.White,
                    disabledContainerColor = colors.err.copy(alpha = 0.38f),
                    disabledContentColor = Color.White.copy(alpha = 0.7f),
                ),
                modifier = modifier.defaultMinSize(minHeight = DesignTokens.MinTouchTarget).handCursor(),
            ) {
                if (icon != null) {
                    Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(text = text, fontWeight = FontWeight.SemiBold)
            }
        }

        ActionButtonVariant.OUTLINED -> {
            OutlinedButton(
                onClick = onClick,
                enabled = enabled,
                shape = shape,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = colors.text,
                ),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = androidx.compose.ui.graphics.SolidColor(colors.line),
                ),
                modifier = modifier.defaultMinSize(minHeight = DesignTokens.MinTouchTarget).handCursor(),
            ) {
                if (icon != null) {
                    Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(text = text, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/**
 * 9. AppTextField:
 * Visible outline/tonal fill, floating or top label, proper padding, password show/hide and paste support.
 */
@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    isError: Boolean = false,
    errorMessage: String? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: (@Composable () -> Unit)? = null,
    singleLine: Boolean = true,
    enabled: Boolean = true,
) {
    val colors = LocalDoraiColors.current
    val shape = RoundedCornerShape(10.dp)

    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            label = if (label != null) {
                { Text(label) }
            } else {
                null
            },
            placeholder = if (placeholder != null) {
                { Text(placeholder, color = colors.muted) }
            } else {
                null
            },
            isError = isError,
            singleLine = singleLine,
            visualTransformation = visualTransformation,
            trailingIcon = trailingIcon,
            shape = shape,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = colors.surface2,
                unfocusedContainerColor = colors.surface,
                disabledContainerColor = colors.surface.copy(alpha = 0.5f),
                focusedBorderColor = colors.primary,
                unfocusedBorderColor = colors.line,
                focusedTextColor = colors.text,
                unfocusedTextColor = colors.text,
                focusedLabelColor = colors.primary,
                unfocusedLabelColor = colors.muted,
                errorBorderColor = colors.err,
                errorLabelColor = colors.err,
            ),
        )

        if (isError && !errorMessage.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(horizontal = 4.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = colors.err,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = errorMessage,
                    style = DoraiTypography.labelSmall,
                    color = colors.err,
                )
            }
        }
    }
}

/**
 * 10. EmptyState:
 * Centered icon + title + helper + optional CTA.
 */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    helper: String,
    modifier: Modifier = Modifier,
    ctaText: String? = null,
    onCtaClick: (() -> Unit)? = null,
) {
    val colors = LocalDoraiColors.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(DoraiSpacing.XL),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(colors.surface2)
                .border(1.dp, colors.line, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.muted,
                modifier = Modifier.size(32.dp),
            )
        }

        Spacer(modifier = Modifier.height(DoraiSpacing.MD))

        Text(
            text = title,
            style = DoraiTypography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = colors.text,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(modifier = Modifier.height(DoraiSpacing.XS))

        Text(
            text = helper,
            style = DoraiTypography.bodyMedium,
            color = colors.muted,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = DoraiSpacing.MD),
        )

        if (!ctaText.isNullOrBlank() && onCtaClick != null) {
            Spacer(modifier = Modifier.height(DoraiSpacing.LG))
            ActionButton(
                text = ctaText,
                onClick = onCtaClick,
                variant = ActionButtonVariant.PRIMARY,
            )
        }
    }
}

/**
 * 11. ErrorLogItem:
 * Collapsible error card with status icon, relative time, short human-readable message,
 * collapsed raw URLs / stack trace behind expander, soft red border/fill, and "Retry" and "Copy details".
 */
@Composable
fun ErrorLogItem(
    title: String,
    timeText: String,
    shortMessage: String,
    modifier: Modifier = Modifier,
    detailsText: String? = null,
    hintText: String? = null,
    onRetry: (() -> Unit)? = null,
) {
    val colors = LocalDoraiColors.current
    val clipboardManager = LocalClipboardManager.current
    var isExpanded by remember { mutableStateOf(false) }
    var copied by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(12.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.err.copy(alpha = 0.08f))
            .border(1.dp, colors.err.copy(alpha = 0.35f), shape)
            .padding(DoraiSpacing.MD),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = stringResource(Res.string.status_error),
                tint = colors.err,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(DoraiSpacing.SM))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = DoraiTypography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.text,
                )
                Text(
                    text = timeText,
                    style = DoraiTypography.labelSmall,
                    color = colors.muted,
                )
            }
        }

        Spacer(modifier = Modifier.height(DoraiSpacing.SM))

        Text(
            text = shortMessage,
            style = DoraiType.BodyMedium,
            color = colors.text,
        )

        if (!hintText.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(DoraiSpacing.XS))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = colors.info,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = hintText,
                    style = DoraiTypography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = colors.info,
                )
            }
        }

        AnimatedVisibility(
            visible = isExpanded && !detailsText.isNullOrBlank(),
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            Column {
                Spacer(modifier = Modifier.height(DoraiSpacing.SM))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.codeBackground)
                        .border(1.dp, colors.line, RoundedCornerShape(8.dp))
                        .padding(DoraiSpacing.SM),
                ) {
                    Text(
                        text = detailsText.orEmpty(),
                        style = DoraiType.Code,
                        color = colors.text,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(DoraiSpacing.SM))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (!detailsText.isNullOrBlank()) {
                TextButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.handCursor(),
                ) {
                    Text(
                        text = stringResource(if (isExpanded) Res.string.agent_activity_hide_details else Res.string.agent_activity_details),
                        color = colors.muted,
                        style = DoraiTypography.labelMedium,
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = colors.muted,
                        modifier = Modifier.size(16.dp),
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(1.dp))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(DoraiSpacing.XS)) {
                if (!detailsText.isNullOrBlank()) {
                    ActionButton(
                        text = stringResource(if (copied) Res.string.chat_action_copied else Res.string.a11y_copy_details),
                        icon = Icons.Default.ContentCopy,
                        onClick = {
                            clipboardManager.setText(AnnotatedString(detailsText))
                            copied = true
                        },
                        variant = ActionButtonVariant.SECONDARY,
                        modifier = Modifier.height(36.dp),
                    )
                }

                if (onRetry != null) {
                    ActionButton(
                        text = stringResource(Res.string.action_retry),
                        icon = Icons.Default.Refresh,
                        onClick = onRetry,
                        variant = ActionButtonVariant.PRIMARY,
                        modifier = Modifier.height(36.dp),
                    )
                }
            }
        }
    }
}
