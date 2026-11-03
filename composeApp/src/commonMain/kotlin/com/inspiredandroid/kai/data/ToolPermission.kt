package com.inspiredandroid.kai.data

import kotlinx.serialization.Serializable

@Serializable
data class ToolPermission(
    val toolName: String,
    val scope: String? = null, // e.g., domain or URL
    val policy: ToolPermissionPolicy = ToolPermissionPolicy.ASK_ALWAYS,
)

enum class ToolPermissionPolicy {
    ASK_ALWAYS,
    ALLOW_ONCE,
    ALLOW_SESSION,
    ALWAYS_ALLOW,
}

enum class ToolRiskLevel {
    READ_ONLY,
    EXTERNAL_REQUEST,
    DATA_SCRAPING,
    FILE_WRITE,
    FILE_DELETE,
    SYSTEM_COMMAND,
    OTHER,
}
