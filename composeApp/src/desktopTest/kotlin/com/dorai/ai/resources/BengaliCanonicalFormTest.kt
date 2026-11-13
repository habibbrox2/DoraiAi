package com.dorai.ai.resources

import org.w3c.dom.Element
import java.io.File
import java.nio.file.Files
import java.text.Normalizer
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Canonical-form gate for Bengali resources — policy §5 (Unicode & UTF-8 data integrity).
 *
 * Bengali "য়" has two canonically equivalent encodings:
 *  - U+09DF (precomposed)
 *  - U+09AF U+09BC (decomposed: য + nukta)
 *
 * Bengali nukta composition is excluded from Unicode canonical composition, so NFC does
 * **not** recombine the decomposed pair; the canonical (NFC) form of this character is the
 * decomposed sequence. Mixing both encodings in one file makes byte-level comparison,
 * search and diffing unreliable, and lets a stale copy of the same word appear "different".
 *
 * The gate therefore enforces that every locale resource file is already in NFC form, which
 * implies a single encoding per character. The scope of this rule is intentionally narrow:
 * only the U+09DF <-> U+09AF U+09BC pair is affected, and no other code point may change.
 */
class BengaliCanonicalFormTest {

    private val composeResources: File by lazy {
        val candidates = listOf(
            File("src/commonMain/composeResources"),
            File("composeApp/src/commonMain/composeResources"),
            File(".").canonicalFile.parentFile?.let { File(it, "composeApp/src/commonMain/composeResources") },
        ).filterNotNull()
        candidates.firstOrNull { it.isDirectory && File(it, "values").isDirectory }
            ?: error("Cannot locate composeResources from ${File(".").absolutePath}")
    }

    /** The Bengali letter that has a precomposed and a decomposed spelling. */
    private val precomposedYya = "\u09DF"
    private val decomposedYya = "\u09AF\u09BC"

    private fun localeFiles(): List<File> = composeResources.listFiles { f -> f.isDirectory && f.name.startsWith("values") }
        ?.mapNotNull { dir -> File(dir, "strings.xml").takeIf { it.isFile } }
        ?: emptyList()

    private fun stringElements(file: File): List<Pair<String, String>> {
        val doc = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = false
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        }.newDocumentBuilder().parse(file)
        val els = doc.getElementsByTagName("string")
        return (0 until els.length).map { i ->
            val el = els.item(i) as Element
            el.getAttribute("name") to el.textContent
        }
    }

    @Test
    fun `bengali resources use a single canonical spelling of the yya letter`() {
        val failures = mutableListOf<String>()
        for (file in localeFiles()) {
            val locale = file.parentFile.name
            for ((name, value) in stringElements(file)) {
                if (value.contains(precomposedYya)) {
                    failures += "$locale: string '$name' still uses the precomposed spelling " +
                        "(U+09DF) while the rest of the resources use the decomposed form"
                }
            }
        }
        assertTrue(failures.isEmpty(), failures.joinToString("\n"))
    }

    @Test
    fun `bengali resources are already in unicode canonical form`() {
        val failures = mutableListOf<String>()
        for (file in localeFiles()) {
            val locale = file.parentFile.name
            val raw = Files.readString(file.toPath(), Charsets.UTF_8)
            val canonical = Normalizer.normalize(raw, Normalizer.Form.NFC)
            if (canonical != raw) {
                failures += "$locale: file is not NFC-normalized (${raw.length - canonical.length} code units differ)"
            }
        }
        assertTrue(failures.isEmpty(), failures.joinToString("\n"))
    }

    @Test
    fun `canonical normalization only affects the yya letter`() {
        // Scope check: NFC must not touch anything except the U+09DF / U+09AF U+09BC pair.
        val probe = "ক্ষ য় অ ই ঞ্জ ও ঔ ৳ 1,2 emoji 😀"
        val normalized = Normalizer.normalize(probe, Normalizer.Form.NFC)
        val allowed = setOf(precomposedYya, "য", "়")
        val differing = probe.toSet() - normalized.toSet()
        assertEquals(emptySet(), differing - allowed, "NFC changed unrelated code points")
    }

    @Test
    fun `both yya spellings are canonically equivalent`() {
        // Equivalence + round trip: the two spellings of য় must stay equivalent under NFC
        // and repeated normalization must be stable.
        val precomposed = "য়"
        val decomposed = "য" + "়"
        assertEquals(
            Normalizer.normalize(precomposed, Normalizer.Form.NFC),
            Normalizer.normalize(decomposed, Normalizer.Form.NFC),
        )
        val once = Normalizer.normalize(decomposed, Normalizer.Form.NFC)
        assertEquals(once, Normalizer.normalize(once, Normalizer.Form.NFC))
    }

    @Test
    fun `bengali letters keep their conjunct and vowel sign structure`() {
        // A normalization must never split a cluster: the conjunct and the vowel sign have to
        // survive as one grapheme cluster each.
        val conjunct = "ক্ষ"
        val vowelSign = "কী"
        assertEquals(3, conjunct.length) // ক + ্ + ষ
        assertEquals(2, vowelSign.length) // ক + ী
        assertEquals(conjunct, Normalizer.normalize(conjunct, Normalizer.Form.NFC))
        assertEquals(vowelSign, Normalizer.normalize(vowelSign, Normalizer.Form.NFC))
    }
}
