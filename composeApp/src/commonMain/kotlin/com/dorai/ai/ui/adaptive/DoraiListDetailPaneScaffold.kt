package com.dorai.ai.ui.adaptive

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dorai.ai.theme.tokens.LocalDoraiColors

/**
 * Adaptive two-pane layout for Chat and Settings screens:
 * - [Compact]: Shows single pane ([detailPane] by default, with [listPane] rendered
 *   inside a modal drawer or custom sheet by the caller).
 * - [Medium]: List pane (250dp) + Detail pane (rest).
 * - [Expanded]: List pane (270dp) + Detail pane (rest).
 * - [Large]: List pane (300dp) + Detail pane (rest).
 */
@Composable
fun DoraiListDetailPaneScaffold(
    listPane: @Composable () -> Unit,
    detailPane: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    windowWidthClass: DoraiWindowWidthClass = LocalDoraiWindowWidthClass.current,
) {
    val colors = LocalDoraiColors.current

    if (windowWidthClass.isCompact) {
        Box(modifier = modifier.fillMaxSize()) {
            detailPane()
        }
    } else {
        val listWidth = windowWidthClass.listPaneWidth

        Row(modifier = modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .width(listWidth)
                    .fillMaxHeight()
                    .background(colors.background)
                    .border(width = 1.dp, color = colors.line),
            ) {
                listPane()
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(colors.background),
            ) {
                detailPane()
            }
        }
    }
}
