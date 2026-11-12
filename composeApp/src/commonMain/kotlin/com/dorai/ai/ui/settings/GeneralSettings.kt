package com.dorai.ai.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.dorai.ai.data.AppLanguage
import com.dorai.ai.data.AppSettings
import com.dorai.ai.ui.DoraiTypography
import com.dorai.ai.ui.components.DoraiSlider
import com.dorai.ai.ui.handCursor
import com.dorai.ai.ui.theme.DesignTokens
import com.dorai.ai.ui.theme.LocalDesignColorTokens
import dorai.composeapp.generated.resources.Res
import dorai.composeapp.generated.resources.settings_daemon_mode
import dorai.composeapp.generated.resources.settings_daemon_mode_description
import dorai.composeapp.generated.resources.settings_restore_last_chat
import dorai.composeapp.generated.resources.settings_restore_last_chat_description
import dorai.composeapp.generated.resources.settings_ui_scale
import dorai.composeapp.generated.resources.tts_description
import dorai.composeapp.generated.resources.tts_speech_speed
import dorai.composeapp.generated.resources.tts_tip
import dorai.composeapp.generated.resources.tts_title
import dorai.composeapp.generated.resources.tts_value_format
import dorai.composeapp.generated.resources.tts_voice_pitch
import dorai.composeapp.generated.resources.tts_volume
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import kotlin.math.roundToInt

@Composable
internal fun GeneralContent(
    uiState: SettingsUiState,
    actions: SettingsActions,
    androidUpdateContent: (@Composable () -> Unit)? = null,
) {
    StaggeredSettingsColumns(
        start = {
            if (uiState.showDaemonToggle) {
                SettingsCard {
                    DaemonModeToggle(
                        isDaemonEnabled = uiState.isDaemonEnabled,
                        onToggleDaemon = actions.onToggleDaemon,
                    )
                }
            }
            SettingsCard {
                RestoreLastChatToggle(
                    isRestoreLastChatEnabled = uiState.isRestoreLastChatEnabled,
                    onToggleRestoreLastChat = actions.onToggleRestoreLastChat,
                )
            }
            SettingsCard {
                LanguageSwitch(
                    appLanguage = uiState.appLanguage,
                    onChangeAppLanguage = actions.onChangeAppLanguage,
                )
            }
            SettingsCard {
                TtsSettingsSection(appLanguage = uiState.appLanguage)
            }
            if (uiState.showVoiceSection) {
                SettingsCard {
                    VoiceSettingsSection(uiState = uiState, actions = actions)
                }
            }
        },
        end = {
            if (uiState.showUiScale) {
                SettingsCard {
                    UiScaleSection(
                        uiScale = uiState.uiScale,
                        onChangeUiScale = actions.onChangeUiScale,
                    )
                }
            }
            SettingsCard {
                ExportImportSection(
                    onExportSettings = actions.onExportSettings,
                    onPrepareExport = actions.onPrepareExport,
                    onImportSettings = actions.onImportSettings,
                )
            }
        },
    )
    androidUpdateContent?.invoke()
}

@Composable
private fun DaemonModeToggle(
    isDaemonEnabled: Boolean,
    onToggleDaemon: (Boolean) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        ToggleableHeadline(
            title = stringResource(Res.string.settings_daemon_mode),
            description = stringResource(Res.string.settings_daemon_mode_description),
            checked = isDaemonEnabled,
            onCheckedChange = onToggleDaemon,
        )
    }
}

@Composable
private fun RestoreLastChatToggle(
    isRestoreLastChatEnabled: Boolean,
    onToggleRestoreLastChat: (Boolean) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        ToggleableHeadline(
            title = stringResource(Res.string.settings_restore_last_chat),
            description = stringResource(Res.string.settings_restore_last_chat_description),
            checked = isRestoreLastChatEnabled,
            onCheckedChange = onToggleRestoreLastChat,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun LanguageSwitch(
    appLanguage: AppLanguage,
    onChangeAppLanguage: (AppLanguage) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Language",
            style = DoraiTypography.titleMedium,
            color = LocalDesignColorTokens.current.textPrimary,
        )
        Text(
            text = "App display language (default: Bengali)",
            style = DoraiTypography.bodyMedium,
            color = LocalDesignColorTokens.current.textSecondary,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AppLanguage.entries.forEach { language ->
                ThemeModeButton(
                    label = language.displayName,
                    isSelected = language == appLanguage,
                    onClick = { onChangeAppLanguage(language) },
                )
            }
        }
    }
}

@Composable
private fun ThemeModeButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val selectedBg = DesignTokens.PrimaryCyan
    val selectedText = DesignTokens.BrandNavy
    val unselectedBg = LocalDesignColorTokens.current.surfaceRaised
    val unselectedText = LocalDesignColorTokens.current.textPrimary
    val unselectedBorder = LocalDesignColorTokens.current.borderSubtle

    Box(
        modifier = Modifier
            .clip(SettingsShapes.pill)
            .handCursor()
            .clickable { onClick() }
            .background(if (isSelected) selectedBg else unselectedBg, SettingsShapes.pill)
            .border(
                if (!isSelected) {
                    BorderStroke(1.dp, unselectedBorder)
                } else {
                    BorderStroke(1.dp, DesignTokens.PrimaryCyan)
                },
                SettingsShapes.pill,
            ),
    ) {
        Text(
            text = label,
            style = DoraiTypography.labelLarge,
            fontWeight = if (isSelected) androidx.compose.ui.text.font.FontWeight.SemiBold else androidx.compose.ui.text.font.FontWeight.Normal,
            color = if (isSelected) selectedText else unselectedText,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        )
    }
}

@Composable
private fun UiScaleSection(
    uiScale: Float,
    onChangeUiScale: (Float) -> Unit,
) {
    var sliderValue by remember(uiScale) { mutableStateOf(uiScale) }
    val steps = 14 // 16 snap points from 50% to 200% in 10% increments (14 intermediate)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(Res.string.settings_ui_scale),
                style = DoraiTypography.titleMedium,
                color = LocalDesignColorTokens.current.textPrimary,
            )
            Text(
                text = "${(sliderValue * 100).roundToInt()}%",
                style = DoraiTypography.labelLarge,
                color = LocalDesignColorTokens.current.textPrimary,
            )
        }
        DoraiSlider(
            value = sliderValue,
            onValueChange = { sliderValue = it },
            onValueChangeFinished = { onChangeUiScale(sliderValue) },
            valueRange = 0.5f..2.0f,
            steps = steps,
        )
    }
}

@Composable
private fun TtsSettingsSection(appLanguage: AppLanguage) {
    val appSettings: AppSettings = koinInject()
    var rate by remember { mutableFloatStateOf(appSettings.getTtsRate()) }
    var pitch by remember { mutableFloatStateOf(appSettings.getTtsPitch()) }
    var volume by remember { mutableFloatStateOf(appSettings.getTtsVolume()) }

    LaunchedEffect(rate) { appSettings.setTtsRate(rate) }
    LaunchedEffect(pitch) { appSettings.setTtsPitch(pitch) }
    LaunchedEffect(volume) { appSettings.setTtsVolume(volume) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(Res.string.tts_title),
            style = DoraiTypography.titleMedium,
            color = LocalDesignColorTokens.current.textPrimary,
        )
        Text(
            text = stringResource(Res.string.tts_description),
            style = DoraiTypography.bodyMedium,
            color = LocalDesignColorTokens.current.textSecondary,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
        )

        Text(text = stringResource(Res.string.tts_value_format, stringResource(Res.string.tts_speech_speed), (rate * 100).roundToInt()) + "%", style = DoraiTypography.labelLarge, color = LocalDesignColorTokens.current.textPrimary)
        DoraiSlider(
            value = rate,
            onValueChange = { rate = it },
            valueRange = 0.7f..1.3f,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(8.dp))
        Text(text = stringResource(Res.string.tts_value_format, stringResource(Res.string.tts_voice_pitch), (pitch * 100).roundToInt()) + "%", style = DoraiTypography.labelLarge, color = LocalDesignColorTokens.current.textPrimary)
        DoraiSlider(
            value = pitch,
            onValueChange = { pitch = it },
            valueRange = 0.8f..1.2f,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(8.dp))
        Text(text = stringResource(Res.string.tts_value_format, stringResource(Res.string.tts_volume), (volume * 100).roundToInt()) + "%", style = DoraiTypography.labelLarge, color = LocalDesignColorTokens.current.textPrimary)
        DoraiSlider(
            value = volume,
            onValueChange = { volume = it },
            valueRange = 0.0f..1.0f,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(Res.string.tts_tip),
            style = DoraiTypography.bodySmall,
            color = LocalDesignColorTokens.current.textSecondary,
        )
    }
}
