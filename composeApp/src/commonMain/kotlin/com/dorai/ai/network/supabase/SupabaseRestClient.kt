package com.dorai.ai.network.supabase

import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess

class SupabaseRestClient(
    private val httpClient: HttpClient,
) {
    suspend fun select(
        config: SupabaseConfig,
        table: String,
        queryParameters: Map<String, String> = emptyMap(),
    ): Result<String> = runCatching {
        require(config.isValid) { "Invalid Supabase configuration." }

        val queryString = if (queryParameters.isNotEmpty()) {
            "?" + queryParameters.map { "${it.key}=${it.value}" }.joinToString("&")
        } else {
            ""
        }
        val url = "${config.cleanUrl}/rest/v1/$table$queryString"

        val response = httpClient.get(url) {
            header("apikey", config.anonKey)
            header("Authorization", "Bearer ${config.userToken ?: config.anonKey}")
            contentType(ContentType.Application.Json)
        }

        val body = response.bodyAsText()
        if (!response.status.isSuccess()) {
            throw IllegalStateException("Supabase select failed [HTTP ${response.status.value}]: $body")
        }
        body
    }

    suspend fun insertOrUpdate(
        config: SupabaseConfig,
        table: String,
        jsonBody: String,
        upsert: Boolean = true,
    ): Result<String> = runCatching {
        require(config.isValid) { "Invalid Supabase configuration." }

        val url = "${config.cleanUrl}/rest/v1/$table"
        val response = httpClient.post(url) {
            header("apikey", config.anonKey)
            header("Authorization", "Bearer ${config.userToken ?: config.anonKey}")
            contentType(ContentType.Application.Json)
            if (upsert) {
                header("Prefer", "resolution=merge-duplicates,return=representation")
            } else {
                header("Prefer", "return=representation")
            }
            setBody(jsonBody)
        }

        val body = response.bodyAsText()
        if (!response.status.isSuccess()) {
            throw IllegalStateException("Supabase upsert failed [HTTP ${response.status.value}]: $body")
        }
        body
    }

    suspend fun delete(
        config: SupabaseConfig,
        table: String,
        filterColumn: String,
        filterValue: String,
    ): Result<Unit> = runCatching {
        require(config.isValid) { "Invalid Supabase configuration." }

        val url = "${config.cleanUrl}/rest/v1/$table?$filterColumn=eq.$filterValue"
        val response = httpClient.delete(url) {
            header("apikey", config.anonKey)
            header("Authorization", "Bearer ${config.userToken ?: config.anonKey}")
        }

        if (!response.status.isSuccess()) {
            val body = response.bodyAsText()
            throw IllegalStateException("Supabase delete failed [HTTP ${response.status.value}]: $body")
        }
    }

    suspend fun rpc(
        config: SupabaseConfig,
        functionName: String,
        jsonParams: String,
    ): Result<String> = runCatching {
        require(config.isValid) { "Invalid Supabase configuration." }

        val url = "${config.cleanUrl}/rest/v1/rpc/$functionName"
        val response = httpClient.post(url) {
            header("apikey", config.anonKey)
            header("Authorization", "Bearer ${config.userToken ?: config.anonKey}")
            contentType(ContentType.Application.Json)
            setBody(jsonParams)
        }

        val body = response.bodyAsText()
        if (!response.status.isSuccess()) {
            throw IllegalStateException("Supabase RPC '$functionName' failed [HTTP ${response.status.value}]: $body")
        }
        body
    }
}
