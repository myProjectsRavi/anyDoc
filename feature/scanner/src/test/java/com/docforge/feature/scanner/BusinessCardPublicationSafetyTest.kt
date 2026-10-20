package com.docforge.feature.scanner

import com.docforge.core.pdf.withStagedOutputFile
import java.nio.file.Files
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class BusinessCardPublicationSafetyTest {

    @Test
    fun writerFailure_publishesNoPartialVCard() = runBlocking {
        val directory = Files.createTempDirectory("anydoc-business-card").toFile()
        try {
            try {
                withStagedOutputFile(directory, "contact", "vcf") { staged ->
                    staged.writeText("BEGIN:VCARD\nPARTIAL")
                    error("synthetic writer failure")
                }
                fail("Expected writer failure")
            } catch (_: IllegalStateException) {
                // Expected.
            }

            assertFalse(directory.resolve("contact.vcf").exists())
            assertTrue(directory.listFiles().orEmpty().none { it.name.startsWith(".anydoc_stage_") })
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun cancellation_publishesNoPartialVCard() = runBlocking {
        val directory = Files.createTempDirectory("anydoc-business-card-cancel").toFile()
        try {
            try {
                withStagedOutputFile(directory, "contact", "vcf") { staged ->
                    staged.writeText("BEGIN:VCARD\nPARTIAL")
                    throw CancellationException("synthetic cancellation")
                }
                fail("Expected cancellation")
            } catch (_: CancellationException) {
                // Expected.
            }

            assertFalse(directory.resolve("contact.vcf").exists())
            assertTrue(directory.listFiles().orEmpty().none { it.name.startsWith(".anydoc_stage_") })
        } finally {
            directory.deleteRecursively()
        }
    }
}
