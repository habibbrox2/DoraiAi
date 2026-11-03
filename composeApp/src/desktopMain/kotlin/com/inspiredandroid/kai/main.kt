@file:Suppress("ktlint:standard:filename")
@file:OptIn(ExperimentalDesktopTarget::class)

package com.inspiredandroid.kai

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.navigation.compose.rememberNavController
import com.inspiredandroid.kai.updater.AutoUpdater
import kai.composeapp.generated.resources.Res
import kai.composeapp.generated.resources.logo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import nl.marc_apps.tts.TextToSpeechEngine
import nl.marc_apps.tts.TextToSpeechInstance
import nl.marc_apps.tts.experimental.ExperimentalDesktopTarget
import nl.marc_apps.tts.rememberTextToSpeechOrNull
import org.jetbrains.compose.resources.painterResource

fun main() {
    System.setProperty("apple.awt.application.appearance", "system")
    // Help AWT/Skiko pick up HiDPI on Linux/Wayland (Sway, GNOME fractional scaling).
    // Without this, the JVM ignores GDK_SCALE and renders at 1× on a hi-res monitor.
    if (System.getProperty("sun.java2d.uiScale.enabled") == null) {
        System.setProperty("sun.java2d.uiScale.enabled", "true")
    }
    if (System.getProperty("sun.java2d.uiScale") == null) {
        System.setProperty("sun.java2d.uiScale", "auto")
    }
    application {
        val windowState = rememberWindowState(size = DpSize(1280.dp, 800.dp))
        Window(
            onCloseRequest = ::exitApplication,
            state = windowState,
            title = "Brox Kai 2.0",
            icon = painterResource(Res.drawable.logo),
        ) {
            // Defer TTS initialization until after the first frame
            var ttsReady by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { ttsReady = true }
            val textToSpeech: TextToSpeechInstance? = if (ttsReady) {
                rememberTextToSpeechOrNull(TextToSpeechEngine.Google)
            } else {
                null
            }

            val navController = rememberNavController()
            App(
                navController = navController,
                textToSpeech = textToSpeech,
            )

            // Auto-update check (desktop only)
            val updaterScope = remember { CoroutineScope(Dispatchers.IO) }
            val updateState = remember { mutableStateOf<AutoUpdater.UpdateState>(AutoUpdater.UpdateState.Idle) }
            val checker = remember { AutoUpdater.createUpdateChecker(updaterScope) { updateState.value = it } }
            LaunchedEffect(Unit) { checker.onStart() }
            LaunchedEffect(updateState.value) {
                val state = updateState.value
                if (state !is AutoUpdater.UpdateState.Idle) println("[updater] state=$state")
            }
            AutoUpdateDialog(updateState = updateState.value, onDismiss = { /* keep state */ })
        }
    }
}

@Composable
private fun AutoUpdateDialog(
    updateState: AutoUpdater.UpdateState,
    onDismiss: () -> Unit,
) {
    when (updateState) {
        is AutoUpdater.UpdateState.UpdateAvailable -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("Update Available") },
                text = {
                    Text(
                        "Brox Kai 2.0 ${updateState.version} is ready.\n\n${updateState.releaseNotes.take(500)}",
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        java.awt.Desktop.getDesktop().browse(java.net.URI(updateState.downloadUrl))
                        onDismiss()
                    }) { Text("Download & Install") }
                },
                dismissButton = {
                    Button(onClick = onDismiss) { Text("Later") }
                },
            )
        }

        else -> Unit
    }
}
