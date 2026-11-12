package com.dorai.ai.voice

import com.dorai.ai.data.SettingsJsonValue
import kotlinx.serialization.Serializable

/**
 * Persisted download state for one catalog entry. Persisted as a single JSON array
 * under one key in the encrypted settings, following the existing store pattern.
 * Everything the user sees (progress, delete, re-download) is derived from this list.
 */
@Serializable
data class VoiceModelManifestEntry(
    val modelId: String,
    val state: VoiceModelState = VoiceModelState.ABSENT,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val verifiedFileNames: List<String> = emptyList(),
    val failedReason: String? = null,
)

@Serializable
data class VoiceManifest(
    val entries: List<VoiceModelManifestEntry> = emptyList(),
)

/**
 * JSON-backed manifest. The value is derived from, and written back to, one key in the
 * encrypted settings store (see [AppSettings.getVoiceModelManifestJson]).
 */
class VoiceManifestStore(
    private val read: () -> String,
    private val write: (String) -> Unit,
) {

    private val store = SettingsJsonValue(
        read = read,
        write = write,
        serializer = VoiceManifest.serializer(),
        label = "VoiceManifest",
        default = { VoiceManifest() },
    )

    fun get(): VoiceManifest = store.get()

    fun set(manifest: VoiceManifest) {
        store.set(manifest)
    }

    /**
     * Read-modify-write without a mutex. Two callers (the worker and Settings) rarely
     * race, and a lost update only loses a progress tick — the authoritative write
     * happens when the download finishes and READY is set.
     */
    fun update(transform: (VoiceManifest) -> VoiceManifest): VoiceManifest {
        val updated = transform(get())
        set(updated)
        return updated
    }
}
