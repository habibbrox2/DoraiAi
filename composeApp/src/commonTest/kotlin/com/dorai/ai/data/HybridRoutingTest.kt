package com.dorai.ai.data

import com.dorai.ai.inference.LocalChatRequest
import com.dorai.ai.inference.LocalChatResult
import com.dorai.ai.inference.LocalModel
import com.dorai.ai.inference.LocalRuntime
import com.dorai.ai.inference.LocalRuntimeId
import com.dorai.ai.inference.RuntimeAvailability
import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HybridRoutingTest {

    private class FakeLocalRuntime(
        override val id: LocalRuntimeId = LocalRuntimeId.LITERT_LM,
        var isAvailable: Boolean = true,
        private val supportedModels: Set<String> = setOf("gemma-2b-it", "gemini-nano"),
    ) : LocalRuntime {
        override suspend fun availability(): RuntimeAvailability = if (isAvailable) RuntimeAvailability.AVAILABLE else RuntimeAvailability.DEVICE_UNSUPPORTED

        override fun supportsModel(modelId: String): Boolean = modelId in supportedModels

        override suspend fun chat(request: LocalChatRequest): LocalChatResult = LocalChatResult("Mock local response")

        override fun releaseInBackground() {}
    }

    private fun sampleLocalModel(
        id: String = "gemma-2b-it",
        contextWindow: Int = 2048,
    ) = LocalModel(
        id = id,
        displayName = "Gemma 2B IT",
        fileName = "gemma-2b-it.bin",
        sizeBytes = 1_000_000_000L,
        downloadUrl = "https://example.com/model.bin",
        sha256 = "dummy-sha256",
        gpuMemoryMb = 2048,
        defaultContextTokens = 1024,
        maxContextTokens = contextWindow,
        kvPerTokenBytes = 64,
    )

    private fun sampleEligibility(
        availability: RuntimeAvailability = RuntimeAvailability.AVAILABLE,
        contextTokens: Int = 500,
        maxContextTokens: Int = 2048,
        hasAttachments: Boolean = false,
        supportsVision: Boolean = false,
        hasActiveSkill: Boolean = false,
        usesMcp: Boolean = false,
        usesEmailOrScheduling: Boolean = false,
        requiredTools: List<String> = emptyList(),
        modelSupportsTools: Boolean = false,
        isAgenticFlow: Boolean = false,
        thermalThrottled: Boolean = false,
        isForeground: Boolean = true,
        quotaExceeded: Boolean = false,
    ) = LocalEligibility(
        availability = availability,
        contextTokens = contextTokens,
        maxContextTokens = maxContextTokens,
        hasAttachments = hasAttachments,
        supportsVision = supportsVision,
        hasActiveSkill = hasActiveSkill,
        usesMcp = usesMcp,
        usesEmailOrScheduling = usesEmailOrScheduling,
        requiredTools = requiredTools,
        modelSupportsTools = modelSupportsTools,
        isAgenticFlow = isAgenticFlow,
        thermalThrottled = thermalThrottled,
        isForeground = isForeground,
        quotaExceeded = quotaExceeded,
    )

    // =========================================================================
    // 1. AppSettings Persistence for RoutingMode
    // =========================================================================

    @Test
    fun `default routing mode is MANUAL`() {
        val settings = MapSettings()
        val appSettings = AppSettings(settings)
        assertEquals(RoutingMode.MANUAL, appSettings.getRoutingMode())
    }

    @Test
    fun `all routing modes can be persisted and restored`() {
        val settings = MapSettings()
        val appSettings = AppSettings(settings)

        for (mode in RoutingMode.entries) {
            appSettings.setRoutingMode(mode)
            assertEquals(mode, appSettings.getRoutingMode(), "Expected $mode to be saved and retrieved")
        }
    }

    @Test
    fun `corrupt routing mode string falls back safely to MANUAL`() {
        val settings = MapSettings()
        settings.putString("routing_mode", "INVALID_UNKNOWN_MODE")
        val appSettings = AppSettings(settings)

        assertEquals(RoutingMode.MANUAL, appSettings.getRoutingMode())
    }

    // =========================================================================
    // 2. Deterministic Fail-Closed Guarantee for LOCAL_ONLY
    // =========================================================================

    @Test
    fun `LOCAL_ONLY routes to local runtime when eligible`() {
        val policy = RoutingPolicy()
        val runtime = FakeLocalRuntime()
        val model = sampleLocalModel()
        val eligibility = sampleEligibility()
        val configuredCloud = listOf(Service.Gemini, Service.Anthropic)

        val decision = policy.decide(
            mode = RoutingMode.LOCAL_ONLY,
            localRuntime = runtime,
            localModel = model,
            eligibility = eligibility,
            configured = configuredCloud,
        )

        assertEquals(RoutingMode.LOCAL_ONLY, decision.mode)
        assertEquals(LocalRuntimeId.LITERT_LM, decision.runtimeId)
        assertEquals("gemma-2b-it", decision.modelId)
        assertNull(decision.service, "LOCAL_ONLY must never select a cloud service")
        assertTrue(decision.localEligible)
        assertFalse(decision.usesCloud, "LOCAL_ONLY must never use cloud")
        assertTrue(decision.reason.isNotBlank())
    }

    @Test
    fun `LOCAL_ONLY fails closed when local runtime is unavailable`() {
        val policy = RoutingPolicy()
        val runtime = FakeLocalRuntime(isAvailable = false)
        val model = sampleLocalModel()
        val eligibility = sampleEligibility(availability = RuntimeAvailability.DEVICE_UNSUPPORTED)
        val configuredCloud = listOf(Service.Gemini, Service.Anthropic)

        val decision = policy.decide(
            mode = RoutingMode.LOCAL_ONLY,
            localRuntime = runtime,
            localModel = model,
            eligibility = eligibility,
            configured = configuredCloud,
        )

        assertEquals(RoutingMode.LOCAL_ONLY, decision.mode)
        assertNull(decision.runtimeId)
        assertNull(decision.modelId)
        assertNull(decision.service, "CRITICAL: LOCAL_ONLY must NEVER fall back to cloud")
        assertFalse(decision.localEligible)
        assertFalse(decision.usesCloud, "CRITICAL: LOCAL_ONLY must never set usesCloud to true")
        assertTrue(decision.reason.contains("Local runtime unavailable") || decision.reason.contains("Local Only"))
    }

    @Test
    fun `LOCAL_ONLY fails closed when no local model is installed`() {
        val policy = RoutingPolicy()
        val configuredCloud = listOf(Service.Gemini)

        val decision = policy.decide(
            mode = RoutingMode.LOCAL_ONLY,
            localRuntime = null,
            localModel = null,
            eligibility = sampleEligibility(),
            configured = configuredCloud,
        )

        assertNull(decision.runtimeId)
        assertNull(decision.service, "CRITICAL: LOCAL_ONLY must NEVER fall back to cloud")
        assertFalse(decision.localEligible)
        assertFalse(decision.usesCloud)
    }

    // =========================================================================
    // 3. RoutingMode.LOCAL_FIRST
    // =========================================================================

    @Test
    fun `LOCAL_FIRST prefers local runtime when eligible`() {
        val policy = RoutingPolicy()
        val runtime = FakeLocalRuntime()
        val model = sampleLocalModel()
        val configuredCloud = listOf(Service.Gemini)

        val decision = policy.decide(
            mode = RoutingMode.LOCAL_FIRST,
            localRuntime = runtime,
            localModel = model,
            eligibility = sampleEligibility(),
            configured = configuredCloud,
        )

        assertEquals(RoutingMode.LOCAL_FIRST, decision.mode)
        assertEquals(LocalRuntimeId.LITERT_LM, decision.runtimeId)
        assertEquals("gemma-2b-it", decision.modelId)
        assertNull(decision.service)
        assertFalse(decision.usesCloud)
    }

    @Test
    fun `LOCAL_FIRST falls back to cloud pre-flight when local is ineligible`() {
        val policy = RoutingPolicy()
        val runtime = FakeLocalRuntime()
        val model = sampleLocalModel()
        val configuredCloud = listOf(Service.Gemini, Service.Anthropic)
        // Ineligible due to thermal throttling
        val eligibility = sampleEligibility(thermalThrottled = true)

        val decision = policy.decide(
            mode = RoutingMode.LOCAL_FIRST,
            localRuntime = runtime,
            localModel = model,
            eligibility = eligibility,
            configured = configuredCloud,
        )

        assertEquals(RoutingMode.LOCAL_FIRST, decision.mode)
        assertNull(decision.runtimeId)
        assertEquals(Service.Gemini, decision.service)
        assertFalse(decision.localEligible)
        assertTrue(decision.usesCloud)
        assertTrue(decision.reason.contains("Local First") && decision.reason.contains("thermally throttled"))
    }

    // =========================================================================
    // 4. RoutingMode.CLOUD_FIRST
    // =========================================================================

    @Test
    fun `CLOUD_FIRST selects primary cloud service when available`() {
        val policy = RoutingPolicy()
        val runtime = FakeLocalRuntime()
        val model = sampleLocalModel()
        val configuredCloud = listOf(Service.Anthropic, Service.Gemini)

        val decision = policy.decide(
            mode = RoutingMode.CLOUD_FIRST,
            localRuntime = runtime,
            localModel = model,
            eligibility = sampleEligibility(),
            configured = configuredCloud,
        )

        assertEquals(RoutingMode.CLOUD_FIRST, decision.mode)
        assertEquals(Service.Anthropic, decision.service)
        assertNull(decision.runtimeId)
        assertTrue(decision.usesCloud)
        assertEquals("Cloud First: cloud primary", decision.reason)
    }

    @Test
    fun `CLOUD_FIRST prioritizes first configured cloud service over local`() {
        val policy = RoutingPolicy()
        val runtime = FakeLocalRuntime()
        val model = sampleLocalModel()
        val configuredCloud = listOf(Service.Gemini)

        val decision = policy.decide(
            mode = RoutingMode.CLOUD_FIRST,
            localRuntime = runtime,
            localModel = model,
            eligibility = sampleEligibility(),
            configured = configuredCloud,
        )

        assertEquals(RoutingMode.CLOUD_FIRST, decision.mode)
        assertEquals(Service.Gemini, decision.service)
        assertNull(decision.runtimeId)
        assertFalse(decision.localEligible)
        assertTrue(decision.usesCloud)
    }

    // =========================================================================
    // 5. RoutingMode.AUTOMATIC_HYBRID
    // =========================================================================

    @Test
    fun `AUTOMATIC_HYBRID picks local for simple conversational turns`() {
        val policy = RoutingPolicy()
        val runtime = FakeLocalRuntime()
        val model = sampleLocalModel()
        val configuredCloud = listOf(Service.Gemini)

        val decision = policy.decide(
            mode = RoutingMode.AUTOMATIC_HYBRID,
            localRuntime = runtime,
            localModel = model,
            eligibility = sampleEligibility(contextTokens = 200),
            configured = configuredCloud,
        )

        assertEquals(RoutingMode.AUTOMATIC_HYBRID, decision.mode)
        assertEquals(LocalRuntimeId.LITERT_LM, decision.runtimeId)
        assertNull(decision.service)
        assertFalse(decision.usesCloud)
    }

    @Test
    fun `AUTOMATIC_HYBRID picks cloud when MCP or tools are required`() {
        val policy = RoutingPolicy()
        val runtime = FakeLocalRuntime()
        val model = sampleLocalModel()
        val configuredCloud = listOf(Service.Gemini)

        val decision = policy.decide(
            mode = RoutingMode.AUTOMATIC_HYBRID,
            localRuntime = runtime,
            localModel = model,
            eligibility = sampleEligibility(usesMcp = true),
            configured = configuredCloud,
        )

        assertEquals(RoutingMode.AUTOMATIC_HYBRID, decision.mode)
        assertNull(decision.runtimeId)
        assertEquals(Service.Gemini, decision.service)
        assertTrue(decision.usesCloud)
    }

    // =========================================================================
    // 6. RoutingMode.MANUAL
    // =========================================================================

    @Test
    fun `MANUAL preserves user configured cloud service order`() {
        val policy = RoutingPolicy()
        val runtime = FakeLocalRuntime()
        val model = sampleLocalModel()
        val configuredCloud = listOf(Service.OpenAI, Service.Anthropic)

        val decision = policy.decide(
            mode = RoutingMode.MANUAL,
            localRuntime = runtime,
            localModel = model,
            eligibility = sampleEligibility(),
            configured = configuredCloud,
        )

        assertEquals(RoutingMode.MANUAL, decision.mode)
        assertEquals(Service.OpenAI, decision.service)
        assertNull(decision.runtimeId)
        assertTrue(decision.usesCloud)
    }

    // =========================================================================
    // 7. Explainability & Transparency
    // =========================================================================

    @Test
    fun `every decision produces a human-readable reason for the badge`() {
        val policy = RoutingPolicy()
        val runtime = FakeLocalRuntime()
        val model = sampleLocalModel()

        for (mode in RoutingMode.entries) {
            val decision = policy.decide(
                mode = mode,
                localRuntime = runtime,
                localModel = model,
                eligibility = sampleEligibility(),
                configured = listOf(Service.Gemini),
            )
            assertNotNull(decision.reason)
            assertTrue(decision.reason.isNotBlank(), "Mode $mode must have a non-blank explainability reason")
        }
    }
}
