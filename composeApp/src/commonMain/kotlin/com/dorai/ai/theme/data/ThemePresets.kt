package com.dorai.ai.theme.data

/**
 * Built-in presets for the 2027 premium theme engine.
 *
 * Dark/Light mirror the app's current Material schemes (the premium obsidian identity).
 * Lilac, Dusk, Seafoam, Graphite, Daybreak, Sherbet, Paper and Carbon mirror the schemes
 * already declared in `ui/Theme.kt`, promoted to full surface/text/outline tiers so they
 * can drive the entire app. Midnight follows the documented brand-navy palette
 * (docs/features/appearance.md). OG Shine and Peach are new: a magenta-gold "shine"
 * dark preset for the OG brand and a soft warm light preset.
 */
object ThemePresets {

    const val DARK_ID = "dark"
    const val LIGHT_ID = "light"
    const val OG_SHINE_ID = "og_shine"
    const val PEACH_ID = "peach"
    const val MIDNIGHT_ID = "midnight"
    const val LILAC_ID = "lilac"
    const val DUSK_ID = "dusk"
    const val SEAFOAM_ID = "seafoam"
    const val GRAPHITE_ID = "graphite"
    const val DAYBREAK_ID = "daybreak"
    const val SHERBET_ID = "sherbet"
    const val PAPER_ID = "paper"
    const val CARBON_ID = "carbon"

    val dark = ThemePalette(
        id = DARK_ID,
        displayName = "Dark",
        isDark = true,
        background = "#050A1C",
        surface = "#08132B",
        surfaceElevated = "#0D1C38",
        surfaceRaised = "#13284A",
        primary = "#25CDE2",
        accent = "#8B78FF",
        onPrimary = "#08132B",
        onBackground = "#F1F8FF",
        onSurface = "#F1F8FF",
        onSurfaceVariant = "#C4D3E8",
        outline = "#2B3A55",
        outlineVariant = "#1AFFFFFF",
    )

    val light = ThemePalette(
        id = LIGHT_ID,
        displayName = "Light",
        isDark = false,
        background = "#FFFFFF",
        surface = "#FFFFFF",
        surfaceElevated = "#EBF1F7",
        surfaceRaised = "#E2E8F0",
        primary = "#245FD8",
        accent = "#377CFF",
        onPrimary = "#FFFFFF",
        onBackground = "#000000",
        onSurface = "#000000",
        onSurfaceVariant = "#475569",
        outline = "#94A3B8",
        outlineVariant = "#CBD5E1",
    )

    val ogShine = ThemePalette(
        id = OG_SHINE_ID,
        displayName = "OG Shine",
        isDark = true,
        background = "#14082B",
        surface = "#1E1038",
        surfaceElevated = "#29184A",
        surfaceRaised = "#35205E",
        primary = "#FF2E9A",
        accent = "#FFC24B",
        onPrimary = "#1B0B2E",
        onBackground = "#FBE9FF",
        onSurface = "#FBE9FF",
        onSurfaceVariant = "#D8B4D8",
        outline = "#46286B",
        outlineVariant = "#2E1B47",
    )

    val peach = ThemePalette(
        id = PEACH_ID,
        displayName = "Peach",
        isDark = false,
        background = "#FFF4EC",
        surface = "#FFFFFF",
        surfaceElevated = "#FFE8D6",
        surfaceRaised = "#FFD8BF",
        primary = "#D9531E",
        accent = "#B45309",
        onPrimary = "#140C07",
        onBackground = "#3B2B23",
        onSurface = "#3B2B23",
        onSurfaceVariant = "#6B4E3D",
        outline = "#E5B8A0",
        outlineVariant = "#F5D9C8",
    )

    val midnight = ThemePalette(
        id = MIDNIGHT_ID,
        displayName = "Midnight",
        isDark = true,
        background = "#06152E",
        surface = "#0E2A4D",
        surfaceElevated = "#173A5E",
        surfaceRaised = "#1F4A73",
        primary = "#25E2F5",
        accent = "#377CFF",
        onPrimary = "#06152E",
        onBackground = "#E6F4FF",
        onSurface = "#E6F4FF",
        onSurfaceVariant = "#A8C6E0",
        outline = "#2A4A6E",
        outlineVariant = "#17304F",
    )

    val lilac = ThemePalette(
        id = LILAC_ID,
        displayName = "Lilac",
        isDark = false,
        background = "#FAF7FF",
        surface = "#F3F0FF",
        surfaceElevated = "#EDE9FE",
        surfaceRaised = "#E0D9FA",
        primary = "#6B46C1",
        accent = "#8B5CF6",
        onPrimary = "#FFFFFF",
        onBackground = "#1E1B4B",
        onSurface = "#1E1B4B",
        onSurfaceVariant = "#4C1D95",
        outline = "#8B5CF6",
        outlineVariant = "#E9D8FD",
    )

    val dusk = ThemePalette(
        id = DUSK_ID,
        displayName = "Dusk",
        isDark = true,
        background = "#0F0A19",
        surface = "#1E1625",
        surfaceElevated = "#241A33",
        surfaceRaised = "#2D2138",
        primary = "#F59E0B",
        accent = "#A855F7",
        onPrimary = "#1C1917",
        onBackground = "#F3E8FF",
        onSurface = "#F3E8FF",
        onSurfaceVariant = "#D8B4FE",
        outline = "#A855F7",
        outlineVariant = "#4C1D95",
    )

    val seafoam = ThemePalette(
        id = SEAFOAM_ID,
        displayName = "Seafoam",
        isDark = false,
        background = "#F0FDF4",
        surface = "#ECFDF5",
        surfaceElevated = "#D1FAE5",
        surfaceRaised = "#A9F0D1",
        primary = "#047857",
        accent = "#10B981",
        onPrimary = "#FFFFFF",
        onBackground = "#064E3B",
        onSurface = "#064E3B",
        onSurfaceVariant = "#047857",
        outline = "#10B981",
        outlineVariant = "#A7F3D0",
    )

    val graphite = ThemePalette(
        id = GRAPHITE_ID,
        displayName = "Graphite",
        isDark = true,
        background = "#0F0F0F",
        surface = "#1A1A1A",
        surfaceElevated = "#252525",
        surfaceRaised = "#2E2E2E",
        primary = "#94A3B8",
        accent = "#64748B",
        onPrimary = "#0F172A",
        onBackground = "#E5E5E5",
        onSurface = "#E5E5E5",
        onSurfaceVariant = "#B0B0B0",
        outline = "#475569",
        outlineVariant = "#333333",
    )

    val daybreak = ThemePalette(
        id = DAYBREAK_ID,
        displayName = "Daybreak",
        isDark = false,
        background = "#F8FAFF",
        surface = "#F1F5FF",
        surfaceElevated = "#E0E7FF",
        surfaceRaised = "#C7D2FE",
        primary = "#2563EB",
        accent = "#3B82F6",
        onPrimary = "#FFFFFF",
        onBackground = "#0F172A",
        onSurface = "#0F172A",
        onSurfaceVariant = "#312E81",
        outline = "#3B82F6",
        outlineVariant = "#DBEAFE",
    )

    val sherbet = ThemePalette(
        id = SHERBET_ID,
        displayName = "Sherbet",
        isDark = false,
        background = "#FFF7FB",
        surface = "#FEF2F8",
        surfaceElevated = "#FCE7F3",
        surfaceRaised = "#FBCFE8",
        primary = "#D91C78",
        accent = "#F472B6",
        onPrimary = "#FFFFFF",
        onBackground = "#500724",
        onSurface = "#500724",
        onSurfaceVariant = "#9D174D",
        outline = "#EC4899",
        outlineVariant = "#FCE7F3",
    )

    val paper = ThemePalette(
        id = PAPER_ID,
        displayName = "Paper",
        isDark = false,
        background = "#FAF7F2",
        surface = "#F5F3F0",
        surfaceElevated = "#E7E5E4",
        surfaceRaised = "#D6D3D1",
        primary = "#92400E",
        accent = "#D97757",
        onPrimary = "#FFFFFF",
        onBackground = "#1C1917",
        onSurface = "#1C1917",
        onSurfaceVariant = "#44403C",
        outline = "#A8A29E",
        outlineVariant = "#D6D3D1",
    )

    val carbon = ThemePalette(
        id = CARBON_ID,
        displayName = "Carbon",
        isDark = true,
        background = "#000000",
        surface = "#0A0A0A",
        surfaceElevated = "#111111",
        surfaceRaised = "#1A1A1A",
        primary = "#60A5FA",
        accent = "#3B82F6",
        onPrimary = "#000000",
        onBackground = "#E5E5E5",
        onSurface = "#E5E5E5",
        onSurfaceVariant = "#A3A3A3",
        outline = "#3B82F6",
        outlineVariant = "#1E3A8A",
    )

    /** All built-in presets, in display order. */
    val all: List<ThemePalette> = listOf(
        dark,
        light,
        ogShine,
        peach,
        midnight,
        lilac,
        dusk,
        seafoam,
        graphite,
        daybreak,
        sherbet,
        paper,
        carbon,
    )

    /** Look up a built-in preset by id, or null when the id belongs to no preset. */
    fun find(id: String): ThemePalette? = all.firstOrNull { it.id == id }
}
