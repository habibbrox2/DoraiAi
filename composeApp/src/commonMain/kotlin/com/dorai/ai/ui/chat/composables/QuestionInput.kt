package com.dorai.ai.ui.chat.composables

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.dorai.ai.Platform
import com.dorai.ai.currentPlatform
import com.dorai.ai.data.ServiceEntry
import com.dorai.ai.data.imageExtensions
import com.dorai.ai.prefersReducedMotion
import com.dorai.ai.skills.SkillManifest
import com.dorai.ai.ui.DoraiShapes
import com.dorai.ai.ui.chat.ChatSpacing
import com.dorai.ai.ui.handCursor
import com.dorai.ai.ui.outlineTextFieldColors
import com.dorai.ai.ui.settings.SettingsModel
import com.dorai.ai.ui.theme.DesignTokens
import com.dorai.ai.ui.theme.LocalDesignColorTokens
import dorai.composeapp.generated.resources.Res
import dorai.composeapp.generated.resources.ic_attach
import dorai.composeapp.generated.resources.ic_file
import dorai.composeapp.generated.resources.ic_image
import dorai.composeapp.generated.resources.ic_stop
import dorai.composeapp.generated.resources.ic_up
import dorai.composeapp.generated.resources.prompt_ask_question
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.extension
import io.github.vinceglb.filekit.name
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource

/**
 * Detects slash-command query string the user is currently typing, or null if
 * the cursor isn't inside a leading `/<token>`.
 */
internal fun detectSlashQuery(text: String, cursor: Int): String? {
    if (!text.startsWith('/')) return null
    val firstSpace = text.indexOfFirst { it.isWhitespace() }
    val tokenEnd = if (firstSpace < 0) text.length else firstSpace
    if (cursor > tokenEnd) return null
    return text.substring(1, tokenEnd).lowercase()
}

/**
 * Shortens a filename that is too long to display in a chip.
 * Returns the first [maxChars] characters of the base name followed by `�` and the original extension.
 */
fun truncateFileName(name: String, maxChars: Int = 16): String {
    if (name.length <= maxChars) return name
    val dotIndex = name.lastIndexOf('.')
    return if (dotIndex > 0 && dotIndex < name.length - 1) {
        val base = name.substring(0, dotIndex)
        val ext = name.substring(dotIndex) // includes the dot
        val keep = (maxChars - ext.length - 1).coerceAtLeast(1)
        "${base.take(keep)}…$ext"
    } else {
        "${name.take(maxChars - 1)}…"
    }
}

@Composable
internal fun TrailingIcon(
    icon: org.jetbrains.compose.resources.DrawableResource = Res.drawable.ic_up,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPulsing: Boolean = false,
    isStopAction: Boolean = false,
    contentDescription: String,
) {
    val prefersReducedMotion = prefersReducedMotion()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed && !prefersReducedMotion) 0.95f else 1f,
        animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
        label = "sendButtonPressScale",
    )
    val pulseModifier = if (isPulsing && !prefersReducedMotion) {
        val infiniteTransition = rememberInfiniteTransition()
        val pulseScale by infiniteTransition.animateFloat(
            initialValue = 0.92f,
            targetValue = 1.05f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 650, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        )
        Modifier.graphicsLayer {
            scaleX = pulseScale
            scaleY = pulseScale
        }
    } else {
        Modifier
    }

    val gradientBrush = if (isStopAction) {
        Brush.linearGradient(
            colors = listOf(
                DesignTokens.Error,
                DesignTokens.Error.copy(alpha = 0.8f),
            ),
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                DesignTokens.BrandCyan,
                DesignTokens.BrandBlue,
            ),
        )
    }

    Box(
        modifier = modifier
            .size(48.dp)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .shadow(
                elevation = 8.dp,
                shape = CircleShape,
                ambientColor = if (isStopAction) DesignTokens.Error.copy(alpha = 0.3f) else DesignTokens.BrandCyan.copy(alpha = 0.3f),
                spotColor = if (isStopAction) DesignTokens.Error.copy(alpha = 0.4f) else DesignTokens.BrandBlue.copy(alpha = 0.4f),
            )
            .clip(CircleShape)
            .background(brush = gradientBrush, shape = CircleShape)
            .border(
                1.dp,
                if (isStopAction) DesignTokens.Error.copy(alpha = 0.6f) else DesignTokens.BrandCyan.copy(alpha = 0.6f),
                CircleShape,
            )
            .handCursor()
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                role = Role.Button,
            ) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            vectorResource(icon),
            modifier = Modifier.size(24.dp).then(pulseModifier),
            contentDescription = contentDescription,
            tint = if (isStopAction) Color.White else DesignTokens.BrandNavy,
        )
    }
}

@Composable
internal fun CircleIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = LocalDesignColorTokens.current.textSecondary,
    contentDescription: String,
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable { onClick() }
            .handCursor(),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            modifier = Modifier.size(22.dp),
            contentDescription = contentDescription,
            tint = tint,
        )
    }
}

/**
 * 2027 Universal Multimodal Composer.
 *
 * Implements Phase 3 specifications from docs/ui-upgrade-plan-7-premium-chat.md.
 * Features auto-growing pill, rich attachment preview chips, slash commands integration,
 * fluid send/stop morph, and desktop Enter/Shift+Enter key handling.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuestionInput(
    files: ImmutableList<PlatformFile>,
    addFile: (PlatformFile) -> Unit,
    removeFile: (PlatformFile) -> Unit,
    ask: (String) -> Unit,
    supportedFileExtensions: ImmutableList<String>,
    textState: TextFieldValue,
    onTextStateChange: (TextFieldValue) -> Unit,
    isLoading: Boolean = false,
    cancel: () -> Unit = {},
    availableServices: ImmutableList<ServiceEntry> = persistentListOf(),
    onSelectService: (String) -> Unit = {},
    availableModels: ImmutableList<SettingsModel> = persistentListOf(),
    isLoadingModels: Boolean = false,
    onSelectModel: (String) -> Unit = {},
    onLoadModels: () -> Unit = {},
    installedSkills: ImmutableList<SkillManifest> = persistentListOf(),
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        // Slash autocomplete popup
        if (installedSkills.isNotEmpty()) {
            val slashQuery = remember(textState.text, textState.selection) {
                detectSlashQuery(textState.text, textState.selection.start)
            }
            if (slashQuery != null) {
                SkillAutocomplete(
                    skills = installedSkills,
                    query = slashQuery,
                    onSelect = { skill ->
                        val text = textState.text
                        val firstSpace = text.indexOfFirst { it.isWhitespace() }
                        val rest = if (firstSpace < 0) "" else text.substring(firstSpace)
                        val newText = "/${skill.id}$rest"
                        val cursor = ("/" + skill.id + " ").length
                        onTextStateChange(
                            TextFieldValue(
                                text = if (rest.isEmpty()) "/${skill.id} " else newText,
                                selection = TextRange(cursor.coerceAtMost(if (rest.isEmpty()) cursor else newText.length)),
                            ),
                        )
                    },
                )
                Spacer(Modifier.padding(top = 4.dp))
            }
        }

        // Multimodal Attachment Preview Strip
        if (files.isNotEmpty()) {
            FlowRow(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                for (file in files) {
                    key(file.name) {
                        val icon = if (file.extension.lowercase() in imageExtensions) {
                            Res.drawable.ic_image
                        } else {
                            Res.drawable.ic_file
                        }
                        SuggestionChip(
                            modifier = Modifier.handCursor(),
                            onClick = { removeFile(file) },
                            icon = {
                                Icon(
                                    modifier = Modifier.size(16.dp),
                                    painter = painterResource(icon),
                                    contentDescription = null,
                                    tint = DesignTokens.PrimaryCyan,
                                )
                            },
                            label = {
                                DisableSelection {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        Text(
                                            modifier = Modifier.handCursor(),
                                            text = truncateFileName(file.name),
                                            color = LocalDesignColorTokens.current.textPrimary,
                                        )
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove attachment",
                                            tint = LocalDesignColorTokens.current.textTertiary,
                                            modifier = Modifier.size(14.dp),
                                        )
                                    }
                                }
                            },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = LocalDesignColorTokens.current.surfaceRaised,
                            ),
                            border = SuggestionChipDefaults.suggestionChipBorder(
                                enabled = true,
                                borderColor = LocalDesignColorTokens.current.borderSubtle,
                            ),
                        )
                    }
                }
            }
        }

        fun submitQuestion() {
            val text = textState.text
            if (text.isNotBlank()) {
                ask(text.trim())
                onTextStateChange(TextFieldValue(""))
            }
        }

        val allowFileAttachment = supportedFileExtensions.isNotEmpty()
        val filePickerLauncher = if (allowFileAttachment) {
            rememberFilePickerLauncher(
                type = FileKitType.File(extensions = supportedFileExtensions),
            ) { file ->
                if (file != null) addFile(file)
            }
        } else {
            null
        }

        val focusRequester = remember { FocusRequester() }
        val maxComposerHeight = 240.dp
        val composerShape = RoundedCornerShape(28.dp)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = ChatSpacing.md, vertical = ChatSpacing.sm),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 56.dp)
                    .heightIn(min = 56.dp, max = maxComposerHeight)
                    .shadow(
                        elevation = 12.dp,
                        shape = composerShape,
                        ambientColor = DesignTokens.PrimaryCyan.copy(alpha = 0.14f),
                        spotColor = DesignTokens.AgentViolet.copy(alpha = 0.20f),
                    )
                    .clip(composerShape)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.94f), composerShape)
                    .border(BorderStroke(1.dp, DesignTokens.PrimaryCyan.copy(alpha = 0.48f)), composerShape),
            ) {
                TextField(
                    value = textState,
                    onValueChange = onTextStateChange,
                    modifier = Modifier
                        .focusRequester(focusRequester)
                        .defaultMinSize(minHeight = 56.dp)
                        .heightIn(min = 56.dp, max = maxComposerHeight)
                        .fillMaxWidth()
                        .onPreviewKeyEvent { keyEvent ->
                            if (currentPlatform !is Platform.Mobile &&
                                keyEvent.type == KeyEventType.KeyDown &&
                                keyEvent.key == Key.Enter &&
                                !keyEvent.isShiftPressed
                            ) {
                                submitQuestion()
                                true
                            } else {
                                false
                            }
                        },
                    colors = outlineTextFieldColors().copy(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                    ),
                    placeholder = {
                        Text(
                            stringResource(Res.string.prompt_ask_question),
                            color = LocalDesignColorTokens.current.textTertiary,
                        )
                    },
                    trailingIcon = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(end = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            if (availableServices.size > 1) {
                                ServiceSelector(
                                    services = availableServices,
                                    onSelectService = onSelectService,
                                    models = availableModels,
                                    isLoadingModels = isLoadingModels,
                                    onSelectModel = onSelectModel,
                                    onLoadModels = onLoadModels,
                                )
                            }
                            if (isLoading) {
                                TrailingIcon(
                                    icon = Res.drawable.ic_stop,
                                    onClick = cancel,
                                    isPulsing = true,
                                    isStopAction = true,
                                    contentDescription = "Cancel generation",
                                )
                            } else if (textState.text.isNotBlank()) {
                                TrailingIcon(
                                    icon = Res.drawable.ic_up,
                                    onClick = { submitQuestion() },
                                    isStopAction = false,
                                    contentDescription = "Send question",
                                )
                            }
                        }
                    },
                    keyboardActions = if (currentPlatform !is Platform.Mobile) {
                        KeyboardActions(onSend = { submitQuestion() })
                    } else {
                        KeyboardActions()
                    },
                    leadingIcon = if (filePickerLauncher != null) {
                        {
                            CircleIconButton(
                                icon = vectorResource(Res.drawable.ic_attach),
                                onClick = { filePickerLauncher.launch() },
                                modifier = Modifier.padding(start = 6.dp),
                                tint = LocalDesignColorTokens.current.textSecondary,
                                contentDescription = "Attach file",
                            )
                        }
                    } else {
                        null
                    },
                    keyboardOptions = KeyboardOptions(
                        imeAction = if (currentPlatform is Platform.Mobile) ImeAction.Default else ImeAction.Send,
                    ),
                )
            }
        }
    }
}
