package com.dorai.ai.voice

/** Where a voice model is in its lifecycle. */
enum class VoiceModelState {
    /** Nothing downloaded yet. */
    ABSENT,

    /** The worker is streaming bytes to `.part` files. */
    DOWNLOADING,

    /** A foreground download was paused; the `.part` files remain for resume. */
    PAUSED,

    /** Every file passed length + SHA-256 verification and was promoted to its final name. */
    READY,

    /** Verification or network failed. Re-download starts from zero. */
    FAILED,
}

enum class VoiceModelType {
    /** On-device offline TTS model (Coqui bn VITS). */
    TTS,

    /** On-device offline STT model (sherpa-onnx), reserved for Phase 2 continuation. */
    STT,
}

/**
 * One file belonging to a catalog entry. Every file pins its own immutable commit URL,
 * size and digest — they are verified independently and promoted atomically.
 */
data class VoiceModelFile(
    val name: String,
    val sizeBytes: Long,
    val sha256: String,
)

/**
 * A downloadable voice model, pinned to an immutable HuggingFace commit.
 * `licenseVerified` stays false until the plan's license sign-off records the model
 * as shippable; the settings UI never offers such a model for download.
 */
data class VoiceModel(
    val id: String,
    val type: VoiceModelType,
    val displayName: String,
    val description: String,
    val downloadUrlPrefix: String,
    val files: List<VoiceModelFile>,
    val licenseId: String,
    val licenseVerified: Boolean,
) {

    val totalSizeBytes: Long
        get() = files.sumOf { it.sizeBytes }

    fun fileUrl(file: VoiceModelFile): String = downloadUrlPrefix + "/" + file.name
}
