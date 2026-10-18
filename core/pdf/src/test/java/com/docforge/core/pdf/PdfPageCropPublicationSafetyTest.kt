package com.docforge.core.pdf

import java.nio.file.Files
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class PdfPageCropPublicationSafetyTest {

    @Test
    fun cropWriterFailure_publishesNoPartialFinal() = runBlocking {
        val directory = Files.createTempDirectory("anydoc-crop-output").toFile()
        try {
            try {
                withStagedOutputFile(directory, "cropped", "pdf") { staged ->
                    staged.writeText("partial cropped pdf")
                    error("synthetic crop writer failure")
                }
                fail("Expected crop writer failure")
            } catch (_: IllegalStateException) {
                // Expected.
            }

            assertFalse(directory.resolve("cropped.pdf").exists())
            assertTrue(directory.listFiles().orEmpty().none { it.name.startsWith(".anydoc_stage_") })
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun cropCancellation_publishesNoPartialFinal() = runBlocking {
        val directory = Files.createTempDirectory("anydoc-crop-cancel").toFile()
        try {
            try {
                withStagedOutputFile(directory, "cropped", "pdf") { staged ->
                    staged.writeText("partial cropped pdf")
                    throw CancellationException("synthetic crop cancellation")
                }
                fail("Expected crop cancellation")
            } catch (_: CancellationException) {
                // Expected.
            }

            assertFalse(directory.resolve("cropped.pdf").exists())
            assertTrue(directory.listFiles().orEmpty().none { it.name.startsWith(".anydoc_stage_") })
        } finally {
            directory.deleteRecursively()
        }
    }
}
