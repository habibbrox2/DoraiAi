package com.dorai.ai.ui.components.v2

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dorai.ai.theme.engine.doraiEdgeStyle
import com.dorai.ai.theme.tokens.DoraiSpacing
import com.dorai.ai.theme.tokens.LocalDoraiColors
import dorai.composeapp.generated.resources.Res
import dorai.composeapp.generated.resources.tool_action_allow_once
import dorai.composeapp.generated.resources.tool_action_allow_session
import dorai.composeapp.generated.resources.tool_action_deny
import dorai.composeapp.generated.resources.tool_approval_prompt
import org.jetbrains.compose.resources.stringResource

/**
 * UI v2 Blocking Tool Approval Actions:
 * - Shows an amber warning note: "This tool requires your approval before running."
 * - Three buttons:
 *   1. Allow once (primary action)
 *   2. Allow for session (tonal action)
 *   3. Deny (outlined error action)
 */
@Composable
fun DoraiApprovalActions(
    modifier: Modifier = Modifier,
    onAllowOnce: (() -> Unit)? = null,
    onAllowSession: (() -> Unit)? = null,
    onDeny: (() -> Unit)? = null,
) {
    val colors = LocalDoraiColors.current

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = DoraiSpacing.SM),
        ) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = null,
                tint = colors.warn,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(DoraiSpacing.SM))
            Text(
                text = stringResource(Res.string.tool_approval_prompt),
                color = colors.warn,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(DoraiSpacing.SM),
        ) {
            if (onAllowOnce != null) {
                Button(
                    onClick = onAllowOnce,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primary,
                        contentColor = colors.onPrimary,
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .doraiEdgeStyle(
                            shape = RoundedCornerShape(10.dp),
                            cornerRadius = 10.dp,
                            edgeColor = colors.primary,
                        ),
                ) {
                    Text(
                        text = stringResource(Res.string.tool_action_allow_once),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        maxLines = 1,
                    )
                }
            }

            if (onAllowSession != null) {
                Button(
                    onClick = onAllowSession,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.surface2,
                        contentColor = colors.text,
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = stringResource(Res.string.tool_action_allow_session),
                        fontWeight = FontWeight.Normal,
                        fontSize = 13.sp,
                        maxLines = 1,
                    )
                }
            }

            if (onDeny != null) {
                OutlinedButton(
                    onClick = onDeny,
                    border = BorderStroke(1.dp, colors.err),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = colors.err,
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = stringResource(Res.string.tool_action_deny),
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
