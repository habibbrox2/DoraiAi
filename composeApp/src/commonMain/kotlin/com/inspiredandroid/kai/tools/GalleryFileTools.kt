package com.inspiredandroid.kai.tools

import com.inspiredandroid.kai.network.tools.ParameterSchema
import com.inspiredandroid.kai.network.tools.ToolInfo
import com.inspiredandroid.kai.network.tools.ToolSchema
import kai.composeapp.generated.resources.Res

object GalleryRenameTool {
    val schema = ToolSchema(
        name = "gallery_rename",
        description = "Rename a file/item in the user's gallery/media library.",
        parameters = mapOf(
            "id" to ParameterSchema("integer", "Media item ID (from gallery_search)", true),
            "newName" to ParameterSchema("string", "New display name for the file", true),
        ),
    )
    val toolInfo = ToolInfo(
        id = "gallery_rename",
        name = "Rename Gallery File",
        description = "Rename a file in the gallery/media library",
    )
}

object GalleryDeleteTool {
    val schema = ToolSchema(
        name = "gallery_delete",
        description = "Delete a file/item from the user's gallery/media library. This action requires permission.",
        parameters = mapOf(
            "id" to ParameterSchema("integer", "Media item ID (from gallery_search)", true),
        ),
    )
    val toolInfo = ToolInfo(
        id = "gallery_delete",
        name = "Delete Gallery File",
        description = "Delete a file from the gallery/media library",
    )
}

object GalleryMoveTool {
    val schema = ToolSchema(
        name = "gallery_move",
        description = "Move a file/item in the user's gallery/media library to a different location/path.",
        parameters = mapOf(
            "id" to ParameterSchema("integer", "Media item ID (from gallery_search)", true),
            "newRelativePath" to ParameterSchema("string", "New relative path/location (e.g., Pictures/Folder/)", true),
        ),
    )
    val toolInfo = ToolInfo(
        id = "gallery_move",
        name = "Move Gallery File",
        description = "Move a file in the gallery/media library",
    )
}

object GalleryCopyTool {
    val schema = ToolSchema(
        name = "gallery_copy",
        description = "Copy a file/item in the user's gallery/media library.",
        parameters = mapOf(
            "id" to ParameterSchema("integer", "Media item ID (from gallery_search)", true),
            "newName" to ParameterSchema("string", "Optional new name for the copied file", false),
        ),
    )
    val toolInfo = ToolInfo(
        id = "gallery_copy",
        name = "Copy Gallery File",
        description = "Copy a file in the gallery/media library",
    )
}
