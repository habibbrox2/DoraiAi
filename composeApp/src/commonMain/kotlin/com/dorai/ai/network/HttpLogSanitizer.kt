package com.dorai.ai.network

/**
 * Removes credential material from Ktor debug-log lines before they reach stdout/logcat.
 *
 * Debug builds log full request/response details (`LogLevel.BODY`), which includes
 * headers such as `Authorization: Bearer …` and `x-goog-api-key: …`. This sanitizer
 * redacts the values of known credential headers so debug logs stay useful for
 * debugging without leaking provider API keys or cookies.
 *
 * Values are replaced with a fixed `[REDACTED]` marker; the header names and line
 * structure are preserved so logs remain greppable.
 */
internal object HttpLogSanitizer {

    private val SENSITIVE_HEADER_LINE = Regex(
        pattern = """^(?<prefix>\s*[-|>]*\s*)(?<name>authorization|proxy-authorization|x-goog-api-key|x-api-key|api-key|apikey|cookie|set-cookie|x-auth-token)(?<sep>\s*[:=]\s*).*$""",
        options = setOf(RegexOption.IGNORE_CASE),
    )

    /** Standalone bearer/key tokens that can appear in header dumps or error messages. */
    private val INLINE_TOKEN = Regex(
        pattern = """(?i)(bearer|key|ghs_|ghu_|ghp_|github_pat_)\s*[A-Za-z0-9._~+/=-]{10,}""",
    )

    fun sanitize(message: String): String = message
        .lineSequence()
        .joinToString("\n") { line ->
            if (SENSITIVE_HEADER_LINE.containsMatchIn(line)) {
                SENSITIVE_HEADER_LINE.replace(line) { match ->
                    "${match.groups["prefix"]!!.value}${match.groups["name"]!!.value}${match.groups["sep"]!!.value}[REDACTED]"
                }
            } else {
                line
            }
        }
        .let { INLINE_TOKEN.replace(it) { match -> "${match.groupValues[1]} [REDACTED]" } }
}
