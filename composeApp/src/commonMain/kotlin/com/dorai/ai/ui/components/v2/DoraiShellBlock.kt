package com.dorai.ai.ui.components.v2

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dorai.ai.theme.tokens.DoraiSpacing
import com.dorai.ai.theme.tokens.LocalDoraiColors

@Composable
fun DoraiShellBlock(
    command: String?,
    output: String?,
    error: String?,
    modifier: Modifier = Modifier,
) {
    val colors = LocalDoraiColors.current
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(colors.codeBackground)
            .padding(DoraiSpacing.MD)
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
}
