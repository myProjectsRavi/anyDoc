package com.docforge.core.pdf

import java.nio.file.Files
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class PdfPasswordPublicationSafetyTest {

    @Test
    fun passwordWriterFailure_publishesNoPartialFinal() = runBlocking {
        val directory = Files.createTempDirectory("anydoc-password-output").toFile()
        try {
            try {
                withStagedOutputFile(directory, "protected", "pdf") { staged ->
                    staged.writeText("partial protected pdf")
                    error("synthetic password writer failure")
                }
                fail("Expected password writer failure")
            } catch (_: IllegalStateException) {
                // Expected.
            }

            assertFalse(directory.resolve("protected.pdf").exists())
            assertTrue(directory.listFiles().orEmpty().none { it.name.startsWith(".anydoc_stage_") })
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun passwordCancellation_publishesNoPartialFinal() = runBlocking {
        val directory = Files.createTempDirectory("anydoc-password-cancel").toFile()
        try {
            try {
                withStagedOutputFile(directory, "unlocked", "pdf") { staged ->
                    staged.writeText("partial unlocked pdf")
                    throw CancellationException("synthetic password cancellation")
                }
                fail("Expected password cancellation")
            } catch (_: CancellationException) {
                // Expected.
            }

            assertFalse(directory.resolve("unlocked.pdf").exists())
            assertTrue(directory.listFiles().orEmpty().none { it.name.startsWith(".anydoc_stage_") })
        } finally {
            directory.deleteRecursively()
        }
    }
}
