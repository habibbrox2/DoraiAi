package com.inspiredandroid.kai.updater

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText

/**
 * WasmJs implementation of [httpGet] using ktor.
 */
internal actual suspend fun httpGet(url: String, headers: Map<String, String>): String {
    val client = HttpClient { expectSuccess = true }
    try {
        return client.get(url) {
            headers.forEach { (key, value) -> header(key, value) }
        }.bodyAsText()
    } finally {
        client.close()
    }
}

/**
 * Auto-update is not applicable on web.
 */
internal actual fun currentPlatformKey(): String = "none"
