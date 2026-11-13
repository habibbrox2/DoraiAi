package com.dorai.ai.resources

import java.io.File
import java.nio.file.Files
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Locale coverage non-regression gate.
 *
 * 54 locales lag behind the English source and fall back to English for the missing keys.
 * That backlog is a translation task, not a build failure — but it must not grow silently:
 * a locale that loses keys it already had is a real regression (a translation was dropped,
 * a file was truncated, a merge clobbered a file).
 *
 * The recorded baseline lives in `scripts/locale-coverage-baseline.json` and is refreshed
 * with `pwsh scripts/locale-coverage.ps1` after a translation pass.
 *
 * See docs/development/bengali-language-and-rendering-policy.md section 4.
 */
class LocaleCoverageBaselineTest {

    private val repoRoot: File by lazy {
        val candidates = listOf(
            File(".").canonicalFile,
            File(".").canonicalFile.parentFile,
            File("..").canonicalFile,
        )
        candidates.firstOrNull { File(it, "scripts/locale-coverage-baseline.json").isFile }
            ?: error("Cannot locate scripts/locale-coverage-baseline.json from ${File(".").absolutePath}")
    }

    private val composeResources: File by lazy {
        val candidates = listOf(
            File("src/commonMain/composeResources"),
            File("composeApp/src/commonMain/composeResources"),
            File(".").canonicalFile.parentFile?.let { File(it, "composeApp/src/commonMain/composeResources") },
        ).filterNotNull()
        candidates.firstOrNull { it.isDirectory && File(it, "values").isDirectory }
            ?: error("Cannot locate composeResources from ${File(".").absolutePath}")
    }

    private fun stringKeyCount(dir: File): Int {
        val file = File(dir, "strings.xml")
        if (!file.isFile) return 0
        val doc = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = false
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        }.newDocumentBuilder().parse(file)
        return doc.getElementsByTagName("string").length
    }

    private fun baseline(): Map<String, Int> {
        val file = File(repoRoot, "scripts/locale-coverage-baseline.json")
        val raw = Files.readString(file.toPath(), Charsets.UTF_8)
        val localeBlock = Regex("\"locales\"\\s*:\\s*\\{(.*?)\\n  \\}", RegexOption.DOT_MATCHES_ALL)
            .find(raw)?.groupValues?.get(1).orEmpty()
        val counts = mutableMapOf<String, Int>()
        for (match in Regex("\"(values[^\"]*)\"\\s*:\\s*(\\d+)").findAll(localeBlock)) {
            counts[match.groupValues[1]] = match.groupValues[2].toInt()
        }
        assertTrue(counts.isNotEmpty(), "Could not read locale counts from ${file.absolutePath}")
        return counts
    }

    @Test
    fun `no locale lost keys relative to the recorded baseline`() {
        val baseline = baseline()
        val failures = mutableListOf<String>()
        composeResources.listFiles { f -> f.isDirectory && f.name.startsWith("values") }
            ?.sortedBy { it.name }
            ?.forEach { dir ->
                val recorded = baseline[dir.name]
                if (recorded == null) {
                    failures += "${dir.name}: not present in scripts/locale-coverage-baseline.json"
                    return@forEach
                }
                val current = stringKeyCount(dir)
                if (current < recorded) {
                    failures += "${dir.name}: $current keys now, baseline recorded $recorded (lost ${recorded - current})"
                }
            }
        assertTrue(failures.isEmpty(), failures.joinToString("\n"))
    }

    @Test
    fun `english and bengali remain complete`() {
        val failures = mutableListOf<String>()
        val defaultCount = stringKeyCount(File(composeResources, "values"))
        val bengaliCount = stringKeyCount(File(composeResources, "values-bn"))
        if (defaultCount != bengaliCount) {
            failures += "values-bn has $bengaliCount keys, values has $defaultCount"
        }
        val baseline = baseline()
        if (baseline["values"] != defaultCount) {
            failures += "values has $defaultCount keys, baseline recorded ${baseline["values"]}"
        }
        if (baseline["values-bn"] != bengaliCount) {
            failures += "values-bn has $bengaliCount keys, baseline recorded ${baseline["values-bn"]}"
        }
        assertTrue(failures.isEmpty(), failures.joinToString("\n"))
    }
}
