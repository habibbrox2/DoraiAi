package com.inspiredandroid.kai.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.inspiredandroid.kai.ui.AccentPrimary
import com.inspiredandroid.kai.ui.BorderSoft
import com.inspiredandroid.kai.ui.KaiShapes
import com.inspiredandroid.kai.ui.KaiTypography
import com.inspiredandroid.kai.ui.SurfaceElevated
import com.inspiredandroid.kai.ui.theme.LocalDesignColorTokens
import kai.composeapp.generated.resources.Res
import org.jetbrains.compose.resources.vectorResource

/**
 * Circular avatar with optional initials fallback and status indicator dot.
 *
 * @param initials Optional 1-2 character initials shown when no image is provided
 * @param size Diameter of the avatar (default 40.dp)
 * @param borderColor Border color (default BorderSoft)
 * @param statusColor Optional status dot color (null = no dot)
 * @param modifier Compose modifier
 */
@Composable
fun KaiAvatar(
    initials: String? = null,
    size: Dp = 40.dp,
    borderColor: Color = BorderSoft,
    statusColor: Color? = null,
    modifier: Modifier = Modifier,
) {
    val colors = LocalDesignColorTokens.current
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .border(BorderStroke(1.dp, borderColor), CircleShape)
            .background(SurfaceElevated, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (initials != null && initials.isNotEmpty()) {
            Text(
                text = initials.take(2).uppercase(),
                style = KaiTypography.labelMedium,
                color = AccentPrimary,
                modifier = Modifier.align(Alignment.Center),
            )
        } else {
            // Fallback: simple dot when no initials provided
            Box(
                modifier = Modifier
                    .size(size * 0.5f)
                    .clip(CircleShape)
                    .background(colors.textSecondary),
            )
        }

        // Status indicator dot (bottom-right)
        if (statusColor != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(size * 0.25f)
                    .clip(CircleShape)
                    .background(statusColor)
                    .border(BorderStroke(1.dp, BorderSoft), CircleShape),
            )
        }
    }
}
