package com.docforge.core.pdf

import java.nio.file.Files
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class PdfHeaderFooterPublicationSafetyTest {

    @Test
    fun headerFooterWriterFailure_publishesNoPartialFinal() = runBlocking {
        val directory = Files.createTempDirectory("anydoc-header-footer-output").toFile()
        try {
            try {
                withStagedOutputFile(directory, "headerfooter", "pdf") { staged ->
                    staged.writeText("partial header footer pdf")
                    error("synthetic header footer writer failure")
                }
                fail("Expected header footer writer failure")
            } catch (_: IllegalStateException) {
                // Expected.
            }

            assertFalse(directory.resolve("headerfooter.pdf").exists())
            assertTrue(directory.listFiles().orEmpty().none { it.name.startsWith(".anydoc_stage_") })
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun headerFooterCancellation_publishesNoPartialFinal() = runBlocking {
        val directory = Files.createTempDirectory("anydoc-header-footer-cancel").toFile()
        try {
            try {
                withStagedOutputFile(directory, "headerfooter", "pdf") { staged ->
                    staged.writeText("partial header footer pdf")
                    throw CancellationException("synthetic header footer cancellation")
                }
                fail("Expected header footer cancellation")
            } catch (_: CancellationException) {
                // Expected.
            }

            assertFalse(directory.resolve("headerfooter.pdf").exists())
            assertTrue(directory.listFiles().orEmpty().none { it.name.startsWith(".anydoc_stage_") })
        } finally {
            directory.deleteRecursively()
        }
    }
}
