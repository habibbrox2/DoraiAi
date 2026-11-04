package com.dorai.ai.data

import com.dorai.ai.network.OpenAICompatibleConnectionException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class KeylessFreeTierFallbackTest {

    @Test
    fun `auto alias targets Kilo first then only OpenCode free models`() {
        assertEquals("auto", KeylessFreeTierFallback.AUTO_MODEL_ID)
        assertEquals(Service.Kilo, KeylessFreeTierFallback.targets.first().service)
        assertEquals("kilo-auto/free", KeylessFreeTierFallback.targets.first().modelId)

        val openCodeFreeModelIds = Service.OpenCode.defaultModels
            .filter { it.subtitle.contains("free", ignoreCase = true) }
            .map { it.id }
        val configuredOpenCodeTargets = KeylessFreeTierFallback.targets
            .filter { it.service == Service.OpenCode }
            .map { it.modelId }

        assertEquals(openCodeFreeModelIds, configuredOpenCodeTargets)
        assertTrue(configuredOpenCodeTargets.isNotEmpty())
        assertFalse(configuredOpenCodeTargets.any { it.contains("sonnet") || it.contains("gpt-") })
    }

    @Test
    fun `fallback returns first successful keyless target`() = runTest {
        val attempts = mutableListOf<KeylessFallbackTarget>()

        val result = KeylessFreeTierFallback.execute { target ->
            attempts += target
            if (target.service == Service.Kilo) {
                throw OpenAICompatibleConnectionException()
            }
            "answer"
        }

        assertEquals("answer", result)
        assertEquals(
            listOf(Service.Kilo, Service.OpenCode),
            attempts.map { it.service },
        )
    }

    @Test
    fun `fallback does not continue after a tool-loop side effect`() = runTest {
        val attempts = mutableListOf<KeylessFallbackTarget>()
        val failure = OpenAICompatibleConnectionException()

        val thrown = try {
            KeylessFreeTierFallback.execute(
                canContinueAfterFailure = { false },
            ) { target ->
                attempts += target
                throw failure
            }
            error("Expected fallback to stop after the failure")
        } catch (caught: OpenAICompatibleConnectionException) {
            caught
        }

        assertSame(failure, thrown)
        assertEquals(listOf(Service.Kilo), attempts.map { it.service })
    }

    @Test
    fun `fallback exhaustion throws the last provider error`() = runTest {
        val failures = mutableListOf<OpenAICompatibleConnectionException>()

        val thrown = try {
            KeylessFreeTierFallback.execute { target ->
                OpenAICompatibleConnectionException().also { failures += it }.let { throw it }
            }
            error("Expected all fallback targets to fail")
        } catch (caught: OpenAICompatibleConnectionException) {
            caught
        }

        assertSame(failures.last(), thrown)
        assertEquals(
            1 + Service.OpenCode.defaultModels.count { it.subtitle.contains("free", ignoreCase = true) },
            failures.size,
        )
    }

    @Test
    fun `Kilo and OpenCode stay internal but remain resolvable`() {
        assertTrue(KeylessFreeTierFallback.isInternalService(Service.Kilo))
        assertTrue(KeylessFreeTierFallback.isInternalService(Service.OpenCode))
        assertFalse(KeylessFreeTierFallback.isInternalService(Service.OpenAI))
        assertEquals(Service.Kilo, Service.fromId("kilo"))
        assertEquals(Service.OpenCode, Service.fromId("opencode"))
    }

    @Test
    fun `credentials for hidden targets never contain an API key`() {
        val credentials = KeylessFreeTierFallback.credentialsFor(KeylessFreeTierFallback.targets.first())

        assertEquals("", credentials.apiKey)
        assertEquals("kilo-auto/free", credentials.modelId)
    }
}
