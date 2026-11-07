package com.dorai.ai.ui.adaptive

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import dorai.composeapp.generated.resources.Res
import dorai.composeapp.generated.resources.nav_chat
import dorai.composeapp.generated.resources.nav_home
import dorai.composeapp.generated.resources.nav_settings
import dorai.composeapp.generated.resources.nav_workspace
import org.jetbrains.compose.resources.StringResource

/**
 * The four primary destinations in UI v2, strictly in this order:
 * 1. Home (Dashboard)
 * 2. Chat
 * 3. Workspace
 * 4. Settings
 */
enum class DoraiDestination(
    val id: String,
    val labelRes: StringResource,
    val icon: ImageVector,
) {
    Home("home", Res.string.nav_home, Icons.Filled.Home),
    Chat("chat", Res.string.nav_chat, Icons.AutoMirrored.Filled.Chat),
    Workspace("work", Res.string.nav_workspace, Icons.Filled.Folder),
    Settings("settings", Res.string.nav_settings, Icons.Filled.Settings),
    ;

    companion object {
        fun fromId(id: String?): DoraiDestination = entries.firstOrNull { it.id == id } ?: Home
    }
}
