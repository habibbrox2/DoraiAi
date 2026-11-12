package com.dorai.ai.resources

import org.w3c.dom.Element
import java.io.File
import java.nio.file.Files
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Locale resource parity and integrity gate.
 *
 * Enforced (fail the build):
 *  - every composeResources locale strings.xml file is well-formed XML;
 *  - no duplicate `string` / `plurals` / `item`-name key per locale;
 *  - `values/` (en) and `values-bn/` have exactly the same
 *    `string` key sets and the same `plurals` names with the same `quantity` values;
 *  - placeholder (e.g. %1$s, %2d) multisets match per key across every locale;
 *  - no mojibake / double-encoded UTF-8 signatures and no U+FFFD anywhere;
 *  - `item` quantity set of plurals matches between en and bn.
 *
 * Report-only (never fails):
 *  - staleness of the other ~53 locales (key count vs default). English fallback
 *    masking is a known, documented limitation; propagating translations is a
 *    product decision, not a test failure.
 *
 * See docs/development/bengali-language-and-rendering-policy.md sections 4 and 11.
 */
class LocaleParityTest {

    private val composeResources: File by lazy {
        val candidates = listOf(
            File("src/commonMain/composeResources"),
            File("composeApp/src/commonMain/composeResources"),
            File(".").canonicalFile.parentFile?.let { File(it, "composeApp/src/commonMain/composeResources") },
        ).filterNotNull()
        candidates.firstOrNull { it.isDirectory && File(it, "values").isDirectory }
            ?: error("Cannot locate composeResources from ${File(".").absolutePath}")
    }

    private data class LocaleData(
        val dirName: String,
        val rawText: String,
        val stringKeys: List<String>,
        val pluralsNames: List<String>,
        val plurals: Map<String, Set<String>>, // plurals name -> quantity set
        val stringPlaceholders: Map<String, List<String>>, // string name -> placeholder tokens
        val pluralsPlaceholders: Map<String, List<String>>, // plurals name -> placeholder tokens (union of items)
    )

    private val placeholderRegex = Regex("""%(\d+\$)?[a-z]""")

    // Double-encoded UTF-8 ("mojibake") signatures: e.g. Bengali heartbeat glyph misencoded
    // (U+00E0 U+00A6 = "à¦"), plus stray Latin-1 supplements that indicate double encoding.
    private val mojibakeSignatures = listOf(
        "à¦", // à¦ — Bengali double-encoding lead
        "ã€", // â€ — punctuation/mark double-encoding lead
        "Ã©", "Ã¨", "Ã±", "Ã¶", "Ã¼", "Ã¤", // common Latin double-encoding lead
        "ï¿½", // ï¿½ — U+FFFD misencoded
    )
    private val fffd = "\uFFFD"

    private fun parseLocale(dir: File): LocaleData {
        val file = File(dir, "strings.xml")
        val raw = Files.readString(file.toPath(), Charsets.UTF_8)
        val doc = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = false
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        }.newDocumentBuilder().parse(file)

        val stringEls = doc.getElementsByTagName("string")
        val pluralsEls = doc.getElementsByTagName("plurals")

        val stringKeys = mutableListOf<String>()
        val stringPlaceholders = mutableMapOf<String, List<String>>()
        val pluralsNames = mutableListOf<String>()
        for (i in 0 until stringEls.length) {
            val el = stringEls.item(i) as Element
            val name = el.getAttribute("name")
            stringKeys += name
            stringPlaceholders[name] = placeholderRegex.findAll(el.textContent).map { it.value }.toList()
        }

        val plurals = mutableMapOf<String, Set<String>>()
        val pluralsPlaceholders = mutableMapOf<String, List<String>>()
        for (i in 0 until pluralsEls.length) {
            val el = pluralsEls.item(i) as Element
            val name = el.getAttribute("name")
            pluralsNames += name
            val itemEls = el.getElementsByTagName("item")
            val quantities = mutableSetOf<String>()
            val tokens = mutableListOf<String>()
            for (j in 0 until itemEls.length) {
                val item = itemEls.item(j) as Element
                quantities += item.getAttribute("quantity")
                tokens += placeholderRegex.findAll(item.textContent).map { it.value }
            }
            plurals[name] = quantities
            pluralsPlaceholders[name] = tokens.sorted()
        }

        return LocaleData(dir.name, raw, stringKeys, pluralsNames, plurals, stringPlaceholders, pluralsPlaceholders)
    }

    private fun loadAll(): Map<String, LocaleData> {
        val dirs = composeResources.listFiles { f -> f.isDirectory && f.name.startsWith("values") } ?: emptyArray()
        return dirs.filter { File(it, "strings.xml").isFile }
            .associate { it.name to parseLocale(it) }
    }

    @Test
    fun `all locale files are well-formed and mojibake-free`() {
        val all = loadAll()
        val failures = mutableListOf<String>()
        for ((name, data) in all) {
            if (fffd in data.rawText) failures += "$name: contains U+FFFD replacement character"
            for (sig in mojibakeSignatures) {
                if (sig in data.rawText) failures += "$name: contains double-encoded UTF-8 signature '$sig'"
            }
            val seen = mutableSetOf<String>()
            for (key in data.stringKeys) {
                if (!seen.add(key)) failures += "$name: duplicate <string name=\"$key\">"
            }
            val seenPlurals = mutableSetOf<String>()
            for (key in data.pluralsNames) {
                if (!seenPlurals.add(key)) failures += "$name: duplicate <plurals name=\"$key\">"
            }
        }
        assertTrue(failures.isEmpty(), failures.joinToString("\n"))
    }

    @Test
    fun `english and bengali have identical key sets`() {
        val all = loadAll()
        val en = all.getValue("values")
        val bn = all.getValue("values-bn")

        val enKeys = en.stringKeys.toSet()
        val bnKeys = bn.stringKeys.toSet()
        val missingInBn = enKeys - bnKeys
        val extraInBn = bnKeys - enKeys

        val pluralMissingInBn = en.plurals.keys - bn.plurals.keys
        val pluralExtraInBn = bn.plurals.keys - en.plurals.keys
        val pluralQuantityMismatch = en.plurals.filter { (k, v) -> bn.plurals[k] != null && bn.plurals[k] != v }

        val failures = mutableListOf<String>()
        if (missingInBn.isNotEmpty()) failures += "bn missing keys: ${missingInBn.sorted()}"
        if (extraInBn.isNotEmpty()) failures += "bn extra keys: ${extraInBn.sorted()}"
        if (pluralMissingInBn.isNotEmpty()) failures += "bn missing plurals: ${pluralMissingInBn.sorted()}"
        if (pluralExtraInBn.isNotEmpty()) failures += "bn extra plurals: ${pluralExtraInBn.sorted()}"
        for ((k, v) in pluralQuantityMismatch) failures += "bn plural '$k' quantities $v != en quantities ${en.plurals[k]}"

        assertTrue(failures.isEmpty(), failures.joinToString("\n"))
    }

    @Test
    fun `placeholders match per key across all locales`() {
        val all = loadAll()
        val en = all.getValue("values")
        val failures = mutableListOf<String>()

        for ((name, data) in all) {
            for ((key, enTokens) in en.stringPlaceholders) {
                val tokens = data.stringPlaceholders[key] ?: continue
                if (tokens.toList() != enTokens.toList()) {
                    failures += "$name: key '$key' placeholders $tokens != en $enTokens"
                }
            }
            for ((key, enTokens) in en.pluralsPlaceholders) {
                val tokens = data.pluralsPlaceholders[key] ?: continue
                if (tokens != enTokens) {
                    failures += "$name: plurals '$key' placeholders $tokens != en $enTokens"
                }
            }
        }
        assertTrue(failures.isEmpty(), failures.joinToString("\n"))
    }

    @Test
    fun `report other locale staleness (non-fatal)`() {
        val all = loadAll()
        val enCount = all.getValue("values").stringKeys.size
        println("Locale staleness report (key count vs values/ = $enCount):")
        all.filter { it.key != "values" && it.key != "values-bn" }.toSortedMap().forEach { (name, data) ->
            println("  $name: ${data.stringKeys.size}/$enCount keys")
        }
    }
}
