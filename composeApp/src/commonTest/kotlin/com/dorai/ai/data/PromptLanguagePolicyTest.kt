package com.dorai.ai.data

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Pins the language-policy contract: the rules are injected by
 * [PromptLanguageRules.languageRulesFor], never by a soul-content heuristic.
 */
class PromptLanguagePolicyTest {

    @Test
    fun `bengali mode injects bengali rules with required precedence`() {
        val rules = PromptLanguageRules.languageRulesFor(AppLanguage.Bengali)
        assertTrue(rules.contains(PromptLanguageRules.LANGUAGE_RULES_MARKER))
        assertTrue(rules.contains("Bengali"))
        // Precedence: explicit user request > conversation language > app language
        assertTrue(rules.contains("explicit request"), "rules must state explicit-request precedence")
        assertTrue(rules.contains("conversation"), "rules must state conversation-language precedence")
        // Default to Bengali even for English messages
        assertTrue(rules.contains("even if the user's message is in English"))
    }

    @Test
    fun `english mode injects english rules`() {
        val rules = PromptLanguageRules.languageRulesFor(AppLanguage.English)
        assertTrue(rules.contains(PromptLanguageRules.LANGUAGE_RULES_MARKER))
        assertTrue(rules.contains("English by default"))
        assertTrue(rules.contains("explicit request"))
    }

    @Test
    fun `bailout and compaction prompts carry follow-marker`() {
        assertTrue(PromptLanguageRules.FOLLOW_MARKER.contains(PromptLanguageRules.LANGUAGE_RULES_MARKER))
    }

    @Test
    fun `language rules do not forbid explicit user override`() {
        // Policy §1.4/§7: user's explicit language choice always wins.
        for (language in AppLanguage.entries) {
            val rules = PromptLanguageRules.languageRulesFor(language)
            assertTrue(
                Regex("explicit", RegexOption.IGNORE_CASE).containsMatchIn(rules),
                "rules for $language must allow explicit user override",
            )
            assertFalse(rules.contains("NEVER respond in any other language"), "rules for $language must not forbid explicit override")
        }
    }
}
