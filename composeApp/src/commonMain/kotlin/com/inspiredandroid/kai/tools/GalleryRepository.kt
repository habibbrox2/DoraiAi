package com.inspiredandroid.kai.tools

import com.inspiredandroid.kai.data.GalleryMediaItem
import kotlinx.coroutines.flow.Flow

/**
 * Gallery Repository interface - platform-specific implementation via expect/actual pattern.
 * Handles MediaStore queries, filtering, pagination.
 */
interface GalleryRepository {

    /**
     * Search gallery media items with filters and pagination.
     *
     * @param query Optional text search (filename/display name)
     * @param mimeType MIME type filter (e.g. image/jpeg, video/mp4, or specific MIME type)
     * @param dateFrom Earliest DATE_ADDED (epoch ms) - items added after this
     * @param dateTo Latest DATE_ADDED (epoch ms) - items added before this
     * @param minWidth Minimum width (pixels)
     * @param minHeight Minimum height (pixels)
     * @param sortBy Sort order: NEWEST, OLDEST, LARGEST, SMALLEST, NAME_A_TO_Z, NAME_Z_TO_A
     * @param limit Max results (default 50, max 200)
     * @param offset Pagination offset (default 0)
     * @return Flow of media items
     */
    fun search(
        query: String? = null,
        mimeType: String? = null,
        dateFrom: Long? = null,
        dateTo: Long? = null,
        minWidth: Int? = null,
        minHeight: Int? = null,
        sortBy: GallerySortBy = GallerySortBy.NEWEST,
        limit: Int = 50,
        offset: Int = 0,
    ): Flow<List<GalleryMediaItem>>

    /**
     * Returns total count of matching items (for pagination metadata).
     */
    fun count(
        query: String? = null,
        mimeType: String? = null,
        dateFrom: Long? = null,
        dateTo: Long? = null,
        minWidth: Int? = null,
        minHeight: Int? = null,
    ): Int

    /**
     * Find a single media item by ID.
     */
    fun getById(id: Long): GalleryMediaItem?

    /**
     * Check if a content URI is accessible (handles deleted/inaccessible files).
     */
    fun canAccessMediaUri(uri: String): Boolean

    /**
     * Rename a media file/item.
     */
    suspend fun rename(id: Long, newName: String): Boolean = false

    /**
     * Delete a media file/item. Requires permission.
     */
    suspend fun delete(id: Long): Boolean = false

    /**
     * Move a media file/item to a different relative path/location.
     */
    suspend fun move(id: Long, newRelativePath: String): Boolean = false

    /**
     * Copy a media file/item.
     */
    suspend fun copy(id: Long, newName: String? = null): Boolean = false
}

/**
 * Gallery sort options.
 */
enum class GallerySortBy {
    NEWEST, // Newest first (DATE_ADDED desc)
    OLDEST, // Oldest first (DATE_ADDED asc)
    LARGEST, // Largest file (SIZE desc)
    SMALLEST, // Smallest file (SIZE asc)
    NAME_A_TO_Z, // Name A-Z
    NAME_Z_TO_A, // Name Z-A
}
