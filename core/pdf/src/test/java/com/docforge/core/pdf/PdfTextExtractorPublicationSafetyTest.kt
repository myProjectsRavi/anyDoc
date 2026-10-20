package com.docforge.core.pdf

import java.nio.file.Files
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class PdfTextExtractorPublicationSafetyTest {

    @Test
    fun writerFailure_publishesNoPartialTxt() = runBlocking {
        val directory = Files.createTempDirectory("anydoc-text-extract").toFile()
        try {
            try {
                withStagedOutputFile(directory, "extracted", "txt") { staged ->
                    staged.writeText("partial text")
                    error("synthetic writer failure")
                }
                fail("Expected writer failure")
            } catch (_: IllegalStateException) {
                // Expected.
            }

            assertFalse(directory.resolve("extracted.txt").exists())
            assertTrue(directory.listFiles().orEmpty().none { it.name.startsWith(".anydoc_stage_") })
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun cancellation_publishesNoPartialTxt() = runBlocking {
        val directory = Files.createTempDirectory("anydoc-text-cancel").toFile()
        try {
            try {
                withStagedOutputFile(directory, "extracted", "txt") { staged ->
                    staged.writeText("partial text")
                    throw CancellationException("synthetic cancellation")
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
