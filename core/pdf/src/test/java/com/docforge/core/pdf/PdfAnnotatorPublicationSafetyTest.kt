package com.docforge.core.pdf

import java.nio.file.Files
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class PdfAnnotatorPublicationSafetyTest {

    @Test
    fun annotatorWriterFailure_publishesNoPartialFinal() = runBlocking {
        val directory = Files.createTempDirectory("anydoc-annotator-output").toFile()
        try {
            try {
                withStagedOutputFile(directory, "annotated", "pdf") { staged ->
                    staged.writeText("partial annotated pdf")
                    error("synthetic annotator write failure")
                }
                fail("Expected annotator writer failure")
            } catch (_: IllegalStateException) {
                // Expected.
            }

            assertFalse(directory.resolve("annotated.pdf").exists())
            assertTrue(directory.listFiles().orEmpty().none { it.name.startsWith(".anydoc_stage_") })
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun annotatorCancellation_publishesNoPartialFinal() = runBlocking {
        val directory = Files.createTempDirectory("anydoc-annotator-cancel").toFile()
        try {
            try {
                withStagedOutputFile(directory, "annotated", "pdf") { staged ->
                    staged.writeText("partial annotated pdf")
                    throw CancellationException("synthetic annotator cancellation")
                }
                fail("Expected annotator cancellation")
            } catch (_: CancellationException) {
                // Expected.
            }

            assertFalse(directory.resolve("annotated.pdf").exists())
            assertTrue(directory.listFiles().orEmpty().none { it.name.startsWith(".anydoc_stage_") })
        } finally {
            directory.deleteRecursively()
        }
    }
}
