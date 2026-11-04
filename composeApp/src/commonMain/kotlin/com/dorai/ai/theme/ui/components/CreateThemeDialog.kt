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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dorai.ai.theme.data.isValidHexColor
import com.dorai.ai.theme.data.parseHexColor
import com.dorai.ai.ui.DoraiShapes
import com.dorai.ai.ui.DoraiTypography
import com.dorai.ai.ui.theme.LocalDesignColorTokens
import dorai.composeapp.generated.resources.create_theme_save
import dorai.composeapp.generated.resources.create_theme_background
import dorai.composeapp.generated.resources.create_theme_cancel
import dorai.composeapp.generated.resources.create_theme_invalid_hex
import dorai.composeapp.generated.resources.create_theme_name
import dorai.composeapp.generated.resources.create_theme_name_hint
import dorai.composeapp.generated.resources.create_theme_primary
import dorai.composeapp.generated.resources.create_theme_preview
import dorai.composeapp.generated.resources.create_theme_surface
import dorai.composeapp.generated.resources.create_theme_title
import dorai.composeapp.generated.resources.Res
import org.jetbrains.compose.resources.stringResource

/**
 * Builder dialog for a user-created theme. The user names the theme and enters three opaque hex
 * colors (background, surface, primary). Every field shows a live RGB readout for hex/RGB
 * parity; invalid input is flagged inline and Save stays disabled until all three parse.
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
    val canSave = backgroundValid && surfaceValid && primaryValid

    val tokens = LocalDesignColorTokens.current
    val previewBackground = parseHexColor(backgroundHex) ?: Color.Black
    val previewSurface = parseHexColor(surfaceHex) ?: Color.DarkGray
    val previewPrimary = parseHexColor(primaryHex) ?: Color.Cyan

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
                // Live swatch preview of the three chosen colors.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(74.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(previewBackground)
                        .border(1.dp, previewPrimary.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(10.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.65f)
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(previewSurface),
                        )
                        Box(
                            modifier = Modifier
                                .width(72.dp)
                                .height(16.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(previewPrimary),
                        )
                    }
                }
                Text(
                    text = stringResource(Res.string.create_theme_preview),
                    style = DoraiTypography.labelSmall,
                    color = tokens.textTertiary,
                )

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
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, backgroundHex, surfaceHex, primaryHex) },
                enabled = canSave,
                shape = RoundedCornerShape(999.dp),
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
                Text(rgbReadoutOf(value), color = tokens.textTertiary)
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
