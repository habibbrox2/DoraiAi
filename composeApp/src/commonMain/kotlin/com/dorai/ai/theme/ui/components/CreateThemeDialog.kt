package com.dorai.ai.theme.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dorai.ai.theme.data.contrastRatio
import com.dorai.ai.theme.data.isValidHexColor
import com.dorai.ai.theme.data.parseHexColor
import com.dorai.ai.theme.data.readableTextColorFor
import com.dorai.ai.ui.DoraiShapes
import com.dorai.ai.ui.DoraiTypography
import com.dorai.ai.ui.theme.LocalDesignColorTokens
import dorai.composeapp.generated.resources.Res
import dorai.composeapp.generated.resources.create_theme_background
import dorai.composeapp.generated.resources.create_theme_cancel
import dorai.composeapp.generated.resources.create_theme_contrast_background_warning
import dorai.composeapp.generated.resources.create_theme_contrast_critical
import dorai.composeapp.generated.resources.create_theme_contrast_primary_warning
import dorai.composeapp.generated.resources.create_theme_contrast_warning
import dorai.composeapp.generated.resources.create_theme_invalid_hex
import dorai.composeapp.generated.resources.create_theme_name
import dorai.composeapp.generated.resources.create_theme_name_hint
import dorai.composeapp.generated.resources.create_theme_preview
import dorai.composeapp.generated.resources.create_theme_primary
import dorai.composeapp.generated.resources.create_theme_save
import dorai.composeapp.generated.resources.create_theme_surface
import dorai.composeapp.generated.resources.create_theme_title
import org.jetbrains.compose.resources.stringResource

/**
 * Builder dialog for a user-created theme. The user names the theme and enters three opaque hex
 * colors (background, surface, primary). Every field shows a live RGB readout for hex/RGB
 * parity; invalid input is flagged inline and Save stays disabled until all three parse.
 *
 * Color contrast between onPrimary/primary and onBackground/background is validated against
 * WCAG AA guidelines to prevent unreadable button and text combinations.
 *
 * @param onSave invoked with (name, background, surface, primary); the dialog dismisses itself.
 */
@Composable
fun CreateThemeDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, background: String, surface: String, primary: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var name by remember { mutableStateOf("") }
    var backgroundHex by remember { mutableStateOf("#14082B") }
    var surfaceHex by remember { mutableStateOf("#1E1038") }
    var primaryHex by remember { mutableStateOf("#FF2E9A") }

    val backgroundValid = isValidHexColor(backgroundHex)
    val surfaceValid = isValidHexColor(surfaceHex)
    val primaryValid = isValidHexColor(primaryHex)

    val tokens = LocalDesignColorTokens.current
    val previewBackground = parseHexColor(backgroundHex) ?: Color.Black
    val previewSurface = parseHexColor(surfaceHex) ?: Color.DarkGray
    val previewPrimary = parseHexColor(primaryHex) ?: Color.Cyan

    val onPrimary = readableTextColorFor(previewPrimary)
    val onBackground = readableTextColorFor(previewBackground)

    val onPrimaryContrast = if (primaryValid) contrastRatio(onPrimary, previewPrimary) else null
    val onBackgroundContrast = if (backgroundValid) contrastRatio(onBackground, previewBackground) else null

    val isPrimaryContrastCritical = onPrimaryContrast != null && onPrimaryContrast < 3.0
    val isPrimaryContrastLow = onPrimaryContrast != null && onPrimaryContrast < 4.5
    val isBackgroundContrastLow = onBackgroundContrast != null && onBackgroundContrast < 4.5

    val hasContrastWarning = isPrimaryContrastLow || isBackgroundContrastLow
    val hasCriticalContrastFailure = isPrimaryContrastCritical || (onBackgroundContrast != null && onBackgroundContrast < 3.0)

    val canSave = backgroundValid && surfaceValid && primaryValid && !hasCriticalContrastFailure

    val contrastWarningMessage = when {
        hasCriticalContrastFailure -> stringResource(Res.string.create_theme_contrast_critical)
        isPrimaryContrastLow && isBackgroundContrastLow -> stringResource(Res.string.create_theme_contrast_warning)
        isPrimaryContrastLow -> stringResource(Res.string.create_theme_contrast_primary_warning)
        isBackgroundContrastLow -> stringResource(Res.string.create_theme_contrast_background_warning)
        else -> null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = stringResource(Res.string.create_theme_title),
                style = DoraiTypography.titleLarge,
                color = tokens.textPrimary,
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // Live swatch preview of the three chosen colors with real text contrast.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(previewBackground)
                        .border(1.dp, previewPrimary.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(10.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth(0.7f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(previewSurface)
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = name.trim().ifEmpty { stringResource(Res.string.create_theme_name_hint) },
                                style = DoraiTypography.labelSmall,
                                color = onBackground,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(DoraiShapes.pill)
                                .background(previewPrimary)
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = stringResource(Res.string.create_theme_save),
                                style = DoraiTypography.labelSmall,
                                color = onPrimary,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
                Text(
                    text = stringResource(Res.string.create_theme_preview),
                    style = DoraiTypography.labelSmall,
                    color = tokens.textTertiary,
                )

                if (contrastWarningMessage != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f))
                            .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = contrastWarningMessage,
                            style = DoraiTypography.bodySmall,
                            color = tokens.textPrimary,
                        )
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(Res.string.create_theme_name)) },
                    placeholder = { Text(stringResource(Res.string.create_theme_name_hint)) },
                    singleLine = true,
                    shape = DoraiShapes.row,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = tokens.textPrimary,
                        unfocusedTextColor = tokens.textPrimary,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = tokens.textSecondary,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                HexColorField(
                    value = backgroundHex,
                    onValueChange = { backgroundHex = it },
                    label = stringResource(Res.string.create_theme_background),
                    isValid = backgroundValid,
                    contrastRatio = onBackgroundContrast,
                    isContrastWarning = isBackgroundContrastLow,
                )
                HexColorField(
                    value = surfaceHex,
                    onValueChange = { surfaceHex = it },
                    label = stringResource(Res.string.create_theme_surface),
                    isValid = surfaceValid,
                )
                HexColorField(
                    value = primaryHex,
                    onValueChange = { primaryHex = it },
                    label = stringResource(Res.string.create_theme_primary),
                    isValid = primaryValid,
                    contrastRatio = onPrimaryContrast,
                    isContrastWarning = isPrimaryContrastLow,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, backgroundHex, surfaceHex, primaryHex) },
                enabled = canSave,
                shape = DoraiShapes.pill,
            ) {
                Text(stringResource(Res.string.create_theme_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.create_theme_cancel))
            }
        },
    )
}

@Composable
private fun HexColorField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isValid: Boolean,
    contrastRatio: Double? = null,
    isContrastWarning: Boolean = false,
) {
    val tokens = LocalDesignColorTokens.current
    val errorColor = MaterialTheme.colorScheme.error
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it.uppercase().take(9)) },
        label = { Text(label) },
        placeholder = { Text("#1E1E2E") },
        singleLine = true,
        shape = DoraiShapes.row,
        isError = value.isNotBlank() && !isValid,
        supportingText = {
            if (value.isNotBlank() && !isValid) {
                Text(stringResource(Res.string.create_theme_invalid_hex), color = errorColor)
            } else if (isValid) {
                val rgb = rgbReadoutOf(value)
                val ratioText = if (contrastRatio != null) {
                    val formatted = ((contrastRatio * 10).toInt() / 10.0).toString()
                    " Ã‚Â· Contrast $formatted:1"
                } else {
                    ""
                }
                val textColor = if (isContrastWarning) errorColor else tokens.textTertiary
                Text(rgb + ratioText, color = textColor)
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = tokens.textPrimary,
            unfocusedTextColor = tokens.textPrimary,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            focusedLabelColor = MaterialTheme.colorScheme.primary,
            unfocusedLabelColor = tokens.textSecondary,
            errorSupportingTextColor = errorColor,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}

/** Human-readable `RGB r, g, b` readout for a valid hex string. */
private fun rgbReadoutOf(hex: String): String {
    val color = parseHexColor(hex) ?: return ""
    fun channel(v: Float): Int = (v.coerceIn(0f, 1f) * 255f).toInt()
    return "RGB ${channel(color.red)}, ${channel(color.green)}, ${channel(color.blue)}"
}
