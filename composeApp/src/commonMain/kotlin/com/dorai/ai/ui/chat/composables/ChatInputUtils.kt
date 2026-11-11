package com.dorai.ai.ui.chat.composables

/**
 * Detects slash-command query string the user is currently typing, or null if
 * the cursor isn't inside a leading `/<token>`.
 */
internal fun detectSlashQuery(text: String, cursor: Int): String? {
    if (!text.startsWith('/')) return null
    val firstSpace = text.indexOfFirst { it.isWhitespace() }
    val tokenEnd = if (firstSpace < 0) text.length else firstSpace
    if (cursor > tokenEnd) return null
    return text.substring(1, tokenEnd).lowercase()
}

/**
 * Shortens a filename that is too long to display in a chip.
 * Returns the first [maxChars] characters of the base name followed by `…` and the original extension.
 */
fun truncateFileName(name: String, maxChars: Int = 16): String {
    if (name.length <= maxChars) return name
    val dotIndex = name.lastIndexOf('.')
    return if (dotIndex > 0 && dotIndex < name.length - 1) {
        val base = name.substring(0, dotIndex)
        val ext = name.substring(dotIndex) // includes the dot
        val keep = (maxChars - ext.length - 1).coerceAtLeast(1)
        "${base.take(keep)}…$ext"
    } else {
        "${name.take(maxChars - 1)}…"
    }
}
