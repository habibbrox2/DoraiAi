package com.dorai.ai.inference

import com.dorai.ai.data.Service

/**
 * The identity of an on-device inference runtime. Each installed model carries exactly one
 * runtime; [Service.LiteRT] remains the single on-device [Service] so settings keys, the
 * local allowlist, and the "local errors never fall back to cloud" guard are not split
 * across two service ids.
 */
enum class LocalRuntimeId {
    /** Google LiteRT-LM: downloaded weights from a pinned HuggingFace revision, shipped
     * native lib, Android + Desktop + iOS. */
    LITERT_LM,

    /** Gemini Nano via AICore + ML Kit GenAI: system-provided by Play Services, Play Store
     * flavor only. Not downloadable or deletable by the app. */
    AICORE,
}

/**
 * How a model's bytes reach the device. Nothing is ever bundled in the APK/AAB.
 */
sealed interface ModelDelivery {
    /** Runtime-downloaded weights from a pinned HuggingFace revision. */
    data class Downloadable(
        val url: String,
        val revision: String,
        val sizeBytes: Long,
        val sha256: String,
    ) : ModelDelivery

    /** System-provided by the OS/Play Services; not downloadable or deletable by the app. */
    object SystemProvided : ModelDelivery
}

/**
 * Whether a runtime can serve a turn right now.
 */
enum class RuntimeAvailability {
    AVAILABLE,
    NEEDS_DOWNLOAD,
    DEVICE_UNSUPPORTED,
    OS_TOO_OLD,
    DISABLED_BY_FLAVOR,
}

/**
 * Who published the model and under what terms. Shown in the model detail sheet so users
 * can see provenance before downloading.
 */
data class Provenance(
    val publisher: String,
    val source: String, // e.g. "HuggingFace litert-community", "Google AICore"
    val modelCardUrl: String? = null,
)

/**
 * A model's declared license. [id] is a stable machine value; [name] is human-readable.
 */
data class ModelLicense(
    val id: String, // e.g. "Apache-2.0", "Gemma Terms of Use", "LFM1.0"
    val name: String,
    val url: String? = null,
)

/**
 * Runtime-specific metadata stored alongside a catalog entry.
 */
data class LocalRuntimeInfo(
    val modelId: String,
    val runtime: LocalRuntimeId,
    val delivery: ModelDelivery,
    val license: ModelLicense,
    val provenance: Provenance,
)

/**
 * What [LocalRuntime.chat] receives. Non-streaming in Phase 0.
 */
data class LocalChatRequest(
    val messages: List<InferenceMessage>,
    val systemPrompt: String?,
    val tools: List<LocalTool> = emptyList(),
)

/**
 * The result of one on-device chat turn. Routing metadata is attached at the gateway level,
 * not here, to keep this package free of the data layer.
 */
data class LocalChatResult(
    val content: String,
    val reasoningContent: String? = null,
)

/**
 * A single on-device inference runtime. Implementations wrap [LocalInferenceEngine] (LiteRT)
 * or a platform runtime (AICore). All methods are safe to call from common code.
 */
interface LocalRuntime {
    val id: LocalRuntimeId

    /** Whether this runtime can serve a turn right now, before any inference is attempted. */
    suspend fun availability(): RuntimeAvailability

    /**
     * What the model file for [modelId] declares it can do. Null means *unknown*, not
     * *incapable*.
     */
    suspend fun capabilities(modelId: String): LocalModelCapabilities? = null

    suspend fun chat(request: LocalChatRequest): LocalChatResult

    /** Fire-and-forget release so the GPU driver can reclaim memory. */
    fun releaseInBackground()
}

/**
 * Registry of available on-device runtimes. The LiteRT runtime is always registered; the
 * AICore runtime is registered only in `playStore` builds on supported devices.
 */
interface LocalRuntimeRegistry {
    val runtimes: List<LocalRuntime>

    fun runtimeFor(modelId: String): LocalRuntime?

    fun register(runtime: LocalRuntime)

    fun unregister(runtimeId: LocalRuntimeId)
}

/** Default, empty registry for platforms/tests that have no runtimes registered yet. */
object EmptyLocalRuntimeRegistry : LocalRuntimeRegistry {
    override val runtimes: List<LocalRuntime> = emptyList()
    override fun runtimeFor(modelId: String): LocalRuntime? = null
    override fun register(runtime: LocalRuntime) = Unit
    override fun unregister(runtimeId: LocalRuntimeId) = Unit
}
