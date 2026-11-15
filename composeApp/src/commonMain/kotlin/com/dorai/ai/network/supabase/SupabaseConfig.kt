package com.dorai.ai.network.supabase

data class SupabaseConfig(
    val url: String,
    val anonKey: String,
    val userToken: String? = null,
) {
    val isValid: Boolean get() = url.isNotBlank() && url.startsWith("https://") && anonKey.isNotBlank()
    val cleanUrl: String get() = url.trim().removeSuffix("/")
}
