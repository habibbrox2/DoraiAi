package com.dorai.ai.data

import com.dorai.ai.network.dtos.anthropic.AnthropicModelsResponseDto
import com.dorai.ai.network.dtos.gemini.GeminiModelsResponseDto
import com.dorai.ai.network.dtos.openaicompatible.OpenAICompatibleModelResponseDto
import com.dorai.ai.network.modeldiscovery.DiscoveredModel
import com.dorai.ai.toIsoDate
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
 *  1. Release date descending (nulls last)
 *  2. Context window descending (nulls last)
 *  3. Model id ascending (stable tiebreaker)
 */
internal val newestFirstComparator: Comparator<SettingsModel> = Comparator { a, b ->
    val dateA = a.releaseDate
    val dateB = b.releaseDate
    when {
        dateA != null && dateB == null -> return@Comparator -1

        dateA == null && dateB != null -> return@Comparator 1

        dateA != null && dateB != null -> {
            val cmp = dateB.compareTo(dateA)
            if (cmp != 0) return@Comparator cmp
        }
    }
    val ctxA = a.contextWindow
    val ctxB = b.contextWindow
    when {
        ctxA != null && ctxB == null -> return@Comparator -1

        ctxA == null && ctxB != null -> return@Comparator 1

        ctxA != null && ctxB != null -> {
            val cmp = ctxB.compareTo(ctxA)
            if (cmp != 0) return@Comparator cmp
        }
    }
    a.id.compareTo(b.id)
}

/**
 * Legacy mappers kept as thin delegates over [ModelCapabilityPipeline.toSettingsModels]:
 * each one first reshapes its provider DTO into the pipeline's [DiscoveredModel] input
 * (only the fields that provider's API actually carries), then hands the list to the
 * unified filtering / dedupe / catalog / sort / selection pipeline. RemoteDataRepository
 * switches to calling the pipeline directly in a later task.
 */

internal fun mapAnthropicModels(
    models: List<AnthropicModelsResponseDto.ModelInfo>,
    selectedModelId: String,
): List<SettingsModel> {
    // An empty provider response stays empty: the pipeline would otherwise apply
    // ensureSelectedModelPresent and inject a manual entry for the selected id, which the
    // legacy mapper never did.
    if (models.isEmpty()) return emptyList()
    return ModelCapabilityPipeline.toSettingsModels(
        service = Service.Anthropic,
        models = models.map { DiscoveredModel(id = it.id, displayName = it.display_name) },
        selectedModelId = selectedModelId,
    )
}

internal fun mapGeminiModels(
    models: List<GeminiModelsResponseDto.Model>,
    selectedModelId: String,
): List<SettingsModel> {
    if (models.isEmpty()) return emptyList()
    return ModelCapabilityPipeline.toSettingsModels(
        service = Service.Gemini,
        models = models
            .filter { it.supportedGenerationMethods?.contains("generateContent") == true }
            .map {
                DiscoveredModel(
                    id = it.name.removePrefix("models/"),
                    displayName = it.displayName,
                    contextWindow = it.inputTokenLimit,
                )
            },
        selectedModelId = selectedModelId,
    )
}

internal fun mapOpenAICompatibleModels(
    models: List<OpenAICompatibleModelResponseDto.Model>,
    service: Service,
    selectedModelId: String,
): List<SettingsModel> {
    if (models.isEmpty()) return emptyList()
    return ModelCapabilityPipeline.toSettingsModels(
        service = service,
        models = models.map {
            DiscoveredModel(
                id = it.id,
                displayName = it.name,
                contextWindow = it.context_window ?: it.context_length,
                releaseDate = it.created?.toIsoDate(),
                isActive = it.isActive,
                modelType = it.type,
            )
        },
        selectedModelId = selectedModelId,
    )
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
