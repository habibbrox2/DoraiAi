package com.inspiredandroid.kai.tools

import android.Manifest
import android.app.DownloadManager
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.java.KoinJavaComponent.inject

/**
 * Specialized controller for Downloads access assessment.
 * Downloads access comes from multiple sources with different permission models:
 * 1. DownloadManager - only downloads initiated via DownloadManager API
 * 2. MediaStore - media files (images, videos, audio) in Downloads folder
 * 3. Storage Access Framework (SAF) - user-selected files/folders
 * 4. READ_EXTERNAL_STORAGE (legacy, API ≤28)
 *
 * IMPORTANT: No single source provides complete visibility into all downloads.
 * This controller honestly reports what sources are available and their limitations.
 */
class DownloadsPermissionController(private val context: Context) {

    /**
     * Checks if DownloadManager is accessible and can be queried.
     * DownloadManager only shows downloads initiated through the DownloadManager API
     * (e.g., browser downloads, app-initiated downloads). It does NOT show files
     * copied to Downloads via file managers, USB, or other means.
     */
    fun canQueryDownloadManager(): Boolean = true // DownloadManager query doesn't require special permission

    /**
     * Checks if MediaStore can be queried for download files.
     * Requires media permissions (READ_MEDIA_IMAGES/VIDEO for media files in Downloads).
     * Non-media files (PDF, APK, ZIP) in Downloads are typically NOT in MediaStore.
     */
    fun canQueryMediaStoreDownloads(): Boolean {
        val galleryController = GalleryPermissionController(context)
        return galleryController.canQueryMediaStore()
    }

    /**
     * Checks if SAF (Storage Access Framework) is available.
     * SAF allows user-selected file/folder access without manifest permissions.
     * Requires user interaction (picker) for each access grant.
     */
    fun isSafAvailable(): Boolean = true // SAF is available on all Android versions

    /**
     * Checks if the legacy READ_EXTERNAL_STORAGE permission is granted (API ≤28).
     */
    fun hasLegacyStoragePermission(): Boolean {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.S_V2) return false
        return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) ==
            PackageManager.PERMISSION_GRANTED
    }

    /**
     * Returns the current downloads access state, detailing which sources are available.
     */
    fun getDownloadsAccessState(): DownloadsAccessState {
        val sources = mutableListOf<DownloadsSourceInfo>()

        // DownloadManager is always available but has limited coverage
        sources += DownloadsSourceInfo(
            name = "DownloadManager",
            isAvailable = true,
            coverage = DownloadsCoverage.PARTIAL,
            limitations = listOf(
                "Only downloads initiated via DownloadManager API",
                "Does not show files copied via file manager, USB, or other apps",
            ),
        )

        // MediaStore for media files in Downloads
        if (canQueryMediaStoreDownloads()) {
            sources += DownloadsSourceInfo(
                name = "MediaStore",
                isAvailable = true,
                coverage = DownloadsCoverage.MEDIA_ONLY,
                limitations = listOf(
                    "Only media files (images, videos, audio)",
                    "Does not show PDF, APK, ZIP, or other non-media files",
                ),
            )
        } else if (hasLegacyStoragePermission()) {
            sources += DownloadsSourceInfo(
                name = "MediaStore (legacy)",
                isAvailable = true,
                coverage = DownloadsCoverage.MEDIA_ONLY,
                limitations = listOf(
                    "Only media files (images, videos, audio)",
                    "Requires READ_EXTERNAL_STORAGE permission",
                ),
            )
        }

        // SAF is always available but requires user interaction
        sources += DownloadsSourceInfo(
            name = "Storage Access Framework (SAF)",
            isAvailable = true,
            coverage = DownloadsCoverage.USER_SELECTED,
            limitations = listOf(
                "Requires user to select files/folders via picker",
                "Access is per-URI and can be revoked by user",
            ),
        )

        // Calculate overall coverage
        val overallCoverage = when {
            hasLegacyStoragePermission() && canQueryMediaStoreDownloads() -> DownloadsCoverage.FULL
            canQueryMediaStoreDownloads() -> DownloadsCoverage.MEDIA_PARTIAL
            else -> DownloadsCoverage.PARTIAL
        }

        return DownloadsAccessState(
            sources = sources,
            overallCoverage = overallCoverage,
            hasLegacyPermission = hasLegacyStoragePermission(),
        )
    }

    /**
     * Tests if DownloadManager can actually return results.
     * Performs a lightweight query to verify access.
     */
    suspend fun testDownloadManagerAccess(): Boolean = withContext(Dispatchers.IO) {
        try {
            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val query = DownloadManager.Query()
            query.setFilterByStatus(DownloadManager.STATUS_SUCCESSFUL)
            dm.query(query).use { cursor ->
                cursor.moveToFirst() // Just test if query executes
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Tests if MediaStore can be queried for Downloads folder media.
     */
    suspend fun testMediaStoreDownloadsAccess(): Boolean = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return@withContext hasLegacyStoragePermission()
        }
        try {
            val resolver: ContentResolver = context.contentResolver
            val projection = arrayOf(MediaStore.MediaColumns._ID)
            val selection = "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?"
            val selectionArgs = arrayOf("%Download%")
            resolver.query(
                MediaStore.Files.getContentUri("external"),
                projection,
                selection,
                selectionArgs,
                null,
            )?.use { cursor ->
                cursor.moveToFirst() // Just test if query executes
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Returns a user-friendly summary of downloads access (for UI display).
     */
    fun getAccessSummary(): String {
        val state = getDownloadsAccessState()
        val availableSources = state.sources.filter { it.isAvailable }.map { it.name }.joinToString(", ")
        return "Downloads access: ${state.overallCoverage.displayName}. Available sources: $availableSources"
    }

    /**
     * Returns a user-friendly summary in Bengali.
     */
    fun getAccessSummaryBengali(): String {
        val state = getDownloadsAccessState()
        val availableSources = state.sources.filter { it.isAvailable }.map { it.name }.joinToString(", ")
        return "ডাউনলোড অ্যাক্সেস: ${state.overallCoverage.displayNameBengali}। উপলব্ধ সোর্স: $availableSources"
    }
}

/**
 * Overall downloads access state.
 */
data class DownloadsAccessState(
    val sources: List<DownloadsSourceInfo>,
    val overallCoverage: DownloadsCoverage,
    val hasLegacyPermission: Boolean,
)
