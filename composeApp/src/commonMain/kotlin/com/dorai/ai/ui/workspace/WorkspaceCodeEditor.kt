package com.dorai.ai.ui.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.dorai.ai.ui.KaiTypography
import com.dorai.ai.ui.theme.DesignTokens
import com.dorai.ai.ui.theme.LocalDesignColorTokens
import com.dorai.ai.ui.workspace.WorkspaceSpacing
import dorai.composeapp.generated.resources.Res
import dorai.composeapp.generated.resources.workspace_editor_accept
import dorai.composeapp.generated.resources.workspace_editor_agent_changes
import dorai.composeapp.generated.resources.workspace_editor_branch
import dorai.composeapp.generated.resources.workspace_editor_edit
import dorai.composeapp.generated.resources.workspace_editor_next
import dorai.composeapp.generated.resources.workspace_editor_preview
import dorai.composeapp.generated.resources.workspace_editor_previous
import dorai.composeapp.generated.resources.workspace_editor_reject
import dorai.composeapp.generated.resources.workspace_editor_run
import dorai.composeapp.generated.resources.workspace_editor_save
import dorai.composeapp.generated.resources.workspace_editor_search
import dorai.composeapp.generated.resources.workspace_editor_status_line
import dorai.composeapp.generated.resources.workspace_editor_toggle_diff
import dorai.composeapp.generated.resources.workspace_editor_undo
import kotlinx.collections.immutable.toImmutableList
import org.jetbrains.compose.resources.stringResource

/** A single syntax-highlighted line of code. */
@Immutable
data class CodeLine(
    val number: Int,
    val segments: List<CodeSegment>,
    val isAdded: Boolean = false,
    val isRemoved: Boolean = false,
)

/** One styled segment within a code line. */
@Immutable
data class CodeSegment(
    val text: String,
    val style: CodeStyle = CodeStyle.Plain,
)

enum class CodeStyle {
    Plain,
    Keyword,
    Type,
    Function,
    String,
    Comment,
    Annotation,
    Number,
}

/** Code theme colors matching the design guide syntax palette. */
object CodeTheme {
    val Keyword = Color(0xFFC39BFF)
    val Type = Color(0xFFF2B659)
    val Function = Color(0xFF7DB2FF)
    val String = Color(0xFF8FD6A0)
    val Comment = Color(0xFF5E6B7B)
    val Annotation = Color(0xFFF29E74)
    val Number = Color(0xFFF2B659)
    val Plain = Color(0xFFE7ECF2)
    val LineNumber = Color(0xFF4A5666)
    val AddedBackground = Color(0xFF10261B)
    val RemovedBackground = Color(0xFF2A1519)
    val AddedEdge = Color(0xFF5ED394)
    val RemovedEdge = Color(0xFFF27C86)
}

/** Maps a [CodeStyle] to its palette color. */
fun CodeStyle.color(): Color = when (this) {
    CodeStyle.Plain -> CodeTheme.Plain
    CodeStyle.Keyword -> CodeTheme.Keyword
    CodeStyle.Type -> CodeTheme.Type
    CodeStyle.Function -> CodeTheme.Function
    CodeStyle.String -> CodeTheme.String
    CodeStyle.Comment -> CodeTheme.Comment
    CodeStyle.Annotation -> CodeTheme.Annotation
    CodeStyle.Number -> CodeTheme.Number
}

/** Code editor screen with syntax highlighting, diff preview, search/replace, key row. */
@Composable
fun WorkspaceCodeEditor(
    filePath: String,
    lines: List<CodeLine>,
    onDiffToggled: (Boolean) -> Unit,
    onRun: () -> Unit,
    onUndo: () -> Unit,
    onSearchChanged: (String) -> Unit,
    isDiffMode: Boolean = false,
    showSearch: Boolean = false,
    searchQuery: String = "",
    onCloseSearch: () -> Unit = {},
    content: String = "",
    onContentChange: (String) -> Unit = {},
    editable: Boolean = false,
    onEditableChange: (Boolean) -> Unit = {},
    isDirty: Boolean = false,
    onSave: () -> Unit = {},
    onSearchToggle: () -> Unit = {},
    language: String = "Text",
    readOnly: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val colorTokens = LocalDesignColorTokens.current

    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        // ─── Top bar ───
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = WorkspaceSpacing.sm, vertical = WorkspaceSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.Folder,
                contentDescription = null,
                tint = colorTokens.textSecondary,
            )
            Column(
                modifier = Modifier.weight(1f).padding(start = WorkspaceSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = filePath.substringAfterLast("/"),
                    style = KaiTypography.titleMedium,
                    color = colorTokens.textPrimary,
                    maxLines = 1,
                )
                Text(
                    text = filePath,
                    style = KaiTypography.labelSmall,
                    color = colorTokens.textSecondary,
                    maxLines = 1,
                )
            }
            IconButton(onClick = onUndo) {
                Icon(
                    imageVector = Icons.Filled.Undo,
                    contentDescription = stringResource(Res.string.workspace_editor_undo),
                    tint = colorTokens.textSecondary,
                )
            }
            IconButton(onClick = onSearchToggle) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = stringResource(Res.string.workspace_editor_search),
                    tint = if (showSearch) DesignTokens.AccentPrimary else colorTokens.textSecondary,
                )
            }
            IconButton(onClick = { onDiffToggled(!isDiffMode) }) {
                Icon(
                    imageVector = Icons.Filled.CompareArrows,
                    contentDescription = stringResource(Res.string.workspace_editor_toggle_diff),
                    tint = if (isDiffMode) DesignTokens.AccentPrimary else colorTokens.textSecondary,
                )
            }
            if (!readOnly) {
                IconButton(onClick = { onEditableChange(!editable) }) {
                    Icon(
                        imageVector = if (editable) Icons.Filled.Visibility else Icons.Filled.Edit,
                        contentDescription = if (editable) stringResource(Res.string.workspace_editor_preview) else stringResource(Res.string.workspace_editor_edit),
                        tint = if (editable) DesignTokens.AccentPrimary else colorTokens.textSecondary,
                    )
                }
            }
            if (isDirty) {
                IconButton(onClick = onSave) {
                    Icon(
                        imageVector = Icons.Filled.Save,
                        contentDescription = stringResource(Res.string.workspace_editor_save),
                        tint = DesignTokens.Success,
                    )
                }
            }
            IconButton(onClick = onRun) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = stringResource(Res.string.workspace_editor_run),
                    tint = DesignTokens.Success,
                )
            }
        }

        // ─── Search/Replace panel ───
        if (showSearch) {
            SearchReplacePanel(
                query = searchQuery,
                onQueryChange = onSearchChanged,
                onClose = onCloseSearch,
            )
        }

        // ─── Code area ───
        if (editable) {
            EditableCodeArea(
                content = content,
                onContentChange = onContentChange,
                modifier = Modifier.fillMaxWidth().weight(1f),
            )
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.background),
            ) {
                // Line numbers
                LazyColumn(
                    modifier = Modifier
                        .width(44.dp)
                        .fillMaxHeight(),
                ) {
                    items(lines, key = { it.number }) { line ->
                        Text(
                            text = line.number.toString(),
                            style = KaiTypography.labelSmall,
                            color = CodeTheme.LineNumber,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            textAlign = TextAlign.End,
                        )
                    }
                }

                // Code lines
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                ) {
                    items(lines, key = { it.number }) { line ->
                        CodeRow(
                            line = line,
                            isDiffMode = isDiffMode,
                        )
                    }
                }
            }
        }

        // ─── Agent changes bar ───
        AgentChangesBar()

        // ─── Status line ───
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = WorkspaceSpacing.sm)
                .height(24.dp)
                .background(DesignTokens.SurfaceDark),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(Res.string.workspace_editor_status_line, "Kotlin", lines.size),
                style = KaiTypography.labelSmall,
                color = colorTokens.textSecondary,
            )
            Text(
                text = stringResource(Res.string.workspace_editor_branch),
                style = KaiTypography.labelSmall,
                color = colorTokens.textSecondary,
                modifier = Modifier.weight(1f),
            )
        }

        // ─── Code key row ───
        CodeKeyRow()
    }
}

/** Single code row with optional diff highlighting. */
@Composable
private fun CodeRow(line: CodeLine, isDiffMode: Boolean) {
    val colorTokens = LocalDesignColorTokens.current
    val bgColor = when {
        isDiffMode && line.isAdded -> CodeTheme.AddedBackground
        isDiffMode && line.isRemoved -> CodeTheme.RemovedBackground
        else -> Color(0xFF0F1419)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp)
            .background(bgColor)
            .clip(RoundedCornerShape(4.dp)),
    ) {
        if (isDiffMode && (line.isAdded || line.isRemoved)) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight()
                    .background(if (line.isAdded) CodeTheme.AddedEdge else CodeTheme.RemovedEdge),
            )
        }
        Text(
            text = buildAnnotatedString {
                line.segments.forEach { segment ->
                    withStyle(SpanStyle(color = segment.style.color())) {
                        append(segment.text)
                    }
                }
            },
            style = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 12.5.sp,
                lineHeight = 1.6.em,
                color = colorTokens.textPrimary,
            ),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}

/** Search / Replace panel. */
@Composable
private fun SearchReplacePanel(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = WorkspaceSpacing.sm, vertical = WorkspaceSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onClose) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = null,
            )
        }
        TextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text(text = stringResource(Res.string.workspace_editor_search), style = KaiTypography.bodyMedium) },
            modifier = Modifier.weight(1f),
            singleLine = true,
            keyboardOptions = KeyboardOptions(),
        )
    }
}

/** Floating agent changes bar: change count, prev/next, Reject/Accept. */
@Composable
private fun AgentChangesBar() {
    val colorTokens = LocalDesignColorTokens.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = WorkspaceSpacing.sm, vertical = WorkspaceSpacing.xs)
            .background(DesignTokens.SurfaceElevated)
            .clip(RoundedCornerShape(12.dp)),
    ) {
        Text(
            text = stringResource(Res.string.workspace_editor_agent_changes, 3),
            style = KaiTypography.labelMedium,
            color = DesignTokens.AccentPrimary,
        )
        TextButton(onClick = { }) {
            Text(text = stringResource(Res.string.workspace_editor_previous), style = KaiTypography.labelMedium)
        }
        TextButton(onClick = { }) {
            Text(text = stringResource(Res.string.workspace_editor_next), style = KaiTypography.labelMedium)
        }
        TextButton(onClick = { }) {
            Text(text = stringResource(Res.string.workspace_editor_reject), style = KaiTypography.labelMedium, color = DesignTokens.Error)
        }
        TextButton(onClick = { }) {
            Text(text = stringResource(Res.string.workspace_editor_accept), style = KaiTypography.labelMedium, color = DesignTokens.Success)
        }
    }
}

/** Code key row: Tab, arrows, symbols. Always visible above keyboard. */
@Composable
fun CodeKeyRow() {
    val colorTokens = LocalDesignColorTokens.current
    val keys = listOf(
        "Tab", "↑", "←", "↓", "→", "{ }", "( )", "[ ]", ";", "\"", "=", "<", ">", "!", ":",
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = WorkspaceSpacing.sm, vertical = WorkspaceSpacing.xs),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            keys.forEach { key ->
                KeyCap(key = key)
            }
        }
    }
}

@Composable
private fun KeyCap(key: String) {
    val colorTokens = LocalDesignColorTokens.current
    Box(
        modifier = Modifier
            .size(34.dp)
            .background(DesignTokens.SurfaceRaised)
            .clip(RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = key,
            style = KaiTypography.labelSmall,
            color = colorTokens.textPrimary,
        )
    }
}

/** Editable code buffer backed by a BasicTextField. */
@Composable
private fun EditableCodeArea(
    content: String,
    onContentChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    val colorTokens = LocalDesignColorTokens.current
    BasicTextField(
        value = content,
        onValueChange = onContentChange,
        modifier = modifier
            .fillMaxHeight()
            .verticalScroll(scrollState)
            .background(DesignTokens.SurfaceDark),
        textStyle = TextStyle(
            fontFamily = FontFamily.Monospace,
            fontSize = 12.5.sp,
            lineHeight = 1.6.em,
            color = colorTokens.textPrimary,
        ),
        keyboardOptions = KeyboardOptions(),
    )
}
