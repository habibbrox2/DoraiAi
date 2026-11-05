package com.dorai.ai.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dorai.composeapp.generated.resources.Res
import dorai.composeapp.generated.resources.logo
import org.jetbrains.compose.resources.painterResource

/**
 * Official DORAI brand logo mark.
 *
 * Renders the official DORAI Hexa-Core brand mark across platforms.
 */
@Composable
fun LogoAnimation(
    modifier: Modifier = Modifier,
    size: Dp = 72.dp,
    contentDescription: String? = "DORAI AI Logo",
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(Res.drawable.logo),
            contentDescription = contentDescription,
            modifier = Modifier.size(size),
        )
    }
}
