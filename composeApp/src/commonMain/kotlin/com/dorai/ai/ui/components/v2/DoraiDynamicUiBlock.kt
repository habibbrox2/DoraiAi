package com.dorai.ai.ui.components.v2

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dorai.ai.theme.engine.doraiEdgeStyle
import com.dorai.ai.theme.tokens.DoraiSpacing
import com.dorai.ai.theme.tokens.LocalDoraiColors
import dorai.composeapp.generated.resources.Res
import dorai.composeapp.generated.resources.dynamic_ui_confirm
import dorai.composeapp.generated.resources.dynamic_ui_submitted
import org.jetbrains.compose.resources.stringResource

data class DynamicUiOption(
    val id: String,
    val title: String,
    val description: String? = null,
)

data class DynamicUiModel(
    val title: String,
    val description: String? = null,
    val options: List<DynamicUiOption>,
    val selectedOptionId: String? = null,
    val isLocked: Boolean = false,
)

/**
 * UI v2 Dynamic UI interactive block:
 * An AI message carrying an interactive block with radio options and a confirm button.
 * Once confirmed, the block locks to prevent double-submissions.
 */
@Composable
fun DoraiDynamicUiBlock(
    model: DynamicUiModel,
    onOptionSelected: (String) -> Unit,
    onConfirm: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalDoraiColors.current
    val shape = RoundedCornerShape(18.dp)

    var currentSelectedId by remember(model.selectedOptionId) {
        mutableStateOf(model.selectedOptionId ?: model.options.firstOrNull()?.id.orEmpty())
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface)
            .border(1.dp, colors.info, shape)
            .padding(DoraiSpacing.MD),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = DoraiSpacing.XS),
        ) {
            DoraiStatusBadge(
                text = "Interactive reply",
                color = colors.info,
            )
        }

        Text(
            text = model.title,
            color = colors.text,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
        )

        if (!model.description.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(DoraiSpacing.XS))
            Text(
                text = model.description,
                color = colors.muted,
                fontSize = 13.sp,
                lineHeight = 18.sp,
            )
        }

        Spacer(modifier = Modifier.height(DoraiSpacing.SM))

        // Options
        model.options.forEach { option ->
            val isSelected = option.id == currentSelectedId
            val optShape = RoundedCornerShape(14.dp)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .heightIn(min = 48.dp)
                    .clip(optShape)
                    .background(
                        if (isSelected) colors.primary.copy(alpha = 0.14f) else colors.surface,
                    )
                    .border(
                        width = 1.dp,
                        color = if (isSelected) colors.primary else colors.line,
                        shape = optShape,
                    )
                    .clickable(
                        enabled = !model.isLocked,
                        role = Role.RadioButton,
                    ) {
                        currentSelectedId = option.id
                        onOptionSelected(option.id)
                    }
                    .padding(DoraiSpacing.MD),
                verticalAlignment = Alignment.Top,
            ) {
                // Radio indicator
                Box(
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .size(20.dp)
                        .clip(CircleShape)
                        .border(
                            2.dp,
                            if (isSelected) colors.primary else colors.muted,
                            CircleShape,
                        )
                        .background(
                            if (isSelected) colors.primary else colors.surface,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(colors.onPrimary),
                        )
                    }
                }

                Spacer(modifier = Modifier.width(DoraiSpacing.MD))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = option.title,
                        color = colors.text,
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    )

                    if (!option.description.isNullOrEmpty()) {
                        Text(
                            text = option.description,
                            color = colors.muted,
                            fontSize = 12.5.sp,
                            lineHeight = 16.sp,
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(DoraiSpacing.MD))

        // Confirm button
        Button(
            onClick = {
                if (!model.isLocked) {
                    onConfirm(currentSelectedId)
                }
            },
            enabled = !model.isLocked && currentSelectedId.isNotEmpty(),
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.primary,
                contentColor = colors.onPrimary,
                disabledContainerColor = colors.surface2,
                disabledContentColor = colors.muted,
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .doraiEdgeStyle(
                    shape = RoundedCornerShape(12.dp),
                    cornerRadius = 12.dp,
                    edgeColor = colors.primary,
                    enabled = !model.isLocked,
                ),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                if (model.isLocked) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(DoraiSpacing.SM))
                }
                Text(
                    text = if (model.isLocked) {
                        stringResource(Res.string.dynamic_ui_submitted)
                    } else {
                        stringResource(Res.string.dynamic_ui_confirm)
                    },
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                )
            }
        }
    }
}
