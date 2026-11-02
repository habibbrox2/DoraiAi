package com.inspiredandroid.kai.ui.markdown

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inspiredandroid.kai.ui.KaiTypography
import com.inspiredandroid.kai.ui.handCursor
import com.inspiredandroid.kai.ui.rememberCopyToClipboard
import com.inspiredandroid.kai.ui.theme.DesignColorTokens
import com.inspiredandroid.kai.ui.theme.LocalDesignColorTokens
import kotlinx.coroutines.delay

/**
 * Git-style unified diff card for chat code fences (Plan 7 polish).
 *
 * Rendering rules:
 * - One card per fence; when a fence carries patches for several files, each file
 *   gets its own strip inside the same card.
 * - The header names the changed file (or "N files changed") and shows the
 *   added/removed totals, so the size of a change is legible before reading it.
 * - Body rows are full-bleed: additions tint green, removals tint red, hunk
 *   headers render as a `@@` band, and context rows keep the old/new line numbers
 *   of the hunk they belong to.
 *
 * Parsing is deliberately forgiving — anything not recognised as a diff marker is
 * treated as context, so a malformed patch still renders as readable text.
 */

internal enum class DiffLineKind { FileHeader, Meta, Hunk, Add, Delete, Context }

internal data class DiffLine(
    val kind: DiffLineKind,
    val text: String,
    val oldNumber: Int? = null,
    val newNumber: Int? = null,
)

internal data class DiffEntry(
    val file: String?,
    val lines: List<DiffLine>,
    val added: Int,
    val removed: Int,
)

private val DIFF_LANGUAGES = setOf("diff", "patch", "git", "git-diff", "udiff")

private val HUNK_REGEX = Regex("""^@@ -(\d+)(?:,\d+)? \+(\d+)(?:,\d+)? @@.*""")

/** True when a fence should be rendered as a diff instead of plain code. */
internal fun isDiffContent(language: String?, code: String): Boolean {
    val lang = language?.lowercase()?.trim()
    if (lang != null && lang in DIFF_LANGUAGES) return true
    if (lang == "text" || lang == "plain" || lang == "") return false
    return looksLikeDiff(code)
}

/**
 * Language-less fences are recognised from their shape: a `diff --git` header, or
 * both sides of a file header followed by a hunk header.
 */
private fun looksLikeDiff(code: String): Boolean {
    var sawMinus = false
    var sawPlus = false
    var sawHunk = false
    for (line in code.lineSequence().take(400)) {
        if (line.startsWith("diff --git ")) return true
        if (line.startsWith("--- ") && !sawPlus) {
            sawMinus = true
        } else if (line.startsWith("+++ ") && sawMinus) {
            sawPlus = true
        } else if (line.startsWith("@@ -")) {
            sawHunk = true
        }
        if (sawPlus && sawHunk) return true
    }
    return false
}

private fun sidePath(raw: String): String? {
    val path = raw.substringBefore('\t').trim()
    if (path == "/dev/null") return null
    return path.removePrefix("a/").removePrefix("b/").takeIf { it.isNotBlank() }
}

private fun fileNameFromGitHeader(line: String): String? {
    val tokens = line.removePrefix("diff --git ").split(' ')
    val candidate = tokens.lastOrNull { it.startsWith("b/") } ?: tokens.lastOrNull()
    return candidate?.let { sidePath(it) }
}

private fun isMetaLine(line: String): Boolean = line.startsWith("index ") ||
    line.startsWith("new file mode") ||
    line.startsWith("deleted file mode") ||
    line.startsWith("old mode") ||
    line.startsWith("new mode") ||
    line.startsWith("similarity index") ||
    line.startsWith("dissimilarity index") ||
    line.startsWith("rename from") ||
    line.startsWith("rename to") ||
    line.startsWith("Binary files ") ||
    line.startsWith("GIT binary patch")

internal fun parseDiff(code: String): List<DiffEntry> {
    val entries = mutableListOf<DiffEntry>()
    var file: String? = null
    var lines = mutableListOf<DiffLine>()
    var added = 0
    var removed = 0
    var oldNumber: Int? = null
    var newNumber: Int? = null

    fun flush() {
        if (file != null || lines.isNotEmpty()) {
            entries += DiffEntry(file, lines.toList(), added, removed)
        }
        file = null
        lines = mutableListOf()
        added = 0
        removed = 0
        oldNumber = null
        newNumber = null
    }

    for (raw in code.lines()) {
        val hunk = HUNK_REGEX.matchEntire(raw)
        when {
            raw.startsWith("diff --git ") -> {
                flush()
                file = fileNameFromGitHeader(raw)
                lines += DiffLine(DiffLineKind.FileHeader, raw)
            }

            hunk != null -> {
                oldNumber = hunk.groupValues[1].toIntOrNull()
                newNumber = hunk.groupValues[2].toIntOrNull()
                lines += DiffLine(DiffLineKind.Hunk, raw)
            }

            // Order matters: the file markers must match before the +/- line cases.
            raw.startsWith("--- ") -> {
                if (lines.any { it.kind == DiffLineKind.Hunk }) flush()
                if (file == null) file = sidePath(raw.removePrefix("--- "))
                lines += DiffLine(DiffLineKind.FileHeader, raw)
            }

            raw.startsWith("+++ ") -> {
                if (file == null) file = sidePath(raw.removePrefix("+++ "))
                lines += DiffLine(DiffLineKind.FileHeader, raw)
            }

            isMetaLine(raw) -> lines += DiffLine(DiffLineKind.Meta, raw)

            raw.startsWith("+") -> {
                added++
                lines += DiffLine(DiffLineKind.Add, raw, newNumber = newNumber)
                newNumber = newNumber?.plus(1)
            }

            raw.startsWith("-") -> {
                removed++
                lines += DiffLine(DiffLineKind.Delete, raw, oldNumber = oldNumber)
                oldNumber = oldNumber?.plus(1)
            }

            raw.startsWith("\\") -> lines += DiffLine(DiffLineKind.Meta, raw)

            else -> {
                lines += DiffLine(DiffLineKind.Context, raw, oldNumber, newNumber)
                oldNumber = oldNumber?.plus(1)
                newNumber = newNumber?.plus(1)
            }
        }
    }
    flush()
    return entries
}

/** Semantic diff colors (kept as design constants); surface/text roles come from [LocalDesignColorTokens]. */
private object DiffPalette {
    val added = Color(0xFF4ADE80)
    val removed = Color(0xFFF87171)
    val hunkText = Color(0xFF93C5FD)
    val addedBg = Color(0xFF16A34A).copy(alpha = 0.16f)
    val removedBg = Color(0xFFDC2626).copy(alpha = 0.18f)
    val hunkBg = Color(0xFF1E293B).copy(alpha = 0.55f)
}

@Composable
internal fun DiffCard(
    code: String,
    modifier: Modifier = Modifier,
) {
    val entries = remember(code) { parseDiff(code) }
    val copyToClipboard = rememberCopyToClipboard()
    var isCopied by remember { mutableStateOf(false) }

    LaunchedEffect(isCopied) {
        if (isCopied) {
            delay(2000)
            isCopied = false
        }
    }

    val totalAdded = entries.sumOf { it.added }
    val totalRemoved = entries.sumOf { it.removed }
    val title = when {
        entries.isEmpty() -> "diff"
        entries.size == 1 -> entries.first().file ?: "diff"
        else -> "${entries.size} files changed"
    }
    val blockShape = RoundedCornerShape(10.dp)
    val tokens = LocalDesignColorTokens.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(blockShape)
            .background(tokens.surfaceRaised)
            .border(1.dp, tokens.borderSoft, blockShape),
    ) {
        // Header: file name plus patch size, so the change is scannable at a glance.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(tokens.cardSurface)
                .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.Code,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = tokens.textMuted,
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                style = KaiTypography.labelSmall,
                fontFamily = FontFamily.Monospace,
                color = tokens.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            DiffStat(text = "+$totalAdded", color = DiffPalette.added)
            DiffStat(text = "-$totalRemoved", color = DiffPalette.removed)
            if (isCopied) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Copied",
                        modifier = Modifier.size(14.dp),
                        tint = DiffPalette.added,
                    )
                    Text(
                        text = "Copied!",
                        style = KaiTypography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = DiffPalette.added,
                    )
                }
            } else {
                IconButton(
                    onClick = {
                        copyToClipboard(code)
                        isCopied = true
                    },
                    modifier = Modifier.size(32.dp).handCursor(),
                ) {
                    Icon(
                        imageVector = Icons.Filled.ContentCopy,
                        contentDescription = "Copy diff",
                        modifier = Modifier.size(14.dp),
                        tint = tokens.textMuted,
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(tokens.borderSoft),
        )

        Column(modifier = Modifier.fillMaxWidth()) {
            entries.forEachIndexed { index, entry ->
                if (index > 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(tokens.borderSoft),
                    )
                }
                if (entries.size > 1) {
                    FileStrip(entry = entry, tokens = tokens)
                }
                entry.lines.forEach { line ->
                    DiffLineRow(line, tokens)
                }
            }
        }
    }
}

/** Compact `+n` / `-n` badge used in the card header. */
@Composable
private fun DiffStat(
    text: String,
    color: Color,
) {
    Box(
        modifier = Modifier
            .padding(start = 6.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 5.dp, vertical = 1.dp),
    ) {
        Text(
            text = text,
            style = KaiTypography.labelSmall,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium,
            color = color,
        )
    }
}

/** Per-file strip shown when a single fence holds patches for more than one file. */
@Composable
private fun FileStrip(entry: DiffEntry, tokens: DesignColorTokens) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(tokens.surfaceElevated)
            .padding(horizontal = 12.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = entry.file ?: "file",
            modifier = Modifier.weight(1f),
            style = KaiTypography.labelSmall,
            fontFamily = FontFamily.Monospace,
            color = tokens.textMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = "+${entry.added}",
            modifier = Modifier.padding(start = 8.dp),
            style = KaiTypography.labelSmall,
            fontFamily = FontFamily.Monospace,
            color = DiffPalette.added,
        )
        Text(
            text = "-${entry.removed}",
            modifier = Modifier.padding(start = 6.dp),
            style = KaiTypography.labelSmall,
            fontFamily = FontFamily.Monospace,
            color = DiffPalette.removed,
        )
    }
}

@Composable
private fun DiffLineRow(line: DiffLine, tokens: DesignColorTokens) {
    val background: Color
    val foreground: Color
    when (line.kind) {
        DiffLineKind.Add -> {
            background = DiffPalette.addedBg
            foreground = DiffPalette.added
        }

        DiffLineKind.Delete -> {
            background = DiffPalette.removedBg
            foreground = DiffPalette.removed
        }

        DiffLineKind.Hunk -> {
            background = DiffPalette.hunkBg
            foreground = DiffPalette.hunkText
        }

        DiffLineKind.FileHeader -> {
            background = Color.Transparent
            foreground = tokens.textMuted
        }

        DiffLineKind.Meta -> {
            background = Color.Transparent
            foreground = tokens.textMuted
        }

        DiffLineKind.Context -> {
            background = Color.Transparent
            foreground = tokens.textPrimary
        }
    }

    if (line.kind == DiffLineKind.FileHeader ||
        line.kind == DiffLineKind.Hunk ||
        line.kind == DiffLineKind.Meta
    ) {
        Text(
            text = line.text,
            modifier = Modifier
                .fillMaxWidth()
                .background(background)
                .padding(horizontal = 12.dp, vertical = 1.dp),
            color = foreground,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            lineHeight = 19.sp,
        )
        return
    }

    val sign = when (line.kind) {
        DiffLineKind.Add -> "+"
        DiffLineKind.Delete -> "-"
        else -> " "
    }
    val content = when (line.kind) {
        DiffLineKind.Context -> line.text
        else -> line.text.drop(1)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(background),
        verticalAlignment = Alignment.Top,
    ) {
        DiffLineNumber(line.oldNumber, tokens)
        DiffLineNumber(line.newNumber, tokens)
        Text(
            text = sign,
            modifier = Modifier.width(14.dp),
            color = foreground,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            lineHeight = 19.sp,
            textAlign = TextAlign.Start,
        )
        Text(
            text = content,
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp),
            color = foreground,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            lineHeight = 19.sp,
        )
    }
}

@Composable
private fun DiffLineNumber(value: Int?, tokens: DesignColorTokens) {
    Text(
        text = value?.toString() ?: "",
        modifier = Modifier.width(34.dp).padding(end = 6.dp),
        color = tokens.textMuted,
        fontFamily = FontFamily.Monospace,
        fontSize = 11.sp,
        lineHeight = 19.sp,
        textAlign = TextAlign.End,
    )
}
