package com.inspiredandroid.kai.tools

import com.inspiredandroid.kai.data.GalleryMediaItem
import com.inspiredandroid.kai.network.tools.Tool
import com.inspiredandroid.kai.permissionQualifier
import kotlinx.coroutines.flow.first
import org.koin.java.KoinJavaComponent
import org.koin.java.KoinJavaComponent.inject

/**
 * GallerySearchTool-এর অ্যান্ড্রয়েড ইমপ্লিমেন্টেশন।
 * GalleryRepository ব্যবহার করে সার্চ এক্সিকিউট করে।
 */
class GallerySearchToolImpl : Tool {
    private val galleryRepository: GalleryRepository by inject(GalleryRepository::class.java)
    private val galleryPermissionController: GalleryPermissionController by inject(GalleryPermissionController::class.java)
    private val permissionController: PermissionController =
        KoinJavaComponent.get(PermissionController::class.java, permissionQualifier(AppPermission.GALLERY))

    override val schema = GallerySearchTool.schema

    override suspend fun execute(args: Map<String, Any>): Any = try {
        if (!galleryPermissionController.canQueryMediaStore()) {
            val granted = permissionController.requestPermission()
            if (!granted) {
                return mapOf(
                    "success" to false,
                    "error" to "Gallery permission not granted. Please allow photo and media access.",
                    "messageBengali" to "গ্যালারির অনুমতি প্রদান করা হয়নি। দয়া করে ছবির অনুমতি দিন।",
                )
            }
        }
        val query = args["query"] as? String
        val mimeType = args["mimeType"] as? String
        val dateFrom = (args["dateFrom"] as? Number)?.toLong()
        val dateTo = (args["dateTo"] as? Number)?.toLong()
        val minWidth = (args["minWidth"] as? Number)?.toInt()
        val minHeight = (args["minHeight"] as? Number)?.toInt()
        val sortBy = when (args["sortBy"] as? String) {
            "OLDEST" -> GallerySortBy.OLDEST
            "LARGEST" -> GallerySortBy.LARGEST
            "SMALLEST" -> GallerySortBy.SMALLEST
            "NAME_A_TO_Z" -> GallerySortBy.NAME_A_TO_Z
            "NAME_Z_TO_A" -> GallerySortBy.NAME_Z_TO_A
            else -> GallerySortBy.NEWEST
        }
        val limit = ((args["limit"] as? Number)?.toInt() ?: (args["limit"] as? String)?.toIntOrNull() ?: 50).coerceIn(1, 200)
        val offset = ((args["offset"] as? Number)?.toInt() ?: (args["offset"] as? String)?.toIntOrNull() ?: 0).coerceAtLeast(0)

        val items = galleryRepository.search(
            query = query,
            mimeType = mimeType,
            dateFrom = dateFrom,
            dateTo = dateTo,
            minWidth = minWidth,
            minHeight = minHeight,
            sortBy = sortBy,
            limit = limit,
            offset = offset,
        ).first()

        val totalCount = galleryRepository.count(
            query = query,
            mimeType = mimeType,
            dateFrom = dateFrom,
            dateTo = dateTo,
            minWidth = minWidth,
            minHeight = minHeight,
        )

        val hasMore = offset + items.size < totalCount

        mapOf(
            "success" to true,
            "items" to items.map { it.toMap() },
            "totalCount" to totalCount,
            "limit" to limit,
            "offset" to offset,
            "hasMore" to hasMore,
            "message" to if (items.isEmpty()) "No media found matching criteria" else "Found ${items.size} of $totalCount items",
            "messageBengali" to if (items.isEmpty()) "প্রাসঙ্গিক মিডিয়া পাওয়া যায়নি" else "$totalCount টির মধ্যে ${items.size} টি ফলাফল পাওয়া গেছে",
        )
    } catch (e: Exception) {
        mapOf(
            "success" to false,
            "error" to "Gallery search error: ${e.message}",
            "messageBengali" to "গ্যালারি অনুসন্ধানে সমস্যা হয়েছে: ${e.message}",
        )
    }
}

/**
 * GalleryMediaItem-কে ম্যাপে রূপান্তর করে (JSON সিরিয়ালাইজেশনের জন্য)।
 */
private fun GalleryMediaItem.toMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "contentUri" to contentUri,
    "displayName" to displayName,
    "mimeType" to mimeType,
    "width" to width,
    "height" to height,
    "sizeBytes" to sizeBytes,
    "humanReadableSize" to humanReadableSize(),
    "dateAdded" to dateAdded,
    "dateModified" to dateModified,
    "dateTaken" to dateTaken,
    "relativePath" to relativePath,
    "isPending" to isPending,
    "isFavorite" to isFavorite,
    "isImage" to isImage,
    "isVideo" to isVideo,
    "resolution" to resolutionString,
)
