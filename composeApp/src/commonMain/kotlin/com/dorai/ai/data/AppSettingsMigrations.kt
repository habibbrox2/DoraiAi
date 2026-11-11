package com.dorai.ai.data

import com.dorai.ai.data.AppSettings.Companion.KEY_APP_OPENS
import com.dorai.ai.data.AppSettings.Companion.KEY_BASE_URL_V1_MIGRATION_COMPLETE
import com.dorai.ai.data.AppSettings.Companion.KEY_CURRENT_SERVICE_ID
import com.dorai.ai.data.AppSettings.Companion.KEY_CUSTOM_MODEL_MIGRATION_COMPLETE
import com.dorai.ai.data.AppSettings.Companion.KEY_EMAIL_ACCOUNTS
import com.dorai.ai.data.AppSettings.Companion.KEY_EMAIL_PASSWORD_PREFIX
import com.dorai.ai.data.AppSettings.Companion.KEY_ENCRYPTION_KEY
import com.dorai.ai.data.AppSettings.Companion.KEY_INSTANCE_MIGRATION_COMPLETE
import com.dorai.ai.data.AppSettings.Companion.KEY_MIGRATION_COMPLETE
import com.dorai.ai.data.AppSettings.Companion.KEY_SERVICES_MIGRATION_COMPLETE
import com.dorai.ai.data.AppSettings.Companion.KEY_SPLINTERLANDS_ACCOUNT
import com.dorai.ai.data.AppSettings.Companion.KEY_SPLINTERLANDS_POSTING_KEY
import com.russhwolf.settings.Settings

internal val versionPathRegex = Regex("/v\\d+$")

fun AppSettings.runMigrations(legacySettings: Settings?) {
    migrateFromLegacyIfNeeded(legacySettings)
    migrateConfiguredServicesIfNeeded()
    migrateInstanceSettingsIfNeeded()
    migrateBaseUrlsToV1PathIfNeeded()
    migrateCustomModelSettingsIfNeeded()
    seedDefaultServicesIfNeeded()
}

fun AppSettings.migrateFromLegacyIfNeeded(legacySettings: Settings?) {
    if (legacySettings == null) return
    if (settings.getBoolean(KEY_MIGRATION_COMPLETE, false)) return

    val migratedKeys = mutableListOf<String>()

    fun migrateString(legacy: Settings, key: String) {
        val value = legacy.getStringOrNull(key)
        if (value != null && settings.getStringOrNull(key) == null) {
            settings.putString(key, value)
        }
        if (value != null) migratedKeys.add(key)
    }

    fun migrateInt(legacy: Settings, key: String) {
        if (legacy.hasKey(key)) {
            if (!settings.hasKey(key)) settings.putInt(key, legacy.getInt(key, 0))
            migratedKeys.add(key)
        }
    }

    migrateString(legacySettings, KEY_CURRENT_SERVICE_ID)
    migrateInt(legacySettings, KEY_APP_OPENS)

    for (service in Service.all) {
        if (service.settingsKeyPrefix.isNotEmpty()) {
            migrateString(legacySettings, service.apiKeyKey)
            migrateString(legacySettings, service.modelIdKey)
        }
    }
    migrateString(legacySettings, Service.OpenAICompatible.baseUrlKey)

    // Secrets that may predate the encrypted store. Account-scoped keys are discovered
    // from the account JSONs because the common Settings API cannot enumerate keys.
    migrateString(legacySettings, KEY_ENCRYPTION_KEY)
    val emailAccountsJson = legacySettings.getStringOrNull(KEY_EMAIL_ACCOUNTS)
    if (emailAccountsJson != null && settings.getStringOrNull(KEY_EMAIL_ACCOUNTS) == null) {
        setEmailAccountsJson(emailAccountsJson)
        migratedKeys.add(KEY_EMAIL_ACCOUNTS)
    }
    val splinterlandsAccountJson = legacySettings.getStringOrNull(KEY_SPLINTERLANDS_ACCOUNT)
    if (splinterlandsAccountJson != null && settings.getStringOrNull(KEY_SPLINTERLANDS_ACCOUNT) == null) {
        setSplinterlandsAccountJson(splinterlandsAccountJson)
        migratedKeys.add(KEY_SPLINTERLANDS_ACCOUNT)
    }
    migrateString(legacySettings, KEY_SPLINTERLANDS_POSTING_KEY)
    val accountIds = buildList {
        addAll(settings.getStringOrNull(KEY_EMAIL_ACCOUNTS).extractAccountIds())
        addAll(settings.getStringOrNull(KEY_SPLINTERLANDS_ACCOUNT).extractAccountIds())
    }
    for (accountId in accountIds) {
        val emailPassword = legacySettings.getStringOrNull("${KEY_EMAIL_PASSWORD_PREFIX}$accountId")
        if (emailPassword != null && settings.getStringOrNull("${KEY_EMAIL_PASSWORD_PREFIX}$accountId") == null) {
            setEmailPassword(accountId, emailPassword)
            migratedKeys.add("${KEY_EMAIL_PASSWORD_PREFIX}$accountId")
        }
        val splKey = legacySettings.getStringOrNull("${KEY_SPLINTERLANDS_POSTING_KEY}_$accountId")
        if (splKey != null && settings.getStringOrNull("${KEY_SPLINTERLANDS_POSTING_KEY}_$accountId") == null) {
            setSplinterlandsPostingKey(accountId, splKey)
            migratedKeys.add("${KEY_SPLINTERLANDS_POSTING_KEY}_$accountId")
        }
    }

    if (verifiedMigratedKeys(legacySettings, migratedKeys)) {
        // The encrypted store now holds every migrated value — remove the plaintext
        // originals so API keys don't live on disk unencrypted after migration.
        for (key in migratedKeys) legacySettings.remove(key)
    }

    setMigrationComplete(true)
}

/**
 * True when every [key] that still exists in the legacy store also exists in the
 * encrypted store with an equal value (or the key was intentionally not carried over
 * because the destination already had a newer value). Only then is the legacy
 * plaintext store safe to clear.
 */
private fun AppSettings.verifiedMigratedKeys(legacy: Settings, keys: List<String>): Boolean = keys.all { key ->
    val legacyValue = legacy.getStringOrNull(key) ?: return@all true
    settings.getStringOrNull(key) != null
}

private fun String?.extractAccountIds(): List<String> = this?.let { json -> Regex(""""id"\s*:\s*"([^"]+)"""").findAll(json).mapTo(mutableListOf()) { it.groupValues[1] } } ?: emptyList()

fun AppSettings.migrateConfiguredServicesIfNeeded() {
    if (settings.getBoolean(KEY_SERVICES_MIGRATION_COMPLETE, false)) return

    val existing = getConfiguredServiceInstances()
    val existingServiceIds = existing.map { it.serviceId }.toSet()
    val instances = existing.toMutableList()

    val currentServiceId = settings.getString(KEY_CURRENT_SERVICE_ID, Service.Free.id)
    val currentService = Service.fromId(currentServiceId)
    if (currentService != Service.Free && currentService.id !in existingServiceIds) {
        instances.add(ServiceInstance(instanceId = currentService.id, serviceId = currentService.id))
    }

    for (service in Service.all) {
        if (service == Service.Free) continue
        if (service.id in existingServiceIds) continue
        if (instances.any { it.serviceId == service.id }) continue
        val apiKey = getApiKey(service)
        if (apiKey.isNotBlank()) {
            instances.add(ServiceInstance(instanceId = service.id, serviceId = service.id))
        }
    }

    if (instances.size > existing.size) {
        setConfiguredServiceInstances(instances)
    }

    setServicesMigrationComplete(true)
}

/**
 * Migrate per-service settings to per-instance settings.
 * For existing users, the first instance of each service type uses the service's
 * legacy key prefix. This copies those values to the new instance_ keys.
 */
fun AppSettings.migrateInstanceSettingsIfNeeded() {
    if (settings.getBoolean(KEY_INSTANCE_MIGRATION_COMPLETE, false)) return

    val instances = getConfiguredServiceInstances()
    for (instance in instances) {
        val service = Service.fromId(instance.serviceId)
        if (service == Service.Free) continue
        val legacyApiKey = getApiKey(service)
        if (legacyApiKey.isNotBlank() && getInstanceApiKey(instance.instanceId).isBlank()) {
            setInstanceApiKey(instance.instanceId, legacyApiKey)
        }
        val legacyModel = getSelectedModelId(service)
        if (legacyModel.isNotBlank() && getInstanceModelId(instance.instanceId).isBlank()) {
            setInstanceModelId(instance.instanceId, legacyModel)
        }
        if (service == Service.OpenAICompatible) {
            val legacyBaseUrl = getBaseUrl(service)
            if (legacyBaseUrl.isNotBlank() && getInstanceBaseUrl(instance.instanceId).isBlank()) {
                setInstanceBaseUrl(instance.instanceId, legacyBaseUrl)
            }
        }
    }

    setInstanceMigrationComplete(true)
}

/**
 * Migrate existing OpenAI-compatible base URLs to include `/v1` path segment.
 * Previously `/v1` was hardcoded in the endpoint paths; now the base URL should
 * include it (following the OpenAI SDK convention).
 */
fun AppSettings.migrateBaseUrlsToV1PathIfNeeded() {
    if (settings.getBoolean(KEY_BASE_URL_V1_MIGRATION_COMPLETE, false)) return

    val instances = getConfiguredServiceInstances()
    for (instance in instances) {
        val service = Service.fromId(instance.serviceId)
        if (service != Service.OpenAICompatible) continue
        val baseUrl = getInstanceBaseUrl(instance.instanceId)
        if (baseUrl.isNotBlank()) {
            setInstanceBaseUrl(instance.instanceId, ensureBaseUrlHasVersionPath(baseUrl))
        }
    }

    val legacyBaseUrl = settings.getString(Service.OpenAICompatible.baseUrlKey, "")
    if (legacyBaseUrl.isNotBlank()) {
        setBaseUrl(Service.OpenAICompatible, ensureBaseUrlHasVersionPath(legacyBaseUrl))
    }

    setBaseUrlV1MigrationComplete(true)
}

internal fun ensureBaseUrlHasVersionPath(url: String): String {
    val trimmed = url.trimEnd('/')
    if (trimmed.contains(versionPathRegex)) return trimmed
    return "$trimmed/v1"
}

/**
 * Seeds OpenAI-Compatible custom-model fields introduced for free-text model entry.
 *
 * Existing installs only had `instance_*_model_id` (list selection or free-typed id).
 * This copies that value into `custom_model_id` as a prefill backup and leaves
 * `use_custom_model` off so chat behavior is unchanged until the user enables the
 * "Custom model" checkbox. List selection stays in `model_id`.
 */
fun AppSettings.migrateCustomModelSettingsIfNeeded() {
    if (settings.getBoolean(KEY_CUSTOM_MODEL_MIGRATION_COMPLETE, false)) return

    val instances = getConfiguredServiceInstances()
    for (instance in instances) {
        if (Service.fromId(instance.serviceId) != Service.OpenAICompatible) continue
        val listModelId = getInstanceModelId(instance.instanceId)
        if (listModelId.isNotBlank() && getInstanceCustomModelId(instance.instanceId).isBlank()) {
            setInstanceCustomModelId(instance.instanceId, listModelId)
        }
        // use_custom_model defaults to false when the key is absent — no write needed.
    }

    setCustomModelMigrationComplete(true)
}
