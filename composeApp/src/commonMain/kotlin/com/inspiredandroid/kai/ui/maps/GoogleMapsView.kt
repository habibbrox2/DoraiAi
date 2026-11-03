package com.inspiredandroid.kai.ui.maps

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Cross-platform Google Maps view for a given coordinate.
 *
 * On Android this renders an embedded Google Maps in a WebView. On other platforms
 * (where a WebView is not available without extra dependencies) it falls back to
 * opening the location in the system browser via the provided [onOpenInBrowser] callback.
 */
@Composable
expect fun GoogleMapsView(
    latitude: Double,
    longitude: Double,
    modifier: Modifier = Modifier,
)
