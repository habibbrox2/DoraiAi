package com.dorai.ai.inference

import kotlin.concurrent.Volatile

/**
 * Deterministic, multiplatform-safe [LocalRuntimeRegistry] implementation.
 *
 * Runtimes are registered explicitly by platform source sets. Runtime selection uses explicit
 * capability negotiation and model compatibility rather than arbitrary registration order.
 */
class DefaultLocalRuntimeRegistry : LocalRuntimeRegistry {

    @Volatile
    private var entries: List<LocalRuntime> = emptyList()

    override val runtimes: List<LocalRuntime>
        get() = entries

    override fun runtimeFor(runtimeId: LocalRuntimeId): LocalRuntime? = entries.firstOrNull { it.id == runtimeId }

    override fun runtimeFor(modelId: String): LocalRuntime? {
        val targetId = if (modelId.equals(AICORE_MODEL_ID, ignoreCase = true) ||
            modelId.startsWith("aicore", ignoreCase = true) ||
            modelId.contains("nano", ignoreCase = true)
        ) {
            LocalRuntimeId.AICORE
        } else {
            LocalRuntimeId.LITERT_LM
        }
        val explicitMatch = runtimeFor(targetId)
        if (explicitMatch != null && explicitMatch.supportsModel(modelId)) {
            return explicitMatch
        }
        return entries.filter { it.supportsModel(modelId) }.minByOrNull { it.id.ordinal }
    }

    override fun selectRuntime(
        modelId: String,
        requiredCapabilities: LocalModelCapabilities?,
    ): LocalRuntime? {
        val candidates = entries.filter { runtime ->
            if (!runtime.supportsModel(modelId)) return@filter false
            if (requiredCapabilities != null) {
                if (requiredCapabilities.supportsVision && runtime.id == LocalRuntimeId.AICORE) {
                    return@filter false
                }
                if (requiredCapabilities.supportsFunctionCalling && runtime.id == LocalRuntimeId.AICORE) {
                    return@filter false
                }
            }
            true
        }
        if (candidates.isEmpty()) return null

        val preferred = runtimeFor(modelId)
        if (preferred != null && candidates.contains(preferred)) {
            return preferred
        }
        return candidates.minByOrNull { it.id.ordinal }
    }

    override fun register(runtime: LocalRuntime) {
        entries = (entries.filterNot { it.id == runtime.id } + runtime).sortedBy { it.id.ordinal }
    }

    override fun unregister(runtimeId: LocalRuntimeId) {
        entries = entries.filterNot { it.id == runtimeId }
    }
}
