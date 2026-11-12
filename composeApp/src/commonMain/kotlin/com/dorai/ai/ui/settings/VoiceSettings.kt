package com.dorai.ai.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dorai.ai.ui.DoraiTypography
import com.dorai.ai.ui.theme.LocalDesignColorTokens
import com.dorai.ai.voice.VoiceModelEntry
import com.dorai.ai.voice.VoiceModelState
import com.dorai.ai.voice.isDownloadable
import dorai.composeapp.generated.resources.Res
import dorai.composeapp.generated.resources.settings_voice_delete
import dorai.composeapp.generated.resources.settings_voice_download
import dorai.composeapp.generated.resources.settings_voice_downloading
import dorai.composeapp.generated.resources.settings_voice_failed
import dorai.composeapp.generated.resources.settings_voice_failed_body
import dorai.composeapp.generated.resources.settings_voice_license_pending
import dorai.composeapp.generated.resources.settings_voice_mobile_data
import dorai.composeapp.generated.resources.settings_voice_mobile_data_description
import dorai.composeapp.generated.resources.settings_voice_mode
import dorai.composeapp.generated.resources.settings_voice_mode_description
import dorai.composeapp.generated.resources.settings_voice_models_title
import dorai.composeapp.generated.resources.settings_voice_pause
import dorai.composeapp.generated.resources.settings_voice_paused
import dorai.composeapp.generated.resources.settings_voice_ready
import dorai.composeapp.generated.resources.settings_voice_redownload
import dorai.composeapp.generated.resources.settings_voice_resume
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource

/**
 * The Voice section of Settings.
 *
 * Voice Mode is off by default: the feature plan keeps it behind the toggle until the
 * on-device measurement gate has been signed off. The description says plainly which
 * recognizer is used. Below it, the downloadable model catalog is listed — each entry
 * shows its state, and stays undownloadable until its license is signed off.
 */
@Composable
internal fun VoiceSettingsSection(
    uiState: SettingsUiState,
    actions: SettingsActions,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        ToggleableHeadline(
            title = stringResource(Res.string.settings_voice_mode),
            description = stringResource(Res.string.settings_voice_mode_description),
            checked = uiState.isVoiceModeEnabled,
            onCheckedChange = actions.onToggleVoiceMode,
        )

        if (uiState.voiceModels.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            VoiceModelsTicker(uiState = uiState, onTick = actions.onVoiceModelsTick)
            Text(
                text = stringResource(Res.string.settings_voice_models_title),
                style = DoraiTypography.titleSmall,
                color = LocalDesignColorTokens.current.textPrimary,
            )
            uiState.voiceModels.forEach { entry ->
                VoiceModelRow(
                    entry = entry,
                    onDownload = { actions.onVoiceModelDownload(entry.model.id) },
                    onPause = { actions.onVoiceModelPause(entry.model.id) },
                    onDelete = { actions.onVoiceModelDelete(entry.model.id) },
                    onReDownload = { actions.onVoiceModelReDownload(entry.model.id) },
                )
            }

            Spacer(Modifier.height(12.dp))
            ToggleableHeadline(
                title = stringResource(Res.string.settings_voice_mobile_data),
                description = stringResource(Res.string.settings_voice_mobile_data_description),
                checked = uiState.isVoiceMobileDataAllowed,
                onCheckedChange = actions.onToggleVoiceMobileData,
            )
        }
    }
}

/**
 * Drives the one-second manifest refresh while a download is in flight. Lives in the
 * UI layer on purpose: an unbounded polling loop inside the ViewModel would never let
 * a test dispatcher go idle and hang the whole desktop test suite. The loop only starts
 * when some entry is DOWNLOADING or PAUSED, so idle screens cost nothing.
 */
@Composable
private fun VoiceModelsTicker(uiState: SettingsUiState, onTick: () -> Unit) {
    val inFlight = uiState.voiceModels.any { it.state == VoiceModelState.DOWNLOADING || it.state == VoiceModelState.PAUSED }
    if (!inFlight) return
    LaunchedEffect(inFlight) {
        while (true) {
            delay(1_000)
            onTick()
        }
    }
}

@Composable
internal fun VoiceModelRow(
    entry: VoiceModelEntry,
    onDownload: () -> Unit,
    onPause: () -> Unit,
    onDelete: () -> Unit,
    onReDownload: () -> Unit,
) {
    val colors = LocalDesignColorTokens.current
    val model = entry.model

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = model.displayName,
                    style = DoraiTypography.bodyMedium,
                    color = colors.textPrimary,
                )
                Text(
                    text = "${model.totalSizeBytes / 1024 / 1024} MB · ${model.licenseId}",
                    style = DoraiTypography.bodySmall,
                    color = colors.textSecondary,
                )
                when (entry.state) {
                    VoiceModelState.DOWNLOADING -> Text(
                        text = stringResource(Res.string.settings_voice_downloading, (entry.progressFraction * 100).toInt()),
                        style = DoraiTypography.bodySmall,
                        color = colors.textSecondary,
                    )

                    VoiceModelState.PAUSED -> Text(
                        text = stringResource(Res.string.settings_voice_paused),
                        style = DoraiTypography.bodySmall,
                        color = colors.textSecondary,
                    )

                    VoiceModelState.READY -> Text(
                        text = stringResource(Res.string.settings_voice_ready),
                        style = DoraiTypography.bodySmall,
                        color = colors.textPrimary,
                    )

                    VoiceModelState.FAILED -> {
                        Text(
                            text = stringResource(Res.string.settings_voice_failed),
                            style = DoraiTypography.bodySmall,
                            color = colors.error,
                        )
                        entry.failedReason?.let { reason ->
                            Text(
                                text = stringResource(Res.string.settings_voice_failed_body, reason),
                                style = DoraiTypography.bodySmall,
                                color = colors.textSecondary,
                            )
                        }
                    }

                    VoiceModelState.ABSENT -> {
                        if (!model.isDownloadable()) {
                            Text(
                                text = stringResource(Res.string.settings_voice_license_pending),
                                style = DoraiTypography.bodySmall,
                                color = colors.textSecondary,
                            )
                        }
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.End) {
            when (entry.state) {
                VoiceModelState.ABSENT, VoiceModelState.FAILED -> {
                    if (entry.canDownload) {
                        TextButton(onClick = onDownload) {
                            Text(text = stringResource(Res.string.settings_voice_download))
                        }
                    }
                }

                VoiceModelState.DOWNLOADING -> {
                    TextButton(onClick = onPause) {
                        Text(text = stringResource(Res.string.settings_voice_pause))
                    }
                }

                VoiceModelState.PAUSED -> {
                    TextButton(onClick = onDownload) {
                        Text(text = stringResource(Res.string.settings_voice_resume))
                    }
                    TextButton(onClick = onDelete) {
                        Text(text = stringResource(Res.string.settings_voice_delete))
                    }
                }

                VoiceModelState.READY -> {
                    TextButton(onClick = onDelete) {
                        Text(text = stringResource(Res.string.settings_voice_delete))
                    }
                    TextButton(onClick = onReDownload) {
                        Text(text = stringResource(Res.string.settings_voice_redownload))
                    }
                }
            }
        }
    }
}
