package com.docforge.core.pdf

import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/** Unknown identity must not permit an unverified staged file to be published. */
class PdfStagedUnknownIdentityPublicationTest {
    @Test fun unavailableIdentityRejectsBeforeWriter() = runBlocking {
        val root = Files.createTempDirectory("stage-no-identity").toFile()
        try {
            var staged: File? = null
            var writerCalled = false
            try {
                withStagedOutputFilesUsingIdentityReaders(
                    root,
                    listOf(StagedOutputRequest<Int>("output", "pdf") {
                        writerCalled = true
                        1
                    }),
                    readNioFileKey = { file -> staged = file; null },
                    readAndroidIdentity = { null }
                )
                fail("Expected unknown identity to prevent publication")
            } catch (expected: IllegalStateException) {
                assertTrue(expected.message.orEmpty().contains("identity"))
            }
            assertFalse(writerCalled)
            assertTrue("Unverified staging entry must not be deleted", requireNotNull(staged).exists())
            assertFalse(File(root, "output.pdf").exists())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun foreignReplacementWithUnknownIdentityIsNeverPublished() = runBlocking {
        val root = Files.createTempDirectory("stage-unknown-replacement").toFile()
        try {
            val foreign = File(root, "foreign.bin").apply { writeText("foreign data") }
            var staged: File? = null
            var writerCalled = false
            try {
                withStagedOutputFilesUsingIdentityReaders(
                    root,
                    listOf(StagedOutputRequest<Int>("output", "pdf") {
                        writerCalled = true
                        1
                    }),
                    readNioFileKey = { null },
                    readAndroidIdentity = { file ->
                        staged = file
                        Files.move(foreign.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING)
                        null
                    }
                )
                fail("Expected unknown identity to prevent publication")
            } catch (expected: IllegalStateException) {
                assertTrue(expected.message.orEmpty().contains("identity"))
            }
            assertFalse(writerCalled)
            assertFalse(foreign.exists())
            assertEquals("foreign data", requireNotNull(staged).readText())
            assertFalse(File(root, "output.pdf").exists())
        } finally {
            root.deleteRecursively()
        }
    }
}
