package com.dorai.ai.voice

/**
 * Pinned voice model catalog.
 *
 * Every entry pins an immutable HuggingFace commit (never `main`), the exact byte size
 * of each file, and the file's SHA-256 — they are verified after download and before
 * the model is handed to anything. The digest is echoed in the `.sha256` marker next
 * to the model, following the LiteRT pattern.
 *
 * `licenseVerified` reflects the plan's license sign-off. The Coqui bn VITS entry was
 * signed off by the project owner on 2026-11-13 (see `docs/knowledge/voice/log.md`),
 * accepting the upstream repo's silence on licensing (the model lineage is Coqui TTS,
 * typically MPL-2.0, but the hosting repo declares none). Keep the pin, the digests and
 * the sign-off in sync — a bump changes all three.
 */
val VOICE_MODEL_CATALOG = listOf(
    VoiceModel(
        id = "coqui-vits-bn-custom-female",
        type = VoiceModelType.TTS,
        displayName = "Coqui TTS — Bangla (female)",
        description = "Offline Bangla text-to-speech voice, Coqui VITS converted to ONNX.",
        downloadUrlPrefix =
        "https://huggingface.co/csukuangfj/vits-coqui-bn-custom_female/resolve/67008db0c4edb2c4fb20d57ccba91b7e4df50ae1",
        files = listOf(
            VoiceModelFile(
                name = "model.onnx",
                sizeBytes = 114_287_329L,
                sha256 = "8d64b23b4069feded76fda6543849516947d06db8dae3d90109e67d12c402323",
            ),
            VoiceModelFile(
                name = "tokens.txt",
                sizeBytes = 585L,
                sha256 = "2509dbac49f4ee52cc9c260f92f8634ce87342d7f3e2cb434b0c3b68c54f7d51",
            ),
            VoiceModelFile(
                name = "config.json",
                sizeBytes = 7_722L,
                sha256 = "9ccaed7559d4a70c3c773603b0d28461559e4f88cf4ea26678cc55c39feaaf69",
            ),
        ),
        licenseId = "MPL-2.0 (claimed, upstream unverified; signed off 2026-11-13)",
        licenseVerified = true,
    ),
)

fun findCatalogVoiceModelById(id: String): VoiceModel? = VOICE_MODEL_CATALOG.find { it.id == id }

/** True when the entry can be offered for download. */
fun VoiceModel.isDownloadable(): Boolean = licenseVerified && files.all { it.sizeBytes > 0 }

/**
 * Name of the sibling marker file that records the verified digest of `modelId`.
 * Written next to the model directory after every file passes verification.
 */
fun voiceModelMarkerFileName(modelId: String): String = "$modelId.sha256"

/**
 * True when the pinned URL resolves to an immutable commit rather than a moving ref.
 * Unit tests fail on `main`/branch URLs; a pin bump must move commit + digest together.
 */
fun isImmutableVoiceModelUrl(url: String): Boolean {
    val commit = Regex("/resolve/([0-9a-f]{40})/").find(url)?.groupValues?.get(1)
    return commit != null && url.startsWith("https://")
}

private const val HEX_DIGITS = "0123456789abcdef"

/** Lowercase hex encoding, matching the catalog format. */
fun ByteArray.toVoiceDigestHex(): String = buildString(size * 2) {
    for (byte in this@toVoiceDigestHex) {
        val value = byte.toInt() and 0xFF
        append(HEX_DIGITS[value ushr 4])
        append(HEX_DIGITS[value and 0x0F])
    }
}
