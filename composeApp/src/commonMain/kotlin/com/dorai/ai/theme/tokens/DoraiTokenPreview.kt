package com.dorai.ai.theme.tokens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.dorai.ai.theme.data.ThemePalette
import com.dorai.ai.theme.data.ThemePresets
import com.dorai.ai.theme.data.ThemeStyle
import com.dorai.ai.theme.engine.LocalDoraiPalette
import com.dorai.ai.theme.engine.LocalDoraiThemeStyle
import com.dorai.ai.theme.engine.doraiEdgeStyle
import com.dorai.ai.theme.engine.doraiSurfaceStyle
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * Token preview: every one of the 13 presets rendered through
 * [DoraiSemanticColors]. Layout uses wrapping rows/columns with min sizes
 * (no fixed heights) so it survives 200% font scale.
 */
@Preview
@Composable
fun DoraiTokenPreview() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(DoraiSpacing.LG),
    ) {
        for (palette in ThemePresets.all) {
            DoraiTokenPresetSection(palette)
        }
        DoraiSpacingAndTypeDemo()
        DoraiEdgeDemo()
    }
}

@Composable
private fun DoraiTokenPresetSection(palette: ThemePalette) {
    val tokens = DoraiSemanticColors.from(palette)
    CompositionLocalProvider(
        LocalDoraiPalette provides palette,
        LocalDoraiColors provides tokens,
        LocalDoraiThemeStyle provides ThemeStyle.Flat,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(tokens.background)
                .padding(DoraiSpacing.LG),
            verticalArrangement = Arrangement.spacedBy(DoraiSpacing.SM),
        ) {
            Text(
                text = palette.displayName,
                style = DoraiType.Body,
                color = tokens.text,
            )
            TokenSwatchRow("background", tokens.background, tokens)
            TokenSwatchRow("surface", tokens.surface, tokens)
            TokenSwatchRow("surface2", tokens.surface2, tokens)
            TokenSwatchRow("line", tokens.line, tokens)
            TokenSwatchRow("text", tokens.text, tokens)
            TokenSwatchRow("muted", tokens.muted, tokens)
            TokenSwatchRow("primary", tokens.primary, tokens)
            TokenSwatchRow("onPrimary", tokens.onPrimary, tokens)
            TokenSwatchRow("accent", tokens.accent, tokens)
            TokenSwatchRow("ok", tokens.ok, tokens)
            TokenSwatchRow("warn", tokens.warn, tokens)
            TokenSwatchRow("err", tokens.err, tokens)
            TokenSwatchRow("info", tokens.info, tokens)
            TokenSwatchRow("codeBackground", tokens.codeBackground, tokens)
            TokenSwatchRow("scrim", tokens.scrim, tokens)
            Text(
                text = "fun main() { println(\"Dorai\") }",
                style = DoraiType.Code,
                color = tokens.text,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(tokens.codeBackground)
                    .padding(DoraiSpacing.SM),
            )
        }
    }
}

@Composable
private fun TokenSwatchRow(label: String, swatch: Color, tokens: DoraiSemanticColors) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DoraiSpacing.SM),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier
                .defaultMinSize(minWidth = 24.dp, minHeight = 24.dp)
                .size(24.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(swatch),
        )
        Text(
            text = label,
            style = DoraiType.BodyMedium,
            color = tokens.text,
        )
    }
}

@Composable
private fun DoraiSpacingAndTypeDemo() {
    val tokens = LocalDoraiColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(tokens.surface)
            .padding(DoraiSpacing.LG),
        verticalArrangement = Arrangement.spacedBy(DoraiSpacing.SM),
    ) {
        Text(text = "Spacing 4 / 8 / 12 / 16 / 24 / 32", style = DoraiType.Body, color = tokens.text)
        Row(horizontalArrangement = Arrangement.spacedBy(DoraiSpacing.SM)) {
            for (gap in listOf(DoraiSpacing.XS, DoraiSpacing.SM, DoraiSpacing.MD, DoraiSpacing.LG, DoraiSpacing.XL, DoraiSpacing.XXL)) {
                Box(
                    modifier = Modifier
                        .size(gap)
                        .clip(RoundedCornerShape(4.dp))
                        .background(tokens.primary),
                )
            }
        }
        Text(text = "Body 16sp / 25.6sp (Bengali-safe 1.6)", style = DoraiType.Body, color = tokens.text)
        Text(text = "Body medium 14sp / 22.4sp", style = DoraiType.BodyMedium, color = tokens.text)
        Text(text = "mono code 13sp / 20.8sp", style = DoraiType.Code, color = tokens.text)
    }
}

@Composable
private fun DoraiEdgeDemo() {
    val tokens = LocalDoraiColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(tokens.surface)
            .padding(DoraiSpacing.LG),
        verticalArrangement = Arrangement.spacedBy(DoraiSpacing.LG),
    ) {
        CompositionLocalProvider(LocalDoraiThemeStyle provides ThemeStyle.Flat) {
            Box(
                modifier = Modifier
                    .doraiEdgeStyle(shape = RoundedCornerShape(24.dp), cornerRadius = 24.dp)
                    .doraiSurfaceStyle(shape = RoundedCornerShape(24.dp), containerColor = tokens.primary),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Flat button",
                    style = DoraiType.Body,
                    color = tokens.onPrimary,
                    modifier = Modifier.padding(DoraiSpacing.LG, DoraiSpacing.MD),
                )
            }
        }
        CompositionLocalProvider(LocalDoraiThemeStyle provides ThemeStyle.ThreeD) {
            Box(
                modifier = Modifier
                    .doraiEdgeStyle(
                        shape = RoundedCornerShape(24.dp),
                        cornerRadius = 24.dp,
                        edgeColor = tokens.primary,
                    )
                    .doraiSurfaceStyle(shape = RoundedCornerShape(24.dp), containerColor = tokens.primary),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "3D edge button",
                    style = DoraiType.Body,
                    color = tokens.onPrimary,
                    modifier = Modifier.padding(DoraiSpacing.LG, DoraiSpacing.MD),
                )
            }
        }
    }
}
