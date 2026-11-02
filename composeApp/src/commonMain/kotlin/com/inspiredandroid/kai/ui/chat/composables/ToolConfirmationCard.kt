package com.inspiredandroid.kai.ui.chat.composables

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.inspiredandroid.kai.ui.AccentPrimary
import com.inspiredandroid.kai.ui.KaiTypography
import com.inspiredandroid.kai.ui.theme.DesignTokens
import com.inspiredandroid.kai.ui.theme.DesignTokens
import kai.composeapp.generated.resources.Res
import kai.composeapp.generated.resources.ic_flag
import org.jetbrains.compose.resources.painterResource

@Composable
fun ToolConfirmationCard(
    toolName: String,
    toolCallId: String,
    arguments: String,
    onDeny: () -> Unit,
    onAllowOnce: () -> Unit,
    onAllowSession: () -> Unit = {},
    onAlwaysAllow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val displayName = toolName.replace("_", " ")
        .split(" ")
        .joinToString(" ") { it.replaceFirstChar { c -> if (c.isLowerCase()) c.titlecase() else c.toString() } }

    val targetInfo = extractTargetInfo(toolName, arguments)

    Column(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(DesignTokens.RadiusCard))
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(DesignTokens.RadiusCard),
                ambientColor = DesignTokens.Warning.copy(alpha = 0.18f),
                spotColor = DesignTokens.Warning.copy(alpha = 0.3f),
            )
            .background(DesignTokens.SurfaceElevated)
            .border(
                BorderStroke(1.dp, DesignTokens.Warning.copy(alpha = 0.45f)),
                RoundedCornerShape(12.dp),
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_flag),
                contentDescription = null,
                tint = DesignTokens.Warning,
                modifier = Modifier.size(20.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Tool Request: $displayName",
                    style = KaiTypography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (targetInfo.isNotEmpty()) {
                    Text(
                        text = targetInfo,
                        style = KaiTypography.labelSmall,
                        color = DesignTokens.SecondaryText,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onDeny,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(DesignTokens.RadiusHud),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = DesignTokens.Error,
                    ),
                    border = BorderStroke(1.dp, DesignTokens.Error.copy(alpha = 0.5f)),
                ) {
                    Text("Deny", style = KaiTypography.labelMedium)
                }
                Button(
                    onClick = onAllowOnce,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(DesignTokens.RadiusHud),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentPrimary.copy(alpha = 0.88f),
                    ),
                ) {
                    Text("Allow once", style = KaiTypography.labelMedium)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onAllowSession,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(DesignTokens.RadiusHud),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DesignTokens.AgentViolet,
                    ),
                ) {
                    Text("Allow session", style = KaiTypography.labelMedium)
                }
                Button(
                    onClick = onAlwaysAllow,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(DesignTokens.RadiusHud),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DesignTokens.Success,
                    ),
                ) {
                    Text("Always allow", style = KaiTypography.labelMedium)
                }
            }
        }
    }
}

private fun extractTargetInfo(toolName: String, arguments: String): String = try {
    when (toolName) {
        "fetch_url" -> {
            val urlMatch = Regex("\"url\"\\s*:\\s*\"([^\"]+)\"").find(arguments)
            val url = urlMatch?.groupValues?.get(1) ?: ""
            if (url.isNotEmpty()) "Fetch: $url" else ""
        }

        "web_search" -> {
            val queryMatch = Regex("\"query\"\\s*:\\s*\"([^\"]+)\"").find(arguments)
            val query = queryMatch?.groupValues?.get(1) ?: ""
            if (query.isNotEmpty()) "Search: $query" else ""
        }

        "execute_shell_command" -> {
            val cmdMatch = Regex("\"command\"\\s*:\\s*\"([^\"]+)\"").find(arguments)
            val cmd = cmdMatch?.groupValues?.get(1) ?: ""
            if (cmd.isNotEmpty()) "Command: $cmd" else "Shell command"
        }

        else -> {
            // Try to find url or common params
            val urlMatch = Regex("\"url\"\\s*:\\s*\"([^\"]+)\"").find(arguments)
            if (urlMatch != null) {
                "Target: ${urlMatch.groupValues[1]}"
            } else {
                ""
            }
        }
    }
} catch (_: Exception) {
    ""
}
