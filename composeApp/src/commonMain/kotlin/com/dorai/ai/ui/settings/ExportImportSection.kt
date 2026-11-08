package com.dorai.ai.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dorai.ai.data.ImportSection
import com.dorai.ai.data.SharedJson
import com.dorai.ai.data.detectImportSections
import com.dorai.ai.saveFileToDevice
import com.dorai.ai.ui.AccentPrimary
import com.dorai.ai.ui.DangerAlert
import com.dorai.ai.ui.DoraiTypography
import com.dorai.ai.ui.components.VerticalScrollbarForScroll
import com.dorai.ai.ui.handCursor
import com.dorai.ai.ui.theme.DesignTokens
import com.dorai.ai.ui.theme.LocalDesignColorTokens
import dorai.composeapp.generated.resources.Res
import dorai.composeapp.generated.resources.settings_export
import dorai.composeapp.generated.resources.settings_export_import_description
import dorai.composeapp.generated.resources.settings_export_import_title
import dorai.composeapp.generated.resources.settings_export_preview_title
import dorai.composeapp.generated.resources.settings_import
import dorai.composeapp.generated.resources.settings_import_error
import dorai.composeapp.generated.resources.settings_import_partial
import dorai.composeapp.generated.resources.settings_import_preview_title
import dorai.composeapp.generated.resources.settings_import_replace_all
import dorai.composeapp.generated.resources.settings_import_replace_all_description
import dorai.composeapp.generated.resources.settings_import_section_conversations
import dorai.composeapp.generated.resources.settings_import_section_email
import dorai.composeapp.generated.resources.settings_import_section_heartbeat
import dorai.composeapp.generated.resources.settings_import_section_mcp
import dorai.composeapp.generated.resources.settings_import_section_memory
import dorai.composeapp.generated.resources.settings_import_section_scheduling
import dorai.composeapp.generated.resources.settings_import_section_services
import dorai.composeapp.generated.resources.settings_import_section_soul
import dorai.composeapp.generated.resources.settings_import_section_tools
import dorai.composeapp.generated.resources.settings_import_success
import dorai.composeapp.generated.resources.settings_mcp_cancel
import dorai.composeapp.generated.resources.settings_sms
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.readBytes
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.launch
import kotlinx.serialization.json.jsonObject
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ExportImportSection(
    onExportSettings: (Set<ImportSection>) -> String,
    onPrepareExport: () -> Map<ImportSection, String?>,
    onImportSettings: (ByteArray, Set<ImportSection>, Boolean) -> ImportResult,
) {
    val isPreview = LocalInspectionMode.current
    val scope = rememberCoroutineScope()
    var importResult by remember { mutableStateOf<ImportResult?>(null) }
    var importPreview by remember { mutableStateOf<Pair<String, ImmutableMap<ImportSection, String?>>?>(null) }
    var exportPreview by remember { mutableStateOf<ImmutableMap<ImportSection, String?>?>(null) }

    val filePickerLauncher = if (!isPreview) {
        rememberFilePickerLauncher(
            type = FileKitType.File(extensions = listOf("json")),
        ) { file ->
            if (file != null) {
                scope.launch {
                    val bytes = file.readBytes()
                    try {
                        val jsonString = bytes.decodeToString()
                        val jsonObject = SharedJson.parseToJsonElement(jsonString).jsonObject
                        val detectedSections = detectImportSections(jsonObject).toImmutableMap()
                        importPreview = jsonString to detectedSections
                    } catch (_: Exception) {
                        importResult = ImportResult.Failure
                    }
                }
            }
        }
    } else {
        null
    }

    importPreview?.let { (jsonString, sectionDetails) ->
        ImportPreviewDialog(
            sectionDetails = sectionDetails,
            onConfirm = { selectedSections, replace ->
                importResult = onImportSettings(jsonString.encodeToByteArray(), selectedSections, replace)
                importPreview = null
            },
            onDismiss = { importPreview = null },
        )
    }

    exportPreview?.let { sectionDetails ->
        ExportPreviewDialog(
            sectionDetails = sectionDetails,
            onConfirm = { selectedSections ->
                val json = onExportSettings(selectedSections)
                exportPreview = null
                scope.launch {
                    saveFileToDevice(
                        bytes = json.encodeToByteArray(),
                        baseName = "dorai-settings",
                        extension = "json",
                    )
                }
            },
            onDismiss = { exportPreview = null },
        )
    }

    val colors = LocalDesignColorTokens.current
    val tileShape = RoundedCornerShape(12.dp)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column {
            Text(
                text = stringResource(Res.string.settings_export_import_title),
                style = DoraiTypography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = stringResource(Res.string.settings_export_import_description),
                style = DoraiTypography.bodySmall,
                color = colors.textSecondary,
            )
        }

        // Export Tile
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(tileShape)
                .background(colors.surfaceElevated)
                .border(1.dp, colors.borderSubtle, tileShape)
                .padding(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(DesignTokens.PrimaryCyan.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Upload,
                            contentDescription = null,
                            tint = DesignTokens.PrimaryCyan,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Column {
                        Text(
                            text = stringResource(Res.string.settings_export),
                            style = DoraiTypography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary,
                        )
                        Text(
                            text = "Save configuration to JSON file",
                            style = DoraiTypography.bodySmall,
                            color = colors.textSecondary,
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                OutlinedButton(
                    onClick = {
                        importResult = null
                        exportPreview = onPrepareExport().toImmutableMap()
                    },
                    modifier = Modifier.handCursor(),
                ) {
                    Text(stringResource(Res.string.settings_export))
                }
            }
        }

        // Import Tile
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(tileShape)
                .background(colors.surfaceElevated)
                .border(1.dp, colors.borderSubtle, tileShape)
                .padding(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(DesignTokens.PrimaryCyan.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            tint = DesignTokens.PrimaryCyan,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Column {
                        Text(
                            text = stringResource(Res.string.settings_import),
                            style = DoraiTypography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary,
                        )
                        Text(
                            text = "Restore configuration from backup",
                            style = DoraiTypography.bodySmall,
                            color = colors.textSecondary,
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                OutlinedButton(
                    onClick = {
                        importResult = null
                        filePickerLauncher?.launch()
                    },
                    modifier = Modifier.handCursor(),
                ) {
                    Text(stringResource(Res.string.settings_import))
                }
            }
        }

        // Import/Export Status Feedback
        importResult?.let { result ->
            val (text, color, icon) = when (result) {
                is ImportResult.Success -> Triple(
                    stringResource(Res.string.settings_import_success),
                    DesignTokens.Success,
                    Icons.Default.Check,
                )

                is ImportResult.PartialSuccess -> Triple(
                    stringResource(Res.string.settings_import_partial, result.errorCount),
                    DesignTokens.Warning,
                    Icons.Default.Check,
                )

                is ImportResult.Failure -> Triple(
                    stringResource(Res.string.settings_import_error),
                    DesignTokens.Error,
                    Icons.Default.ErrorOutline,
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(color.copy(alpha = 0.1f))
                    .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = text,
                        style = DoraiTypography.bodySmall,
                        color = color,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}

@Composable
private fun ImportPreviewDialog(
    sectionDetails: ImmutableMap<ImportSection, String?>,
    onConfirm: (Set<ImportSection>, Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var replace by remember { mutableStateOf(true) }
    var selectedSections by remember(sectionDetails) { mutableStateOf<Set<ImportSection>>(sectionDetails.keys) }
    val sortedEntries = remember(sectionDetails) { sectionDetails.entries.sortedBy { it.key } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(Res.string.settings_import_preview_title))
        },
        text = {
            val importScrollState = rememberScrollState()
            Box {
                Column(modifier = Modifier.verticalScroll(importScrollState)) {
                    Row(
                        verticalAlignment = CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { replace = !replace }
                            .handCursor(),
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(Res.string.settings_import_replace_all),
                                style = DoraiTypography.bodyMedium,
                            )
                            if (replace) {
                                Text(
                                    text = stringResource(Res.string.settings_import_replace_all_description),
                                    style = DoraiTypography.bodySmall,
                                    color = LocalDesignColorTokens.current.textSecondary,
                                )
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        Switch(
                            checked = replace,
                            onCheckedChange = { replace = it },
                            modifier = Modifier.handCursor(),
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    for ((section, count) in sortedEntries) {
                        Row(
                            verticalAlignment = CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedSections = if (section in selectedSections) {
                                        selectedSections - section
                                    } else {
                                        selectedSections + section
                                    }
                                }
                                .handCursor()
                                .padding(vertical = 4.dp),
                        ) {
                            Checkbox(
                                checked = section in selectedSections,
                                onCheckedChange = { checked ->
                                    selectedSections = if (checked) selectedSections + section else selectedSections - section
                                },
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = sectionDisplayName(section),
                                style = DoraiTypography.bodyMedium,
                            )
                            if (count != null) {
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "($count)",
                                    style = DoraiTypography.bodySmall,
                                    color = LocalDesignColorTokens.current.textSecondary,
                                )
                            }
                        }
                    }
                }
                VerticalScrollbarForScroll(
                    scrollState = importScrollState,
                    modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedSections, replace) },
                enabled = selectedSections.isNotEmpty(),
                modifier = Modifier.handCursor(),
            ) {
                Text(stringResource(Res.string.settings_import))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.handCursor(),
            ) {
                Text(stringResource(Res.string.settings_mcp_cancel))
            }
        },
    )
}

@Composable
private fun ExportPreviewDialog(
    sectionDetails: ImmutableMap<ImportSection, String?>,
    onConfirm: (Set<ImportSection>) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedSections by remember(sectionDetails) { mutableStateOf<Set<ImportSection>>(sectionDetails.keys) }
    val sortedEntries = remember(sectionDetails) { sectionDetails.entries.sortedBy { it.key } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(Res.string.settings_export_preview_title))
        },
        text = {
            val exportScrollState = rememberScrollState()
            Box {
                Column(modifier = Modifier.verticalScroll(exportScrollState)) {
                    for ((section, count) in sortedEntries) {
                        Row(
                            verticalAlignment = CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedSections = if (section in selectedSections) {
                                        selectedSections - section
                                    } else {
                                        selectedSections + section
                                    }
                                }
                                .handCursor()
                                .padding(vertical = 4.dp),
                        ) {
                            Checkbox(
                                checked = section in selectedSections,
                                onCheckedChange = { checked ->
                                    selectedSections = if (checked) selectedSections + section else selectedSections - section
                                },
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = sectionDisplayName(section),
                                style = DoraiTypography.bodyMedium,
                            )
                            if (count != null) {
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "($count)",
                                    style = DoraiTypography.bodySmall,
                                    color = LocalDesignColorTokens.current.textSecondary,
                                )
                            }
                        }
                    }
                }
                VerticalScrollbarForScroll(
                    scrollState = exportScrollState,
                    modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedSections) },
                enabled = selectedSections.isNotEmpty(),
                modifier = Modifier.handCursor(),
            ) {
                Text(stringResource(Res.string.settings_export))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.handCursor(),
            ) {
                Text(stringResource(Res.string.settings_mcp_cancel))
            }
        },
    )
}

@Composable
private fun sectionDisplayName(section: ImportSection): String = when (section) {
    ImportSection.SERVICES -> stringResource(Res.string.settings_import_section_services)
    ImportSection.SOUL -> stringResource(Res.string.settings_import_section_soul)
    ImportSection.MEMORY -> stringResource(Res.string.settings_import_section_memory)
    ImportSection.SCHEDULING -> stringResource(Res.string.settings_import_section_scheduling)
    ImportSection.HEARTBEAT -> stringResource(Res.string.settings_import_section_heartbeat)
    ImportSection.EMAIL -> stringResource(Res.string.settings_import_section_email)
    ImportSection.SMS -> stringResource(Res.string.settings_sms)
    ImportSection.SPLINTERLANDS -> "Splinterlands"
    ImportSection.TOOLS -> stringResource(Res.string.settings_import_section_tools)
    ImportSection.MCP -> stringResource(Res.string.settings_import_section_mcp)
    ImportSection.CONVERSATIONS -> stringResource(Res.string.settings_import_section_conversations)
}
