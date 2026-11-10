package com.dorai.ai.ui.maps

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dorai.composeapp.generated.resources.Res
import dorai.composeapp.generated.resources.maps_open_in_browser
import org.jetbrains.compose.resources.stringResource

@Composable
actual fun GoogleMapsView(
    latitude: Double,
    longitude: Double,
    modifier: Modifier,
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("${stringResource(Res.string.maps_open_in_browser)}: https://www.google.com/maps?q=$latitude,$longitude")
    }
}
