package com.inspiredandroid.kai.tools

import android.os.Build
import com.inspiredandroid.kai.network.tools.Tool
import org.koin.java.KoinJavaComponent.inject

/**
 * Android implementation of the gallery status tool.
 * This is registered in Platform.android.kt as part of the tool set.
 */
class GalleryStatusToolImpl : Tool {
    private val galleryPermissionController: GalleryPermissionController by inject(GalleryPermissionController::class.java)

    override val schema = GalleryStatusTool.schema

    override suspend fun execute(args: Map<String, Any>): Any {
        val accessState = galleryPermissionController.getGalleryAccessState()

        return mapOf(
            "success" to true,
            "permissionState" to when (accessState) {
                GalleryAccessState.FULL -> "GRANTED"
                GalleryAccessState.SELECTED_PHOTOS_ONLY -> "SELECTED_PHOTOS_ONLY"
                GalleryAccessState.NONE -> "DENIED"
                GalleryAccessState.REVOKED -> "REVOKED"
            },
            "accessScope" to accessState.name,
            "androidVersion" to Build.VERSION.SDK_INT,
            "canQueryMediaStore" to galleryPermissionController.canQueryMediaStore(),
            "hasSelectedPhotosAccess" to galleryPermissionController.hasSelectedPhotosAccess(),
            "message" to galleryPermissionController.getAccessDescription(),
            "messageBengali" to galleryPermissionController.getAccessDescriptionBengali(),
        )
    }
}
