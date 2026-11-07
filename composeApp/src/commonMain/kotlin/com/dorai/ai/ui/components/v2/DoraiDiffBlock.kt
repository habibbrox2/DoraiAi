package com.dorai.ai.ui.components.v2

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dorai.ai.theme.tokens.LocalDoraiColors

@Composable
fun DoraiDiffBlock(
    diff: String,
    modifier: Modifier = Modifier,
) {
    val colors = LocalDoraiColors.current
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(colors.codeBackground)
            .padding(vertical = 8.dp)
            .horizontalScroll(scrollState),
    ) {
        val lines = diff.lines()
        lines.forEach { line ->
            val isAddition = line.startsWith("+") && !line.startsWith("+++")
            val isDeletion = line.startsWith("-") && !line.startsWith("---")
            val isHunkHeader = line.startsWith("@@")

            val textColor = when {
                isAddition -> colors.ok
                isDeletion -> colors.err
                isHunkHeader -> colors.info
                else -> colors.text
            }

            val bgColor = when {
                isAddition -> colors.ok.copy(alpha = 0.18f)
                isDeletion -> colors.err.copy(alpha = 0.18f)
                else -> Color.Transparent
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(bgColor)
                    .padding(horizontal = 12.dp, vertical = 1.dp),
            ) {
                Text(
                    text = line,
                    color = textColor,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.5.sp,
                    lineHeight = 18.sp,
                )
            }
        }
    }
}
