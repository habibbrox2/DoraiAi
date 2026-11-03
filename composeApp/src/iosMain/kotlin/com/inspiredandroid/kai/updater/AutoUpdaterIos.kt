package com.inspiredandroid.kai.updater

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText

/**
 * iOS implementation of [httpGet] using ktor (darwin engine).
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
 * iOS updates go through the App Store; auto-update is not applicable.
 */
internal actual fun currentPlatformKey(): String = "none"
