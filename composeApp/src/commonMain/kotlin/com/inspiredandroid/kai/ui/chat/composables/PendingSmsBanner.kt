package com.inspiredandroid.kai.ui.chat.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.inspiredandroid.kai.data.SmsDraft
import com.inspiredandroid.kai.data.SmsDraftStatus
import com.inspiredandroid.kai.ui.AccentPrimary
import com.inspiredandroid.kai.ui.DangerAlert
import com.inspiredandroid.kai.ui.KaiTypography
import com.inspiredandroid.kai.ui.handCursor
import com.inspiredandroid.kai.ui.kaiAdaptiveCardBorder
import com.inspiredandroid.kai.ui.kaiAdaptiveCardColors
import com.inspiredandroid.kai.ui.theme.LocalDesignColorTokens
import kai.composeapp.generated.resources.Res
import kai.composeapp.generated.resources.ic_close
import kai.composeapp.generated.resources.sms_draft_banner_discard
import kai.composeapp.generated.resources.sms_draft_banner_dismiss
import kai.composeapp.generated.resources.sms_draft_banner_failed
import kai.composeapp.generated.resources.sms_draft_banner_send
import kai.composeapp.generated.resources.sms_draft_banner_sending
import kai.composeapp.generated.resources.sms_draft_banner_sent
import kai.composeapp.generated.resources.sms_draft_banner_to
import kotlinx.collections.immutable.ImmutableList
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource

/**
 * Stack of cards, one per pending/sending/sent/failed SMS draft. Explicit
 * confirmation gate — the AI stages drafts via `send_sms` / `reply_sms` but
 * nothing leaves the device until the user taps Send here.
 */
@Composable
internal fun PendingSmsBanners(
    drafts: ImmutableList<SmsDraft>,
    onSend: (String) -> Unit,
    onDiscard: (String) -> Unit,
) {
    AnimatedVisibility(
        visible = drafts.isNotEmpty(),
        enter = slideInVertically { -it },
        exit = slideOutVertically { -it },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            for (draft in drafts) {
                // Stable key so a status change on one draft doesn't recompose siblings.
                key(draft.id) {
                    PendingSmsBanner(
                        draft = draft,
                        onSend = { onSend(draft.id) },
                        onDiscard = { onDiscard(draft.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PendingSmsBanner(
    draft: SmsDraft,
    onSend: () -> Unit,
    onDiscard: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp),
        colors = kaiAdaptiveCardColors(),
        border = kaiAdaptiveCardBorder(),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(Res.string.sms_draft_banner_to, draft.address),
                    style = KaiTypography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = LocalDesignColorTokens.current.textPrimary,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val dismissLabel = stringResource(
                    if (draft.status == SmsDraftStatus.SENT) {
                        Res.string.sms_draft_banner_dismiss
                    } else {
                        Res.string.sms_draft_banner_discard
                    },
                )
                IconButton(
                    modifier = Modifier.size(24.dp).handCursor(),
                    onClick = onDiscard,
                ) {
                    Icon(
                        imageVector = vectorResource(Res.drawable.ic_close),
                        contentDescription = dismissLabel,
                        tint = LocalDesignColorTokens.current.textSecondary,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = draft.body,
                style = KaiTypography.bodySmall,
                color = LocalDesignColorTokens.current.textPrimary,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            when (draft.status) {
                SmsDraftStatus.PENDING -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(onClick = onSend) {
                            Text(stringResource(Res.string.sms_draft_banner_send))
                        }
                    }
                }

                SmsDraftStatus.SENDING -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = LocalDesignColorTokens.current.textSecondary,
                        )
                        Spacer(Modifier.size(8.dp))
                        Text(
                            text = stringResource(Res.string.sms_draft_banner_sending),
                            style = KaiTypography.labelSmall,
                            color = LocalDesignColorTokens.current.textSecondary,
                        )
                    }
                }

                SmsDraftStatus.SENT -> Text(
                    text = stringResource(Res.string.sms_draft_banner_sent),
                    style = KaiTypography.labelSmall,
                    color = AccentPrimary,
                )

                SmsDraftStatus.FAILED -> Text(
                    text = stringResource(Res.string.sms_draft_banner_failed, draft.lastError ?: "unknown error"),
                    style = KaiTypography.labelSmall,
                    color = DangerAlert,
                )
            }
        }
    }
}
