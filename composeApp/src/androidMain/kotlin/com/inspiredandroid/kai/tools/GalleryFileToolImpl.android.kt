package com.inspiredandroid.kai.tools

import com.inspiredandroid.kai.network.tools.Tool
import com.inspiredandroid.kai.permissionQualifier
import org.koin.java.KoinJavaComponent
import org.koin.java.KoinJavaComponent.inject

class GalleryRenameToolImpl : Tool {
    private val galleryRepository: GalleryRepository by inject(GalleryRepository::class.java)
    private val galleryPermissionController: GalleryPermissionController by inject(GalleryPermissionController::class.java)
    private val permissionController: PermissionController =
        KoinJavaComponent.get(PermissionController::class.java, permissionQualifier(AppPermission.GALLERY))

    override val schema = GalleryRenameTool.schema

    override suspend fun execute(args: Map<String, Any>): Any = try {
        if (!galleryPermissionController.canQueryMediaStore()) {
            val granted = permissionController.requestPermission()
            if (!granted) {
                return mapOf(
                    "success" to false,
                    "error" to "Gallery permission not granted",
                )
            }
        }
        val id = (args["id"] as? Number)?.toLong() ?: (args["id"] as? String)?.toLongOrNull()
        val newName = args["newName"] as? String
        if (id == null || newName.isNullOrBlank()) {
            return mapOf("success" to false, "error" to "id and newName are required")
        }
        val result = galleryRepository.rename(id, newName)
        mapOf(
            "success" to result,
            "message" to if (result) "File renamed successfully" else "Failed to rename file",
        )
    } catch (e: Exception) {
        mapOf("success" to false, "error" to "Rename error: ${e.message}")
    }
}

class GalleryDeleteToolImpl : Tool {
    private val galleryRepository: GalleryRepository by inject(GalleryRepository::class.java)
    private val galleryPermissionController: GalleryPermissionController by inject(GalleryPermissionController::class.java)
    private val permissionController: PermissionController =
        KoinJavaComponent.get(PermissionController::class.java, permissionQualifier(AppPermission.GALLERY))

    override val schema = GalleryDeleteTool.schema

    override suspend fun execute(args: Map<String, Any>): Any = try {
        if (!galleryPermissionController.canQueryMediaStore()) {
            val granted = permissionController.requestPermission()
            if (!granted) {
                return mapOf(
                    "success" to false,
                    "error" to "Gallery permission not granted",
                )
            }
        }
        val id = (args["id"] as? Number)?.toLong() ?: (args["id"] as? String)?.toLongOrNull()
        if (id == null) {
            return mapOf("success" to false, "error" to "id is required")
        }
        val result = galleryRepository.delete(id)
        mapOf(
            "success" to result,
            "message" to if (result) "File deleted successfully" else "Failed to delete file",
        )
    } catch (e: Exception) {
        mapOf("success" to false, "error" to "Delete error: ${e.message}")
    }
}

class GalleryMoveToolImpl : Tool {
    private val galleryRepository: GalleryRepository by inject(GalleryRepository::class.java)
    private val galleryPermissionController: GalleryPermissionController by inject(GalleryPermissionController::class.java)
    private val permissionController: PermissionController =
        KoinJavaComponent.get(PermissionController::class.java, permissionQualifier(AppPermission.GALLERY))

    override val schema = GalleryMoveTool.schema

    override suspend fun execute(args: Map<String, Any>): Any = try {
        if (!galleryPermissionController.canQueryMediaStore()) {
            val granted = permissionController.requestPermission()
            if (!granted) {
                return mapOf(
                    "success" to false,
                    "error" to "Gallery permission not granted",
                )
            }
        }
        val id = (args["id"] as? Number)?.toLong() ?: (args["id"] as? String)?.toLongOrNull()
        val newRelativePath = args["newRelativePath"] as? String
        if (id == null || newRelativePath.isNullOrBlank()) {
            return mapOf("success" to false, "error" to "id and newRelativePath are required")
        }
        val result = galleryRepository.move(id, newRelativePath)
        mapOf(
            "success" to result,
            "message" to if (result) "File moved successfully" else "Failed to move file",
        )
    } catch (e: Exception) {
        mapOf("success" to false, "error" to "Move error: ${e.message}")
    }
}

class GalleryCopyToolImpl : Tool {
    private val galleryRepository: GalleryRepository by inject(GalleryRepository::class.java)
    private val galleryPermissionController: GalleryPermissionController by inject(GalleryPermissionController::class.java)
    private val permissionController: PermissionController =
        KoinJavaComponent.get(PermissionController::class.java, permissionQualifier(AppPermission.GALLERY))

    override val schema = GalleryCopyTool.schema

    override suspend fun execute(args: Map<String, Any>): Any = try {
        if (!galleryPermissionController.canQueryMediaStore()) {
            val granted = permissionController.requestPermission()
            if (!granted) {
                return mapOf(
                    "success" to false,
                    "error" to "Gallery permission not granted",
                )
            }
        }
        val id = (args["id"] as? Number)?.toLong() ?: (args["id"] as? String)?.toLongOrNull()
        val newName = args["newName"] as? String
        if (id == null) {
            return mapOf("success" to false, "error" to "id is required")
        }
        val result = galleryRepository.copy(id, newName)
        mapOf(
            "success" to result,
            "message" to if (result) "File copied successfully" else "Failed to copy file",
        )
    } catch (e: Exception) {
        mapOf("success" to false, "error" to "Copy error: ${e.message}")
    }
}
