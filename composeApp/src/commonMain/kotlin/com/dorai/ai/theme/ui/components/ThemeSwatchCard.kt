package com.dorai.ai.theme.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dorai.ai.theme.data.ThemePalette
import com.dorai.ai.ui.handCursor
import com.dorai.ai.ui.theme.DesignTokens
import com.dorai.ai.ui.DoraiShapes
import com.dorai.ai.ui.theme.LocalDesignColorTokens
import dorai.composeapp.generated.resources.create_theme_remove
import dorai.composeapp.generated.resources.Res
import org.jetbrains.compose.resources.stringResource

/**
 * Preview swatch for one theme palette: two mockup content bars, accent dots and a
 * bottom-right accent pill rendered in the palette's own colors. Selecting shows a cyan ring
 * animated with a spring. Custom themes get a small remove badge.
 */
@Composable
fun ThemeSwatchCard(
    palette: ThemePalette,
    selected: Boolean,
    onClick: () -> Unit,
    onDelete: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val shape = DoraiShapes.input
    val interactionSource = remember { MutableInteractionSource() }

    val ringWidth by animateDpAsState(
        targetValue = if (selected) 2.5.dp else 1.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium),
        label = "swatchRingWidth",
    )
    val ringColor by animateColorAsState(
        targetValue = if (selected) DesignTokens.PrimaryCyan else palette.outlineColor,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
        label = "swatchRingColor",
    )
    val cardWidth = 104.dp
    val cardHeight = 78.dp

    Column(modifier = modifier.width(cardWidth)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(cardHeight)
                .clip(shape)
                .background(palette.backgroundColor)
                .handCursor()
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    role = Role.RadioButton,
                    onClick = onClick,
                )
                .border(BorderStroke(ringWidth, ringColor), shape)
                .semantics { contentDescription = palette.displayName },
        ) {
            // Mockup content: title bar + secondary bar, accent dots, accent pill bottom-right.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.72f)
                        .height(9.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(palette.surfaceElevatedColor),
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.44f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(palette.onSurfaceColor.copy(alpha = 0.32f)),
                )
                Spacer(Modifier.weight(1f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(Modifier.size(6.dp).clip(CircleShape).background(palette.primaryColor))
                        Box(Modifier.size(6.dp).clip(CircleShape).background(palette.accentColor))
                        Box(Modifier.size(6.dp).clip(CircleShape).background(palette.outlineColor))
                    }
                    Spacer(Modifier.weight(1f))
                    Box(
                        modifier = Modifier
                            .width(30.dp)
                            .height(15.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(palette.accentColor),
                    )
                }
            }

            if (palette.isCustom && onDelete != null) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(26.dp)
                        .padding(2.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(Res.string.create_theme_remove),
                        tint = palette.onSurfaceColor,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }
        Text(
            text = palette.displayName,
            style = LocalTextStyle.current.copy(fontSize = 11.sp, lineHeight = 14.sp),
            color = LocalDesignColorTokens.current.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp, start = 2.dp, end = 2.dp),
        )
    }
}
