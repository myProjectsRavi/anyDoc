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
