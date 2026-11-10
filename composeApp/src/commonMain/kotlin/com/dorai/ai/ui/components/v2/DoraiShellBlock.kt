package com.dorai.ai.ui.components.v2

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dorai.ai.theme.tokens.DoraiSpacing
import com.dorai.ai.theme.tokens.LocalDoraiColors
import com.dorai.ai.ui.rememberCopyToClipboard
import dorai.composeapp.generated.resources.Res
import dorai.composeapp.generated.resources.a11y_copy
import org.jetbrains.compose.resources.stringResource

@Composable
fun DoraiShellBlock(
    command: String?,
    output: String?,
    error: String?,
    modifier: Modifier = Modifier,
    exitCode: Int? = null,
    durationText: String? = null,
) {
    val colors = LocalDoraiColors.current
    val scrollState = rememberScrollState()
    val copyToClipboard = rememberCopyToClipboard()

    val fullText = buildString {
        if (!command.isNullOrEmpty()) appendLine("$ $command")
        if (!output.isNullOrEmpty()) appendLine(output)
        if (!error.isNullOrEmpty()) appendLine(error)
    }.trimEnd()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(colors.codeBackground)
            .padding(DoraiSpacing.MD),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "TERMINAL",
                    color = colors.muted.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                )
                if (fullText.isNotEmpty()) {
                    IconButton(
                        onClick = { copyToClipboard(fullText) },
                        modifier = Modifier.size(24.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = stringResource(Res.string.a11y_copy),
                            tint = colors.muted,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(DoraiSpacing.XS))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState),
            ) {
                if (!command.isNullOrEmpty()) {
                    Text(
                        text = "$ $command",
                        color = colors.info,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.5.sp,
                        lineHeight = 18.sp,
                    )
                }

                if (!output.isNullOrEmpty()) {
                    Text(
                        text = output,
                        color = colors.text,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.5.sp,
                        lineHeight = 18.sp,
                    )
                }

                if (!error.isNullOrEmpty()) {
                    Text(
                        text = error,
                        color = colors.err,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.5.sp,
                        lineHeight = 18.sp,
                    )
                }
            }

            if (exitCode != null || durationText != null) {
                Spacer(modifier = Modifier.height(DoraiSpacing.SM))
                val footer = listOfNotNull(
                    exitCode?.let { "exit $it" },
                    durationText,
                ).joinToString(" · ")
                Text(
                    text = footer,
                    color = colors.muted,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                )
            }
        }
    }
}
