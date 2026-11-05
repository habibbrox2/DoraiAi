package com.dorai.ai.ui.chat.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.dorai.ai.ui.handCursor
import com.dorai.ai.ui.theme.LocalDesignColorTokens
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Composable
internal fun SmallIconButton(
    iconResource: DrawableResource,
    contentDescription: String? = null,
    onClick: () -> Unit,
) {
    SmallIconButtonBox(onClick) {
        Icon(
            modifier = Modifier.size(20.dp),
            painter = painterResource(iconResource),
            contentDescription = contentDescription,
            tint = LocalDesignColorTokens.current.textPrimary,
        )
    }
}

@Composable
internal fun SmallIconButton(
    imageVector: ImageVector,
    contentDescription: String? = null,
    onClick: () -> Unit,
) {
    SmallIconButtonBox(onClick) {
        Icon(
            modifier = Modifier.size(20.dp),
            imageVector = imageVector,
            contentDescription = contentDescription,
            tint = LocalDesignColorTokens.current.textPrimary,
        )
    }
}

@Composable
private fun SmallIconButtonBox(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier.size(36.dp).clip(CircleShape).handCursor().clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) { content() }
}
