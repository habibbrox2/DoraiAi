package com.dorai.ai.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.dorai.ai.ui.DoraiShapes
import com.dorai.ai.ui.chat.ChatSpacing
import com.dorai.ai.ui.handCursor
import com.dorai.ai.ui.theme.LocalDesignTokens
import dorai.composeapp.generated.resources.Res
import dorai.composeapp.generated.resources.a11y_search
import dorai.composeapp.generated.resources.action_clear
import org.jetbrains.compose.resources.stringResource

@Composable
fun DoraiSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    // Optional override; defaults to Res.string.action_clear so the button is
    // never unlabeled.
    clearContentDescription: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions(
        keyboardType = KeyboardType.Text,
        imeAction = ImeAction.Search,
    ),
) {
    val tokens = LocalDesignTokens.current
    val resolvedClearDescription = clearContentDescription ?: stringResource(Res.string.action_clear)
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth().padding(horizontal = ChatSpacing.sm, vertical = ChatSpacing.sm),
        shape = DoraiShapes.input,
        singleLine = true,
        placeholder = { Text(placeholder) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = stringResource(Res.string.a11y_search),
                tint = tokens.MutedText,
            )
        },
        trailingIcon = if (query.isNotEmpty()) {
            {
                IconButton(
                    onClick = { onQueryChange("") },
                    modifier = Modifier.handCursor(),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Clear,
                        contentDescription = resolvedClearDescription,
                        tint = tokens.MutedText,
                    )
                }
            }
        } else {
            null
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = tokens.BorderFocus,
            unfocusedBorderColor = tokens.BorderSubtle,
            disabledBorderColor = tokens.BorderSubtle,
            focusedContainerColor = tokens.SurfaceElevated,
            unfocusedContainerColor = tokens.SurfaceElevated,
            focusedTextColor = tokens.PrimaryText,
            unfocusedTextColor = tokens.PrimaryText,
            focusedPlaceholderColor = tokens.MutedText,
            unfocusedPlaceholderColor = tokens.MutedText,
        ),
        keyboardOptions = keyboardOptions,
    )
}
