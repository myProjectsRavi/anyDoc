package com.docforge.core.pdf

import java.nio.file.Files
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class PdfACompliancePublicationSafetyTest {

    @Test
    fun pdfAWriterFailure_publishesNoPartialFinal() = runBlocking {
        val directory = Files.createTempDirectory("anydoc-pdfa-output").toFile()
        try {
            try {
                withStagedOutputFile(directory, "archive", "pdf") { staged ->
                    staged.writeText("partial pdfa")
                    error("synthetic PDF/A writer failure")
                }
                fail("Expected PDF/A writer failure")
            } catch (_: IllegalStateException) {
                // Expected.
            }

            assertFalse(directory.resolve("archive.pdf").exists())
            assertTrue(directory.listFiles().orEmpty().none { it.name.startsWith(".anydoc_stage_") })
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun pdfACancellation_publishesNoPartialFinal() = runBlocking {
        val directory = Files.createTempDirectory("anydoc-pdfa-cancel").toFile()
        try {
            try {
                withStagedOutputFile(directory, "archive", "pdf") { staged ->
                    staged.writeText("partial pdfa")
                    throw CancellationException("synthetic PDF/A cancellation")
                }
                fail("Expected PDF/A cancellation")
            } catch (_: CancellationException) {
                // Expected.
            }

            assertFalse(directory.resolve("archive.pdf").exists())
            assertTrue(directory.listFiles().orEmpty().none { it.name.startsWith(".anydoc_stage_") })
        } finally {
            directory.deleteRecursively()
        }
    }
}
