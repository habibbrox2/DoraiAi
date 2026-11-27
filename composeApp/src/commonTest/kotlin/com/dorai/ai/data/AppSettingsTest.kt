package com.dorai.ai.data

import com.dorai.ai.cloud.DoraiCloudSession
import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AppSettingsTest {

    @Test
    fun `restore last chat setting defaults to true and round-trips`() {
        val settings = MapSettings()
        val appSettings = AppSettings(settings)

        assertTrue(appSettings.isRestoreLastChatEnabled(), "default must be true (restore last conversation)")
        appSettings.setRestoreLastChatEnabled(false)
        assertFalse(appSettings.isRestoreLastChatEnabled())
        appSettings.setRestoreLastChatEnabled(true)
        assertTrue(appSettings.isRestoreLastChatEnabled())
    }

    @Test
    fun `migration only runs once so deleted services stay deleted`() {
        val settings = MapSettings()
        val appSettings = AppSettings(settings)

        // Set up a legacy API key for OpenAI
        appSettings.setApiKey(Service.OpenAI, "sk-test-key")

        // First run — migrates and adds the service
        appSettings.migrateConfiguredServicesIfNeeded()
        assertEquals(1, appSettings.getConfiguredServiceInstances().size)
        assertEquals(Service.OpenAI.id, appSettings.getConfiguredServiceInstances()[0].serviceId)

        // User deletes the service
        appSettings.setConfiguredServiceInstances(emptyList())
        appSettings.removeInstanceSettings(Service.OpenAI.id)
        assertTrue(appSettings.getConfiguredServiceInstances().isEmpty())

        // Second run — flag prevents re-migration, deleted service stays deleted
        appSettings.migrateConfiguredServicesIfNeeded()
        assertTrue(appSettings.getConfiguredServiceInstances().isEmpty())
    }

    @Test
    fun `Anthropic credential persistence via instance settings`() {
        val settings = MapSettings()
        val appSettings = AppSettings(settings)

        val instanceId = appSettings.generateInstanceId(Service.Anthropic.id)
        assertEquals("anthropic", instanceId)

        appSettings.setInstanceApiKey(instanceId, "sk-ant-test-key")
        assertEquals("sk-ant-test-key", appSettings.getInstanceApiKey(instanceId))

        appSettings.setInstanceModelId(instanceId, "claude-sonnet-4-20250514")
        assertEquals("claude-sonnet-4-20250514", appSettings.getInstanceModelId(instanceId))
    }

    // region Base URL v1 migration

    @Test
    fun `base URL migration appends v1 to plain host URL`() {
        val settings = MapSettings()
        val appSettings = AppSettings(settings)

        appSettings.setConfiguredServiceInstances(
            listOf(ServiceInstance("compat1", "openai-compatible")),
        )
        appSettings.setInstanceBaseUrl("compat1", "http://localhost:11434")

        appSettings.migrateBaseUrlsToV1PathIfNeeded()

        assertEquals("http://localhost:11434/v1", appSettings.getInstanceBaseUrl("compat1"))
    }

    @Test
    fun `base URL migration does not double-append v1`() {
        val settings = MapSettings()
        val appSettings = AppSettings(settings)

        appSettings.setConfiguredServiceInstances(
            listOf(ServiceInstance("compat1", "openai-compatible")),
        )
        appSettings.setInstanceBaseUrl("compat1", "http://localhost:11434/v1")

        appSettings.migrateBaseUrlsToV1PathIfNeeded()

        assertEquals("http://localhost:11434/v1", appSettings.getInstanceBaseUrl("compat1"))
    }

    // region Custom model

    @Test
    fun `effective model id uses custom when flag is on`() {
        val appSettings = AppSettings(MapSettings())
        val id = "compat1"
        appSettings.setInstanceModelId(id, "listed-model")
        appSettings.setInstanceCustomModelId(id, "glm-4.7-flash")
        assertEquals("listed-model", appSettings.getInstanceEffectiveModelId(id))
        appSettings.setInstanceUseCustomModel(id, true)
        assertEquals("glm-4.7-flash", appSettings.getInstanceEffectiveModelId(id))
        appSettings.setInstanceUseCustomModel(id, false)
        assertEquals("listed-model", appSettings.getInstanceEffectiveModelId(id))
    }

    @Test
    fun `custom model migration prefills custom id from list model without enabling flag`() {
        val appSettings = AppSettings(MapSettings())
        appSettings.setConfiguredServiceInstances(
            listOf(ServiceInstance("compat1", "openai-compatible")),
        )
        appSettings.setInstanceModelId("compat1", "my-local-model")

        appSettings.migrateCustomModelSettingsIfNeeded()

        assertFalse(appSettings.getInstanceUseCustomModel("compat1"))
        assertEquals("my-local-model", appSettings.getInstanceCustomModelId("compat1"))
        assertEquals("my-local-model", appSettings.getInstanceModelId("compat1"))
        assertEquals("my-local-model", appSettings.getInstanceEffectiveModelId("compat1"))

        // Second run is a no-op
        appSettings.setInstanceCustomModelId("compat1", "other")
        appSettings.migrateCustomModelSettingsIfNeeded()
        assertEquals("other", appSettings.getInstanceCustomModelId("compat1"))
    }

    @Test
    fun `removeInstanceSettings clears custom model keys`() {
        val appSettings = AppSettings(MapSettings())
        val id = "compat1"
        appSettings.setInstanceUseCustomModel(id, true)
        appSettings.setInstanceCustomModelId(id, "custom")
        appSettings.setInstanceModelId(id, "listed")
        appSettings.removeInstanceSettings(id)
        assertFalse(appSettings.getInstanceUseCustomModel(id))
        assertEquals("", appSettings.getInstanceCustomModelId(id))
        assertEquals("", appSettings.getInstanceModelId(id))
    }

    @Test
    fun `auto model flag defaults off and round-trips`() {
        val appSettings = AppSettings(MapSettings())
        val id = "compat1"
        assertFalse(appSettings.getInstanceAutoModelEnabled(id), "Auto must default off")
        appSettings.setInstanceAutoModelEnabled(id, true)
        assertTrue(appSettings.getInstanceAutoModelEnabled(id))
        // Auto never replaces the stored model selection.
        assertEquals("", appSettings.getInstanceModelId(id))
    }

    @Test
    fun `removeInstanceSettings clears the auto model flag`() {
        val appSettings = AppSettings(MapSettings())
        val id = "compat1"
        appSettings.setInstanceAutoModelEnabled(id, true)
        appSettings.removeInstanceSettings(id)
        assertFalse(appSettings.getInstanceAutoModelEnabled(id))
    }

    @Test
    fun `base URL migration handles trailing slash`() {
        val settings = MapSettings()
        val appSettings = AppSettings(settings)

        appSettings.setConfiguredServiceInstances(
            listOf(ServiceInstance("compat1", "openai-compatible")),
        )
        appSettings.setInstanceBaseUrl("compat1", "http://localhost:11434/")

        appSettings.migrateBaseUrlsToV1PathIfNeeded()

        assertEquals("http://localhost:11434/v1", appSettings.getInstanceBaseUrl("compat1"))
    }

    @Test
    fun `base URL migration preserves custom version path`() {
        val settings = MapSettings()
        val appSettings = AppSettings(settings)

        appSettings.setConfiguredServiceInstances(
            listOf(ServiceInstance("compat1", "openai-compatible")),
        )
        appSettings.setInstanceBaseUrl("compat1", "https://my-provider.com/api/v1")

        appSettings.migrateBaseUrlsToV1PathIfNeeded()

        assertEquals("https://my-provider.com/api/v1", appSettings.getInstanceBaseUrl("compat1"))
    }

    @Test
    fun `base URL migration skips non-OpenAI-compatible instances`() {
        val settings = MapSettings()
        val appSettings = AppSettings(settings)

        appSettings.setConfiguredServiceInstances(
            listOf(ServiceInstance("openai", "openai")),
        )
        appSettings.setInstanceBaseUrl("openai", "https://api.openai.com")

        appSettings.migrateBaseUrlsToV1PathIfNeeded()

        // Should not be modified
        assertEquals("https://api.openai.com", appSettings.getInstanceBaseUrl("openai"))
    }

    @Test
    fun `base URL migration only runs once`() {
        val settings = MapSettings()
        val appSettings = AppSettings(settings)

        appSettings.setConfiguredServiceInstances(
            listOf(ServiceInstance("compat1", "openai-compatible")),
        )
        appSettings.setInstanceBaseUrl("compat1", "http://localhost:11434")

        appSettings.migrateBaseUrlsToV1PathIfNeeded()
        assertEquals("http://localhost:11434/v1", appSettings.getInstanceBaseUrl("compat1"))

        // Reset to a plain URL — second run should NOT migrate
        appSettings.setInstanceBaseUrl("compat1", "http://other-host:8080")
        appSettings.migrateBaseUrlsToV1PathIfNeeded()
        assertEquals("http://other-host:8080", appSettings.getInstanceBaseUrl("compat1"))
    }

    @Test
    fun `base URL migration also migrates legacy per-service key`() {
        val settings = MapSettings()
        val appSettings = AppSettings(settings)

        appSettings.setConfiguredServiceInstances(emptyList())
        appSettings.setBaseUrl(Service.OpenAICompatible, "http://localhost:11434")

        appSettings.migrateBaseUrlsToV1PathIfNeeded()

        assertEquals("http://localhost:11434/v1", appSettings.getBaseUrl(Service.OpenAICompatible))
    }

    @Test
    fun `base URL migration skips blank base URLs`() {
        val settings = MapSettings()
        val appSettings = AppSettings(settings)

        appSettings.setConfiguredServiceInstances(
            listOf(ServiceInstance("compat1", "openai-compatible")),
        )
        // Don't set a base URL — it should remain blank
        assertFalse(settings.getBoolean("base_url_v1_migration_complete", false))

        appSettings.migrateBaseUrlsToV1PathIfNeeded()

        assertEquals("", appSettings.getInstanceBaseUrl("compat1"))
        assertTrue(settings.getBoolean("base_url_v1_migration_complete", false))
    }

    // endregion

    @Test
    fun `migration adds services with legacy API keys`() {
        val settings = MapSettings()
        val appSettings = AppSettings(settings)

        appSettings.setApiKey(Service.OpenAI, "sk-test-key")
        appSettings.setApiKey(Service.Gemini, "gemini-key")

        appSettings.migrateConfiguredServicesIfNeeded()

        val instances = appSettings.getConfiguredServiceInstances()
        assertEquals(2, instances.size)
        assertTrue(instances.any { it.serviceId == Service.OpenAI.id })
        assertTrue(instances.any { it.serviceId == Service.Gemini.id })
    }

    // region Default provider seeding

    @Test
    fun `a bare install starts with no default provider active`() {
        val appSettings = AppSettings(MapSettings())

        appSettings.seedDefaultServicesIfNeeded()

        val instances = appSettings.getConfiguredServiceInstances()
        assertTrue(instances.isEmpty())
    }

    @Test
    fun `seeding keeps the services an install already has`() {
        val appSettings = AppSettings(MapSettings())
        appSettings.setConfiguredServiceInstances(listOf(ServiceInstance(Service.OpenAI.id, Service.OpenAI.id)))

        appSettings.seedDefaultServicesIfNeeded()

        val instances = appSettings.getConfiguredServiceInstances()
        assertEquals(1, instances.size)
        assertEquals(Service.OpenAI.id, instances.first().serviceId)
    }

    @Test
    fun `seeding runs once so default remains empty`() {
        val appSettings = AppSettings(MapSettings())

        appSettings.seedDefaultServicesIfNeeded()
        assertEquals(0, appSettings.getConfiguredServiceInstances().size)

        appSettings.setConfiguredServiceInstances(
            listOf(ServiceInstance(Service.OpenCode.id, Service.OpenCode.id)),
        )
        appSettings.seedDefaultServicesIfNeeded()

        val instances = appSettings.getConfiguredServiceInstances()
        assertEquals(1, instances.size)
        assertEquals(Service.OpenCode.id, instances.first().serviceId)
    }

    @Test
    fun `dorai cloud session round-trips and clears`() {
        val appSettings = AppSettings(MapSettings())
        assertNull(appSettings.getDoraiCloudSession())

        val session = DoraiCloudSession(
            userId = "firebase-uid-123",
            email = "user@example.test",
            accessToken = "id-token",
            refreshToken = null,
            expiresAtEpochSeconds = 1_900_000_000L,
        )
        appSettings.setDoraiCloudSession(session)
        val restored = appSettings.getDoraiCloudSession()
        assertEquals(session.userId, restored?.userId)
        assertEquals(session.email, restored?.email)
        assertEquals(session.accessToken, restored?.accessToken)
        assertEquals(session.expiresAtEpochSeconds, restored?.expiresAtEpochSeconds)

        appSettings.setDoraiCloudSession(null)
        assertNull(appSettings.getDoraiCloudSession())
    }

    @Test
    fun `dorai cloud session expiry respects leeway`() {
        val session = DoraiCloudSession(
            userId = "u",
            email = null,
            accessToken = "t",
            refreshToken = null,
            expiresAtEpochSeconds = 1_000L,
        )
        assertFalse(session.isExpired(nowEpochSeconds = 900L, leewaySeconds = 60))
        assertTrue(session.isExpired(nowEpochSeconds = 950L, leewaySeconds = 60))
        assertTrue(session.isExpired(nowEpochSeconds = 1001L, leewaySeconds = 0))
    }

    @Test
    fun `dorai cloud session migration compatibility parses legacy supabase-shaped json`() {
        val settings = MapSettings()
        // Legacy Supabase-shaped session payload (same DTO fields).
        settings.putString(
            "dorai_cloud_session",
            """{"user_id":"legacy-uuid","email":"old@example.test","access_token":"old-access","refresh_token":"old-refresh","expires_at":1900000000}""",
        )
        val appSettings = AppSettings(settings)
        val restored = appSettings.getDoraiCloudSession()
        assertEquals("legacy-uuid", restored?.userId)
        assertEquals("old@example.test", restored?.email)
        assertEquals("old-access", restored?.accessToken)
        assertEquals("old-refresh", restored?.refreshToken)
        assertEquals(1_900_000_000L, restored?.expiresAtEpochSeconds)
    }

    @Test
    fun `dorai cloud session invalid json returns null`() {
        val settings = MapSettings()
        settings.putString("dorai_cloud_session", "{not-json")
        val appSettings = AppSettings(settings)
        assertNull(appSettings.getDoraiCloudSession())
    }

    // endregion
}
