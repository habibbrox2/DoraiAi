package com.dorai.ai.text

/**
 * Grapheme-cluster-safe text truncation helpers.
 *
 * Bengali grapheme clusters (base + virama + consonant), combining vowel signs,
 * and ZWJ emoji must never be split at a cut point — splitting yields detached
 * mātrā glyphs or replacement characters that corrupt what the user reads.
 *
 * Implemented in pure Kotlin (no platform dependencies) so the behavior is
 * identical and testable on every target. It follows UAX #29 extended-grapheme-
 * cluster rules for the sequences this app produces: surrogate pairs, ZWJ
 * sequences (emoji and Indic), variation selectors, and combining marks
 * (Latin, Bengali and the other Indic/Arabic blocks that use the same
 * virama mechanism). Complex script exceptions beyond these ranges are
 * handled by never splitting before a combining mark, which is the safe
 * direction.
 */

private fun nextCodePointIndex(text: String, index: Int): Int {
    if (index >= text.length) return index
    val high = text[index]
    return if (high.isHighSurrogate() && index + 1 < text.length && text[index + 1].isLowSurrogate()) {
        index + 2
    } else {
        index + 1
    }
}

private fun codePointAt(text: String, index: Int): Int {
    val first = text[index]
    if (first.isHighSurrogate() && index + 1 < text.length && text[index + 1].isLowSurrogate()) {
        val high = first.code - 0xD800
        val low = text[index + 1].code - 0xDC00
        return 0x10000 + (high shl 10) + low
    }
    return first.code
}

/** True for code points that extend the previous grapheme cluster (combining marks, vowel signs, virama, nukta, variation selectors). */
private fun isGraphemeExtend(cp: Int): Boolean = when (cp) {
    0x0981, 0x0982, 0x0983, 0x09BC, 0x09D7 -> true

    // Bengali signs + nukta
    in 0x09BE..0x09C4 -> true

    // Bengali vowel signs (AA..R)
    in 0x09CB..0x09CD -> true

    // Bengali vowel sign O, OO, virama
    in 0x0300..0x036F -> true

    // combining diacritical marks
    in 0x0483..0x0489 -> true

    // combining Cyrillic
    in 0x0591..0x05BD -> true

    // Hebrew points
    in 0x05BF..0x05C7 -> true

    in 0x05C8..0x05CD -> true

    in 0x0610..0x061A -> true

    // Arabic signs
    in 0x064B..0x065F -> true

    0x0670 -> true

    in 0x06D6..0x06ED -> true

    in 0x0901..0x0903 -> true

    // Devanagari signs
    0x093C -> true

    // nukta
    in 0x093E..0x094D -> true

    // Devanagari vowel signs + virama
    in 0x0951..0x0957 -> true

    in 0x0962..0x0963 -> true

    in 0x0A01..0x0A03 -> true

    // Gurmukhi signs
    0x0A3C -> true

    in 0x0A3E..0x0A4D -> true

    in 0x0A81..0x0A83 -> true

    // Gujarati signs
    0x0ABC -> true

    in 0x0ABE..0x0ACD -> true

    in 0x0B01..0x0B03 -> true

    // Oriya
    0x0B3C -> true

    in 0x0B3E..0x0B4D -> true

    in 0x0BBE..0x0BCD -> true

    // Tamil
    in 0x0C01..0x0C03 -> true

    // Telugu
    0x0C3C -> true

    in 0x0C3E..0x0C4D -> true

    in 0x0CBE..0x0CCD -> true

    // Kannada
    in 0x0D02..0x0D4D -> true

    // Malayalam (inclusive of vowel signs and virama)
    in 0x0DCA..0x0DDF -> true

    in 0xE31..0xE3A -> true

    // Thai
    in 0xE47..0xE4E -> true

    in 0xFE00..0xFE0F -> true

    // variation selectors
    in 0xFE20..0xFE2F -> true

    // combining half marks
    in 0x20D0..0x20FF -> true

    // enclosing combining marks
    in 0xE0100..0xE01EF -> true

    // variation selectors supplement
    0x200D -> true

    // ZWJ
    else -> false
}

/** True for regional indicator symbols — pairs make an emoji flag. */
private fun isRegionalIndicator(cp: Int): Boolean = cp in 0x1F1E6..0x1F1FF

/** Virama / vowel-killer code points — a consonant following one joins the same cluster (conjunct formation). */
private val VIRAMAS = setOf(
    0x094D, // Devanagari
    0x09CD, // Bengali
    0x0A4D, // Gurmukhi
    0x0ACD, // Gujarati
    0x0B4D, // Oriya
    0x0BCD, // Tamil
    0x0C4D, // Telugu
    0x0CCD, // Kannada
    0x0D4D, // Malayalam
    0x0DCA, // Sinhala
)

/** Index just past the grapheme cluster starting at [start]. */
private fun clusterEnd(text: String, start: Int): Int {
    var end = nextCodePointIndex(text, start)
    // Regional indicator pair collapses into one cluster.
    val startCp = codePointAt(text, start)
    if (isRegionalIndicator(startCp)) {
        val second = nextCodePointIndex(text, end)
        if (second != end && isRegionalIndicator(codePointAt(text, end))) {
            end = second
        }
    }
    // After a virama or a ZWJ, the following code point joins this cluster
    // (conjunct formation / explicit join). Bengali: ক + ্ + ষ = one cluster.
    var needJoinedConsonant = false
    while (end < text.length) {
        val cp = codePointAt(text, end)
        if (cp == 0x200D || cp in VIRAMAS || isGraphemeExtend(cp)) {
            if (cp == 0x200D || cp in VIRAMAS) needJoinedConsonant = true
            end = nextCodePointIndex(text, end)
            continue
        }
        if (needJoinedConsonant) {
            end = nextCodePointIndex(text, end)
            needJoinedConsonant = false
            continue
        }
        break
    }
    return end
}

/** Number of grapheme clusters in [text]. Never fails; runs in O(length). */
internal fun graphemeClusterCount(text: String): Int {
    var index = 0
    var count = 0
    while (index < text.length) {
        index = clusterEnd(text, index)
        count += 1
    }
    return count
}

/**
 * How many grapheme clusters must be deleted from the end of [text] to remove at least
 * [codeUnits] UTF-16 code units, never splitting a cluster. Returns the cluster count;
 * callers that can only delete per grapheme (terminal backspaces) use this directly.
 */
internal fun graphemeSafeDeleteCount(text: String, codeUnits: Int): Int {
    if (codeUnits <= 0 || text.isEmpty()) return 0
    val ends = mutableListOf<Int>()
    var index = 0
    while (index < text.length) {
        index = clusterEnd(text, index)
        ends += index
    }
    var total = 0
    var units = 0
    for (i in ends.indices.reversed()) {
        val clusterUnits = ends[i] - (if (i == 0) 0 else ends[i - 1])
        units += clusterUnits
        total += 1
        if (units >= codeUnits) break
    }
    return total
}

/**
 * Returns the longest prefix of [text] containing at most [maxGraphemes]
 * grapheme clusters. Never splits a cluster.
 */
internal fun graphemeSafeTake(text: String, maxGraphemes: Int): String {
    if (maxGraphemes <= 0 || text.isEmpty()) return ""
    var index = 0
    var count = 0
    while (index < text.length && count < maxGraphemes) {
        index = clusterEnd(text, index)
        count++
    }
    return if (index >= text.length) text else text.substring(0, index)
}

/** Appends U+2026 when the input was actually truncated. */
internal fun graphemeSafeTruncateWithEllipsis(text: String, maxGraphemes: Int): String {
    if (maxGraphemes <= 0) return ""
    val taken = graphemeSafeTake(text, maxGraphemes)
    return if (taken.length == text.length) text else taken.trimEnd() + "…"
}
