package com.dorai.ai.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import com.dorai.ai.PlatformBackHandler
import com.dorai.ai.ui.chat.composables.FullScreenImageViewerOverlay
import com.dorai.ai.ui.chat.composables.FullScreenUrlImageViewerOverlay

val LocalShowFullScreenImage = staticCompositionLocalOf<(ImageBitmap) -> Unit> { { } }

/** Show a full-screen viewer for a remotely-loaded image URL (e.g. markdown image previews). */
val LocalShowFullScreenImageUrl = staticCompositionLocalOf<(String) -> Unit> { { } }

@Composable
fun FullScreenImageHost(content: @Composable () -> Unit) {
    var image by remember { mutableStateOf<ImageBitmap?>(null) }
    var imageUrl by remember { mutableStateOf<String?>(null) }
    val show = remember { { bitmap: ImageBitmap -> image = bitmap } }
    val showUrl = remember { { url: String -> imageUrl = url } }
    val dismiss = remember {
        {
            image = null
            imageUrl = null
        }
    }

    Box(Modifier.fillMaxSize()) {
        CompositionLocalProvider(
            LocalShowFullScreenImage provides show,
            LocalShowFullScreenImageUrl provides showUrl,
        ) {
            content()
        }
        image?.let { bmp ->
            FullScreenImageViewerOverlay(bitmap = bmp, onDismiss = dismiss)
            PlatformBackHandler(enabled = true, onBack = dismiss)
        }
        imageUrl?.let { url ->
            FullScreenUrlImageViewerOverlay(url = url, onDismiss = dismiss)
            PlatformBackHandler(enabled = true, onBack = dismiss)
        }
    }
}
