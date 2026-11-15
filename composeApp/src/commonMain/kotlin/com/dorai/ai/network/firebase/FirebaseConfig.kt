package com.dorai.ai.network.firebase

data class FirebaseConfig(
    val projectId: String,
    val apiKey: String,
    val appId: String? = null,
) {
    val isValid: Boolean get() = projectId.isNotBlank() && apiKey.isNotBlank()
    val cleanProjectId: String get() = projectId.trim()
}
