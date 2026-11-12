package com.dorai.ai.voice

// Turns assistant text into something a speech engine can say out loud.
//
// The rules come from the offline-voice feature plan:
//  - nothing that cannot be spoken survives (markdown syntax, code, raw URLs, emoji);
//  - numbers in prose are read as Bengali words, so "৫" is spoken "পাঁচ";
//  - Bengali and Latin words that are glued together get a space between them, so the
//    synthesizer treats them as two words instead of one unpronounceable token.
//
// Everything here is pure and platform independent. No Unicode property escapes are
// used — those are not available on every KMP target — and no normalisation is applied
// to Bengali text: the policy forbids reordering or stripping Bengali code points, so
// the script is only ever *inspected*, never rewritten.

/** The Bengali script block (U+0980..U+09FF). */
private const val BENGALI_BLOCK_FIRST = 0x0980
private const val BENGALI_BLOCK_LAST = 0x09FF

/** Bengali digits ০..৯ (U+09E6..U+09EF). */
private const val BENGALI_DIGIT_FIRST = 0x09E6
private const val BENGALI_DIGIT_LAST = 0x09EF

/**
 * Pictograph ranges that are never spoken. Ranges only — the joiner (U+200D), the
 * variation selector (U+FE0F) and the emoji modifiers are deliberately left alone,
 * because the policy forbids stripping those from Bengali or any other text.
 */
private fun isEmoji(code: Int): Boolean = code in 0x1F000..0x1FAFF ||
    code in 0x2600..0x27BF ||
    code in 0x2B00..0x2BFF

private fun isBengali(char: Char): Boolean = char.code in BENGALI_BLOCK_FIRST..BENGALI_BLOCK_LAST

private fun isLatinLetter(char: Char): Boolean = char in 'a'..'z' || char in 'A'..'Z'

/** The numeric value of an ASCII or Bengali digit. */
private fun digitValue(char: Char): Int = when {
    char in '0'..'9' -> char - '0'
    char.code in BENGALI_DIGIT_FIRST..BENGALI_DIGIT_LAST -> char.code - BENGALI_DIGIT_FIRST
    else -> 0
}

/**
 * Prepares [text] for speech: removes what cannot be said, reads numbers as Bengali
 * words, and separates Bengali from Latin words that are glued together.
 */
fun sanitizeForSpeech(text: String): String {
    if (text.isEmpty()) return ""
    var result = stripFencedCode(text)
    result = stripInlineCode(result)
    result = stripImages(result)
    result = stripLinkUrls(result)
    result = stripBareUrls(result)
    result = stripMarkdownMarkers(result)
    result = removeEmoji(result)
    result = separateScripts(result)
    result = spellOutNumbers(result)
    return result
}

/**
 * Splits [text] into the sentences that are spoken one after another.
 *
 * Bengali prose ends a sentence with a danda (।), so that is a boundary together with
 * `?` and `!`. A period also ends a sentence, unless it belongs to an abbreviation
 * ("e.g.", "Mr.") or to a version or decimal. A blank line — a paragraph break — is a
 * boundary too.
 */
fun splitSpeakableSentences(text: String): List<String> {
    val sentences = mutableListOf<String>()
    val current = StringBuilder()
    var index = 0
    while (index < text.length) {
        val char = text[index]
        when {
            char == '।' || char == '?' || char == '!' -> {
                current.append(char)
                sentences.addIfNotBlank(current)
                current.clear()
                index++
                while (index < text.length && text[index].isWhitespace()) index++
            }

            char == '.' && isSentencePeriod(text, index) -> {
                current.append(char)
                sentences.addIfNotBlank(current)
                current.clear()
                index++
                while (index < text.length && text[index].isWhitespace()) index++
            }

            char == '\n' || char == '\r' -> {
                var next = index + 1
                while (next < text.length && (text[next] == ' ' || text[next] == '\t')) next++
                if (next < text.length && text[next] == '\n') {
                    // A blank line is a paragraph break and ends the sentence.
                    sentences.addIfNotBlank(current)
                    current.clear()
                    index = next + 1
                    while (index < text.length && text[index].isWhitespace()) index++
                } else {
                    if (current.isNotEmpty() && !current.last().isWhitespace()) current.append(' ')
                    index = next
                }
            }

            else -> {
                current.append(char)
                index++
            }
        }
    }
    sentences.addIfNotBlank(current)
    return sentences
}

/**
 * Prepares [text] for speech and splits it into the sentences that are spoken one after
 * another.
 */
fun toVoiceSentences(text: String): List<String> = splitSpeakableSentences(sanitizeForSpeech(text))

private fun MutableList<String>.addIfNotBlank(builder: StringBuilder) {
    val sentence = builder.toString().trim()
    if (sentence.isNotEmpty()) add(sentence)
}

/** Drops fenced code blocks; their content is not something a person would say aloud. */
private fun stripFencedCode(text: String): String {
    val out = StringBuilder()
    var openFence: String? = null
    for (line in text.lines()) {
        val trimmed = line.trimStart()
        val marker = when {
            trimmed.startsWith("```") -> "```"
            trimmed.startsWith("~~~") -> "~~~"
            else -> null
        }
        if (marker != null) {
            if (openFence == null) {
                openFence = marker
            } else if (openFence == marker) {
                openFence = null
            }
            continue
        }
        if (openFence == null) {
            out.append(line).append('\n')
        }
    }
    return out.toString().trimEnd('\n')
}

/** Removes inline code spans, keeping the surrounding words. */
private fun stripInlineCode(text: String): String {
    val out = StringBuilder()
    var index = 0
    while (index < text.length) {
        if (text[index] == '`') {
            var fenceLength = 0
            while (index + fenceLength < text.length && text[index + fenceLength] == '`') fenceLength++
            val closing = text.indexOf("`".repeat(fenceLength), index + fenceLength)
            if (closing >= 0) {
                out.append(' ')
                index = closing + fenceLength
                continue
            }
        }
        out.append(text[index])
        index++
    }
    return out.toString()
}

/** Removes images entirely — an image has no spoken form. */
private fun stripImages(text: String): String {
    val out = StringBuilder()
    var index = 0
    while (index < text.length) {
        if (text[index] == '!' && index + 1 < text.length && text[index + 1] == '[') {
            val bracket = findClosing(text, index + 1, '[', ']')
            if (bracket > 0 && bracket + 1 < text.length && text[bracket + 1] == '(') {
                val paren = findClosing(text, bracket + 1, '(', ')')
                if (paren > 0) {
                    index = paren + 1
                    out.append(' ')
                    continue
                }
            }
        }
        out.append(text[index])
        index++
    }
    return out.toString()
}

/**
 * Keeps the label of a markdown link and drops its target. The label is what the
 * writer meant the reader to hear.
 */
private fun stripLinkUrls(text: String): String {
    val out = StringBuilder()
    var index = 0
    while (index < text.length) {
        if (text[index] == '[') {
            val bracket = findClosing(text, index, '[', ']')
            if (bracket > 0 && bracket + 1 < text.length && text[bracket + 1] == '(') {
                val paren = findClosing(text, bracket + 1, '(', ')')
                if (paren > 0) {
                    out.append(text, index + 1, bracket)
                    index = paren + 1
                    continue
                }
            }
        }
        out.append(text[index])
        index++
    }
    return out.toString()
}

/** Removes URLs that are not markdown links; nobody wants a URL read aloud. */
private fun stripBareUrls(text: String): String {
    val out = StringBuilder()
    var index = 0
    while (index < text.length) {
        val schemeLength = when {
            text.startsWith("https://", index) -> 8
            text.startsWith("http://", index) -> 7
            text.startsWith("www.", index) -> 4
            else -> 0
        }
        if (schemeLength > 0) {
            var end = index + schemeLength
            while (end < text.length && !text[end].isWhitespace() && text[end] != ')' && text[end] != '>' && text[end] != '"') end++
            out.append(' ')
            index = end
            continue
        }
        out.append(text[index])
        index++
    }
    return out.toString()
}

/** Removes block quotes, headings, list markers, table pipes and emphasis markers. */
private fun stripMarkdownMarkers(text: String): String {
    val withoutRules = text.lines()
        .filterNot { isHorizontalRule(it) }
        .joinToString("\n") { stripLineMarkers(it) }
    return stripPairedEmphasis(withoutRules)
}

private fun isHorizontalRule(line: String): Boolean {
    val trimmed = line.trim()
    if (trimmed.length < 3) return false
    val char = trimmed[0]
    if (char != '-' && char != '*' && char != '_') return false
    return trimmed.all { it == char }
}

private fun stripLineMarkers(line: String): String {
    var result = line.trimStart()
    while (result.startsWith(">")) {
        result = result.removePrefix(">").trimStart()
    }
    while (result.startsWith("#")) {
        result = result.removePrefix("#").trimStart()
    }
    if (result.startsWith("- ") || result.startsWith("* ") || result.startsWith("+ ")) {
        result = result.substring(2).trimStart()
    } else {
        // An ordered-list marker is ASCII digits plus a dot. Bengali digits are left
        // alone on purpose: a line starting with a Bengali year ("২০২৬. ...") is prose,
        // not a list item.
        var digits = 0
        while (digits < result.length && result[digits] in '0'..'9') digits++
        if (digits > 0 && digits + 1 < result.length && result[digits] == '.' && result[digits + 1] == ' ') {
            result = result.substring(digits + 2).trimStart()
        }
    }
    return result.replace("|", " ")
}

/**
 * Removes paired emphasis markers but keeps the emphasised words. A single underscore
 * is left alone on purpose: `snake_case` is a word, not emphasis.
 */
private fun stripPairedEmphasis(text: String): String {
    val out = StringBuilder()
    var index = 0
    while (index < text.length) {
        val marker = emphasisMarkerAt(text, index)
        if (marker != null) {
            val closing = findEmphasisClose(text, index + marker.length, marker)
            if (closing > 0) {
                out.append(text, index + marker.length, closing)
                index = closing + marker.length
                continue
            }
        }
        out.append(text[index])
        index++
    }
    return out.toString()
}

private fun emphasisMarkerAt(text: String, index: Int): String? {
    val char = text[index]
    val marker = when {
        char == '*' && text.startsWith("**", index) -> "**"
        char == '*' -> "*"
        char == '~' && text.startsWith("~~", index) -> "~~"
        else -> return null
    }
    val before = if (index == 0) ' ' else text[index - 1]
    if (!before.isWhitespace() && before != '(' && before != '[') return null
    return marker
}

private fun findEmphasisClose(text: String, from: Int, marker: String): Int {
    var index = from
    while (index < text.length) {
        if (text.startsWith(marker, index)) {
            val after = if (index + marker.length >= text.length) ' ' else text[index + marker.length]
            if (after.isWhitespace() || !after.isLetterOrDigit()) return index
        }
        index++
    }
    return -1
}

/**
 * Removes pictographs; they are decoration, not speech.
 *
 * A Kotlin `Char` is a UTF-16 code unit, so an emoji above U+FFFF arrives as a
 * surrogate pair. The pair is read as one code point and dropped as a unit — treating
 * the halves separately would leave a broken character behind. The joiner and the
 * variation selector are not pictographs and are deliberately left alone.
 */
private fun removeEmoji(text: String): String {
    val out = StringBuilder()
    var index = 0
    while (index < text.length) {
        val char = text[index]
        if (char.isHighSurrogate() && index + 1 < text.length && text[index + 1].isLowSurrogate()) {
            if (!isEmoji(codePointAt(text, index))) {
                out.append(char).append(text[index + 1])
            }
            index += 2
            continue
        }
        if (!isEmoji(char.code)) out.append(char)
        index++
    }
    return out.toString()
}

/** The Unicode code point at [index], combining a surrogate pair when there is one. */
private fun codePointAt(text: String, index: Int): Int {
    val high = text[index]
    val low = text[index + 1]
    return (high.code - 0xD800) * 0x400 + (low.code - 0xDC00) + 0x10000
}

/**
 * Puts a space between a Bengali word and a Latin word that are written without one,
 * so the synthesizer reads them as two words instead of one unpronounceable token.
 */
private fun separateScripts(text: String): String {
    val out = StringBuilder()
    for (index in text.indices) {
        val char = text[index]
        if (index > 0) {
            val previous = text[index - 1]
            val mixed = (isBengali(previous) && isLatinLetter(char)) ||
                (isLatinLetter(previous) && isBengali(char))
            if (mixed) out.append(' ')
        }
        out.append(char)
    }
    return out.toString()
}

/**
 * Reads numbers in prose as Bengali words.
 *
 * A number that belongs to an identifier, a version, a currency amount or a decimal
 * is left alone: those are read as written, and the policy forbids changing the digit
 * format of machine readable values.
 */
private fun spellOutNumbers(text: String): String {
    val out = StringBuilder()
    var index = 0
    while (index < text.length) {
        val char = text[index]
        if (char.isDigit()) {
            val start = index
            var end = index
            while (end < text.length) {
                if (text[end].isDigit()) {
                    end++
                    continue
                }
                // A thousands separator belongs to the number, but only when digits
                // follow it — "1,000" is one number, "1," is not.
                if (text[end] == ',' && end + 1 < text.length && text[end + 1].isDigit()) {
                    end++
                    continue
                }
                break
            }
            val run = text.substring(start, end)
            index = end
            val before = if (start > 0) text[start - 1] else ' '
            val after = if (index < text.length) text[index] else ' '
            val value = digitsToLong(run)
            if (value != null && isProseNumber(before, after)) {
                out.append(bengaliNumberToWords(value))
            } else {
                out.append(run)
            }
            continue
        }
        out.append(char)
        index++
    }
    return out.toString()
}

private fun digitsToLong(run: String): Long? {
    var value = 0L
    for (char in run) {
        if (char == ',') continue
        val digit = digitValue(char)
        if (value > (Long.MAX_VALUE - digit) / 10) return null
        value = value * 10 + digit
    }
    return value
}

/**
 * True when the number run is a standalone number in prose.
 *
 * The run has to start a token — whitespace, the start of the text or an opening
 * bracket — so an identifier such as "৳৫০" or "v2" is left as written. It must not be
 * followed by a Latin letter, a digit or a dot, so "5GB" and "2.1" keep their written
 * form. A Bengali letter may follow: that is a classifier, as in "৫টি".
 */
private fun isProseNumber(before: Char, after: Char): Boolean {
    val startsToken = before.isWhitespace() ||
        before == '(' || before == '[' || before == '{' ||
        before == '"' || before == '\'' || before == '‘' || before == '“'
    if (!startsToken) return false
    if (after.isDigit() || after == '.') return false
    if (after.isLetter() && !isBengali(after)) return false
    return true
}

/**
 * True when the period at [index] ends a sentence rather than belonging to a decimal,
 * a version, an initial or an abbreviation.
 */
private fun isSentencePeriod(text: String, index: Int): Boolean {
    if (index == 0) return false
    val previous = text[index - 1]
    if (!previous.isLetter()) return false
    val next = if (index + 1 < text.length) text[index + 1] else ' '
    if (!next.isWhitespace()) return false
    val token = trailingToken(text, index - 1).trimEnd('.').lowercase()
    return token.length > 1 && token !in ABBREVIATIONS
}

/** The run of letters and dots ending at [endIndex]. */
private fun trailingToken(text: String, endIndex: Int): String {
    var start = endIndex + 1
    while (start > 0 && (text[start - 1].isLetter() || text[start - 1] == '.')) start--
    return text.substring(start, endIndex + 1)
}

/** Words that end in a period without ending a sentence. */
private val ABBREVIATIONS = setOf(
    "e.g",
    "i.e",
    "etc",
    "vs",
    "mr",
    "mrs",
    "ms",
    "dr",
    "prof",
    "sr",
    "jr",
    "fig",
    "approx",
    "inc",
    "ltd",
    "al",
)

private fun findClosing(text: String, openIndex: Int, open: Char, close: Char): Int {
    var depth = 0
    for (index in openIndex until text.length) {
        val char = text[index]
        if (char == open) {
            depth++
        } else if (char == close) {
            depth--
            if (depth == 0) return index
        }
    }
    return -1
}

// ---------------------------------------------------------------------------
// Bengali number words
// ---------------------------------------------------------------------------

/**
 * The words for the numbers 0..99. Bengali writes 21 as "একুশ" and 46 as "ছেচল্লিশ",
 * so every value below a hundred is a single word rather than a tens part plus a units
 * part.
 */
private val BENGALI_ONES = listOf(
    "", "এক", "দুই", "তিন", "চার", "পাঁচ", "ছয়", "সাত", "আট", "নয়",
    "দশ", "এগারো", "বারো", "তেরো", "চৌদ্দ", "পনেরো", "ষোলো", "সতেরো", "আঠারো", "উনিশ",
    "বিশ", "একুশ", "বাইশ", "তেইশ", "চব্বিশ", "পঁচিশ", "ছাব্বিশ", "সাতাশ", "আঠাশ", "ঊনত্রিশ",
    "ত্রিশ", "একত্রিশ", "বত্রিশ", "তেত্রিশ", "চৌত্রিশ", "পঁয়ত্রিশ", "ছত্রিশ", "সাঁইত্রিশ", "আটত্রিশ", "ঊনচল্লিশ",
    "চল্লিশ", "একচল্লিশ", "বিয়াল্লিশ", "তেতাল্লিশ", "চুয়াল্লিশ", "পঞ্চাশ", "ছেচল্লিশ", "সাতচল্লিশ", "আটচল্লিশ", "ঊনপঞ্চাশ",
    "পঞ্চাশ", "একান্ন", "বায়ান্ন", "তিপ্পান্ন", "চুয়ান্ন", "পঞ্চান্ন", "ছাপ্পান্ন", "সাতান্ন", "আটান্ন", "ঊনষাট্টি",
    "ষাট", "একষট্টি", "বাষট্টি", "তেষট্টি", "চৌষট্টি", "পঁয়ষট্টি", "ছেষট্টি", "সাতষট্টি", "আটষট্টি", "ঊনসত্তর",
    "সত্তর", "একাত্তর", "বাহাত্তর", "তিয়াত্তর", "চুয়াত্তর", "পঁচাত্তর", "ছিয়াত্তর", "সাতাত্তর", "আটাত্তর", "ঊনআশি",
    "আশি", "একাশি", "বিরাশি", "তিরাশি", "চুরাশি", "পঁচাশি", "ছিয়াশি", "সাতাশি", "আটাশি", "ঊননব্বই",
    "নব্বই", "একানব্বই", "বিরানব্বই", "তিরানব্বই", "চুরানব্বই", "পঁচানব্বই", "ছিয়ানব্বই", "সাতানব্বই", "আটানব্বই", "নিরানব্বই",
)

private val BENGALI_HUNDREDS = listOf(
    "", "এক", "দুই", "তিন", "চার", "পাঁচ", "ছয়", "সাত", "আট", "নয়",
)

/**
 * Reads [value] as Bengali words, using the Indian numbering system the language
 * actually uses: hundred, thousand, lakh, crore.
 */
fun bengaliNumberToWords(value: Long): String {
    if (value == 0L) return "শূন্য"
    if (value < 0) return "ঋণাত্মক " + bengaliNumberToWords(-value)
    return buildString { appendBengaliNumber(this, value) }
}

private fun appendBengaliNumber(out: StringBuilder, value: Long) {
    if (value < 100L) {
        out.append(bengaliTwoDigits(value.toInt()))
        return
    }
    if (value < 1_000L) {
        out.append(BENGALI_HUNDREDS[(value / 100L).toInt()]).append("শত")
        val rest = (value % 100L).toInt()
        if (rest > 0) {
            out.append(' ').append(bengaliTwoDigits(rest))
        }
        return
    }
    if (value < 100_000L) {
        appendBengaliNumber(out, value / 1_000L)
        out.append(" হাজার")
        val rest = (value % 1_000L).toInt()
        if (rest > 0) {
            out.append(' ')
            appendBengaliNumber(out, rest.toLong())
        }
        return
    }
    if (value < 10_000_000L) {
        appendBengaliNumber(out, value / 100_000L)
        out.append(" লক্ষ")
        val rest = (value % 100_000L).toInt()
        if (rest > 0) {
            out.append(' ')
            appendBengaliNumber(out, rest.toLong())
        }
        return
    }
    appendBengaliNumber(out, value / 10_000_000L)
    out.append(" কোটি")
    val rest = (value % 10_000_000L).toInt()
    if (rest > 0) {
        out.append(' ')
        appendBengaliNumber(out, rest.toLong())
    }
}

private fun bengaliTwoDigits(value: Int): String = BENGALI_ONES[value]
