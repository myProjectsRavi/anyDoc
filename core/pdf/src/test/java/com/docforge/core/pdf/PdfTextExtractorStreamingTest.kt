package com.docforge.core.pdf

import java.io.IOException
import java.nio.file.Files
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class PdfTextExtractorStreamingTest {

    @Test
    fun chunkedWrites_arePublishedWithoutLosingWhitespace() {
        val file = Files.createTempFile("anydoc-streamed-text", ".txt").toFile()
        try {
            val chars = writeExtractedPdfText(file) { writer ->
                writer.write("  leading ")
                writer.write(charArrayOf('t', 'e', 'x', 't'), 0, 4)
                writer.write('\n'.code)
            }
            assertEquals("  leading text\n", file.readText(Charsets.UTF_8))
            assertEquals("  leading text\n".length, chars)
        } finally {
            file.delete()
        }
    }

    @Test
    fun whitespaceOnly_replacesStagedContentsWithFallback() {
        val file = Files.createTempFile("anydoc-blank-text", ".txt").toFile()
        try {
            val chars = writeExtractedPdfText(file) { writer ->
                writer.write(" \t\n")
                writer.write("\u2003")
            }
            val expected = "[No extractable text found in this PDF.]"
            assertEquals(expected, file.readText(Charsets.UTF_8))
            assertEquals(expected.length, chars)
        } finally {
            file.delete()
        }
    }

    @Test
    fun emptyExtraction_usesFallback() {
        val file = Files.createTempFile("anydoc-empty-text", ".txt").toFile()
        try {
            val chars = writeExtractedPdfText(file) { _ -> }
            val expected = "[No extractable text found in this PDF.]"
            assertEquals(expected, file.readText(Charsets.UTF_8))
            assertEquals(expected.length, chars)
        } finally {
            file.delete()
        }
    }

    @Test
    fun unicodeAndSurrogatePairs_countUtf16Characters() {
        val file = Files.createTempFile("anydoc-unicode-text", ".txt").toFile()
        try {
            val text = "नमस्ते 🌍 café 漢字"
            val chars = writeExtractedPdfText(file) { writer ->
                writer.write(text, 0, text.length)
            }
            assertEquals(text, file.readText(Charsets.UTF_8))
            assertEquals(text.length, chars)
        } finally {
            file.delete()
        }
    }

    @Test
    fun checkedCharacterCount_rejectsOverflow() {
        assertEquals(Int.MAX_VALUE, checkedExtractedCharCount(Int.MAX_VALUE - 1, 1))
        try {
            checkedExtractedCharCount(Int.MAX_VALUE, 1)
            fail("Expected overflow rejection")
        } catch (_: IOException) {
            // A staged publication must fail rather than wrap the result count.
        }
    }

    @Test
    fun extractionFailure_cleansStagedFileAndDoesNotPublish() = runBlocking {
        val directory = Files.createTempDirectory("anydoc-streaming-error").toFile()
        try {
            try {
                withStagedOutputFile(directory, "extracted", "txt") { staged ->
                    writeExtractedPdfText(staged) { writer ->
                        writer.write("partial")
                        throw IOException("synthetic PDFBox writer failure")
                    }
                }
                fail("Expected writer failure")
            } catch (_: IOException) {
                // Expected.
            }
            assertFalse(directory.resolve("extracted.txt").exists())
            assertTrue(directory.listFiles().orEmpty().none { it.name.startsWith(".anydoc_stage_") })
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun cancellation_cleansStagedFileAndDoesNotPublish() = runBlocking {
        val directory = Files.createTempDirectory("anydoc-streaming-cancel").toFile()
        try {
            try {
                withStagedOutputFile(directory, "extracted", "txt") { staged ->
                    writeExtractedPdfText(staged) { writer ->
                        writer.write("partial")
                        throw CancellationException("synthetic cancellation")
                    }
                }
                fail("Expected cancellation")
            } catch (_: CancellationException) {
                // Expected.
            }
            assertFalse(directory.resolve("extracted.txt").exists())
            assertTrue(directory.listFiles().orEmpty().none { it.name.startsWith(".anydoc_stage_") })
        } finally {
            directory.deleteRecursively()
        }
    }
}
