package com.dorai.ai.ui.chat.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dorai.ai.network.UiError
import com.dorai.ai.ui.handCursor
import com.dorai.ai.ui.theme.LocalDesignColorTokens
import dorai.composeapp.generated.resources.Res
import dorai.composeapp.generated.resources.ic_refresh
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource

@Composable
internal fun uiErrorText(error: UiError): String = when (error) {
    is UiError.Resource -> stringResource(error.resource)
    is UiError.Text -> error.message
    is UiError.ResourceWithDetail -> "${stringResource(error.resource)}: ${error.detail}"
}

@Composable
internal fun ErrorMessage(
    error: UiError,
    retry: () -> Unit,
) {
    val text = uiErrorText(error)
    Column(
        modifier = Modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = text,
            color = LocalDesignColorTokens.current.textPrimary,
        )
        Spacer(Modifier.height(8.dp))
        IconButton(
            modifier = Modifier.handCursor(),
            onClick = retry,
        ) {
            Icon(
                imageVector = vectorResource(Res.drawable.ic_refresh),
                contentDescription = "Retry",
                tint = LocalDesignColorTokens.current.textPrimary,
            )
        }
    }
}
