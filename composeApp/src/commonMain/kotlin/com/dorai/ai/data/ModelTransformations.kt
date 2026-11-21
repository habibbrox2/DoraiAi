package com.dorai.ai.data

import com.dorai.ai.ui.settings.SettingsModel

/**
 * Model id substrings that mark a model as non-chat. Any model whose
 * lowercased id contains one of these is filtered out of the picker.
 * Covers: voice/TTS, embeddings, moderation, OCR, safety, image gen,
 * video gen, retrieval, reward, translation, and other non-chat APIs.
 */
private val nonChatPatterns = listOf(
    "embed",
    "tts",
    "transcribe",
    "realtime",
    "moderation",
    "ocr",
    "guard",
    "safety",
    "reward",
    "voxtral",
    "whisper",
    "orpheus",
    "leanstral",
    "vibe-cli",
    "streampetr",
    "nvclip",
    "deplot",
    "paligemma",
    "gliner",
    "nemoretriever",
    "nemotron-parse",
    "riva-translate",
    "kosmos",
    "nano-banana",
    "lyria",
    "imagen",
    "image",
    "aqa",
    "veo",
    "native-audio",
    "live-",
    "bge-",
    "shieldgemma",
)

internal fun isChatModel(modelId: String): Boolean {
    val lower = modelId.lowercase()
    return nonChatPatterns.none { lower.contains(it) }
}

/**
 * Unified "newest first" sort applied to every provider's model list.
 *
 * 1. Release date descending (nulls last)
 * 2. Context window descending (nulls last)
 * 3. Model id ascending (stable tiebreaker)
 */
internal val newestFirstComparator: Comparator<SettingsModel> = Comparator { a, b ->
    /** 1. Sort by release date descending (nulls last). */
    val dateA = a.releaseDate
    val dateB = b.releaseDate
    if (dateA != null && dateB == null) return@Comparator -1
    if (dateA == null && dateB != null) return@Comparator 1
    if (dateA != null && dateB != null) {
        val cmp = dateB.compareTo(dateA)
        if (cmp != 0) return@Comparator cmp
    }

    /** 2. Sort by context window descending (nulls last). */
    val ctxA = a.contextWindow
    val ctxB = b.contextWindow
    if (ctxA != null && ctxB == null) return@Comparator -1
    if (ctxA == null && ctxB != null) return@Comparator 1
    if (ctxA != null && ctxB != null) {
        val cmp = ctxB.compareTo(ctxA)
        if (cmp != 0) return@Comparator cmp
    }

    // 3. Sort by model id ascending (stable tiebreaker).
    a.id.compareTo(b.id)
}

/**
 * Ensures [selectedModelId] appears in [models] and is marked selected.
 *
 * Previous [SettingsModel.isManualEntry] rows are dropped first so retyping a custom id
 * replaces the prior draft instead of accumulating every intermediate string in the picker.
 * When the id is blank, only provider-sourced models remain (all deselected) so the caller
 * may auto-select a default. When the id is missing from the list, a single synthetic
 * manual entry is prepended so the choice is not overwritten on refresh.
 */
internal fun ensureSelectedModelPresent(
    models: List<SettingsModel>,
    selectedModelId: String,
): List<SettingsModel> {
    val providerModels = models.filter { !it.isManualEntry }
    if (selectedModelId.isEmpty()) {
        return providerModels.map { it.copy(isSelected = false) }
    }
    val matchInProvider = providerModels.any { it.id == selectedModelId }
    return if (matchInProvider) {
        providerModels.map { it.copy(isSelected = it.id == selectedModelId) }
    } else {
        // Keep a manual row only when the id is not already a real list entry.
        // If [models] already had this id as a manual entry, rebuild it cleanly.
        listOf(
            SettingsModel(
                id = selectedModelId,
                subtitle = "",
                isSelected = true,
                isManualEntry = true,
            ),
        ) + providerModels.map { it.copy(isSelected = false) }
    }
}
