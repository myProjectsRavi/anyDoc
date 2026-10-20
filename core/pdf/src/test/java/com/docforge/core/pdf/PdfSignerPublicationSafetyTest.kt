package com.docforge.core.pdf

import java.nio.file.Files
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class PdfSignerPublicationSafetyTest {

    @Test
    fun signerWriterFailure_publishesNoPartialFinal() = runBlocking {
        val directory = Files.createTempDirectory("anydoc-signer-output").toFile()
        try {
            try {
                withStagedOutputFile(directory, "signed", "pdf") { staged ->
                    staged.writeText("partial signed pdf")
                    error("synthetic signer write failure")
                }
                fail("Expected signer writer failure")
            } catch (_: IllegalStateException) {
                // Expected.
            }

            assertFalse(directory.resolve("signed.pdf").exists())
            assertTrue(directory.listFiles().orEmpty().none { it.name.startsWith(".anydoc_stage_") })
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun signerCancellation_publishesNoPartialFinal() = runBlocking {
        val directory = Files.createTempDirectory("anydoc-signer-cancel").toFile()
        try {
            try {
                withStagedOutputFile(directory, "signed", "pdf") { staged ->
                    staged.writeText("partial signed pdf")
                    throw CancellationException("synthetic signer cancellation")
                }
                fail("Expected signer cancellation")
            } catch (_: CancellationException) {
                // Expected.
            }

            assertFalse(directory.resolve("signed.pdf").exists())
            assertTrue(directory.listFiles().orEmpty().none { it.name.startsWith(".anydoc_stage_") })
        } finally {
            directory.deleteRecursively()
        }
    }
}
