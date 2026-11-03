package com.inspiredandroid.kai.tools

import com.inspiredandroid.kai.data.DownloadsItem
import kotlinx.coroutines.flow.Flow

/**
 * Downloads repository interface - platform-specific implementation via expect/actual pattern.
 * Aggregates data from DownloadManager, MediaStore, SAF.
 */
interface DownloadsRepository {

    /**
     * Lists download items with filters and pagination.
     *
     * @param source Data source (DOWNLOAD_MANAGER, MEDIA_STORE, SAF, ALL)
     * @param query Optional text search (filename/display name)
     * @param mimeType MIME type filter (e.g. image/jpeg, video/mp4, application/pdf)
     * @param dateFrom Earliest date (epoch ms)
     * @param dateTo Latest date (epoch ms)
     * @param status DownloadManager status filter
     * @param sortBy Sort order
     * @param limit Max results (default 50, max 200)
     * @param offset Pagination offset (default 0)
     * @return Flow of download items
     */
    fun list(
        source: DownloadsSource? = null,
        query: String? = null,
        mimeType: String? = null,
        dateFrom: Long? = null,
        dateTo: Long? = null,
        status: Int? = null,
        sortBy: DownloadsSortBy = DownloadsSortBy.NEWEST,
        limit: Int = 50,
        offset: Int = 0,
    ): Flow<List<DownloadsItem>>

    /**
     * Returns total count of matching items (for pagination metadata).
     */
    fun count(
        source: DownloadsSource? = null,
        query: String? = null,
        mimeType: String? = null,
        dateFrom: Long? = null,
        dateTo: Long? = null,
        status: Int? = null,
    ): Int

    /**
     * Find a single download item by ID.
     */
    fun getById(id: Long, source: DownloadsSource): DownloadsItem?

    /**
     * Check if a content URI is accessible.
     */
    fun canAccessMediaUri(uri: String): Boolean

    /**
     * SAF-based user-selected directory access.
     * Returns content URI.
     */
    suspend fun pickDirectory(): String?

    /**
     * Take persistable URI permission for SAF URI.
     */
    suspend fun takePersistableUriPermission(uri: String): Boolean

    /**
     * Returns downloads coverage state for reporting.
     */
    fun getCoverageState(): DownloadsCoverageState
}

/**
 * Download source types.
 */
enum class DownloadsSource {
    DOWNLOAD_MANAGER,
    MEDIA_STORE,
    SAF,
    ALL,
}

/**
 * Download sort options.
 */
enum class DownloadsSortBy {
    NEWEST, // Newest first (date desc)
    OLDEST, // Oldest first (date asc)
    LARGEST, // Largest file (size desc)
    SMALLEST, // Smallest file (size asc)
    NAME_A_TO_Z, // Name A-Z
    NAME_Z_TO_A, // Name Z-A
}

/**
 * Downloads coverage state.
 */
data class DownloadsCoverageState(
    val overallCoverage: DownloadsCoverage,
    val sources: List<DownloadsSourceInfo>,
    val hasLegacyPermission: Boolean,
    val canQueryDownloadManager: Boolean,
    val canQueryMediaStore: Boolean,
    val isSafAvailable: Boolean,
)

/**
 * Single download source info.
 */
data class DownloadsSourceInfo(
    val name: String,
    val isAvailable: Boolean,
    val coverage: DownloadsCoverage,
    val limitations: List<String>,
)

/**
 * Coverage level.
 */
enum class DownloadsCoverage(
    val displayName: String,
    val displayNameBengali: String,
) {
    FULL("Full access", "পূর্ণ অ্যাক্সেস"),
    MEDIA_ONLY("Media only", "শুধু মিডিয়া"),
    PARTIAL("Partial", "আংশিক"),
    USER_SELECTED("User-selected", "ব্যবহারকারী-নির্বাচিত"),
    MEDIA_PARTIAL("Media (partial)", "মিডিয়া (আংশিক)"),
}
