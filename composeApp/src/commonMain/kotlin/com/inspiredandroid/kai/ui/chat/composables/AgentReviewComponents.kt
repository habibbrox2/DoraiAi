package com.inspiredandroid.kai.ui.chat.composables

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.inspiredandroid.kai.filereview.FileReviewPreview
import com.inspiredandroid.kai.filereview.FileReviewProposal
import com.inspiredandroid.kai.filereview.FileReviewState
import com.inspiredandroid.kai.ui.KaiTypography
import com.inspiredandroid.kai.ui.chat.activities.ActivityDetails
import com.inspiredandroid.kai.ui.chat.activities.ActivityStatus
import com.inspiredandroid.kai.ui.chat.activities.DiffLineKind
import com.inspiredandroid.kai.ui.chat.activities.SideBySideDiffLine
import com.inspiredandroid.kai.ui.chat.activities.computeSideBySideDiff
import com.inspiredandroid.kai.ui.theme.DesignTokens

/** Review annotations are presentation-only; they do not apply or revert filesystem changes. */
@Composable
internal fun FileChangeReviewCard(
    activityId: String,
    details: ActivityDetails.FileOperation,
    status: ActivityStatus,
    proposal: FileReviewProposal? = null,
    preview: FileReviewPreview? = null,
    onAccept: (String) -> Unit = {},
    onReject: (String) -> Unit = {},
    onRollback: (String) -> Unit = {},
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("${details.operation} · ${safeActivityPreview(details.path, 240)}", style = KaiTypography.labelSmall.copy(fontWeight = FontWeight.SemiBold), color = DesignTokens.BrandCyan)
        if (proposal != null) {
            Text("Transaction · ${proposal.state.name.replace('_', ' ')}", style = KaiTypography.labelSmall.copy(fontWeight = FontWeight.SemiBold), color = when (proposal.state) {
                FileReviewState.ACCEPTED, FileReviewState.ROLLED_BACK -> DesignTokens.Success
                FileReviewState.CONFLICT, FileReviewState.FAILED, FileReviewState.RECOVERY_REQUIRED -> DesignTokens.Error
                else -> DesignTokens.Warning
            })
            when {
                preview?.originalText != null && preview.proposedText != null -> SideBySideFileDiff(safeActivityPreview(preview.originalText, 30_000), safeActivityPreview(preview.proposedText, 30_000))
                preview?.proposedText != null -> ActivityCodePreview("Proposed content", preview.proposedText)

                preview?.originalText != null -> ActivityCodePreview("Original content", preview.originalText)

                else -> Text(
                    "Staged preview unavailable. The target file has not been changed by this proposal.",
                    style = KaiTypography.labelSmall,
                    color = DesignTokens.TertiaryText,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (proposal.state == FileReviewState.PROPOSED || proposal.state == FileReviewState.CONFLICT) {
                    OutlinedButton(onClick = { onAccept(proposal.id) }, enabled = proposal.state == FileReviewState.PROPOSED) { Text("Accept") }
                    OutlinedButton(onClick = { onReject(proposal.id) }) {
                        Text("Reject")
                    }
                }
                if (proposal.state == FileReviewState.ACCEPTED && proposal.rollbackAvailable) OutlinedButton(onClick = { onRollback(proposal.id) }) { Text("Rollback") }
            }
        } else when {
            details.oldContent != null && details.newContent != null -> SideBySideFileDiff(
                before = safeActivityPreview(details.oldContent, 30_000),
                after = safeActivityPreview(details.newContent, 30_000),
            )

            !details.diff.isNullOrBlank() -> ActivityCodePreview("Patch supplied by tool", details.diff)

            !details.newContent.isNullOrBlank() -> ActivityCodePreview("New content · original snapshot unavailable", details.newContent, DesignTokens.Success)

            !details.oldContent.isNullOrBlank() -> ActivityCodePreview("Original content · updated snapshot unavailable", details.oldContent, DesignTokens.Error)

            else -> Text("The tool did not provide before/after content. A source diff cannot be computed from this activity.", style = KaiTypography.labelSmall, color = DesignTokens.TertiaryText)
        }
        details.changeSummary?.takeIf { it.isNotBlank() }?.let { ActivityCodePreview("Tool result", it) }
        if (proposal == null && (status == ActivityStatus.COMPLETED || status == ActivityStatus.FAILED)) {
            var reviewState by rememberSaveable(activityId) { mutableStateOf("unreviewed") }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { reviewState = "reviewed" }) { Text(if (reviewState == "reviewed") "Reviewed ✓" else "Mark reviewed") }
                OutlinedButton(onClick = { reviewState = "needs_changes" }) { Text(if (reviewState == "needs_changes") "Flagged ✓" else "Needs changes") }
            }
            Text(
                when (reviewState) {
                    "reviewed" -> "Review marked complete for this session."
                    "needs_changes" -> "Flagged for follow-up. No file was changed."
                    else -> "Review status is local to this screen and does not modify the file."
                },
                style = KaiTypography.labelSmall,
                color = DesignTokens.TertiaryText,
            )
        }
    }
}

@Composable
private fun SideBySideFileDiff(before: String, after: String) {
    val diff = remember(before, after) { computeSideBySideDiff(before, after) }
    Surface(modifier = Modifier.fillMaxWidth(), color = DesignTokens.SurfaceDark, shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp), border = BorderStroke(1.dp, DesignTokens.BorderSubtle)) {
        Column {
            Row(modifier = Modifier.fillMaxWidth().background(DesignTokens.SurfaceElevated).padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Before", modifier = Modifier.weight(1f), style = KaiTypography.labelSmall, color = DesignTokens.Error)
                Text("After", modifier = Modifier.weight(1f), style = KaiTypography.labelSmall, color = DesignTokens.Success)
            }
            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                DiffPane(diff.lines, isOld = true, modifier = Modifier.weight(1f))
                DiffPane(diff.lines, isOld = false, modifier = Modifier.weight(1f))
            }
            if (diff.truncated) Text("Diff limited to first 250 lines per side.", modifier = Modifier.padding(8.dp), style = KaiTypography.labelSmall, color = DesignTokens.Warning)
            if (diff.lines.none { it.kind != DiffLineKind.UNCHANGED }) Text("No textual changes detected.", modifier = Modifier.padding(8.dp), style = KaiTypography.labelSmall, color = DesignTokens.TertiaryText)
        }
    }
}

@Composable
private fun DiffPane(lines: List<SideBySideDiffLine>, isOld: Boolean, modifier: Modifier = Modifier) {
    Column(modifier = modifier.widthIn(min = 170.dp).padding(6.dp)) {
        lines.forEach { line ->
            val number = if (isOld) line.oldLineNumber else line.newLineNumber
            val content = if (isOld) line.oldText else line.newText
            val tint = when {
                line.kind == DiffLineKind.UNCHANGED -> DesignTokens.SecondaryText
                isOld -> DesignTokens.Error
                else -> DesignTokens.Success
            }
            val annotated = buildAnnotatedString {
                append((number?.toString() ?: "·").padStart(4))
                append(" │ ")
                withStyle(SpanStyle(color = tint)) { append(content.orEmpty()) }
            }
            Text(annotated, style = KaiTypography.labelSmall.copy(fontFamily = FontFamily.Monospace), maxLines = 1, color = DesignTokens.SecondaryText)
        }
    }
}
