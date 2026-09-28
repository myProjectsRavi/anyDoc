package com.docforge.core.pdf

import java.nio.file.Files
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class PdfComparePublicationSafetyTest {

    @Test
    fun compareOutput_writerFailure_publishesNoPartialFinal() = runBlocking {
        val directory = Files.createTempDirectory("anydoc-compare-output").toFile()
        try {
            try {
                withStagedOutputFile(
                    directory = directory,
                    baseName = "comparison",
                    extension = "pdf"
                ) { staged ->
                    staged.writeText("partial compare pdf")
                    error("synthetic compare write failure")
                }
                fail("Expected compare writer failure")
            } catch (_: IllegalStateException) {
                // Expected.
            }

            assertFalse(directory.resolve("comparison.pdf").exists())
            assertTrue(directory.listFiles().orEmpty().none { it.name.startsWith(".anydoc_stage_") })
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun compareOutput_cancellation_publishesNoPartialFinal() = runBlocking {
        val directory = Files.createTempDirectory("anydoc-compare-cancel").toFile()
        try {
            try {
                withStagedOutputFile(
                    directory = directory,
                    baseName = "comparison",
                    extension = "pdf"
                ) { staged ->
                    staged.writeText("partial compare pdf")
                    throw CancellationException("synthetic cancellation")
                }
                fail("Expected cancellation")
            } catch (_: CancellationException) {
                // Expected.
            }

            assertFalse(directory.resolve("comparison.pdf").exists())
            assertTrue(directory.listFiles().orEmpty().none { it.name.startsWith(".anydoc_stage_") })
        } finally {
            directory.deleteRecursively()
        }
    }
}
