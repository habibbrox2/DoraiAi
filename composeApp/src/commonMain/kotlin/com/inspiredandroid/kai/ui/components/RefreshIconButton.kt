package com.inspiredandroid.kai.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.inspiredandroid.kai.ui.handCursor
import com.inspiredandroid.kai.ui.theme.LocalDesignColorTokens

@Composable
fun RefreshIconButton(
    onClick: () -> Unit,
    isRefreshing: Boolean,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val colors = LocalDesignColorTokens.current
    IconButton(
        onClick = onClick,
        enabled = !isRefreshing,
        modifier = modifier.handCursor(),
    ) {
        if (isRefreshing) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = colors.textSecondary,
            )
        } else {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = contentDescription,
                tint = colors.textSecondary,
            )
        }
    }
}
