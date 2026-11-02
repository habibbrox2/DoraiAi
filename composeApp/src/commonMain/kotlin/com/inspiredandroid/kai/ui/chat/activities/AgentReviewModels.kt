package com.inspiredandroid.kai.ui.chat.activities

/** Observable metrics for one assistant tool-call turn. */
data class AgentRunSummary(
    val total: Int,
    val completed: Int,
    val running: Int,
    val failed: Int,
    val cancelled: Int,
    val fileCount: Int,
    val terminalCount: Int,
    val durationMs: Long,
)

fun summarizeAgentRun(activities: List<AgentActivity>, nowMs: Long): AgentRunSummary {
    if (activities.isEmpty()) return AgentRunSummary(0, 0, 0, 0, 0, 0, 0, 0)
    val active = activities.any { it.status == ActivityStatus.PENDING || it.status == ActivityStatus.RUNNING }
    val startedAt = activities.minOf { it.startTime }
    val endedAt = if (active) nowMs else activities.mapNotNull { it.endTime }.maxOrNull() ?: activities.maxOf { it.startTime }
    val files = activities.mapNotNull { (it.details as? ActivityDetails.FileOperation)?.path }
        .filter { it.isNotBlank() && it != "File operation" }
        .distinct()
    return AgentRunSummary(
        total = activities.size,
        completed = activities.count { it.status == ActivityStatus.COMPLETED },
        running = activities.count { it.status == ActivityStatus.PENDING || it.status == ActivityStatus.RUNNING },
        failed = activities.count { it.status == ActivityStatus.FAILED || it.status == ActivityStatus.TIMEOUT },
        cancelled = activities.count { it.status == ActivityStatus.CANCELLED || it.status == ActivityStatus.DENIED },
        fileCount = files.size,
        terminalCount = activities.count { it.type == ActivityType.SHELL_COMMAND },
        durationMs = (endedAt - startedAt).coerceAtLeast(0),
    )
}

fun formatAgentRunDuration(durationMs: Long): String = when {
    durationMs < 1_000 -> "${durationMs}ms"
    durationMs < 60_000 -> "${durationMs / 1_000}s"
    else -> "${durationMs / 60_000}m ${(durationMs % 60_000) / 1_000}s"
}

enum class DiffLineKind { UNCHANGED, ADDED, REMOVED, MODIFIED }
data class SideBySideDiffLine(val oldLineNumber: Int?, val newLineNumber: Int?, val oldText: String?, val newText: String?, val kind: DiffLineKind)
data class SideBySideDiff(val lines: List<SideBySideDiffLine>, val truncated: Boolean)

/** Bounded LCS diff: at most 250 lines per side to keep CPU/memory predictable. */
fun computeSideBySideDiff(before: String, after: String, maxLines: Int = 250): SideBySideDiff {
    require(maxLines in 1..250) { "maxLines must be between 1 and 250" }
    val allOld = before.split('\n')
    val allNew = after.split('\n')
    val oldLines = allOld.take(maxLines)
    val newLines = allNew.take(maxLines)
    val lcs = Array(oldLines.size + 1) { IntArray(newLines.size + 1) }
    for (i in oldLines.indices.reversed()) for (j in newLines.indices.reversed()) {
        lcs[i][j] = if (oldLines[i] == newLines[j]) lcs[i + 1][j + 1] + 1 else maxOf(lcs[i + 1][j], lcs[i][j + 1])
    }
    data class Raw(val oldNo: Int?, val newNo: Int?, val old: String?, val new: String?, val kind: DiffLineKind)
    val raw = mutableListOf<Raw>()
    var i = 0
    var j = 0
    while (i < oldLines.size || j < newLines.size) {
        when {
            i < oldLines.size && j < newLines.size && oldLines[i] == newLines[j] -> { raw += Raw(i + 1, j + 1, oldLines[i], newLines[j], DiffLineKind.UNCHANGED); i++; j++ }
            j < newLines.size && (i == oldLines.size || lcs[i][j + 1] > lcs[i + 1][j]) -> { raw += Raw(null, j + 1, null, newLines[j], DiffLineKind.ADDED); j++ }
            else -> { raw += Raw(i + 1, null, oldLines[i], null, DiffLineKind.REMOVED); i++ }
        }
    }
    val aligned = mutableListOf<SideBySideDiffLine>()
    var cursor = 0
    while (cursor < raw.size) {
        val current = raw[cursor]
        val next = raw.getOrNull(cursor + 1)
        if (current.kind == DiffLineKind.REMOVED && next?.kind == DiffLineKind.ADDED) {
            aligned += SideBySideDiffLine(current.oldNo, next.newNo, current.old, next.new, DiffLineKind.MODIFIED)
            cursor += 2
        } else {
            aligned += SideBySideDiffLine(current.oldNo, current.newNo, current.old, current.new, current.kind)
            cursor++
        }
    }
    return SideBySideDiff(aligned, allOld.size > maxLines || allNew.size > maxLines)
}
