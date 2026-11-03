package com.inspiredandroid.kai.tools

import com.inspiredandroid.kai.network.tools.Tool
import org.koin.java.KoinJavaComponent.inject

/**
 * Android implementation of the downloads status tool.
 * This is registered in Platform.android.kt as part of the tool set.
 */
class DownloadsStatusToolImpl : Tool {
    private val downloadsPermissionController: DownloadsPermissionController by inject(DownloadsPermissionController::class.java)

    override val schema = DownloadsStatusTool.schema

    override suspend fun execute(args: Map<String, Any>): Any {
        val accessState = downloadsPermissionController.getDownloadsAccessState()

        val sourcesInfo = accessState.sources

        return mapOf(
            "success" to true,
            "overallCoverage" to accessState.overallCoverage.displayName,
            "sources" to sourcesInfo.map { mapOf("name" to it.name, "isAvailable" to it.isAvailable, "coverage" to it.coverage.name) },
            "hasLegacyPermission" to accessState.hasLegacyPermission,
            "canQueryDownloadManager" to downloadsPermissionController.canQueryDownloadManager(),
            "canQueryMediaStore" to downloadsPermissionController.canQueryMediaStoreDownloads(),
            "isSafAvailable" to downloadsPermissionController.isSafAvailable(),
            "message" to downloadsPermissionController.getAccessSummary(),
            "messageBengali" to downloadsPermissionController.getAccessSummaryBengali(),
            "details" to mapOf(
                "source_details" to accessState.sources.map { source ->
                    mapOf(
                        "name" to source.name,
                        "isAvailable" to source.isAvailable,
                        "coverage" to source.coverage.name,
                        "limitations" to source.limitations,
                    )
                },
                "coverage_explanation" to mapOf(
                    "FULL" to "All file types accessible via legacy permission + MediaStore",
                    "MEDIA_PARTIAL" to "Media files via MediaStore; non-media files not visible",
                    "PARTIAL" to "Only DownloadManager entries visible; limited coverage",
                    "MEDIA_ONLY" to "Only media files (images, videos, audio) in Downloads",
                    "USER_SELECTED" to "Files/folders explicitly selected by user via SAF picker",
                ),
            ),
        )
    }
}
