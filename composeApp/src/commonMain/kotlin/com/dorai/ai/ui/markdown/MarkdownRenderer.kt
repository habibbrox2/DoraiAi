package com.dorai.ai.ui.markdown

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import com.dorai.ai.ui.AccentPrimary
import com.dorai.ai.ui.BorderSoft
import com.dorai.ai.ui.DoraiTypography
import com.dorai.ai.ui.components.LocalShowFullScreenImageUrl
import com.dorai.ai.ui.dynamicui.DoraiUiRenderer
import com.dorai.ai.ui.dynamicui.FrozenSubmission
import com.dorai.ai.ui.handCursor
import com.dorai.ai.ui.markdown.math.MathFormula
import com.dorai.ai.ui.theme.LocalDesignColorTokens
import dorai.composeapp.generated.resources.Res
import dorai.composeapp.generated.resources.a11y_image
import dorai.composeapp.generated.resources.chat_table_truncated_rows
import dorai.composeapp.generated.resources.image_unavailable
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.compose.resources.stringResource

/** Max table rows rendered inline before a "… and N more rows" footer truncates the rest. */
private const val MAX_TABLE_ROWS = 100

/**
 * Render a parsed [MarkdownDocument] as a Compose layout. Each block becomes one child of the
 * outer [Column]; inline content is rendered as [androidx.compose.ui.text.AnnotatedString].
 *
 * DORAI UI blocks dispatch to [DoraiUiRenderer]; pass `isInteractive = false` to render them as
 * read-only (completed historical messages keep their layout but disable buttons/inputs).
 */
@Composable
fun MarkdownContent(
    document: MarkdownDocument,
    modifier: Modifier = Modifier,
    isInteractive: Boolean = false,
    onUiCallback: (event: String, data: Map<String, String>) -> Unit = { _, _ -> },
    frozen: FrozenSubmission? = null,
) {
    val currentContentColor = LocalContentColor.current
    val effectiveColor = if (currentContentColor != androidx.compose.ui.graphics.Color.Unspecified) {
        currentContentColor
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    CompositionLocalProvider(LocalContentColor provides effectiveColor) {
        Column(modifier) {
            RenderBlocks(document.blocks, isInteractive, onUiCallback, frozen)
        }
    }
}

@Composable
fun MarkdownContent(
    content: String,
    modifier: Modifier = Modifier,
    isInteractive: Boolean = false,
    onUiCallback: (event: String, data: Map<String, String>) -> Unit = { _, _ -> },
    frozen: FrozenSubmission? = null,
) {
    val doc = remember(content) {
        runCatching { parseMarkdown(content) }.getOrElse {
            MarkdownDocument(persistentListOf(Paragraph(persistentListOf(com.dorai.ai.ui.markdown.Text(content)))))
        }
    }
    MarkdownContent(doc, modifier, isInteractive, onUiCallback, frozen)
}

/**
 * Render a list of markdown blocks, keeping text runs inside a [SelectionContainer]
 * while DORAI UI blocks — which host interactive controls — are composed outside of any
 * selection container so taps and text selection do not fight over the same gestures.
 */
@Composable
private fun RenderBlocks(
    blocks: List<BlockNode>,
    isInteractive: Boolean,
    onUiCallback: (String, Map<String, String>) -> Unit,
    frozen: FrozenSubmission?,
) {
    // Split into segments of text blocks and interactive DORAI UI blocks. Text runs are
    // wrapped in a single SelectionContainer for native text selection; DORAI UI blocks
    // are composed outside any selection container so their controls remain interactive.
    val segments = mutableListOf<Pair<Boolean, IntRange>>()
    var runStart = 0
    var index = 0
    while (index < blocks.size) {
        if (blocks[index] is DoraiUiBlock) {
            if (index > runStart) segments += false to (runStart until index)
            segments += true to (index..index)
            runStart = index + 1
        }
        index += 1
    }
    if (blocks.size > runStart) segments += false to (runStart until blocks.size)

    segments.forEachIndexed { segmentIndex, (isUi, range) ->
        if (isUi) {
            val block = blocks[range.first]
            key(segmentIndex, block) {
                BlockRenderer(block, isInteractive, onUiCallback, frozen)
            }
        } else {
            SelectionContainer {
                Column {
                    for (i in range) {
                        val block = blocks[i]
                        key(i, block) {
                            BlockRenderer(block, isInteractive, onUiCallback, frozen)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BlockRenderer(
    block: BlockNode,
    isInteractive: Boolean,
    onUiCallback: (String, Map<String, String>) -> Unit,
    frozen: FrozenSubmission?,
) {
    when (block) {
        is Heading -> HeadingBlock(block)

        is Paragraph -> ParagraphBlock(block)

        is CodeFence -> {
            if (block.code.isNotBlank() || !block.language.isNullOrBlank()) {
                if (isDiffContent(block.language, block.code)) {
                    // Git-style card for unified diffs; plain code keeps the code fence.
                    DiffCard(
                        code = block.code,
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                } else {
                    CodeFenceBlock(
                        language = block.language,
                        code = block.code,
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                }
            }
        }

        is Blockquote -> BlockquoteBlock(block, isInteractive, onUiCallback, frozen)

        is BulletList -> BulletListBlock(block, isInteractive, onUiCallback, frozen)

        is OrderedList -> OrderedListBlock(block, isInteractive, onUiCallback, frozen)

        is Table -> TableBlock(block)

        HorizontalRule -> HorizontalDivider(Modifier.padding(vertical = 8.dp))

        is DisplayMath -> DisplayMathBlock(block)

        is DoraiUiBlock -> DoraiUiRenderer(
            node = block.node,
            isInteractive = isInteractive,
            onCallback = onUiCallback,
            frozen = frozen,
            modifier = Modifier.padding(vertical = 8.dp),
        )

        is DoraiUiError -> CodeFenceBlock(
            language = "json",
            code = block.rawJson,
            modifier = Modifier.padding(vertical = 4.dp),
        )
    }
}

@Composable
private fun HeadingBlock(block: Heading) {
    val style = when (block.level) {
        1 -> DoraiTypography.headlineSmall
        2 -> DoraiTypography.titleLarge
        3 -> DoraiTypography.titleMedium
        4 -> DoraiTypography.titleSmall
        5 -> DoraiTypography.bodyLarge.copy(fontWeight = FontWeight.Bold)
        else -> DoraiTypography.bodyMedium.copy(fontWeight = FontWeight.Bold)
    }
    InlineContent(
        inlines = block.inlines,
        style = style,
        modifier = Modifier.padding(vertical = 4.dp),
    )
}

@Composable
private fun ParagraphBlock(block: Paragraph) {
    if (block.inlines.size == 1 && block.inlines[0] is Image) {
        MarkdownImage(block.inlines[0] as Image)
        return
    }
    Column(modifier = Modifier.padding(vertical = 2.dp)) {
        InlineContent(
            inlines = block.inlines,
            style = DoraiTypography.bodyLarge,
        )
        // Inline images can't live inside AnnotatedString — render them as a thumbnail strip under the text.
        val inlineImages = block.inlines.filterIsInstance<Image>()
        if (inlineImages.isNotEmpty()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(top = 6.dp),
            ) {
                inlineImages.forEach { img ->
                    MarkdownImageThumbnail(img)
                }
            }
        }
    }
}

@Composable
private fun MarkdownImage(img: Image) {
    val tokens = LocalDesignColorTokens.current
    val showFullScreen = LocalShowFullScreenImageUrl.current
    val alt = img.alt.ifBlank { stringResource(Res.string.a11y_image) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 360.dp)
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(role = Role.Button, onClickLabel = alt) { showFullScreen(img.src) }
            .handCursor(),
    ) {
        SubcomposeAsyncImage(
            model = img.src,
            contentDescription = alt,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 352.dp),
            loading = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .background(tokens.surfaceRaised),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        color = AccentPrimary,
                        trackColor = AccentPrimary.copy(alpha = 0.15f),
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(28.dp),
                    )
                }
            },
            error = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .background(tokens.surfaceRaised),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = tokens.textMuted,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = stringResource(Res.string.image_unavailable),
                            style = DoraiTypography.labelMedium,
                            color = tokens.textMuted,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
            },
        )
    }
}

@Composable
private fun MarkdownImageThumbnail(img: Image) {
    val tokens = LocalDesignColorTokens.current
    val showFullScreen = LocalShowFullScreenImageUrl.current
    val alt = img.alt.ifBlank { stringResource(Res.string.a11y_image) }
    SubcomposeAsyncImage(
        model = img.src,
        contentDescription = alt,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .size(72.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(tokens.surfaceRaised)
            .clickable(role = Role.Button, onClickLabel = alt) { showFullScreen(img.src) }
            .handCursor(),
        loading = {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(tokens.surfaceRaised),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    color = AccentPrimary,
                    trackColor = AccentPrimary.copy(alpha = 0.15f),
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(16.dp),
                )
            }
        },
        error = {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(tokens.surfaceRaised),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = stringResource(Res.string.image_unavailable),
                    tint = tokens.textMuted,
                    modifier = Modifier.size(16.dp),
                )
            }
        },
    )
}

@Composable
private fun DisplayMathBlock(block: DisplayMath) {
    // Wrap in horizontal scroll so wide formulas overflow cleanly instead of squishing
    // their children into a narrow column (KaTeX/MathJax use the same pattern).
    val scroll = rememberScrollState()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .horizontalScroll(scroll),
        contentAlignment = Alignment.Center,
    ) {
        MathFormula(latex = block.latex, display = true)
    }
}

@Composable
private fun BlockquoteBlock(
    block: Blockquote,
    isInteractive: Boolean,
    onUiCallback: (String, Map<String, String>) -> Unit,
    frozen: FrozenSubmission?,
) {
    Row(modifier = Modifier.padding(vertical = 4.dp).height(IntrinsicSize.Min)) {
        VerticalDivider(
            thickness = 3.dp,
            color = BorderSoft,
            modifier = Modifier.fillMaxHeight(),
        )
        Column(Modifier.padding(start = 8.dp)) {
            RenderBlocks(block.children, isInteractive, onUiCallback, frozen)
        }
    }
}

@Composable
private fun BulletListBlock(
    block: BulletList,
    isInteractive: Boolean,
    onUiCallback: (String, Map<String, String>) -> Unit,
    frozen: FrozenSubmission?,
) {
    Column(modifier = Modifier.padding(vertical = 2.dp)) {
        for (item in block.items) {
            ListItemRow("•", 16.dp, item, isInteractive, onUiCallback, frozen)
        }
    }
}

@Composable
private fun OrderedListBlock(
    block: OrderedList,
    isInteractive: Boolean,
    onUiCallback: (String, Map<String, String>) -> Unit,
    frozen: FrozenSubmission?,
) {
    Column(modifier = Modifier.padding(vertical = 2.dp)) {
        block.items.forEachIndexed { index, item ->
            ListItemRow("${block.start + index}.", 24.dp, item, isInteractive, onUiCallback, frozen)
        }
    }
}

@Composable
private fun ListItemRow(
    marker: String,
    markerWidth: androidx.compose.ui.unit.Dp,
    item: ListItem,
    isInteractive: Boolean,
    onUiCallback: (String, Map<String, String>) -> Unit,
    frozen: FrozenSubmission?,
) {
    // The marker column is sized for the default font scale; scale it so "10." still
    // fits when body text doubles, and keep it a minimum rather than a fixed width so
    // an unusually long marker widens instead of being clipped.
    val scaledMarkerWidth = markerWidth * LocalDensity.current.fontScale
    Row {
        Text(
            text = marker,
            style = DoraiTypography.bodyLarge,
            modifier = Modifier.widthIn(min = scaledMarkerWidth).padding(end = 4.dp),
        )
        Column(Modifier.fillMaxWidth()) {
            RenderBlocks(item.children, isInteractive, onUiCallback, frozen)
        }
    }
}

@Composable
private fun TableBlock(block: Table) {
    val tokens = LocalDesignColorTokens.current
    Column(Modifier.padding(vertical = 4.dp)) {
        if (block.headers.any { it.isNotEmpty() }) {
            Row {
                block.headers.forEachIndexed { i, cell ->
                    InlineContent(
                        inlines = cell,
                        style = DoraiTypography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        textAlign = alignTextFor(block.alignments.getOrNull(i)),
                        modifier = Modifier.weight(1f).padding(4.dp),
                    )
                }
            }
            HorizontalDivider()
        }
        // Cap rendered rows: a pathological model-emitted table would otherwise expand into
        // thousands of InlineContent subtrees and stall composition on low-end devices.
        val visibleRows = if (block.rows.size > MAX_TABLE_ROWS) block.rows.subList(0, MAX_TABLE_ROWS) else block.rows
        for (row in visibleRows) {
            Row {
                row.forEachIndexed { i, cell ->
                    InlineContent(
                        inlines = cell,
                        style = DoraiTypography.bodyLarge,
                        textAlign = alignTextFor(block.alignments.getOrNull(i)),
                        modifier = Modifier.weight(1f).padding(4.dp),
                    )
                }
            }
        }
        if (block.rows.size > MAX_TABLE_ROWS) {
            Text(
                text = stringResource(Res.string.chat_table_truncated_rows, block.rows.size - MAX_TABLE_ROWS),
                style = DoraiTypography.bodySmall,
                color = tokens.textSecondary,
                modifier = Modifier.fillMaxWidth().padding(4.dp),
            )
        }
    }
}

private fun alignTextFor(align: ColumnAlign?): TextAlign = when (align) {
    ColumnAlign.LEFT -> TextAlign.Start
    ColumnAlign.CENTER -> TextAlign.Center
    ColumnAlign.RIGHT -> TextAlign.End
    else -> TextAlign.Unspecified
}
