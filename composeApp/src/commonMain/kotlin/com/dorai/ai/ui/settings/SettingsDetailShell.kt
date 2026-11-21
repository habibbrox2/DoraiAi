package com.dorai.ai.ui.settings

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dorai.ai.theme.tokens.DoraiSpacing
import com.dorai.ai.theme.tokens.LocalDoraiColors
import com.dorai.ai.ui.adaptive.DoraiListDetailPaneScaffold
import com.dorai.ai.ui.adaptive.LocalDoraiWindowWidthClass
import dorai.composeapp.generated.resources.Res
import org.jetbrains.compose.resources.stringResource

/**
 * Responsive shell for Settings detail screens.
 *
 * Wraps detail content with adaptive layout:
 * - **compact**: single scrollable detail column
 * - **medium+**: sidebar + detail via DoraiListDetailPaneScaffold
 *
 * This enables all detail screens to share one layout pattern without each
 * screen re-implementing the responsiveness logic.
 *
 * Category scroll state resets when switching tabs.
 */
@Composable
fun SettingsDetailShell(
    category: SettingsCategory,
    content: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
) {
    val widthClass = LocalDoraiWindowWidthClass.current
    val colors = LocalDoraiColors.current

    // Fresh scroll position per category: switching tabs always lands on the header
    val scrollState = remember(category) { ScrollState(initial = 0) }

    if (widthClass.isCompact) {
        // Compact: single scrollable column
        Surface(
            modifier = modifier
                .fillMaxSize()
                .background(colors.background),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(DoraiSpacing.MD),
            ) {
                content()
            }
        }
    } else {
        // Medium+ tablet/desktop: sidebar + detail via existing adaptive scaffold
        DoraiListDetailPaneScaffold(
            listPane = {
                SettingsCategoryListPane(
                    selectedCategory = category,
                    onCategorySelected = {}, // handled by caller at a higher level
                )
            },
            detailPane = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.background),
                ) {
                    SettingsDetailHeader(
                        title = stringResource(category.titleRes),
                        icon = category.icon,
                        description = stringResource(category.subtitleRes),
                        onBack = null, // handled by caller
                    )
                    androidx.compose.foundation.layout.Spacer(
                        modifier = Modifier.height(DoraiSpacing.LG),
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(scrollState),
                    ) {
                        content()
                    }
                }
            },
        )
    }
}
