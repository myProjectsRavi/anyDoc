package com.docforge.core.pdf

import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class PdfStagedAndroidSnapshotFailureTest {
    @Test fun androidReaderFailurePreservesSubstitutedForeignStage() = runBlocking {
        val root = Files.createTempDirectory("android-snapshot-failure").toFile()
        try {
            val foreign = File(root, "foreign.bin").apply { writeText("foreign data") }
            val failure = IllegalStateException("android snapshot failure")
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
                    readAndroidIdentity = { stage ->
                        staged = stage
                        Files.move(foreign.toPath(), stage.toPath(), StandardCopyOption.REPLACE_EXISTING)
                        throw failure
                    }
                )
                fail("Expected Android identity reader failure")
            } catch (actual: IllegalStateException) {
                assertSame(failure, actual)
            }

            assertFalse(writerCalled)
            assertFalse(foreign.exists())
            assertTrue(requireNotNull(staged).isFile)
            assertEquals("foreign data", requireNotNull(staged).readText())
            assertFalse(File(root, "output.pdf").exists())
        } finally {
            root.deleteRecursively()
        }
    }
}
