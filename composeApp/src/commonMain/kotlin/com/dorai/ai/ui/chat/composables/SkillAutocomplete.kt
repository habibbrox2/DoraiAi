package com.dorai.ai.ui.chat.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dorai.ai.skills.SkillManifest
import com.dorai.ai.ui.KaiTypography
import com.dorai.ai.ui.handCursor
import com.dorai.ai.ui.theme.DesignTokens
import com.dorai.ai.ui.theme.LocalDesignColorTokens
import kotlinx.collections.immutable.ImmutableList

/**
 * Drop-down list of skills shown above the chat composer when the user types `/`.
 * Selection replaces the leading `/<query>` token with `/<id> `, leaving the
 * cursor positioned for follow-up args.
 */
@Composable
internal fun SkillAutocomplete(
    skills: ImmutableList<SkillManifest>,
    query: String,
    onSelect: (SkillManifest) -> Unit,
    modifier: Modifier = Modifier,
) {
    val filtered = remember(skills, query) {
        val q = query.lowercase()
        if (q.isEmpty()) {
            skills
        } else {
            skills.filter { it.id.startsWith(q) || it.id.contains(q) }
        }
    }
    if (filtered.isEmpty()) return

    val scrollState = rememberScrollState()
    val shape = RoundedCornerShape(DesignTokens.RadiusCard)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(shape)
            .background(DesignTokens.SurfaceElevated, shape)
            .border(
                width = 1.dp,
                color = DesignTokens.BorderSubtle,
                shape = shape,
            )
            .heightIn(max = 220.dp)
            .verticalScroll(scrollState),
    ) {
        for (skill in filtered) {
            SkillRow(
                skill = skill,
                onClick = { onSelect(skill) },
            )
        }
    }
}

@Composable
private fun SkillRow(skill: SkillManifest, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .handCursor()
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(
            text = "/${skill.id}",
            style = KaiTypography.bodyMedium,
            color = DesignTokens.PrimaryCyan,
            fontWeight = FontWeight.SemiBold,
        )
        if (skill.description.isNotEmpty()) {
            Text(
                text = skill.description,
                style = KaiTypography.bodySmall,
                color = LocalDesignColorTokens.current.textSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
