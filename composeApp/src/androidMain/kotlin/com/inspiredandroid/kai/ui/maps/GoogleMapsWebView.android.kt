package com.inspiredandroid.kai.ui.maps

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Android implementation: renders Google Maps for the given coordinates in a WebView.
 */
@Composable
actual fun GoogleMapsView(
    latitude: Double,
    longitude: Double,
    modifier: Modifier,
) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                webViewClient = WebViewClient()
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                loadUrl("https://www.google.com/maps?q=$latitude,$longitude&z=15&output=embed")
            }
        },
        modifier = modifier,
        update = { view ->
            view.loadUrl("https://www.google.com/maps?q=$latitude,$longitude&z=15&output=embed")
        },
    )
}
