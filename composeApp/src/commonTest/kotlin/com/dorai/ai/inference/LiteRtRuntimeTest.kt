package com.dorai.ai.inference

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

private class FakeInferenceEngine(
    val overrideCapabilities: LocalModelCapabilities? = null,
    val chatResult: String = "fake response",
    var releaseInBackgroundCalled: Boolean = false,
) : LocalInferenceEngine {

    override val engineState: kotlinx.coroutines.flow.StateFlow<EngineState>
        get() = throw UnsupportedOperationException()

    override val downloadingModelId: kotlinx.coroutines.flow.StateFlow<String?>
        get() = throw UnsupportedOperationException()

    override val downloadProgress: kotlinx.coroutines.flow.StateFlow<Float?>
        get() = throw UnsupportedOperationException()

    override val downloadError: kotlinx.coroutines.flow.StateFlow<DownloadError?>
        get() = throw UnsupportedOperationException()

    override val importingFileName: kotlinx.coroutines.flow.StateFlow<String?>
        get() = throw UnsupportedOperationException()

    override val importProgress: kotlinx.coroutines.flow.StateFlow<Float?>
        get() = throw UnsupportedOperationException()

    override val importError: kotlinx.coroutines.flow.StateFlow<ModelImportError?>
        get() = throw UnsupportedOperationException()

    override val currentModelId: String?
        get() = throw UnsupportedOperationException()

    override suspend fun modelCapabilities(modelId: String): LocalModelCapabilities? = overrideCapabilities

    override suspend fun initialize(model: DownloadedModel, contextTokens: Int): Unit = throw UnsupportedOperationException()

    override suspend fun release(): Unit = throw UnsupportedOperationException()

    override fun releaseInBackground() {
        releaseInBackgroundCalled = true
    }

    override suspend fun chat(
        messages: List<InferenceMessage>,
        systemPrompt: String?,
        tools: List<LocalTool>,
    ): String = chatResult

    override fun getDownloadedModels(): List<DownloadedModel> = throw UnsupportedOperationException()
    override fun getAvailableModels(): List<LocalModel> = throw UnsupportedOperationException()
    override fun getImportedLocalModels(): List<LocalModel> = throw UnsupportedOperationException()
    override fun getFreeSpaceBytes(): Long = throw UnsupportedOperationException()
    override fun startDownload(model: LocalModel): Unit = throw UnsupportedOperationException()
    override fun cancelDownload(): Unit = throw UnsupportedOperationException()
    override suspend fun deleteModel(modelId: String): Unit = throw UnsupportedOperationException()
    override suspend fun importModel(source: io.github.vinceglb.filekit.PlatformFile): ModelImportResult = throw UnsupportedOperationException()
    override fun cancelImport(): Unit = throw UnsupportedOperationException()
}

class LiteRtRuntimeTest {

    @Test
    fun availability_whenEnginePresent_isAvailable() = runBlocking {
        val engine = FakeInferenceEngine()
        val runtime = LiteRtRuntime(engine)
        assertEquals(RuntimeAvailability.AVAILABLE, runtime.availability())
    }

    @Test
    fun availability_whenEngineNull_isDeviceUnsupported() = runBlocking {
        val runtime = LiteRtRuntime(null)
        assertEquals(RuntimeAvailability.DEVICE_UNSUPPORTED, runtime.availability())
    }

    @Test
    fun runtimeId_isLiteRtLm() {
        val runtime = LiteRtRuntime(null)
        assertEquals(LocalRuntimeId.LITERT_LM, runtime.id)
    }

    @Test
    fun chat_delegatesToEngine() = runBlocking {
        val engine = FakeInferenceEngine(chatResult = "hello from engine")
        val runtime = LiteRtRuntime(engine)
        val result = runtime.chat(
            LocalChatRequest(
                messages = listOf(InferenceMessage(role = "user", content = "hi")),
                systemPrompt = null,
            ),
        )
        assertEquals("hello from engine", result.content)
    }

    @Test
    fun capabilities_delegatesToEngine() = runBlocking {
        val caps = LocalModelCapabilities(
            supportsFunctionCalling = true,
            supportsThinking = false,
            supportsVision = false,
            supportsAudio = false,
            sampler = null,
        )
        val engine = FakeInferenceEngine(overrideCapabilities = caps)
        val runtime = LiteRtRuntime(engine)
        assertEquals(caps, runtime.capabilities("gemma-4-e2b-it"))
    }

    @Test
    fun capabilities_whenEngineNull_returnsNull() = runBlocking {
        val runtime = LiteRtRuntime(null)
        assertNull(runtime.capabilities("gemma-4-e2b-it"))
    }

    @Test
    fun releaseInBackground_delegatesToEngine() {
        val engine = FakeInferenceEngine()
        val runtime = LiteRtRuntime(engine)
        runtime.releaseInBackground()
        assertTrue(engine.releaseInBackgroundCalled)
    }

    @Test
    fun releaseInBackground_whenEngineNull_doesNotThrow() {
        val runtime = LiteRtRuntime(null)
        runtime.releaseInBackground() // should not throw
    }
}

class DefaultLocalRuntimeRegistryTest {

    @Test
    fun newRegistry_isEmpty() {
        val registry = DefaultLocalRuntimeRegistry()
        assertEquals(0, registry.runtimes.size)
        assertNull(registry.runtimeFor("any-model"))
    }

    @Test
    fun register_addsRuntime() {
        val registry = DefaultLocalRuntimeRegistry()
        val runtime = LiteRtRuntime(null)
        registry.register(runtime)
        assertEquals(1, registry.runtimes.size)
        assertSame(runtime, registry.runtimeFor("gemma-4-e2b-it"))
    }

    @Test
    fun register_replacesSameRuntimeId() {
        val registry = DefaultLocalRuntimeRegistry()
        val first = LiteRtRuntime(null)
        val second = LiteRtRuntime(null)
        registry.register(first)
        registry.register(second)
        assertEquals(1, registry.runtimes.size)
        assertSame(second, registry.runtimes.first())
    }

    @Test
    fun unregister_removesRuntime() {
        val registry = DefaultLocalRuntimeRegistry()
        val runtime = LiteRtRuntime(null)
        registry.register(runtime)
        registry.unregister(LocalRuntimeId.LITERT_LM)
        assertEquals(0, registry.runtimes.size)
        assertNull(registry.runtimeFor("gemma-4-e2b-it"))
    }

    @Test
    fun emptyRegistry_hasNoRuntimes() {
        assertEquals(0, EmptyLocalRuntimeRegistry.runtimes.size)
        assertNull(EmptyLocalRuntimeRegistry.runtimeFor("any-model"))
        assertNull(EmptyLocalRuntimeRegistry.runtimeFor(LocalRuntimeId.LITERT_LM))
    }

    @Test
    fun multipleRuntimes_orderedDeterministically() {
        val registry = DefaultLocalRuntimeRegistry()
        val aicore = FakeTestAiCoreRuntime()
        val litert = LiteRtRuntime(null)

        // Register in reverse ordinal order
        registry.register(aicore)
        registry.register(litert)

        assertEquals(2, registry.runtimes.size)
        // LITERT_LM (ordinal 0) should come before AICORE (ordinal 1)
        assertSame(litert, registry.runtimes[0])
        assertSame(aicore, registry.runtimes[1])
    }

    @Test
    fun runtimeFor_resolvesModelCompatibilityDeterministically() {
        val registry = DefaultLocalRuntimeRegistry()
        val litert = LiteRtRuntime(null)
        val aicore = FakeTestAiCoreRuntime()

        registry.register(litert)
        registry.register(aicore)

        // Catalog and custom litertlm models route to LiteRT
        assertSame(litert, registry.runtimeFor("gemma-4-e2b-it"))
        assertSame(litert, registry.runtimeFor("custom-model"))

        // Gemini nano and aicore models route to AICore
        assertSame(aicore, registry.runtimeFor("gemini-nano"))
        assertSame(aicore, registry.runtimeFor("aicore-nano-preview"))
    }

    @Test
    fun selectRuntime_unregisteredRuntime_returnsNull() {
        val registry = DefaultLocalRuntimeRegistry()
        val aicore = FakeTestAiCoreRuntime()
        registry.register(aicore)

        // Asking for LiteRT model when only AICore registered returns null
        assertNull(registry.selectRuntime("gemma-4-e2b-it"))
    }

    @Test
    fun selectRuntime_onEmptyRegistry_returnsNull() {
        val registry = DefaultLocalRuntimeRegistry()
        assertNull(registry.selectRuntime("gemini-nano"))
        assertNull(registry.selectRuntime("gemma-4-e2b-it"))
    }

    @Test
    fun selectRuntime_withUnsupportedCapability_filtersCandidates() {
        val registry = DefaultLocalRuntimeRegistry()
        val aicore = FakeTestAiCoreRuntime()
        registry.register(aicore)

        val visionRequired = LocalModelCapabilities(
            supportsFunctionCalling = false,
            supportsThinking = false,
            supportsVision = true,
            supportsAudio = false,
            sampler = null,
        )
        // AICore does not support vision; should filter out candidate and return null
        assertNull(registry.selectRuntime("gemini-nano", visionRequired))
    }
}

private class FakeTestAiCoreRuntime : LocalRuntime {
    override val id: LocalRuntimeId = LocalRuntimeId.AICORE
    override suspend fun availability(): RuntimeAvailability = RuntimeAvailability.AVAILABLE
    override suspend fun chat(request: LocalChatRequest): LocalChatResult = LocalChatResult(content = "aicore")
    override fun releaseInBackground() = Unit
}
