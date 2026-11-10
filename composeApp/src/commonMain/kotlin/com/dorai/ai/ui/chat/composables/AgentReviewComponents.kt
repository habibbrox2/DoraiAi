package com.dorai.ai.ui.chat.composables

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
import androidx.compose.material3.MaterialTheme
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
import com.dorai.ai.filereview.FileReviewPreview
import com.dorai.ai.filereview.FileReviewProposal
import com.dorai.ai.filereview.FileReviewState
import com.dorai.ai.ui.DoraiTypography
import com.dorai.ai.ui.chat.activities.ActivityDetails
import com.dorai.ai.ui.chat.activities.ActivityStatus
import com.dorai.ai.ui.chat.activities.DiffLineKind
import com.dorai.ai.ui.chat.activities.SideBySideDiffLine
import com.dorai.ai.ui.chat.activities.computeSideBySideDiff
import com.dorai.ai.ui.theme.DesignTokens
import com.dorai.ai.ui.theme.LocalDesignColorTokens
import dorai.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

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
        Text("${details.operation} · ${safeActivityPreview(details.path, 240)}", style = DoraiTypography.labelSmall.copy(fontWeight = FontWeight.SemiBold), color = DesignTokens.BrandCyan)
        if (proposal != null) {
            Text(
                "Transaction · ${proposal.state.name.replace('_', ' ')}",
                style = DoraiTypography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = when (proposal.state) {
                    FileReviewState.ACCEPTED, FileReviewState.ROLLED_BACK -> DesignTokens.Success
                    FileReviewState.CONFLICT, FileReviewState.FAILED, FileReviewState.RECOVERY_REQUIRED -> DesignTokens.Error
                    else -> DesignTokens.Warning
                },
            )
            when {
                preview?.originalText != null && preview.proposedText != null -> SideBySideFileDiff(
                    safeActivityPreview(preview.originalText, 30_000),
                    safeActivityPreview(preview.proposedText, 30_000),
                )

                preview?.proposedText != null -> ActivityCodePreview(stringResource(Res.string.file_review_proposed_content), preview.proposedText)

                preview?.originalText != null -> ActivityCodePreview(stringResource(Res.string.file_review_original_content), preview.originalText)

                else -> Text(
                    stringResource(Res.string.file_review_staged_preview_unavailable),
                    style = DoraiTypography.labelSmall,
                    color = LocalDesignColorTokens.current.textTertiary,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (proposal.state == FileReviewState.PROPOSED || proposal.state == FileReviewState.CONFLICT) {
                    OutlinedButton(
                        onClick = { onAccept(proposal.id) },
                        enabled = proposal.state == FileReviewState.PROPOSED,
                    ) {
                        Text(stringResource(Res.string.file_review_accept))
                    }
                    OutlinedButton(onClick = { onReject(proposal.id) }) {
                        Text(stringResource(Res.string.file_review_reject))
                    }
                }
                if (proposal.state == FileReviewState.ACCEPTED && proposal.rollbackAvailable) {
                    OutlinedButton(onClick = { onRollback(proposal.id) }) {
                        Text(stringResource(Res.string.file_review_rollback))
                    }
                }
            }
        } else if (details.proposalId != null) {
            Text(
                stringResource(Res.string.file_review_proposal_unavailable),
                style = DoraiTypography.labelSmall,
                color = DesignTokens.Warning,
            )
        } else {
            when {
                details.oldContent != null && details.newContent != null -> SideBySideFileDiff(
                    before = safeActivityPreview(details.oldContent, 30_000),
                    after = safeActivityPreview(details.newContent, 30_000),
                )

                !details.diff.isNullOrBlank() -> ActivityCodePreview(stringResource(Res.string.file_review_patch_supplied), details.diff)

                !details.newContent.isNullOrBlank() -> ActivityCodePreview(
                    stringResource(Res.string.file_review_new_content_original_missing),
                    details.newContent,
                    DesignTokens.Success,
                )

                !details.oldContent.isNullOrBlank() -> ActivityCodePreview(
                    stringResource(Res.string.file_review_original_content_updated_missing),
                    details.oldContent,
                    DesignTokens.Error,
                )

                else -> Text(
                    stringResource(Res.string.file_review_no_before_after),
                    style = DoraiTypography.labelSmall,
                    color = LocalDesignColorTokens.current.textTertiary,
                )
            }
        }
        details.changeSummary?.takeIf { it.isNotBlank() }?.let { ActivityCodePreview(stringResource(Res.string.file_review_tool_result), it) }
        if (details.proposalId == null && (status == ActivityStatus.COMPLETED || status == ActivityStatus.FAILED)) {
            var reviewState by rememberSaveable(activityId) { mutableStateOf("unreviewed") }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { reviewState = "reviewed" }) { Text(if (reviewState == "reviewed") stringResource(Res.string.file_review_reviewed) else stringResource(Res.string.file_review_mark_reviewed)) }
                OutlinedButton(onClick = { reviewState = "needs_changes" }) { Text(if (reviewState == "needs_changes") stringResource(Res.string.file_review_flagged) else stringResource(Res.string.file_review_needs_changes)) }
            }
            Text(
                when (reviewState) {
                    "reviewed" -> stringResource(Res.string.file_review_status_complete)
                    "needs_changes" -> stringResource(Res.string.file_review_followup)
                    else -> stringResource(Res.string.file_review_local_status)
                },
                style = DoraiTypography.labelSmall,
                color = LocalDesignColorTokens.current.textTertiary,
            )
        }
    }
}

@Composable
private fun SideBySideFileDiff(before: String, after: String) {
    val diff = remember(before, after) { computeSideBySideDiff(before, after) }
    Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface, shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp), border = BorderStroke(1.dp, LocalDesignColorTokens.current.borderSubtle)) {
        Column {
            Row(modifier = Modifier.fillMaxWidth().background(LocalDesignColorTokens.current.surfaceElevated).padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(Res.string.file_review_before), modifier = Modifier.weight(1f), style = DoraiTypography.labelSmall, color = DesignTokens.Error)
                Text(stringResource(Res.string.file_review_after), modifier = Modifier.weight(1f), style = DoraiTypography.labelSmall, color = DesignTokens.Success)
            }
            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                DiffPane(diff.lines, isOld = true, modifier = Modifier.weight(1f))
                DiffPane(diff.lines, isOld = false, modifier = Modifier.weight(1f))
            }
            if (diff.truncated) Text(stringResource(Res.string.file_review_diff_truncated), modifier = Modifier.padding(8.dp), style = DoraiTypography.labelSmall, color = DesignTokens.Warning)
            if (diff.lines.none { it.kind != DiffLineKind.UNCHANGED }) Text(stringResource(Res.string.file_review_no_textual_changes), modifier = Modifier.padding(8.dp), style = DoraiTypography.labelSmall, color = LocalDesignColorTokens.current.textTertiary)
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
                line.kind == DiffLineKind.UNCHANGED -> LocalDesignColorTokens.current.textSecondary
                isOld -> DesignTokens.Error
                else -> DesignTokens.Success
            }
            val annotated = buildAnnotatedString {
                append((number?.toString() ?: "—").padStart(4))
                append(" · ")
                withStyle(SpanStyle(color = tint)) { append(content.orEmpty()) }
            }
            Text(annotated, style = DoraiTypography.labelSmall.copy(fontFamily = FontFamily.Monospace), maxLines = 1, color = LocalDesignColorTokens.current.textSecondary)
        }
    }
}
