package com.dorai.ai.inference

/**
 * Adapts an existing [LocalInferenceEngine] (LiteRT on Android/Desktop, iOS on iOS) to the
 * [LocalRuntime] interface.
 *
 * The engine instance is created once by the platform's [createLocalInferenceEngine] and
 * passed in already-constructed. This adapter does not create a second engine, does not
 * duplicate model loading or download logic, and reuses the existing engine's integrity
 * verification, idle release, and context management.
 */
class LiteRtRuntime(private val engine: LocalInferenceEngine?) : LocalRuntime {

    override val id: LocalRuntimeId = LocalRuntimeId.LITERT_LM

    override suspend fun availability(): RuntimeAvailability = if (engine != null) RuntimeAvailability.AVAILABLE else RuntimeAvailability.DEVICE_UNSUPPORTED

    override suspend fun capabilities(modelId: String): LocalModelCapabilities? = engine?.modelCapabilities(modelId)

    override suspend fun chat(request: LocalChatRequest): LocalChatResult {
        val engine = engine ?: throw IllegalStateException("LiteRT inference engine is not available")
        val content = engine.chat(
            messages = request.messages,
            systemPrompt = request.systemPrompt,
            tools = request.tools,
        )
        return LocalChatResult(content = content)
    }

    override fun releaseInBackground() {
        engine?.releaseInBackground()
    }
}
