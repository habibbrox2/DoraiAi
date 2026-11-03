package com.inspiredandroid.kai.tools

import com.inspiredandroid.kai.network.tools.ParameterSchema
import com.inspiredandroid.kai.network.tools.Tool
import com.inspiredandroid.kai.network.tools.ToolInfo
import com.inspiredandroid.kai.network.tools.ToolSchema
import kai.composeapp.generated.resources.Res
import kai.composeapp.generated.resources.tool_downloads_status_description
import kai.composeapp.generated.resources.tool_downloads_status_name

/**
 * Tool to report the current Downloads access status, available sources, and coverage limitations.
 * Honestly reports what download sources are available and their coverage gaps.
 */
object DownloadsStatusTool : Tool {

    override val schema = ToolSchema(
        name = "downloads_status",
        description = "Check the current Downloads access status. Reports available sources (DownloadManager, MediaStore, SAF), their coverage limitations, and overall access level. Does NOT claim full visibility unless actually available.",
        parameters = emptyMap(),
    )

    override suspend fun execute(args: Map<String, Any>): Any = try {
        // This will be implemented platform-specifically via the actual tool registration
        // The actual implementation is in DownloadsStatusToolImpl (Android)
        mapOf(
            "success" to false,
            "error" to "This tool must be implemented platform-specifically",
        )
    } catch (e: Exception) {
        mapOf(
            "success" to false,
            "error" to "Failed to get downloads status: ${e.message}",
        )
    }

    val toolInfo = ToolInfo(
        id = "downloads_status",
        name = "Downloads Status",
        description = "Check Downloads access status, available sources, and coverage limitations",
        nameRes = Res.string.tool_downloads_status_name,
        descriptionRes = Res.string.tool_downloads_status_description,
    )
}

/**
 * Result of the downloads status check.
 */
data class DownloadsStatusResult(
    val overallCoverage: String, // FULL, MEDIA_ONLY, PARTIAL, MEDIA_PARTIAL
    val sources: List<DownloadsSourceInfo>,
    val hasLegacyPermission: Boolean,
    val canQueryDownloadManager: Boolean,
    val canQueryMediaStore: Boolean,
    val isSafAvailable: Boolean,
    val message: String, // English summary
    val messageBengali: String, // Bengali summary
    val details: Map<String, Any> = emptyMap(),
)
