package com.inspiredandroid.kai.data

import com.inspiredandroid.kai.network.ApiException
import com.inspiredandroid.kai.network.ServiceCredentials
import com.inspiredandroid.kai.ui.chat.History
import kotlinx.coroutines.CancellationException

internal data class KeylessFallbackTarget(
    val service: Service,
    val modelId: String,
)

internal object KeylessFreeTierFallback {
    const val AUTO_MODEL_ID = "auto"
    const val DISPLAY_NAME = "Auto"

    val targets: List<KeylessFallbackTarget> = buildList {
        add(KeylessFallbackTarget(Service.Kilo, "kilo-auto/free"))
        Service.OpenCode.defaultModels
            .filter { it.subtitle.contains("free", ignoreCase = true) }
            .forEach { add(KeylessFallbackTarget(Service.OpenCode, it.id)) }
    }

    fun isInternalService(service: Service): Boolean = service == Service.Kilo || service == Service.OpenCode

    fun credentialsFor(target: KeylessFallbackTarget): ServiceCredentials = ServiceCredentials(
        apiKey = "",
        modelId = target.modelId,
    )

    fun canContinueAfterFailure(before: List<History>, after: List<History>): Boolean {
        val priorIds = before.mapTo(mutableSetOf()) { it.id }
        return after.none { entry ->
            entry.id !in priorIds &&
                (entry.role == History.Role.TOOL || !entry.toolCalls.isNullOrEmpty())
        }
    }

    suspend fun <T> execute(
        canContinueAfterFailure: (Exception) -> Boolean = { true },
        onFailure: (KeylessFallbackTarget, Exception, KeylessFallbackTarget?) -> Unit = { _, _, _ -> },
        attempt: suspend (KeylessFallbackTarget) -> T,
    ): T {
        var lastFailure: Exception? = null
        for ((index, target) in targets.withIndex()) {
            try {
                return attempt(target)
            } catch (failure: Exception) {
                if (failure is CancellationException) throw failure
                lastFailure = failure
                val nextTarget = targets.getOrNull(index + 1)
                onFailure(target, failure, nextTarget)
                if (nextTarget == null || !canContinueAfterFailure(failure)) {
                    throw failure
                }
            }
        }
        throw lastFailure ?: IllegalStateException("No keyless free model targets are available")
    }
}




