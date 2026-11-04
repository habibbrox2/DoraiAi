package com.dorai.ai.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dorai.ai.ui.KaiTypography
import com.dorai.ai.ui.theme.DesignTokens
import com.dorai.ai.ui.theme.LocalDesignColorTokens

/**
 * 2027 Premium Obsidian Dialog for confirmations, alerts, and forms.
 *
 * @param onDismissRequest Called when the user requests dismissal (backdrop click / escape)
 * @param title Optional dialog title
 * @param text Optional body text
 * @param confirmButton Optional confirm button (onClick + label)
 * @param dismissButton Optional dismiss button (onClick + label)
 * @param modifier Compose modifier
 */
@Composable
fun KaiDialog(
    onDismissRequest: () -> Unit,
    title: String? = null,
    text: String? = null,
    confirmButton: KaiDialogButton? = null,
    dismissButton: KaiDialogButton? = null,
    modifier: Modifier = Modifier,
) {
    val dialogFocusRequester = remember { FocusRequester() }
    val shape = RoundedCornerShape(16.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp)
            .focusRequester(dialogFocusRequester)
            .onKeyEvent { event ->
                if (event.key == Key.Escape) {
                    onDismissRequest()
                    true
                } else {
                    false
                }
            }
            .onFocusChanged { focused ->
                if (focused.isFocused) {
                    dialogFocusRequester.requestFocus()
                }
            }
            .clip(shape)
            .background(DesignTokens.SurfaceElevated, shape)
            .border(BorderStroke(1.dp, DesignTokens.BorderSubtle), shape)
            .padding(20.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp),
        ) {
            LaunchedEffect(Unit) {
                dialogFocusRequester.requestFocus()
            }

            title?.let { titleText ->
                Text(
                    text = titleText,
                    style = KaiTypography.headlineSmall,
                    color = LocalDesignColorTokens.current.textPrimary,
                )
            }

            text?.let { bodyText ->
                Text(
                    text = bodyText,
                    style = KaiTypography.bodyMedium,
                    color = LocalDesignColorTokens.current.textSecondary,
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                dismissButton?.let { btn ->
                    TextButton(
                        onClick = {
                            btn.onClick()
                            onDismissRequest()
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = LocalDesignColorTokens.current.textSecondary,
                        ),
                        modifier = Modifier.padding(end = 8.dp),
                    ) {
                        Text(text = btn.label)
                    }
                }

                confirmButton?.let { btn ->
                    TextButton(
                        onClick = {
                            btn.onClick()
                            onDismissRequest()
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = DesignTokens.PrimaryCyan,
                        ),
                    ) {
                        Text(text = btn.label, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

/** Button configuration for KaiDialog. */
data class KaiDialogButton(
    val label: String,
    val onClick: () -> Unit,
)
