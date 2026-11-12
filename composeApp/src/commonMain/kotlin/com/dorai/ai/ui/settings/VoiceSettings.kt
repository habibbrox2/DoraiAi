package com.dorai.ai.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dorai.composeapp.generated.resources.Res
import dorai.composeapp.generated.resources.settings_voice_mode
import dorai.composeapp.generated.resources.settings_voice_mode_description
import org.jetbrains.compose.resources.stringResource

/**
 * The Voice section of Settings.
 *
 * Voice Mode is off by default: the feature plan keeps it behind the toggle until the
 * on-device measurement gate has been signed off. The description says plainly which
 * recognizer is used, so the user knows whether the device can work offline.
 */
@Composable
internal fun VoiceSettingsSection(
    isVoiceModeEnabled: Boolean,
    onToggleVoiceMode: (Boolean) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        ToggleableHeadline(
            title = stringResource(Res.string.settings_voice_mode),
            description = stringResource(Res.string.settings_voice_mode_description),
            checked = isVoiceModeEnabled,
            onCheckedChange = onToggleVoiceMode,
        )
    }
}
