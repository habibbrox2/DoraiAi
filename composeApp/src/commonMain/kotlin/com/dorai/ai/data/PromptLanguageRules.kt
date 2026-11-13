package com.dorai.ai.data

/**
 * Builds the CRITICAL LANGUAGE RULES block that is always injected into the
 * system prompt, independent of whatever the user's soul/persona contains.
 *
 * Precedence (Bengali policy §1.4, §7):
 *   1. the user's explicit request for another language,
 *   2. the conversation's established language,
 *   3. the active app language ([AppLanguage]).
 *
 * Bengali mode therefore means: respond in Bengali by default, even to short
 * English acknowledgements or English technical questions. The user can opt out
 * at any time by explicitly asking for another language.
 */
internal object PromptLanguageRules {

    const val LANGUAGE_RULES_MARKER = "CRITICAL LANGUAGE RULES"

    fun languageRulesFor(language: AppLanguage): String = when (language) {
        AppLanguage.Bengali -> """
            $LANGUAGE_RULES_MARKER (active app language: Bengali):
            - Respond in natural Bengali by default, even if the user's message is in English, is a short acknowledgement ("ok", "yes", "haan"), or is mostly code/logs. Explain in Bengali; keep code, commands, paths, URLs, identifiers and product names unchanged (verbatim).
            - Precedence: (1) the user's explicit request for another language wins; (2) otherwise follow the conversation's established language; (3) otherwise respond in Bengali because the app is in Bengali mode.
            - Never silently switch languages mid-response. Do not transliterate Bengali into Latin script, and do not mix in Hindi/Urdu.
            - Do not translate technical terms, API names, model names, file paths or literal values; keep them in their original form.
            - Use correct Bengali grammar, spelling and natural phrasing; avoid machine-like word-by-word translation.
        """.trimIndent()

        AppLanguage.English -> """
            $LANGUAGE_RULES_MARKER (active app language: English):
            - Respond in natural English by default.
            - Precedence: (1) the user's explicit request for another language wins; (2) otherwise follow the conversation's established language; (3) otherwise respond in English.
            - Never silently switch languages mid-response. Keep code, commands, paths, URLs, identifiers and product names in their original form.
        """.trimIndent()
    }

    /** One-line pointer used by tool-limit bailout and history-compaction prompts. */
    const val FOLLOW_MARKER = "Follow the $LANGUAGE_RULES_MARKER in the system prompt; do not switch languages silently."
}
