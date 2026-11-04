package com.dorai.ai.ui.chat.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dorai.ai.ui.DoraiTypography
import com.dorai.ai.ui.handCursor
import com.dorai.ai.ui.rememberCopyToClipboard
import androidx.compose.material3.MaterialTheme
import com.dorai.ai.ui.theme.DesignTokens
import com.dorai.ai.ui.theme.LocalDesignColorTokens
import kotlinx.coroutines.delay

/**
 * ChatGPT-style Shell & Command Execution Window.
 * Features:
 * - macOS / Terminal styled window bar with 3 dots (Red, Yellow, Green)
 * - Command prompt "$ <command>"
 * - Live execution status pill (Running..., Exit 0, Exit 1)
 * - Copy command button with checkmark feedback
 * - Deep dark terminal canvas (#0A0D14)
 * - Monospace stdout & stderr output with collapsible line preview
 */
@Composable
fun ShellExecutionCard(
    command: String,
    modifier: Modifier = Modifier,
    workingDirectory: String? = null,
    stdout: String? = null,
    stderr: String? = null,
    exitCode: Int? = null,
    isRunning: Boolean = false,
) {
    val copyToClipboard = rememberCopyToClipboard()
    var isCopied by remember { mutableStateOf(false) }
    var isExpanded by remember { mutableStateOf(!stdout.isNullOrBlank() || !stderr.isNullOrBlank()) }
    var showAllLines by remember { mutableStateOf(false) }

    LaunchedEffect(isCopied) {
        if (isCopied) {
            delay(2000)
            isCopied = false
        }
    }

    val terminalShape = RoundedCornerShape(12.dp)
    val terminalBg = DesignTokens.TerminalBackground
    val headerBg = LocalDesignColorTokens.current.surfaceElevated
    val borderColor = LocalDesignColorTokens.current.borderSubtle
    val textSlate = LocalDesignColorTokens.current.textPrimary
    val textMuted = LocalDesignColorTokens.current.textMuted
    val accentCyan = Color(0xFF38BDF8)
    val successGreen = DesignTokens.Success
    val errorRed = DesignTokens.Error

    val cleanOutput = stdout?.trimEnd().orEmpty()
    val cleanError = stderr?.trimEnd().orEmpty()
    val outputLines = remember(cleanOutput) {
        if (cleanOutput.isEmpty()) emptyList() else cleanOutput.lines()
    }
    val hasLongOutput = outputLines.size > 8

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(terminalShape)
                .background(terminalBg)
                .border(1.dp, borderColor, terminalShape),
        ) {
            // Terminal Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(headerBg)
                    .clickable { isExpanded = !isExpanded }
                    .handCursor()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                // Window dots + Title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // 3 window dots
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Box(Modifier.size(9.dp).clip(CircleShape).background(Color(0xFFFF5F56)))
                        Box(Modifier.size(9.dp).clip(CircleShape).background(Color(0xFFFFBD2E)))
                        Box(Modifier.size(9.dp).clip(CircleShape).background(Color(0xFF27C93F)))
                    }
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.Terminal,
                        contentDescription = null,
                        tint = accentCyan,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = "Terminal / Shell",
                        style = DoraiTypography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        fontFamily = FontFamily.Monospace,
                        color = textSlate,
                    )
                }

                // Status pill + Copy + Chevron
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (isRunning) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(accentCyan.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                        ) {
                            AnimatedTypingDots(dotColor = accentCyan)
                            Text(
                                text = "Running...",
                                style = DoraiTypography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                color = accentCyan,
                                fontSize = 11.sp,
                            )
                        }
                    } else if (exitCode != null) {
                        val isSuccess = exitCode == 0
                        val statusBg = if (isSuccess) successGreen.copy(alpha = 0.15f) else errorRed.copy(alpha = 0.15f)
                        val statusColor = if (isSuccess) successGreen else errorRed
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(statusBg)
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                        ) {
                            Text(
                                text = if (isSuccess) "? Exit 0" else "? Exit $exitCode",
                                style = DoraiTypography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                fontFamily = FontFamily.Monospace,
                                color = statusColor,
                                fontSize = 11.sp,
                            )
                        }
                    }

                    // Copy command
                    if (isCopied) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            modifier = Modifier.padding(horizontal = 4.dp),
                        ) {
                            Icon(Icons.Default.Check, contentDescription = "Copied", tint = successGreen, modifier = Modifier.size(13.dp))
                            Text("Copied", style = DoraiTypography.labelSmall, fontFamily = FontFamily.Monospace, color = successGreen, fontSize = 11.sp)
                        }
                    } else {
                        IconButton(
                            onClick = {
                                copyToClipboard(command)
                                isCopied = true
                            },
                            modifier = Modifier.size(24.dp).handCursor(),
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy command", tint = textMuted, modifier = Modifier.size(13.dp))
                        }
                    }

                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = textMuted,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            // Command Prompt Row
            SelectionContainer {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    if (!workingDirectory.isNullOrBlank()) {
                        Text(
                            text = "?? cwd: $workingDirectory",
                            style = DoraiTypography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = textMuted,
                            modifier = Modifier.padding(bottom = 3.dp),
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = "$",
                            style = DoraiTypography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            fontFamily = FontFamily.Monospace,
                            color = accentCyan,
                        )
                        Text(
                            text = command,
                            style = DoraiTypography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            fontFamily = FontFamily.Monospace,
                            color = LocalDesignColorTokens.current.textPrimary,
                        )
                    }
                }
            }

            // Expandable Output Body
            AnimatedVisibility(
                visible = isExpanded && (cleanOutput.isNotEmpty() || cleanError.isNotEmpty()),
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .border(
                            width = 1.dp,
                            color = borderColor.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp),
                        )
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                ) {
                    if (cleanOutput.isNotEmpty()) {
                        SelectionContainer {
                            val displayedOutput = if (hasLongOutput && !showAllLines) {
                                outputLines.take(8).joinToString("\n")
                            } else {
                                cleanOutput
                            }
                            Text(
                                text = displayedOutput,
                                style = DoraiTypography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = textSlate,
                                lineHeight = 18.sp,
                            )
                        }

                        if (hasLongOutput) {
                            Text(
                                text = if (showAllLines) "? Show less" else "? Show all ${outputLines.size} lines",
                                style = DoraiTypography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                fontFamily = FontFamily.Monospace,
                                color = accentCyan,
                                modifier = Modifier
                                    .padding(top = 8.dp)
                                    .clickable { showAllLines = !showAllLines }
                                    .handCursor(),
                            )
                        }
                    }

                    if (cleanError.isNotEmpty()) {
                        Spacer(Modifier.height(6.dp))
                        SelectionContainer {
                            Text(
                                text = cleanError,
                                style = DoraiTypography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = errorRed,
                                lineHeight = 18.sp,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * ChatGPT-style General Tool Calling Card.
 * Used for tools like web search, file operations, weather, reminders, etc.
 */
@Composable
fun ToolExecutionCard(
    toolName: String,
    modifier: Modifier = Modifier,
    arguments: String? = null,
    result: String? = null,
    isRunning: Boolean = false,
    isError: Boolean = false,
    initiallyExpanded: Boolean = false,
) {
    val copyToClipboard = rememberCopyToClipboard()
    var isCopied by remember { mutableStateOf(false) }
    var isExpanded by remember(initiallyExpanded) { mutableStateOf(initiallyExpanded) }

    LaunchedEffect(isCopied) {
        if (isCopied) {
            delay(2000)
            isCopied = false
        }
    }

    val cardShape = RoundedCornerShape(DesignTokens.RadiusCard)
    val cardBg = LocalDesignColorTokens.current.surfaceElevated
    val borderColor = LocalDesignColorTokens.current.borderSubtle
    val textSlate = LocalDesignColorTokens.current.textPrimary
    val textMuted = LocalDesignColorTokens.current.textMuted
    val accentTeal = DesignTokens.PrimaryCyan
    val successGreen = DesignTokens.Success
    val errorRed = DesignTokens.Error

    val toolIcon = when {
        toolName.contains("search", ignoreCase = true) -> "??"
        toolName.contains("file", ignoreCase = true) -> "??"
        toolName.contains("weather", ignoreCase = true) -> "??"
        toolName.contains("reminder", ignoreCase = true) || toolName.contains("clock", ignoreCase = true) -> "?"
        toolName.contains("web", ignoreCase = true) || toolName.contains("url", ignoreCase = true) -> "??"
        else -> "??"
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(cardShape)
                .background(cardBg)
                .border(1.dp, borderColor, cardShape),
        ) {
            // Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .handCursor()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f, fill = false),
                ) {
                    Text(text = toolIcon, style = DoraiTypography.titleSmall)
                    Text(
                        text = "Tool call",
                        style = DoraiTypography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        fontFamily = FontFamily.Monospace,
                        color = textSlate,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (isRunning) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(accentTeal.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        ) {
                            AnimatedTypingDots(dotColor = accentTeal)
                            Text(
                                text = "Running",
                                style = DoraiTypography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                color = accentTeal,
                                fontSize = 11.sp,
                            )
                        }
                    } else {
                        val statusText = if (isError) "Failed" else "Done"
                        val statusColor = if (isError) errorRed else successGreen
                        Text(
                            text = if (isError) "? $statusText" else "? $statusText",
                            style = DoraiTypography.labelSmall.copy(fontWeight = FontWeight.Medium),
                            fontFamily = FontFamily.Monospace,
                            color = statusColor,
                            fontSize = 11.sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(statusColor.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }

                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = textMuted,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            // Expanded Arguments & Result
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (!arguments.isNullOrBlank()) {
                        Column {
                            Text(
                                text = "Input:",
                                style = DoraiTypography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                fontFamily = FontFamily.Monospace,
                                color = textMuted,
                            )
                            SelectionContainer {
                                Text(
                                    text = arguments,
                                    style = DoraiTypography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = LocalDesignColorTokens.current.textSecondary,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(LocalDesignColorTokens.current.surfaceRaised)
                                        .padding(8.dp),
                                )
                            }
                        }
                    }

                    if (!result.isNullOrBlank()) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "Output:",
                                    style = DoraiTypography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    fontFamily = FontFamily.Monospace,
                                    color = textMuted,
                                )
                                if (isCopied) {
                                    Text("Copied!", style = DoraiTypography.labelSmall, color = successGreen, fontSize = 11.sp)
                                } else {
                                    IconButton(
                                        onClick = {
                                            copyToClipboard(result)
                                            isCopied = true
                                        },
                                        modifier = Modifier.size(20.dp).handCursor(),
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy output", tint = textMuted, modifier = Modifier.size(12.dp))
                                    }
                                }
                            }
                            SelectionContainer {
                                Text(
                                    text = result,
                                    style = DoraiTypography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = textSlate,
                                    maxLines = 15,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(LocalDesignColorTokens.current.surfaceRaised)
                                        .padding(8.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
