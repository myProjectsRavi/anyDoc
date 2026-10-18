package com.docforge.feature.converter

import com.docforge.core.pdf.withStagedOutputFile
import java.nio.file.Files
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class TextPdfPublicationSafetyTest {

    @Test
    fun writerFailure_publishesNoPartialPdf() = runBlocking {
        val directory = Files.createTempDirectory("anydoc-text-pdf").toFile()
        try {
            try {
                withStagedOutputFile(directory, "text", "pdf") { staged ->
                    staged.writeText("partial pdf")
                    error("synthetic writer failure")
                }
                fail("Expected writer failure")
            } catch (_: IllegalStateException) {
                // Expected.
            }

            assertFalse(directory.resolve("text.pdf").exists())
            assertTrue(directory.listFiles().orEmpty().none { it.name.startsWith(".anydoc_stage_") })
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun cancellation_publishesNoPartialPdf() = runBlocking {
        val directory = Files.createTempDirectory("anydoc-text-cancel").toFile()
        try {
            try {
                withStagedOutputFile(directory, "text", "pdf") { staged ->
                    staged.writeText("partial pdf")
                    throw CancellationException("synthetic cancellation")
                }
                fail("Expected cancellation")
            } catch (_: CancellationException) {
                // Expected.
            }

            assertFalse(directory.resolve("text.pdf").exists())
            assertTrue(directory.listFiles().orEmpty().none { it.name.startsWith(".anydoc_stage_") })
        } finally {
            directory.deleteRecursively()
        }
    }
}
