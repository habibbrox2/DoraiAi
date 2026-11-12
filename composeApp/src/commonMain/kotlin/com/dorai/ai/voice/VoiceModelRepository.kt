package com.dorai.ai.voice

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Backlog-facing view of the model catalog joined with the persisted manifest.
 * Progress is monotonic per file batch; the worker updates [downloadedBytes] while it
 * streams, and writes the terminal state when verification finishes.
 */
class VoiceModelRepository(readJson: () -> String, writeJson: (String) -> Unit) {

    private val store = VoiceManifestStore(readJson, writeJson)

    private val _models = MutableStateFlow(join(store.get()))

    val models: StateFlow<List<VoiceModelEntry>> = _models.asStateFlow()

    private fun join(manifest: VoiceManifest): List<VoiceModelEntry> = VOICE_MODEL_CATALOG.map { model ->
        val persisted = manifest.entries.find { it.modelId == model.id }
        VoiceModelEntry(
            model = model,
            state = persisted?.state ?: VoiceModelState.ABSENT,
            downloadedBytes = persisted?.downloadedBytes ?: 0L,
            totalBytes = persisted?.totalBytes ?: model.totalSizeBytes,
            verifiedFileNames = persisted?.verifiedFileNames ?: emptyList(),
            failedReason = persisted?.failedReason,
        )
    }

    fun refresh() {
        _models.value = join(store.get())
    }

    fun entry(modelId: String): VoiceModelEntry? = _models.value.find { it.model.id == modelId }

    fun setModelState(
        modelId: String,
        state: VoiceModelState,
        downloadedBytes: Long? = null,
        failedReason: String? = null,
    ) {
        val total = findCatalogVoiceModelById(modelId)?.totalSizeBytes ?: 0L
        store.update { manifest ->
            val updated = manifest.entries.map { entry ->
                if (entry.modelId == modelId) {
                    entry.copy(
                        state = state,
                        downloadedBytes = downloadedBytes ?: entry.downloadedBytes,
                        totalBytes = total,
                        failedReason = failedReason,
                    )
                } else {
                    entry
                }
            }
            val known = updated.any { it.modelId == modelId }
            manifest.copy(
                entries = if (known) {
                    updated
                } else {
                    updated + VoiceModelManifestEntry(
                        modelId = modelId,
                        state = state,
                        downloadedBytes = downloadedBytes ?: 0L,
                        totalBytes = total,
                        failedReason = failedReason,
                    )
                },
            )
        }
        refresh()
    }

    fun setVerifiedFiles(modelId: String, fileNames: List<String>) {
        store.update { manifest ->
            val total = findCatalogVoiceModelById(modelId)?.totalSizeBytes ?: 0L
            val updated = manifest.entries.map { entry ->
                if (entry.modelId == modelId) {
                    entry.copy(
                        state = VoiceModelState.READY,
                        downloadedBytes = total,
                        totalBytes = total,
                        verifiedFileNames = fileNames,
                        failedReason = null,
                    )
                } else {
                    entry
                }
            }
            val known = updated.any { it.modelId == modelId }
            manifest.copy(
                entries = if (known) {
                    updated
                } else {
                    updated + VoiceModelManifestEntry(
                        modelId = modelId,
                        state = VoiceModelState.READY,
                        downloadedBytes = total,
                        totalBytes = total,
                        verifiedFileNames = fileNames,
                    )
                },
            )
        }
        refresh()
    }

    fun deleteEntry(modelId: String) {
        store.update { manifest ->
            manifest.copy(entries = manifest.entries.filterNot { it.modelId == modelId })
        }
        refresh()
    }

    fun reset(modelId: String) {
        setModelState(modelId, VoiceModelState.ABSENT, downloadedBytes = 0L)
    }
}

/** Join of a catalog entry with its persisted manifest row, as the UI wants it. */
data class VoiceModelEntry(
    val model: VoiceModel,
    val state: VoiceModelState,
    val downloadedBytes: Long,
    val totalBytes: Long,
    val verifiedFileNames: List<String>,
    val failedReason: String?,
) {
    val canDownload: Boolean
        get() = model.isDownloadable() && (state == VoiceModelState.ABSENT || state == VoiceModelState.FAILED)

    val canDelete: Boolean
        get() = state != VoiceModelState.ABSENT && state != VoiceModelState.DOWNLOADING

    val progressFraction: Float
        get() = if (totalBytes > 0) {
            (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
}
