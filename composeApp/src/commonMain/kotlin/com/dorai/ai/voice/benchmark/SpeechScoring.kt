package com.dorai.ai.voice.benchmark

/**
 * CER/WER scoring for the Phase 0 speech measurement.
 *
 * Both reference and hypothesis are normalised before comparison:
 *  - NFC is applied (the Bengali policy requires NFC before measurement);
 *  - Latin text is case-folded;
 *  - punctuation is replaced by a single space, then whitespace is collapsed.
 *
 * Scores are code-point distances. Bengali is a combining-mark script and a Kotlin
 * `Char` is a UTF-16 code unit, so the edit distance is computed on code points, not
 * on `Char` values.
 */
data class SpeechScore(
    val cer: Double,
    val wer: Double,
)

object SpeechScoring {
    /**
     * Scores [hypothesis] against [reference].
     *
     * CER uses the code-point distance of the two normalised strings; WER uses the
     * word-level edit distance of their whitespace-separated tokens. A score of 0 means
     * identical after normalisation. If the reference is empty, CER/WER is 0 when the
     * hypothesis is empty too, otherwise 1 (all edits against an empty reference).
     */
    fun score(reference: String, hypothesis: String): SpeechScore = SpeechScore(cer(reference, hypothesis), wer(reference, hypothesis))

    fun cer(reference: String, hypothesis: String): Double {
        val normalizedReference = normalizeForScoring(reference)
        val normalizedHypothesis = normalizeForScoring(hypothesis)
        if (normalizedReference.isEmpty()) {
            return if (normalizedHypothesis.isEmpty()) 0.0 else 1.0
        }
        val edits = levenshtein(codePointsOf(normalizedReference), codePointsOf(normalizedHypothesis))
        return edits.toDouble() / codePointsOf(normalizedReference).size
    }

    fun wer(reference: String, hypothesis: String): Double {
        val normalizedReference = normalizeForScoring(reference)
        val normalizedHypothesis = normalizeForScoring(hypothesis)
        if (normalizedReference.isEmpty()) {
            return if (normalizedHypothesis.isEmpty()) 0.0 else 1.0
        }
        val referenceWords = normalizedReference.split(' ').filter { it.isNotEmpty() }
        val hypothesisWords = normalizedHypothesis.split(' ').filter { it.isNotEmpty() }
        val edits = levenshtein(referenceWords, hypothesisWords)
        return edits.toDouble() / referenceWords.size
    }

    /**
     * Normalises a string for scoring: NFC, case-folded, punctuation-as-space, then
     * whitespace collapsed to single spaces and trimmed.
     */
    fun normalizeForScoring(text: String): String {
        val builder = StringBuilder()
        for (char in normalizeNfc(text).lowercase()) {
            if (isScoringPunctuation(char)) {
                builder.append(' ')
                continue
            }
            if (char.isWhitespace()) {
                builder.append(' ')
                continue
            }
            builder.append(char)
        }
        return builder.toString().split(' ').filter { it.isNotEmpty() }.joinToString(" ")
    }

    private fun isScoringPunctuation(char: Char): Boolean = char.category == CharCategory.OTHER_PUNCTUATION ||
        char.category == CharCategory.DASH_PUNCTUATION ||
        char.category == CharCategory.START_PUNCTUATION ||
        char.category == CharCategory.END_PUNCTUATION ||
        char.category == CharCategory.CONNECTOR_PUNCTUATION ||
        char.category == CharCategory.INITIAL_QUOTE_PUNCTUATION ||
        char.category == CharCategory.FINAL_QUOTE_PUNCTUATION ||
        char.category == CharCategory.CURRENCY_SYMBOL ||
        char.category == CharCategory.OTHER_SYMBOL

    internal fun codePointsOf(text: String): IntArray {
        val codePoints = ArrayList<Int>(text.length)
        var index = 0
        while (index < text.length) {
            val char = text[index]
            if (char.isHighSurrogate() && index + 1 < text.length && text[index + 1].isLowSurrogate()) {
                codePoints.add((char.code - 0xD800) * 0x400 + (text[index + 1].code - 0xDC00) + 0x10000)
                index += 2
                continue
            }
            codePoints.add(char.code)
            index++
        }
        return codePoints.toIntArray()
    }

    internal fun <T> levenshtein(reference: List<T>, hypothesis: List<T>): Int {
        var previous = IntArray(hypothesis.size + 1) { it }
        for (i in reference.indices) {
            val current = IntArray(hypothesis.size + 1) { i + 1 }
            for (j in hypothesis.indices) {
                val cost = if (reference[i] == hypothesis[j]) 0 else 1
                current[j + 1] = minOf(current[j] + 1, previous[j + 1] + 1, previous[j] + cost)
            }
            previous = current
        }
        return previous[hypothesis.size]
    }

    internal fun levenshtein(reference: IntArray, hypothesis: IntArray): Int = levenshtein(reference.toList(), hypothesis.toList())
}
