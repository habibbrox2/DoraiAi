package com.inspiredandroid.kai.tools

import com.inspiredandroid.kai.data.GalleryMediaItem
import com.inspiredandroid.kai.network.tools.ParameterSchema
import com.inspiredandroid.kai.network.tools.Tool
import com.inspiredandroid.kai.network.tools.ToolInfo
import com.inspiredandroid.kai.network.tools.ToolSchema
import kai.composeapp.generated.resources.Res
import kai.composeapp.generated.resources.tool_gallery_search_description
import kai.composeapp.generated.resources.tool_gallery_search_name
import kotlinx.coroutines.flow.Flow

/**
 * গ্যালারি মিডিয়া সার্চ টুল।
 * ফিল্টার, সর্টিং, পেজিনেশন সহ মিডিয়া আইটেম সার্চ করে।
 */
object GallerySearchTool : Tool {

    override val schema = ToolSchema(
        name = "gallery_search",
        description = "Search the user's gallery/media with filters: text query, MIME type, date range, dimensions, sort order, pagination.",
        parameters = mapOf(
            "query" to ParameterSchema("string", "Optional text search (filename/display name)", false),
            "mimeType" to ParameterSchema("string", "Filter: image/*, video/*, or specific (e.g. image/jpeg)", false),
            "dateFrom" to ParameterSchema("integer", "Earliest DATE_ADDED (epoch ms)", false),
            "dateTo" to ParameterSchema("integer", "Latest DATE_ADDED (epoch ms)", false),
            "minWidth" to ParameterSchema("integer", "Minimum width in pixels", false),
            "minHeight" to ParameterSchema("integer", "Minimum height in pixels", false),
            "sortBy" to ParameterSchema("string", "Sort order: NEWEST, OLDEST, LARGEST, SMALLEST, NAME_A_TO_Z, NAME_Z_TO_A", false),
            "limit" to ParameterSchema("integer", "Max results (default 50, max 200)", false),
            "offset" to ParameterSchema("integer", "Pagination offset (default 0)", false),
        ),
    )

    override suspend fun execute(args: Map<String, Any>): Any = try {
        // প্ল্যাটফর্ম-নির্দিষ্ট ইমপ্লিমেন্টেশন:androidMain-এ GallerySearchToolImpl
        mapOf(
            "success" to false,
            "error" to "This tool must be implemented platform-specifically",
        )
    } catch (e: Exception) {
        mapOf(
            "success" to false,
            "error" to "Failed to search gallery: ${e.message}",
        )
    }

    val toolInfo = ToolInfo(
        id = "gallery_search",
        name = "Gallery Search",
        description = "Search authorized gallery/media with filters and pagination",
        nameRes = Res.string.tool_gallery_search_name,
        descriptionRes = Res.string.tool_gallery_search_description,
    )
}

/**
 * গ্যালারি সার্চ রেজাল্ট।
 */
data class GallerySearchResult(
    val items: List<GalleryMediaItem>,
    val totalCount: Int,
    val limit: Int,
    val offset: Int,
    val hasMore: Boolean,
    val message: String = "",
    val messageBengali: String = "",
)
