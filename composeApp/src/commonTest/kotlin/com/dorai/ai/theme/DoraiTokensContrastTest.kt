package com.dorai.ai.theme

import androidx.compose.ui.unit.dp
import com.dorai.ai.theme.data.ThemePresets
import com.dorai.ai.theme.data.contrastRatio
import com.dorai.ai.theme.tokens.DoraiSemanticColors
import com.dorai.ai.theme.tokens.DoraiSpacing
import com.dorai.ai.theme.tokens.DoraiType
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * UI v2 token contrast: for each of the 13 presets (through
 * [DoraiSemanticColors], i.e. the tokens screens actually read), body text
 * and status colors must meet WCAG AA 4.5:1. Failures are collected so one
 * run lists every failing preset + pair instead of stopping at the first.
 */
class DoraiTokensContrastTest {

    @Test
    fun `v2 tokens meet 4_5 to 1 on all thirteen presets`() {
        val failures = mutableListOf<String>()
        fun check(preset: String, pair: String, ratio: Double) {
            if (ratio < 4.5) failures.add("$preset $pair = %.2f (required >= 4.5)".format(ratio))
        }
        for (palette in ThemePresets.all) {
            val t = DoraiSemanticColors.from(palette)
            check(palette.id, "text/background", contrastRatio(t.text, t.background))
            check(palette.id, "text/surface", contrastRatio(t.text, t.surface))
            check(palette.id, "text/surface2", contrastRatio(t.text, t.surface2))
            check(palette.id, "text/codeBackground", contrastRatio(t.text, t.codeBackground))
            check(palette.id, "onPrimary/primary", contrastRatio(t.onPrimary, t.primary))
            check(palette.id, "muted/background", contrastRatio(t.muted, t.background))
            check(palette.id, "muted/surface", contrastRatio(t.muted, t.surface))
            check(palette.id, "ok/surface", contrastRatio(t.ok, t.surface))
            check(palette.id, "ok/background", contrastRatio(t.ok, t.background))
            check(palette.id, "warn/surface", contrastRatio(t.warn, t.surface))
            check(palette.id, "warn/background", contrastRatio(t.warn, t.background))
            check(palette.id, "err/surface", contrastRatio(t.err, t.surface))
            check(palette.id, "err/background", contrastRatio(t.err, t.background))
            check(palette.id, "info/surface", contrastRatio(t.info, t.surface))
            check(palette.id, "info/background", contrastRatio(t.info, t.background))
        }
        assertTrue(failures.isEmpty(), "V2 token contrast failures:\n" + failures.joinToString("\n"))
    }

    @Test
    fun `v2 spacing scale is 4 8 12 16 24 32`() {
        assertTrue(DoraiSpacing.XS == 4.dp, "XS should be 4dp but was ${DoraiSpacing.XS}")
        assertTrue(DoraiSpacing.SM == 8.dp, "SM should be 8dp but was ${DoraiSpacing.SM}")
        assertTrue(DoraiSpacing.MD == 12.dp, "MD should be 12dp but was ${DoraiSpacing.MD}")
        assertTrue(DoraiSpacing.LG == 16.dp, "LG should be 16dp but was ${DoraiSpacing.LG}")
        assertTrue(DoraiSpacing.XL == 24.dp, "XL should be 24dp but was ${DoraiSpacing.XL}")
        assertTrue(DoraiSpacing.XXL == 32.dp, "XXL should be 32dp but was ${DoraiSpacing.XXL}")
    }

    @Test
    fun `v2 body type is 16sp with 1_6 line height`() {
        val ratio = DoraiType.Body.lineHeight.value / DoraiType.Body.fontSize.value
        assertTrue(kotlin.math.abs(ratio - 1.6) < 0.05, "Body line-height ratio $ratio should be ~1.6")
        assertTrue(DoraiType.Body.fontSize.value == 16f, "Body fontSize should be 16sp but was ${DoraiType.Body.fontSize}")
    }
}
