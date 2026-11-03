package com.inspiredandroid.kai.tools

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import org.koin.java.KoinJavaComponent.inject

/**
 * Specialized permission controller for gallery/media access.
 * Handles the complexity of Android version-specific media permissions:
 * - API ≤32: READ_EXTERNAL_STORAGE
 * - API 33+: READ_MEDIA_IMAGES, READ_MEDIA_VIDEO
 * - API 34+: READ_MEDIA_VISUAL_USER_SELECTED (selected photos picker)
 */
class GalleryPermissionController(private val context: Context) {

    /**
     * Checks if the app has any gallery/media access permission granted.
     * This covers the base level of access needed to query MediaStore.
     */
    fun hasBaseGalleryAccess(): Boolean {
        val sdk = Build.VERSION.SDK_INT
        return when {
            sdk <= Build.VERSION_CODES.S_V2 -> hasPermission(Manifest.permission.READ_EXTERNAL_STORAGE)

            sdk >= Build.VERSION_CODES.TIRAMISU ->
                hasPermission(Manifest.permission.READ_MEDIA_IMAGES) ||
                    hasPermission(Manifest.permission.READ_MEDIA_VIDEO) ||
                    (sdk >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && hasPermission(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED))

            else -> false
        }
    }

    private fun hasPermission(permission: String): Boolean = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    /**
     * Checks if the app has access to the selected photos picker (Android 14+).
     * This is a separate permission that allows access to user-selected photos only.
     */
    fun hasSelectedPhotosAccess(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return false
        return hasPermission(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
    }

    /**
     * Returns the current gallery access state as an enum.
     */
    fun getGalleryAccessState(): GalleryAccessState {
        if (hasBaseGalleryAccess()) return GalleryAccessState.FULL
        if (hasSelectedPhotosAccess()) return GalleryAccessState.SELECTED_PHOTOS_ONLY
        return GalleryAccessState.NONE
    }

    /**
     * Checks if the app can query MediaStore for images.
     * This is the minimum requirement for gallery search functionality.
     */
    fun canQueryMediaStore(): Boolean = hasBaseGalleryAccess() || hasSelectedPhotosAccess()

    /**
     * Checks if the app can access a specific media URI.
     * This is needed when accessing individual files via content URIs.
     */
    fun canAccessMediaUri(uri: android.net.Uri): Boolean = try {
        context.contentResolver.openFileDescriptor(uri, "r")?.use { it.close() }
        true
    } catch (_: Exception) {
        false
    }

    /**
     * Returns a user-friendly description of the current access state (for UI display).
     */
    fun getAccessDescription(): String = when (getGalleryAccessState()) {
        GalleryAccessState.FULL -> "Full gallery access granted"
        GalleryAccessState.SELECTED_PHOTOS_ONLY -> "Access to selected photos only"
        GalleryAccessState.NONE -> "Gallery access denied"
        GalleryAccessState.REVOKED -> "Gallery access was revoked"
    }

    /**
     * Returns a user-friendly description in Bengali (for UI display).
     */
    fun getAccessDescriptionBengali(): String = when (getGalleryAccessState()) {
        GalleryAccessState.FULL -> "পুরো গ্যালেরির অ্যাক্সেস প্রদান করা হয়েছে"
        GalleryAccessState.SELECTED_PHOTOS_ONLY -> "নির্বাচিত ছবিগুলোর অ্যাক্সেস মাত্র"
        GalleryAccessState.NONE -> "গ্যালেরির অ্যাক্সেস অস্বীকৃত"
        GalleryAccessState.REVOKED -> "গ্যালেরির অ্যাক্সেস প্রত্যাহার করা হয়েছে"
    }
}

/**
 * Represents the current state of gallery/media access.
 */
enum class GalleryAccessState {
    /** Full access to all media via MediaStore */
    FULL,

    /** Only access to user-selected photos (Android 14+ picker) */
    SELECTED_PHOTOS_ONLY,

    /** No gallery access */
    NONE,

    /** Access was previously granted but has been revoked */
    REVOKED,
}

/**
 * Koin factory for GalleryPermissionController.
 * Usage: val controller by inject<GalleryPermissionController>()
 */
class GalleryPermissionControllerFactory {
    operator fun invoke(): GalleryPermissionController {
        val context: Context by inject(Context::class.java)
        return GalleryPermissionController(context)
    }
}
