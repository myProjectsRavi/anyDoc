package com.docforge.core.pdf

import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class PdfCompareOutputNameTest {
    @Test
    fun ordinaryNamesRemainUnchanged() {
        assertEquals("comparison_2026-final", sanitizedCompareOutputBaseName("comparison_2026-final") {
            fail("Fallback should not be called for nonblank names")
            "unused"
        })
    }

    @Test
    fun unsafeCharactersUseExistingAsciiReplacementPolicy() {
        assertEquals("Q4_review___", sanitizedCompareOutputBaseName("Q4 review?.!"))
        assertEquals("r_sum_", sanitizedCompareOutputBaseName("résumé"))
    }

    @Test
    fun blankInputUsesFallbackBeforeSanitization() {
        assertEquals("compare_123", sanitizedCompareOutputBaseName("   ") { "compare_123" })
        assertEquals("compare_123", sanitizedCompareOutputBaseName("") { "compare_123" })
    }

    @Test
    fun longInputIsTruncatedDeterministically() {
        val input = "a".repeat(10_000) + "suffix"
        val output = sanitizedCompareOutputBaseName(input)
        assertEquals(MAX_COMPARE_OUTPUT_BASE_NAME_LENGTH, output.length)
        assertEquals("a".repeat(MAX_COMPARE_OUTPUT_BASE_NAME_LENGTH), output)
        assertEquals(output, sanitizedCompareOutputBaseName(input))
    }

    @Test
    fun sanitizedUnicodeInputStaysWithinAsciiByteBudget() {
        val output = sanitizedCompareOutputBaseName("名".repeat(500))
        assertEquals(MAX_COMPARE_OUTPUT_BASE_NAME_LENGTH, output.length)
        assertTrue(output.all { it == '_' })
        assertEquals(output.length, output.toByteArray(Charsets.UTF_8).size)
        assertTrue(output.toByteArray(Charsets.UTF_8).size + "_2147483647.pdf".length <= 255)
    }

    @Test
    fun supplementaryUnicodeUsesOneReplacementPerCodePointAtBoundary() {
        assertEquals("_".repeat(200), sanitizedCompareOutputBaseName("😀".repeat(250)))
        assertEquals("a".repeat(199) + "_", sanitizedCompareOutputBaseName("a".repeat(199) + "😀" + "z"))
        assertEquals("a".repeat(198) + "__", sanitizedCompareOutputBaseName("a".repeat(198) + "😀😀"))
    }

    @Test
    fun unpairedSurrogatesUseOneReplacementEach() {
        assertEquals("a".repeat(198) + "_b", sanitizedCompareOutputBaseName("a".repeat(198) + "\uD83D" + "b" + "z"))
        assertEquals("__", sanitizedCompareOutputBaseName("\uDE00\uD83D"))
    }

    @Test
    fun blankUnicodeWhitespaceUsesFallbackLazily() {
        var calls = 0
        assertEquals("compare_456", sanitizedCompareOutputBaseName("\u2003\t\n") {
            calls++
            "compare_456"
        })
        assertEquals(1, calls)
        assertEquals("valid", sanitizedCompareOutputBaseName("valid") {
            calls++
            "unexpected"
        })
        assertEquals(1, calls)
    }

    @Test
    fun hugeSupplementaryInputIsBoundedAndEquivalent() {
        val input = "😀".repeat(250_000) + "suffix"
        assertEquals("_".repeat(200), sanitizedCompareOutputBaseName(input))
    }

    @Test
    fun boundedScannerMatchesLegacyRegexOnDeterministicUtf16Cases() {
        val random = kotlin.random.Random(108)
        val alphabet = charArrayOf('a', 'Z', '0', '-', '_', ' ', '\n', '?',
            '\uD800', '\uDC00', '\uD83D', '\uDE00', '\u00E9', '\u4E2D', '\u2003', '\u0000')
        repeat(10_000) { iteration ->
            val input = buildString {
                repeat(random.nextInt(0, 280)) { append(alphabet[random.nextInt(alphabet.size)]) }
            }
            val expected = input.ifBlank { "compare_123" }
                .replace(Regex("[^a-zA-Z0-9_-]"), "_")
                .take(MAX_COMPARE_OUTPUT_BASE_NAME_LENGTH)
            assertEquals("UTF-16 case $iteration", expected, sanitizedCompareOutputBaseName(input) { "compare_123" })
        }
    }

    @Test
    fun longNameStillSupportsNumberedCollisionWithoutOverwrite() = runBlocking {
        val directory = Files.createTempDirectory("anydoc-compare-long-name").toFile()
        try {
            val baseName = sanitizedCompareOutputBaseName("x".repeat(10_000))
            val original = directory.resolve("$baseName.pdf").apply { writeText("original") }
            val numbered = directory.resolve("${baseName}_1.pdf").apply { writeText("numbered") }
            val result = withStagedOutputFile(directory, baseName, "pdf") { stage ->
                stage.writeText("new")
            }
            assertEquals("${baseName}_2.pdf", result.outputFile.name)
            assertEquals("new", result.outputFile.readText())
            assertEquals("original", original.readText())
            assertEquals("numbered", numbered.readText())
            assertFalse(directory.listFiles().orEmpty().any { it.name.startsWith(".anydoc_stage_") })
        } finally {
            directory.deleteRecursively()
        }
    }
}
