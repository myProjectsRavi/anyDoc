package com.docforge.core.pdf

import java.nio.file.Files
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class PdfFormPublicationSafetyTest {

    @Test
    fun formWriterFailure_publishesNoPartialFinal() = runBlocking {
        val directory = Files.createTempDirectory("anydoc-form-output").toFile()
        try {
            try {
                withStagedOutputFile(directory, "form", "pdf") { staged ->
                    staged.writeText("partial form pdf")
                    error("synthetic form writer failure")
                }
                fail("Expected form writer failure")
            } catch (_: IllegalStateException) {
                // Expected.
            }

            assertFalse(directory.resolve("form.pdf").exists())
            assertTrue(directory.listFiles().orEmpty().none { it.name.startsWith(".anydoc_stage_") })
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun formCancellation_publishesNoPartialFinal() = runBlocking {
        val directory = Files.createTempDirectory("anydoc-form-cancel").toFile()
        try {
            try {
                withStagedOutputFile(directory, "form", "pdf") { staged ->
                    staged.writeText("partial form pdf")
                    throw CancellationException("synthetic form cancellation")
                }
                fail("Expected form cancellation")
            } catch (_: CancellationException) {
                // Expected.
            }

            assertFalse(directory.resolve("form.pdf").exists())
            assertTrue(directory.listFiles().orEmpty().none { it.name.startsWith(".anydoc_stage_") })
        } finally {
            directory.deleteRecursively()
        }
    }
}
