package com.dorai.ai.ui.components.v2

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dorai.ai.theme.tokens.LocalDoraiColors
import dorai.composeapp.generated.resources.Res
import dorai.composeapp.generated.resources.diff_truncated_lines
import org.jetbrains.compose.resources.stringResource

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
        val allLines = diff.lines()
        val visibleLines =
            if (allLines.size > MAX_DIFF_LINES) allLines.subList(0, MAX_DIFF_LINES) else allLines
        // The outer Column lives inside a horizontalScroll, so its children measure with an
        // unbounded max width; pin every row to the widest row so the +/- backgrounds line up
        // (IntrinsicSize.Max) instead of each row sizing to its own line (broken highlight).
        Column(modifier = Modifier.width(IntrinsicSize.Max)) {
            visibleLines.forEachIndexed { index, line ->
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

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(bgColor)
                        .padding(horizontal = 8.dp, vertical = 1.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "${index + 1}".padStart(3, ' '),
                        color = colors.muted.copy(alpha = 0.6f),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.5.sp,
                        lineHeight = 18.sp,
                        modifier = Modifier.width(32.dp),
                    )
                    Text(
                        text = line,
                        color = textColor,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.5.sp,
                        lineHeight = 18.sp,
                    )
                }
            }
            if (allLines.size > MAX_DIFF_LINES) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(Res.string.diff_truncated_lines, allLines.size - MAX_DIFF_LINES),
                        color = colors.muted,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                    )
                }
            }
        }
    }
}

/** Max diff lines rendered inline before a truncation footer is appended. */
private const val MAX_DIFF_LINES = 500
