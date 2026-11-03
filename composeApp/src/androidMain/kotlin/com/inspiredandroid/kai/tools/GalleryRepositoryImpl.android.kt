package com.inspiredandroid.kai.tools

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.inspiredandroid.kai.data.GalleryMediaItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.java.KoinJavaComponent.inject

/**
 * Android GalleryRepository ইমপ্লিমেন্টেশন — MediaStore ব্যবহার করে।
 * পেজিনেশন, ফিল্টারিং, সর্টিং, এবং লাইফসাইকেল-অویهার ফ্লো সরবরাহ করে।
 */
class GalleryRepositoryImpl(
    private val context: Context,
    private val galleryPermissionController: GalleryPermissionController,
) : GalleryRepository {

    private val queryMutex = Mutex()

    private val contentResolver: ContentResolver = context.contentResolver

    /** বেস URI — ইমেজ এবং ভিডিও উভয়ই কভার করবে */
    private val baseUri = MediaStore.Files.getContentUri("external")

    /** প্রজেকশন — শুধু প্রয়োজনীয় কলামগুলোকে সিলেক্ট করে পারফরম্যান্স বাড়াতে */
    private val projection = buildList {
        add(MediaStore.Files.FileColumns._ID)
        add(MediaStore.Files.FileColumns.DISPLAY_NAME)
        add(MediaStore.Files.FileColumns.MIME_TYPE)
        add(MediaStore.Files.FileColumns.WIDTH)
        add(MediaStore.Files.FileColumns.HEIGHT)
        add(MediaStore.Files.FileColumns.SIZE)
        add(MediaStore.Files.FileColumns.DATE_ADDED)
        add(MediaStore.Files.FileColumns.DATE_MODIFIED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            add(MediaStore.Files.FileColumns.RELATIVE_PATH)
            add(MediaStore.Files.FileColumns.IS_PENDING)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            add(MediaStore.Files.FileColumns.IS_FAVORITE)
        }
    }.toTypedArray()

    /** সেলেকশন বিল্ডার — ফিল্টার শর্ত অনুযায়ী WHERE ক্লauses তৈরি করে */
    private fun buildSelection(
        mimeType: String?,
        dateFrom: Long?,
        dateTo: Long?,
        minWidth: Int?,
        minHeight: Int?,
        query: String? = null,
    ): Pair<String, Array<String>> {
        val clauses = mutableListOf<String>()
        val args = mutableListOf<String>()

        // শুধু ইমেজ এবং ভিডিও
        clauses.add("(${MediaStore.Files.FileColumns.MIME_TYPE} LIKE 'image/%' OR ${MediaStore.Files.FileColumns.MIME_TYPE} LIKE 'video/%')")

        if (!query.isNullOrBlank()) {
            clauses.add("${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE ?")
            args.add("%$query%")
        }

        // MIME টাইপ ফিল্টার
        mimeType?.let {
            when {
                it == "image/*" -> clauses.add("${MediaStore.Files.FileColumns.MIME_TYPE} LIKE 'image/%'")

                it == "video/*" -> clauses.add("${MediaStore.Files.FileColumns.MIME_TYPE} LIKE 'video/%'")

                else -> {
                    clauses.add("${MediaStore.Files.FileColumns.MIME_TYPE} = ?")
                    args.add(it)
                }
            }
        }

        // ডেট রেঞ্জ ফিল্টার (DATE_ADDED_epoch_ms — সেকেন্ডে MediaStore-এ)
        dateFrom?.let {
            clauses.add("${MediaStore.Files.FileColumns.DATE_ADDED} >= ?")
            args.add((it / 1000).toString())
        }
        dateTo?.let {
            clauses.add("${MediaStore.Files.FileColumns.DATE_ADDED} <= ?")
            args.add((it / 1000).toString())
        }

        // ডাইমেনশন ফিল্টার
        minWidth?.let {
            clauses.add("${MediaStore.Files.FileColumns.WIDTH} >= ?")
            args.add(it.toString())
        }
        minHeight?.let {
            clauses.add("${MediaStore.Files.FileColumns.HEIGHT} >= ?")
            args.add(it.toString())
        }

        // পেন্ডিং আইটেম বাদ দিন (অ্যাপে শো না করার জন্য)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            clauses.add("${MediaStore.Files.FileColumns.IS_PENDING} = 0")
        }

        // কুইরি ক্যাঞ্চেল না হওয়া loàiئن — IS_PENDING ইনডেক্সড নাও হতে পারে
        val selection = clauses.joinToString(" AND ")
        return Pair(selection, args.toTypedArray())
    }

    /** ORDER BY ক্লজ বিল্ড করে */
    private fun buildOrderBy(sortBy: GallerySortBy): String = when (sortBy) {
        GallerySortBy.NEWEST -> "${MediaStore.Files.FileColumns.DATE_ADDED} DESC"
        GallerySortBy.OLDEST -> "${MediaStore.Files.FileColumns.DATE_ADDED} ASC"
        GallerySortBy.LARGEST -> "${MediaStore.Files.FileColumns.SIZE} DESC"
        GallerySortBy.SMALLEST -> "${MediaStore.Files.FileColumns.SIZE} ASC"
        GallerySortBy.NAME_A_TO_Z -> "${MediaStore.Files.FileColumns.DISPLAY_NAME} ASC"
        GallerySortBy.NAME_Z_TO_A -> "${MediaStore.Files.FileColumns.DISPLAY_NAME} DESC"
    }

    /** Cursor থেকে GalleryMediaItem তৈরি করে */
    private fun cursorToMediaItem(cursor: Cursor): GalleryMediaItem {
        val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID))
        val contentUri = ContentUris.withAppendedId(MediaStore.Files.getContentUri("external"), id).toString()
        val displayName = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)) ?: ""
        val mimeType = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)) ?: ""
        val width = getIntOrNull(cursor, MediaStore.Files.FileColumns.WIDTH)
        val height = getIntOrNull(cursor, MediaStore.Files.FileColumns.HEIGHT)
        val sizeBytes = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE))
        val dateAddedSec = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED))
        val dateModifiedSec = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED))

        // DATE_TAKEN অপশনাল (EXIF)
        val dateTaken = getLongOrNull(cursor, MediaStore.Files.FileColumns.DATE_TAKEN)?.times(1000)

        // RELATIVE_PATH (API 29+)
        val relativePath = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getStringOrNull(cursor, MediaStore.Files.FileColumns.RELATIVE_PATH)
        } else {
            null
        }

        val isPending = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getIntOrNull(cursor, MediaStore.Files.FileColumns.IS_PENDING) == 1
        } else {
            false
        }

        val isFavorite = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getIntOrNull(cursor, MediaStore.Files.FileColumns.IS_FAVORITE) == 1
        } else {
            false
        }

        return GalleryMediaItem(
            id = id,
            contentUri = contentUri,
            displayName = displayName,
            mimeType = mimeType,
            width = width,
            height = height,
            sizeBytes = getLongOrNull(cursor, MediaStore.Files.FileColumns.SIZE) ?: 0L,
            dateAdded = dateAddedSec * 1000L, // epoch ms
            dateModified = (getLongOrNull(cursor, MediaStore.Files.FileColumns.DATE_MODIFIED) ?: 0L) * 1000L,
            dateTaken = dateTaken,
            relativePath = relativePath,
            isPending = isPending,
            isFavorite = isFavorite,
        )
    }

    // হেল্পার মেথডগুলো
    private fun getIntOrNull(cursor: Cursor, column: String): Int? {
        val idx = cursor.getColumnIndex(column)
        return if (idx >= 0 && !cursor.isNull(idx)) cursor.getInt(idx) else null
    }

    private fun getLongOrNull(cursor: Cursor, column: String): Long? {
        val idx = cursor.getColumnIndex(column)
        return if (idx >= 0 && !cursor.isNull(idx)) cursor.getLong(idx) else null
    }

    private fun getStringOrNull(cursor: Cursor, column: String): String? {
        val idx = cursor.getColumnIndex(column)
        return if (idx >= 0 && !cursor.isNull(idx)) cursor.getString(idx) else null
    }

    // ============ GalleryRepository ইন্টারফেস ইমপ্লিমেন্টেশন ============

    override fun search(
        query: String?,
        mimeType: String?,
        dateFrom: Long?,
        dateTo: Long?,
        minWidth: Int?,
        minHeight: Int?,
        sortBy: GallerySortBy,
        limit: Int,
        offset: Int,
    ): Flow<List<GalleryMediaItem>> = flow {
        // কুইরি এক্সিকিউশন IO ডিসপ্যাচারে
        val items = withContext(kotlinx.coroutines.Dispatchers.IO) {
            queryMutex.withLock {
                val (selection, selectionArgs) = buildSelection(mimeType, dateFrom, dateTo, minWidth, minHeight, query)
                val orderBy = buildOrderBy(sortBy)

                var cursor: Cursor? = null
                try {
                    cursor = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        val queryArgs = android.os.Bundle().apply {
                            putInt(android.content.ContentResolver.QUERY_ARG_LIMIT, limit)
                            putInt(android.content.ContentResolver.QUERY_ARG_OFFSET, offset)
                            putString(android.content.ContentResolver.QUERY_ARG_SQL_SORT_ORDER, orderBy)
                            putString(android.content.ContentResolver.QUERY_ARG_SQL_SELECTION, selection)
                            putStringArray(android.content.ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, selectionArgs)
                        }
                        contentResolver.query(baseUri, projection, queryArgs, null)
                    } else {
                        contentResolver.query(
                            baseUri,
                            projection,
                            selection,
                            selectionArgs,
                            "$orderBy LIMIT $limit OFFSET $offset",
                        )
                    }

                    if (cursor != null) {
                        val items = mutableListOf<GalleryMediaItem>()
                        while (cursor.moveToNext()) {
                            try {
                                items.add(cursorToMediaItem(cursor))
                            } catch (e: Exception) {
                                // ইনডিভিজুয়াল আইটেম পার্সিং ফেইল করলে স্কিপ করুন
                            }
                        }
                        items
                    } else {
                        emptyList()
                    }
                } finally {
                    cursor?.close()
                }
            }
        }
        emit(items)
    }

    override fun count(
        query: String?,
        mimeType: String?,
        dateFrom: Long?,
        dateTo: Long?,
        minWidth: Int?,
        minHeight: Int?,
    ): Int = kotlinx.coroutines.runBlocking(kotlinx.coroutines.Dispatchers.IO) {
        queryMutex.withLock {
            val (selection, selectionArgs) = buildSelection(mimeType, dateFrom, dateTo, minWidth, minHeight, query)
            var cursor: Cursor? = null
            try {
                cursor = contentResolver.query(
                    baseUri,
                    arrayOf(MediaStore.Files.FileColumns._ID),
                    selection,
                    selectionArgs,
                    null,
                )
                cursor?.count ?: 0
            } finally {
                cursor?.close()
            }
        }
    }

    override fun getById(id: Long): GalleryMediaItem? = kotlinx.coroutines.runBlocking(kotlinx.coroutines.Dispatchers.IO) {
        val uri = ContentUris.withAppendedId(baseUri, id)
        try {
            contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursorToMediaItem(cursor) else null
            }
        } catch (_: Exception) {
            // লগিং যোগ করা যেতে পারে
            null
        }
    }

    override fun canAccessMediaUri(uri: String): Boolean = kotlinx.coroutines.runBlocking(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val uriObj = Uri.parse(uri)
            contentResolver.openFileDescriptor(uriObj, "r")?.use { it.close() }
            true
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun rename(id: Long, newName: String): Boolean = withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val uri = ContentUris.withAppendedId(baseUri, id)
            val values = android.content.ContentValues()
            values.put(MediaStore.Files.FileColumns.DISPLAY_NAME, newName)
            val result = contentResolver.update(uri, values, null, null)
            result > 0
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun delete(id: Long): Boolean = withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val uri = ContentUris.withAppendedId(baseUri, id)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    val result = contentResolver.delete(uri, null, null)
                    result > 0
                } catch (e: Exception) {
                    false
                }
            } else {
                val result = contentResolver.delete(uri, null, null)
                result > 0
            }
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun move(id: Long, newRelativePath: String): Boolean = withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val uri = ContentUris.withAppendedId(baseUri, id)
                val values = android.content.ContentValues()
                values.put(MediaStore.Files.FileColumns.RELATIVE_PATH, newRelativePath)
                val result = contentResolver.update(uri, values, null, null)
                result > 0
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun copy(id: Long, newName: String?): Boolean = withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val item = getById(id)
            if (item == null) return@withContext false
            val sourceUri = Uri.parse(item.contentUri)
            val inputStream = contentResolver.openInputStream(sourceUri) ?: return@withContext false
            val values = android.content.ContentValues()
            values.put(MediaStore.Files.FileColumns.DISPLAY_NAME, newName ?: "${item.displayName}_copy")
            values.put(MediaStore.Files.FileColumns.MIME_TYPE, item.mimeType)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.put(MediaStore.Files.FileColumns.RELATIVE_PATH, item.relativePath ?: "Download/")
                values.put(MediaStore.Files.FileColumns.IS_PENDING, 1)
            }
            val destUri = contentResolver.insert(baseUri, values) ?: run {
                inputStream.close()
                return@withContext false
            }
            val outputStream = contentResolver.openOutputStream(destUri) ?: run {
                inputStream.close()
                return@withContext false
            }
            inputStream.copyTo(outputStream)
            inputStream.close()
            outputStream.close()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val updateValues = android.content.ContentValues()
                updateValues.put(MediaStore.Files.FileColumns.IS_PENDING, 0)
                contentResolver.update(destUri, updateValues, null, null)
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}
