package com.dorai.ai.data

import com.dorai.ai.inference.LocalChatRequest
import com.dorai.ai.inference.LocalChatResult
import com.dorai.ai.inference.LocalModel
import com.dorai.ai.inference.LocalRuntime
import com.dorai.ai.inference.LocalRuntimeId
import com.dorai.ai.inference.RuntimeAvailability
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private val cloudService = Service.Free

private fun model(
    id: String = "gemma-4-e2b-it",
    maxContextTokens: Int = 32_768,
) = LocalModel(
    id,
    "Test",
    "test.litertlm",
    100,
    "https://example.com/test",
    "a".repeat(64),
    100,
    4_096,
    maxContextTokens,
    10_000,
)

private fun runtime(available: Boolean = true): LocalRuntime = object : LocalRuntime {
    override val id: LocalRuntimeId = LocalRuntimeId.LITERT_LM
    override suspend fun availability(): RuntimeAvailability = if (available) RuntimeAvailability.AVAILABLE else RuntimeAvailability.NEEDS_DOWNLOAD
    override suspend fun chat(request: LocalChatRequest): LocalChatResult = LocalChatResult(content = "ok")
    override fun releaseInBackground() = Unit
}

/** Positional order: available, contextFits, activeSkill, usesMcp, usesEmailOrScheduling,
 * requiredTools, modelSupportsTools, agenticFlow, thermalThrottled. */
private fun eligible(
    available: Boolean = true,
    contextFits: Boolean = true,
    activeSkill: Boolean = false,
    usesMcp: Boolean = false,
    usesEmailOrScheduling: Boolean = false,
    requiredTools: List<String> = emptyList(),
    modelSupportsTools: Boolean = true,
    agenticFlow: Boolean = false,
    thermalThrottled: Boolean = false,
    isForeground: Boolean = true,
    quotaExceeded: Boolean = false,
): LocalEligibility = LocalEligibility(
    if (available) RuntimeAvailability.AVAILABLE else RuntimeAvailability.NEEDS_DOWNLOAD,
    if (contextFits) 1000 else 30_000,
    32_768,
    false,
    false,
    activeSkill,
    usesMcp,
    usesEmailOrScheduling,
    requiredTools,
    modelSupportsTools,
    agenticFlow,
    thermalThrottled,
    isForeground,
    quotaExceeded,
)

class RoutingPolicyTest {

    @Test
    fun defaultModeIsManual() {
        assertEquals(RoutingMode.MANUAL, DefaultRoutingMode)
    }

    @Test
    fun localOnly_eligible_never_uses_cloud() {
        val policy = RoutingPolicy()
        val decision = policy.decide(
            RoutingMode.LOCAL_ONLY,
            runtime(),
            model(),
            eligible(),
            listOf(cloudService),
        )
        assertEquals(RoutingMode.LOCAL_ONLY, decision.mode)
        assertTrue(decision.localEligible)
        assertFalse(decision.usesCloud)
        assertEquals(null, decision.service)
        assertEquals("gemma-4-e2b-it", decision.modelId)
    }

    @Test
    fun localOnly_ineligible_fails_closed_no_cloud() {
        val policy = RoutingPolicy()
        val decision = policy.decide(
            RoutingMode.LOCAL_ONLY,
            runtime(false),
            model(),
            eligible(false),
            listOf(cloudService),
        )
        assertFalse(decision.localEligible)
        assertFalse(decision.usesCloud)
        assertEquals(null, decision.service)
        assertTrue(decision.reason.contains("Local Only"))
    }

    @Test
    fun localOnly_with_no_runtime_fails_closed() {
        val policy = RoutingPolicy()
        val decision = policy.decide(
            RoutingMode.LOCAL_ONLY,
            null,
            null,
            eligible(),
            listOf(cloudService),
        )
        assertFalse(decision.localEligible)
        assertFalse(decision.usesCloud)
        assertEquals(null, decision.service)
    }

    @Test
    fun localOnly_thermal_throttled_fails_closed() {
        val policy = RoutingPolicy()
        // available, contextFits, activeSkill, usesMcp, usesEmailOrScheduling,
        // requiredTools, modelSupportsTools, agenticFlow, thermalThrottled
        val decision = policy.decide(
            RoutingMode.LOCAL_ONLY,
            runtime(),
            model(),
            eligible(true, true, false, false, false, emptyList(), true, false, true),
            listOf(cloudService),
        )
        assertFalse(decision.localEligible)
        assertFalse(decision.usesCloud)
    }

    @Test
    fun manual_uses_cloud_no_local_override() {
        val policy = RoutingPolicy()
        val decision = policy.decide(
            RoutingMode.MANUAL,
            runtime(),
            model(),
            eligible(),
            listOf(cloudService),
        )
        assertEquals(RoutingMode.MANUAL, decision.mode)
        assertFalse(decision.localEligible)
        assertTrue(decision.usesCloud)
        assertEquals(cloudService, decision.service)
    }

    @Test
    fun localFirst_eligible_uses_local() {
        val policy = RoutingPolicy()
        val decision = policy.decide(
            RoutingMode.LOCAL_FIRST,
            runtime(),
            model(),
            eligible(),
            listOf(cloudService),
        )
        assertTrue(decision.localEligible)
        assertFalse(decision.usesCloud)
        assertEquals("gemma-4-e2b-it", decision.modelId)
    }

    @Test
    fun localFirst_ineligible_falls_to_cloud_and_says_why() {
        val policy = RoutingPolicy()
        val decision = policy.decide(
            RoutingMode.LOCAL_FIRST,
            runtime(),
            model(),
            eligible(false),
            listOf(cloudService),
        )
        assertFalse(decision.localEligible)
        assertTrue(decision.usesCloud)
        assertEquals(cloudService, decision.service)
        assertTrue(decision.reason.contains("Local First"))
    }

    @Test
    fun cloudFirst_uses_cloud() {
        val policy = RoutingPolicy()
        val decision = policy.decide(
            RoutingMode.CLOUD_FIRST,
            runtime(),
            model(),
            eligible(),
            listOf(cloudService),
        )
        assertTrue(decision.usesCloud)
        assertEquals(cloudService, decision.service)
        assertFalse(decision.localEligible)
    }

    @Test
    fun hybrid_eligible_uses_local_ineligible_uses_cloud() {
        val policy = RoutingPolicy()
        val local = policy.decide(
            RoutingMode.AUTOMATIC_HYBRID,
            runtime(),
            model(),
            eligible(),
            listOf(cloudService),
        )
        assertTrue(local.localEligible)
        assertFalse(local.usesCloud)

        val cloud = policy.decide(
            RoutingMode.AUTOMATIC_HYBRID,
            runtime(false),
            model(),
            eligible(false),
            listOf(cloudService),
        )
        assertFalse(cloud.localEligible)
        assertTrue(cloud.usesCloud)
    }

    @Test
    fun negotiator_rejects_context_overflow() {
        val negotiator = CapabilityNegotiator()
        // contextFits = false -> eligible(true, false, ...)
        assertFalse(negotiator.isLocalEligible(eligible(true, false)).eligible)
    }

    @Test
    fun negotiator_rejects_cloud_only_tools() {
        val negotiator = CapabilityNegotiator()
        // usesMcp = true -> eligible(true, true, false, true)
        assertFalse(negotiator.isLocalEligible(eligible(true, true, false, true)).eligible)
        // usesEmailOrScheduling = true -> eligible(true, true, false, false, true)
        assertFalse(negotiator.isLocalEligible(eligible(true, true, false, false, true)).eligible)
        // hasActiveSkill = true -> eligible(true, true, true)
        assertFalse(negotiator.isLocalEligible(eligible(true, true, true)).eligible)
        // agenticFlow = true -> eligible(true, true, false, false, false, emptyList(), true, true)
        assertFalse(
            negotiator.isLocalEligible(
                eligible(true, true, false, false, false, emptyList(), true, true),
            ).eligible,
        )
    }

    @Test
    fun negotiator_rejects_disallowed_tool() {
        val negotiator = CapabilityNegotiator()
        val allowlist = setOf("get_local_time")
        // requiredTools = listOf("memory_store") -> eligible(... false emptyList false ..., listOf("memory_store"))
        assertFalse(
            negotiator.isLocalEligible(
                eligible(true, true, false, false, false, listOf("memory_store"), true, false, false),
                allowlist,
            ).eligible,
        )
        assertTrue(
            negotiator.isLocalEligible(
                eligible(true, true, false, false, false, listOf("get_local_time"), true, false, false),
                allowlist,
            ).eligible,
        )
    }

    @Test
    fun negotiator_rejects_background_execution() {
        val negotiator = CapabilityNegotiator()
        val result = negotiator.isLocalEligible(eligible(isForeground = false))
        assertFalse(result.eligible)
        assertTrue(result.reason.contains("foreground"))
    }

    @Test
    fun negotiator_rejects_quota_exceeded() {
        val negotiator = CapabilityNegotiator()
        val result = negotiator.isLocalEligible(eligible(quotaExceeded = true))
        assertFalse(result.eligible)
        assertTrue(result.reason.contains("quota"))
    }

    @Test
    fun negotiator_rejects_attachments_when_model_lacks_vision() {
        val negotiator = CapabilityNegotiator()
        val context = eligible().copy(hasAttachments = true, supportsVision = false)
        val result = negotiator.isLocalEligible(context)
        assertFalse(result.eligible)
        assertTrue(result.reason.contains("Attachments"))
    }

    @Test
    fun negotiator_rejects_tools_when_model_does_not_support_tools() {
        val negotiator = CapabilityNegotiator()
        val context = eligible(requiredTools = listOf("get_time"), modelSupportsTools = false)
        val result = negotiator.isLocalEligible(context)
        assertFalse(result.eligible)
        assertTrue(result.reason.contains("tool"))
    }
}
