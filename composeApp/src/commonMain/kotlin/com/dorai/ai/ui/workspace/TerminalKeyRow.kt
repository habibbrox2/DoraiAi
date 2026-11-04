package com.dorai.ai.ui.workspace

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dorai.ai.ui.DoraiShapes
import com.dorai.ai.ui.handCursor
import com.dorai.ai.ui.theme.DesignTokens
import com.dorai.ai.ui.settings.monoStyle
import com.dorai.ai.workspace.terminal.TerminalKey
import com.dorai.ai.workspace.terminal.TerminalModifiers
import dorai.composeapp.generated.resources.Res
import dorai.composeapp.generated.resources.workspace_terminal_input_mode_content_description
import dorai.composeapp.generated.resources.workspace_terminal_key_down_content_description
import dorai.composeapp.generated.resources.workspace_terminal_key_enter_content_description
import dorai.composeapp.generated.resources.workspace_terminal_key_left_content_description
import dorai.composeapp.generated.resources.workspace_terminal_key_right_content_description
import dorai.composeapp.generated.resources.workspace_terminal_key_up_content_description
import org.jetbrains.compose.resources.stringResource

/** Navigation caps, in the order a keyboard lays them out. */
private val ArrowCaps = listOf(
    Triple(TerminalKey.Left, TerminalArrowLeft, Res.string.workspace_terminal_key_left_content_description),
    Triple(TerminalKey.Up, TerminalArrowUp, Res.string.workspace_terminal_key_up_content_description),
    Triple(TerminalKey.Down, TerminalArrowDown, Res.string.workspace_terminal_key_down_content_description),
    Triple(TerminalKey.Right, TerminalArrowRight, Res.string.workspace_terminal_key_right_content_description),
)

private val KeyCapHeight = 48.dp
private val KeyCapMinWidth = 48.dp

/** Icon caps hold one glyph, so they can be squarer than the lettered ones. */
private val IconKeyCapMinWidth = 48.dp

/** Enter is the row's action key and gets the width to say so. */
private val EnterKeyCapMinWidth = 52.dp
private val KeyCapFontSize = 13.sp
private val KeyCapShape = DoraiShapes.xs

/**
 * The keys no soft keyboard has. Ctrl/Alt/Shift latch for exactly one press —
 * tap Ctrl then C to interrupt — which is how every mobile terminal handles
 * modifiers that have no physical key to hold down.
 *
 * The latch is owned by the caller so it also applies to characters typed on
 * the soft keyboard, not just to presses from this row.
 *
 * Eleven caps are wider than a phone, so the row scrolls — which makes the
 * order a ranking. Ctrl, Esc, Tab and the arrows lead because no soft keyboard
 * offers them at all; Alt and Shift trail. Enter is pinned outside the scroll,
 * so the key that ends every command is never the one that has to be found.
 * A hairline between groups gives a thumb a landmark to aim at.
 *
 * The input-mode cap is pinned next to Enter rather than left in the input bar,
 * because that bar is hidden exactly when the switch is wanted most: while the
 * soft keyboard is up in keyboard mode. [onToggleInputMode] is null on platforms
 * that only have line input, where there is nothing to switch between.
 */
@Composable
internal fun TerminalKeyRow(
    enabled: Boolean,
    latched: TerminalModifiers,
    onLatchChange: (TerminalModifiers) -> Unit,
    onKey: (TerminalKey) -> Unit,
    modifier: Modifier = Modifier,
    rawInput: Boolean = false,
    onToggleInputMode: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(DesignTokens.SurfaceRaised)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // English key names on purpose — not localized. Shells, agent CLIs, and
            // docs all say Ctrl/Esc/Tab/Alt/Shift; matching that beats OS keycap
            // labels (e.g. German Strg) which would disagree with every prompt.
            KeyCap(
                label = "ctrl",
                enabled = enabled,
                active = latched.ctrl,
                onClick = { onLatchChange(latched.copy(ctrl = !latched.ctrl)) },
            )
            KeyCap(label = "esc", enabled = enabled, onClick = { onKey(TerminalKey.Escape) })
            KeyCap(label = "tab", enabled = enabled, onClick = { onKey(TerminalKey.Tab) })

            KeyGroupSeparator()

            ArrowCaps.forEach { (key, icon, description) ->
                IconKeyCap(
                    icon = icon,
                    contentDescription = stringResource(description),
                    enabled = enabled,
                    onClick = { onKey(key) },
                )
            }

            KeyGroupSeparator()

            KeyCap(
                label = "alt",
                enabled = enabled,
                active = latched.alt,
                onClick = { onLatchChange(latched.copy(alt = !latched.alt)) },
            )
            KeyCap(
                label = "shift",
                enabled = enabled,
                active = latched.shift,
                onClick = { onLatchChange(latched.copy(shift = !latched.shift)) },
            )
        }

        onToggleInputMode?.let { toggle ->
            IconKeyCap(
                // Shows where the tap leads, not where the terminal is now.
                icon = if (rawInput) Icons.Default.Edit else Icons.Default.Terminal,
                contentDescription = stringResource(
                    Res.string.workspace_terminal_input_mode_content_description,
                ),
                // A mode switch, not a key: it works on a session that has ended too.
                enabled = true,
                onClick = toggle,
            )
        }

        IconKeyCap(
            icon = TerminalEnter,
            contentDescription = stringResource(Res.string.workspace_terminal_key_enter_content_description),
            enabled = enabled,
            accent = true,
            iconSize = TerminalEnterIconSize,
            minWidth = EnterKeyCapMinWidth,
            onClick = { onKey(TerminalKey.Enter) },
        )
    }
}

@Composable
private fun KeyGroupSeparator(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(18.dp)
            .width(1.dp)
            .background(Color.White.copy(alpha = 0.10f)),
    )
}

@Composable
private fun KeyCap(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false,
) {
    KeyCapSurface(
        enabled = enabled,
        onClick = onClick,
        modifier = modifier,
        active = active,
        contentDescription = label,
    ) { tint ->
        Text(text = label, style = monoStyle(KeyCapFontSize, tint))
    }
}

@Composable
private fun IconKeyCap(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Boolean = false,
    iconSize: Dp = TerminalKeyIconSize,
    minWidth: Dp = IconKeyCapMinWidth,
) {
    KeyCapSurface(
        enabled = enabled,
        onClick = onClick,
        modifier = modifier,
        accent = accent,
        minWidth = minWidth,
        contentDescription = contentDescription,
    ) { tint ->
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(iconSize),
        )
    }
}

@Composable
private fun KeyCapSurface(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    accent: Boolean = false,
    minWidth: Dp = KeyCapMinWidth,
    contentDescription: String? = null,
    content: @Composable (tint: Color) -> Unit,
) {
    val tint = when {
        !enabled -> AnsiPalette[8]

        // Enter is a filled key, so its glyph is the bright thing on the fill,
        // not a green line on grey — that read as decoration rather than a key.
        accent -> AnsiPalette[15]

        active -> AnsiPalette[10]

        else -> AnsiPalette[7]
    }
    val container = when {
        accent && enabled -> AnsiPalette[10].copy(alpha = 0.25f)
        active -> AnsiPalette[10].copy(alpha = 0.22f)
        else -> Color(0xFF262626)
    }

    val buttonColors = ButtonDefaults.buttonColors(
        containerColor = container,
        contentColor = tint,
        disabledContainerColor = container.copy(alpha = 0.5f),
        disabledContentColor = tint.copy(alpha = 0.5f),
    )

    val semanticsModifier = if (contentDescription != null) {
        Modifier.semantics { this.contentDescription = contentDescription }
    } else {
        Modifier
    }

    val focusRequester = remember { FocusRequester() }
    var isFocused by remember { mutableStateOf(false) }

    // Focus ring color - visible when focused
    val focusRingColor = AnsiPalette[10]

    Box(
        modifier = modifier
            .height(KeyCapHeight)
            .defaultMinSize(minWidth = minWidth)
            .clip(KeyCapShape)
            .focusRequester(focusRequester)
            .focusable()
            .onFocusChanged { focused ->
                isFocused = focused.isFocused
            }
            .onKeyEvent { event ->
                when (event.key) {
                    Key.Enter, Key.NumPadEnter, Key.Spacebar -> {
                        onClick()
                        true
                    }

                    else -> false
                }
            }
            .border(
                if (isFocused && enabled) BorderStroke(2.dp, focusRingColor) else BorderStroke(0.dp, Color.Transparent),
                KeyCapShape,
            )
            .padding(horizontal = 8.dp)
            .then(semanticsModifier),
    ) {
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier
                .fillMaxSize()
                .clip(KeyCapShape)
                .handCursor(),
            colors = buttonColors,
            shape = KeyCapShape,
            contentPadding = PaddingValues(0.dp),
        ) {
            content(tint)
        }
    }
}
