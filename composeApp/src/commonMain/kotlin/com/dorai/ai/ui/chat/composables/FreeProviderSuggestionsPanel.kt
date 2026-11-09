package com.dorai.ai.ui.chat.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.key.key
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dorai.ai.data.freeProviderSuggestions
import com.dorai.ai.network.UiError
import com.dorai.ai.ui.CardFill
import com.dorai.ai.ui.DoraiShapes
import com.dorai.ai.ui.DoraiTypography
import com.dorai.ai.ui.SurfaceRaised
import com.dorai.ai.ui.handCursor
import com.dorai.ai.ui.theme.LocalDesignColorTokens
import dorai.composeapp.generated.resources.Res
import dorai.composeapp.generated.resources.action_retry
import dorai.composeapp.generated.resources.error_free_limit_body
import dorai.composeapp.generated.resources.error_free_limit_title
import dorai.composeapp.generated.resources.ic_refresh
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun FreeProviderSuggestionsPanel(
    error: UiError,
    retry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uriHandler = LocalUriHandler.current
    // No verticalScroll — this panel is often a LazyColumn item.
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(Res.string.error_free_limit_title),
            style = DoraiTypography.titleMedium,
            color = LocalDesignColorTokens.current.textPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(Res.string.error_free_limit_body),
            style = DoraiTypography.bodyMedium,
            color = LocalDesignColorTokens.current.textSecondary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = uiErrorText(error),
            style = DoraiTypography.bodySmall,
            color = LocalDesignColorTokens.current.textSecondary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            freeProviderSuggestions.forEach { suggestion ->
                key(suggestion.service.id) {
                    Box(
                        modifier = Modifier
                            .handCursor()
                            .clickable(
                                role = Role.Button,
                                onClick = { uriHandler.openUri(suggestion.signupUrl) },
                            )
                            .clip(DoraiShapes.pill)
                            .background(CardFill, DoraiShapes.pill),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = vectorResource(suggestion.service.icon),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = LocalDesignColorTokens.current.textPrimary,
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = suggestion.service.displayName,
                                style = DoraiTypography.labelMedium,
                                color = LocalDesignColorTokens.current.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        IconButton(
            modifier = Modifier.handCursor(),
            onClick = retry,
        ) {
            Icon(
                imageVector = vectorResource(Res.drawable.ic_refresh),
                contentDescription = stringResource(Res.string.action_retry),
                tint = LocalDesignColorTokens.current.textPrimary,
            )
        }
    }
}
