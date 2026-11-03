package com.inspiredandroid.kai.tools

import android.app.DownloadManager
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import com.inspiredandroid.kai.data.DownloadsItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Android DownloadsRepository ইমপ্লিমেন্টেশন —
 * DownloadManager, MediaStore, SAF থেকে ডাউনলোড ডেটা একীভূত করে।
 */
class DownloadsRepositoryImpl(
    private val context: Context,
    private val downloadsPermissionController: DownloadsPermissionController,
) : DownloadsRepository {

    private val queryMutex = Mutex()
    private val contentResolver: ContentResolver = context.contentResolver
    private val downloadManager: DownloadManager =
        context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    private val baseUri: Uri = MediaStore.Files.getContentUri("external")

    private val mediaStoreProjection = buildList {
        add(MediaStore.Files.FileColumns._ID)
        add(MediaStore.Files.FileColumns.DISPLAY_NAME)
        add(MediaStore.Files.FileColumns.MIME_TYPE)
        add(MediaStore.Files.FileColumns.SIZE)
        add(MediaStore.Files.FileColumns.DATE_ADDED)
        add(MediaStore.Files.FileColumns.DATE_MODIFIED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            add(MediaStore.Files.FileColumns.RELATIVE_PATH)
            add(MediaStore.Files.FileColumns.IS_PENDING)
        }
    }.toTypedArray()

    // ============ DownloadsRepository ইন্টারফেস ইমপ্লিমেন্টেশন ============

    override fun list(
        source: DownloadsSource?,
        query: String?,
        mimeType: String?,
        dateFrom: Long?,
        dateTo: Long?,
        status: Int?,
        sortBy: DownloadsSortBy,
        limit: Int,
        offset: Int,
    ): Flow<List<DownloadsItem>> = flow {
        val items = withContext(Dispatchers.IO) {
            queryMutex.withLock {
                val sourceFilter = source ?: DownloadsSource.ALL
                val allItems = mutableListOf<DownloadsItem>()

                if (sourceFilter == DownloadsSource.ALL || sourceFilter == DownloadsSource.DOWNLOAD_MANAGER) {
                    allItems.addAll(queryDownloadManager(query, mimeType, status))
                }
                if (sourceFilter == DownloadsSource.ALL || sourceFilter == DownloadsSource.MEDIA_STORE) {
                    if (canQueryMediaStore()) {
                        allItems.addAll(queryMediaStore(query, mimeType, dateFrom, dateTo))
                    }
                }

                val sorted = when (sortBy) {
                    DownloadsSortBy.NEWEST -> allItems.sortedByDescending { it.dateAdded }
                    DownloadsSortBy.OLDEST -> allItems.sortedBy { it.dateAdded }
                    DownloadsSortBy.LARGEST -> allItems.sortedByDescending { it.sizeBytes }
                    DownloadsSortBy.SMALLEST -> allItems.sortedBy { it.sizeBytes }
                    DownloadsSortBy.NAME_A_TO_Z -> allItems.sortedBy { it.displayName.lowercase() }
                    DownloadsSortBy.NAME_Z_TO_A -> allItems.sortedByDescending { it.displayName.lowercase() }
                }

                val paged = sorted.drop(offset.coerceAtLeast(0))
                if (limit > 0) paged.take(limit) else paged
            }
        }
        emit(items)
    }

    override fun count(
        source: DownloadsSource?,
        query: String?,
        mimeType: String?,
        dateFrom: Long?,
        dateTo: Long?,
        status: Int?,
    ): Int = runBlocking(Dispatchers.IO) {
        queryMutex.withLock {
            val sourceFilter = source ?: DownloadsSource.ALL
            var total = 0

            if (sourceFilter == DownloadsSource.ALL || sourceFilter == DownloadsSource.DOWNLOAD_MANAGER) {
                total += queryDownloadManager(query, mimeType, status).size
            }
            if (sourceFilter == DownloadsSource.ALL || sourceFilter == DownloadsSource.MEDIA_STORE) {
                if (canQueryMediaStore()) {
                    val (selection, args) = buildMediaStoreSelection(query, mimeType, dateFrom, dateTo)
                    contentResolver.query(
                        baseUri,
                        arrayOf(MediaStore.Files.FileColumns._ID),
                        selection,
                        args,
                        null,
                    )?.use { cursor ->
                        total += cursor.count
                    }
                }
            }
            total
        }
    }

    override fun getById(id: Long, source: DownloadsSource): DownloadsItem? = runBlocking(Dispatchers.IO) {
        when (source) {
            DownloadsSource.DOWNLOAD_MANAGER -> getDownloadManagerById(id)
            DownloadsSource.MEDIA_STORE -> getMediaStoreById(id)
            else -> null
        }
    }

    override fun canAccessMediaUri(uri: String): Boolean = runBlocking(Dispatchers.IO) {
        try {
            contentResolver.openFileDescriptor(Uri.parse(uri), "r")?.use { it.close() }
            true
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun pickDirectory(): String? = withContext(Dispatchers.IO) {
        // SAF directory picker needs an Activity context; wired up when the UI ships the picker.
        null
    }

    override suspend fun takePersistableUriPermission(uri: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            contentResolver.takePersistableUriPermission(Uri.parse(uri), flags)
            true
        } catch (_: Exception) {
            false
        }
    }

    override fun getCoverageState(): DownloadsCoverageState = runBlocking(Dispatchers.IO) {
        val mediaStoreAvailable = canQueryMediaStore()
        val legacyPermission = testLegacyStoragePermission()

        val sources = mutableListOf<DownloadsSourceInfo>()

        sources += DownloadsSourceInfo(
            name = "DownloadManager",
            isAvailable = true,
            coverage = DownloadsCoverage.PARTIAL,
            limitations = listOf(
                "Only downloads initiated via DownloadManager API",
                "Does not show files copied via file manager, USB, or other apps",
            ),
        )

        if (mediaStoreAvailable) {
            sources += DownloadsSourceInfo(
                name = "MediaStore",
                isAvailable = true,
                coverage = DownloadsCoverage.MEDIA_ONLY,
                limitations = listOf(
                    "Only media files (images, videos, audio)",
                    "Does not show PDF, APK, ZIP, or other non-media files",
                ),
            )
        }

        sources += DownloadsSourceInfo(
            name = "Storage Access Framework (SAF)",
            isAvailable = true,
            coverage = DownloadsCoverage.USER_SELECTED,
            limitations = listOf(
                "Requires user to select files/folders via picker",
                "Access is per-URI and can be revoked by user",
            ),
        )

        val overallCoverage = when {
            legacyPermission && mediaStoreAvailable -> DownloadsCoverage.FULL
            mediaStoreAvailable -> DownloadsCoverage.MEDIA_PARTIAL
            else -> DownloadsCoverage.PARTIAL
        }

        DownloadsCoverageState(
            overallCoverage = overallCoverage,
            sources = sources,
            hasLegacyPermission = legacyPermission,
            canQueryDownloadManager = true,
            canQueryMediaStore = mediaStoreAvailable,
            isSafAvailable = true,
        )
    }

    // ============ প্রাইভেট হেল্পার ============

    private fun queryDownloadManager(
        query: String?,
        mimeType: String?,
        status: Int?,
    ): List<DownloadsItem> {
        val dmQuery = DownloadManager.Query()
        status?.let { dmQuery.setFilterByStatus(it) }

        val items = mutableListOf<DownloadsItem>()
        try {
            downloadManager.query(dmQuery)?.use { cursor ->
                while (cursor.moveToNext()) {
                    val item = downloadManagerItem(cursor) ?: continue
                    if (query != null && !item.displayName.contains(query, ignoreCase = true)) continue
                    if (mimeType != null && !matchesMimeType(item.mimeType, mimeType)) continue
                    items.add(item)
                }
            }
        } catch (_: Exception) {
            // DownloadManager unavailable — return what we have
        }
        return items
    }

    private fun queryMediaStore(
        query: String?,
        mimeType: String?,
        dateFrom: Long?,
        dateTo: Long?,
    ): List<DownloadsItem> {
        val (selection, args) = buildMediaStoreSelection(query, mimeType, dateFrom, dateTo)
        val items = mutableListOf<DownloadsItem>()
        try {
            contentResolver.query(
                baseUri,
                mediaStoreProjection,
                selection,
                args,
                "${MediaStore.Files.FileColumns.DATE_ADDED} DESC",
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    items.add(mediaStoreItem(cursor))
                }
            }
        } catch (_: Exception) {
            // MediaStore unavailable — return what we have
        }
        return items
    }

    private fun buildMediaStoreSelection(
        query: String?,
        mimeType: String?,
        dateFrom: Long?,
        dateTo: Long?,
    ): Pair<String?, Array<String>?> {
        val clauses = mutableListOf<String>()
        val args = mutableListOf<String>()

        query?.let {
            clauses.add("${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE ?")
            args.add("%$it%")
        }

        mimeType?.let {
            when (it) {
                "image/*" -> clauses.add("${MediaStore.Files.FileColumns.MIME_TYPE} LIKE 'image/%'")

                "video/*" -> clauses.add("${MediaStore.Files.FileColumns.MIME_TYPE} LIKE 'video/%'")

                "application/*" -> clauses.add("${MediaStore.Files.FileColumns.MIME_TYPE} LIKE 'application/%'")

                else -> {
                    clauses.add("${MediaStore.Files.FileColumns.MIME_TYPE} = ?")
                    args.add(it)
                }
            }
        }

        dateFrom?.let {
            clauses.add("${MediaStore.Files.FileColumns.DATE_ADDED} >= ?")
            args.add((it / 1000).toString())
        }
        dateTo?.let {
            clauses.add("${MediaStore.Files.FileColumns.DATE_ADDED} <= ?")
            args.add((it / 1000).toString())
        }

        val selection = if (clauses.isEmpty()) null else clauses.joinToString(" AND ")
        val selectionArgs = if (args.isEmpty()) null else args.toTypedArray()
        return selection to selectionArgs
    }

    private fun matchesMimeType(actual: String, filter: String): Boolean = when (filter) {
        "image/*" -> actual.startsWith("image/")
        "video/*" -> actual.startsWith("video/")
        "application/*" -> actual.startsWith("application/")
        else -> actual == filter
    }

    private fun downloadManagerItem(cursor: Cursor): DownloadsItem? {
        val id = getLongOrNull(cursor, DownloadManager.COLUMN_ID) ?: return null
        val localUri = getStringOrNull(cursor, DownloadManager.COLUMN_LOCAL_URI)
        return DownloadsItem(
            id = id,
            contentUri = localUri ?: "",
            displayName = getStringOrNull(cursor, DownloadManager.COLUMN_TITLE) ?: "",
            mimeType = mimeTypeForMediaType(getIntOrNull(cursor, DownloadManager.COLUMN_MEDIA_TYPE)),
            sizeBytes = getLongOrNull(cursor, DownloadManager.COLUMN_TOTAL_SIZE_BYTES) ?: 0L,
            dateAdded = getLongOrNull(cursor, DownloadManager.COLUMN_LAST_MODIFIED_TIMESTAMP) ?: 0L,
            dateModified = getLongOrNull(cursor, DownloadManager.COLUMN_LAST_MODIFIED_TIMESTAMP) ?: 0L,
            source = DownloadsItem.DownloadsSource.DOWNLOAD_MANAGER,
            relativePath = null,
            description = getStringOrNull(cursor, DownloadManager.COLUMN_DESCRIPTION),
            mediaType = getIntOrNull(cursor, DownloadManager.COLUMN_MEDIA_TYPE),
            destinationUri = localUri,
            isPending = false,
            status = getIntOrNull(cursor, DownloadManager.COLUMN_STATUS),
            reason = getIntOrNull(cursor, DownloadManager.COLUMN_REASON),
        )
    }

    private fun mediaStoreItem(cursor: Cursor): DownloadsItem {
        val id = getLongOrNull(cursor, MediaStore.Files.FileColumns._ID) ?: 0L
        return DownloadsItem(
            id = id,
            contentUri = ContentUris.withAppendedId(baseUri, id).toString(),
            displayName = getStringOrNull(cursor, MediaStore.Files.FileColumns.DISPLAY_NAME) ?: "",
            mimeType = getStringOrNull(cursor, MediaStore.Files.FileColumns.MIME_TYPE) ?: "",
            sizeBytes = getLongOrNull(cursor, MediaStore.Files.FileColumns.SIZE) ?: 0L,
            dateAdded = (getLongOrNull(cursor, MediaStore.Files.FileColumns.DATE_ADDED) ?: 0L) * 1000L,
            dateModified = (getLongOrNull(cursor, MediaStore.Files.FileColumns.DATE_MODIFIED) ?: 0L) * 1000L,
            source = DownloadsItem.DownloadsSource.MEDIA_STORE,
            relativePath = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                getStringOrNull(cursor, MediaStore.Files.FileColumns.RELATIVE_PATH)
            } else {
                null
            },
            description = null,
            mediaType = null,
            destinationUri = null,
            isPending = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                getIntOrNull(cursor, MediaStore.Files.FileColumns.IS_PENDING) == 1
            } else {
                false
            },
            status = null,
            reason = null,
        )
    }

    private fun getDownloadManagerById(id: Long): DownloadsItem? = try {
        downloadManager.query(DownloadManager.Query().setFilterById(id))?.use { cursor ->
            if (cursor.moveToFirst()) downloadManagerItem(cursor) else null
        }
    } catch (_: Exception) {
        null
    }

    private fun getMediaStoreById(id: Long): DownloadsItem? = try {
        contentResolver.query(
            ContentUris.withAppendedId(baseUri, id),
            mediaStoreProjection,
            null,
            null,
            null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) mediaStoreItem(cursor) else null
        }
    } catch (_: Exception) {
        null
    }

    // DownloadManager reports MEDIA_TYPE as an int (0=unknown, 1=image, 2=video, 3=audio).
    private fun mimeTypeForMediaType(mediaType: Int?): String = when (mediaType) {
        1 -> "image/*"
        2 -> "video/*"
        3 -> "audio/*"
        else -> ""
    }

    private fun getIntOrNull(cursor: Cursor, column: String): Int? {
        val index = cursor.getColumnIndex(column)
        return if (index >= 0 && !cursor.isNull(index)) cursor.getInt(index) else null
    }

    private fun getLongOrNull(cursor: Cursor, column: String): Long? {
        val index = cursor.getColumnIndex(column)
        return if (index >= 0 && !cursor.isNull(index)) cursor.getLong(index) else null
    }

    private fun getStringOrNull(cursor: Cursor, column: String): String? {
        val index = cursor.getColumnIndex(column)
        return if (index >= 0 && !cursor.isNull(index)) cursor.getString(index) else null
    }

    private fun canQueryMediaStore(): Boolean = downloadsPermissionController.canQueryMediaStoreDownloads()

    private fun testLegacyStoragePermission(): Boolean = if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
        ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_EXTERNAL_STORAGE) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
    } else {
        false
    }
}
