package com.inspiredandroid.kai.tools

import com.inspiredandroid.kai.network.tools.ParameterSchema
import com.inspiredandroid.kai.network.tools.Tool
import com.inspiredandroid.kai.network.tools.ToolInfo
import com.inspiredandroid.kai.network.tools.ToolSchema
import kai.composeapp.generated.resources.Res
import kai.composeapp.generated.resources.tool_gallery_status_description
import kai.composeapp.generated.resources.tool_gallery_status_name

/**
 * Tool to report the current gallery/media access permission status and access scope.
 * Returns detailed information about what level of gallery access is available.
 */
object GalleryStatusTool : Tool {

    override val schema = ToolSchema(
        name = "gallery_status",
        description = "Check the current gallery/media access permission status. Returns the access level (full, selected photos only, denied) and human-readable explanations in English and Bengali.",
        parameters = emptyMap(),
    )

    override suspend fun execute(args: Map<String, Any>): Any = try {
        // This will be implemented platform-specifically via the actual tool registration
        // The actual implementation is in GalleryStatusToolImpl (Android)
        mapOf(
            "success" to false,
            "error" to "This tool must be implemented platform-specifically",
        )
    } catch (e: Exception) {
        mapOf(
            "success" to false,
            "error" to "Failed to get gallery status: ${e.message}",
        )
    }

    val toolInfo = ToolInfo(
        id = "gallery_status",
        name = "Gallery Status",
        description = "Check gallery/media access permission status and scope",
        nameRes = Res.string.tool_gallery_status_name,
        descriptionRes = Res.string.tool_gallery_status_description,
    )
}

/**
 * Result of the gallery status check.
 */
data class GalleryStatusResult(
    val permissionState: String, // GRANTED, DENIED, SELECTED_PHOTOS_ONLY, PARTIAL, REVOKED
    val accessScope: String, // FULL, SELECTED_PHOTOS_ONLY, NONE
    val androidVersion: Int,
    val canQueryMediaStore: Boolean,
    val hasSelectedPhotosAccess: Boolean,
    val message: String, // English description
    val messageBengali: String, // Bengali description
    val details: Map<String, Any> = emptyMap(),
)
