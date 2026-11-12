package com.dorai.ai.voice

// Pure helpers shared by the Android download worker and its unit tests.

/**
 * Result of the storage gate. A download may not start unless free space covers
 * sizeBytes plus 25% headroom; the plan pins this as the single storage buffer.
 */
sealed interface StorageGateResult {
    data object Approved : StorageGateResult

    data class Refused(val neededBytes: Long, val freeBytes: Long) : StorageGateResult
}

fun storageGate(entry: VoiceModel, freeBytes: Long): StorageGateResult {
    val needed = entry.totalSizeBytes + entry.totalSizeBytes / 4
    return if (freeBytes >= needed) {
        StorageGateResult.Approved
    } else {
        StorageGateResult.Refused(neededBytes = needed, freeBytes = freeBytes)
    }
}

/**
 * Offset to resume a `.part` file from. Returns the existing partial length when it is
 * usable (at most the target size), otherwise zero — restart from scratch.
 */
fun resumeOffset(partBytes: Long, targetBytes: Long): Long = if (partBytes in 1..targetBytes) partBytes else 0L

/** Progress 0..100 for `bytes` downloaded of `total` (0 total yields 0). */
fun voiceDownloadProgressPercent(downloadedBytes: Long, totalBytes: Long): Int = if (totalBytes <= 0L) 0 else ((downloadedBytes * 100) / totalBytes).coerceIn(0, 100).toInt()
