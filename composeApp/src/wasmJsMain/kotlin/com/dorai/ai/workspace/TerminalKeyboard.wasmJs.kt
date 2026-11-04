package com.dorai.ai.workspace

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.dorai.ai.workspace.terminal.TerminalKey
import com.dorai.ai.workspace.terminal.TerminalModifiers

/** Workspace is Android-only; there is no environment to type into here. */
actual val supportsRawTerminalInput: Boolean = false

@Composable
actual fun PlatformTerminalKeyboard(
    showKeyboardRequest: Int,
    onKey: (TerminalKey, TerminalModifiers) -> Unit,
    onText: (String, TerminalModifiers) -> Unit,
    modifier: Modifier,
) = Unit
