package com.docforge.core.pdf

import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class PdfStagedRollbackSelfSuppressionTest {
    private val identity = StagedAndroidIdentity(9L, 33L)

    @Test fun directPublicationPreservesPrimaryWhenRollbackThrowsSameThrowable() {
        val root = Files.createTempDirectory("staged-same-error").toFile()
        try {
            val stage = File(root, ".stage.part").apply { writeText("ours") }
            val output = File(root, "result.pdf")
            val failure = IllegalStateException("identity read failed")
            var outputReads = 0
            try {
                publishCheckedStagedOutput(
                    stage, output,
                    readNioFileKey = { null },
                    readAndroidIdentity = { file ->
                        if (file == output) {
                            outputReads++
                            throw failure
                        }
                        identity
                    }
                )
                fail("Expected primary identity failure")
            } catch (actual: IllegalStateException) {
                assertSame(failure, actual)
                assertTrue(actual.suppressed.isEmpty())
            }
            assertEquals(2, outputReads)
            assertEquals("ours", output.readText())
            assertFalse(stage.exists())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun directPublicationRetainsDistinctRollbackFailureAsSuppressed() {
        val root = Files.createTempDirectory("staged-distinct-error").toFile()
        try {
            val stage = File(root, ".stage.part").apply { writeText("ours") }
            val output = File(root, "result.pdf")
            val primary = IllegalStateException("primary")
            val cleanup = IllegalStateException("rollback")
            var outputReads = 0
            try {
                publishCheckedStagedOutput(
                    stage, output,
                    readNioFileKey = { null },
                    readAndroidIdentity = { file ->
                        if (file == output) {
                            outputReads++
                            throw if (outputReads == 1) primary else cleanup
                        }
                        identity
                    }
                )
                fail("Expected primary identity failure")
            } catch (actual: IllegalStateException) {
                assertSame(primary, actual)
                assertEquals(1, actual.suppressed.size)
                assertSame(cleanup, actual.suppressed[0])
            }
            assertEquals(2, outputReads)
            assertEquals("ours", output.readText())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun rollbackSuccessPreservesPrimaryAndRemovesOwnedOutput() {
        val root = Files.createTempDirectory("staged-rollback-success").toFile()
        try {
            val stage = File(root, ".stage.part").apply { writeText("ours") }
            val output = File(root, "result.pdf")
            val primary = IllegalStateException("validation failed")
            var outputReads = 0
            try {
                publishCheckedStagedOutput(
                    stage, output,
                    readNioFileKey = { null },
                    readAndroidIdentity = { file ->
                        if (file == output && ++outputReads == 1) throw primary
                        identity
                    }
                )
                fail("Expected validation failure")
            } catch (actual: IllegalStateException) {
                assertSame(primary, actual)
                assertTrue(actual.suppressed.isEmpty())
            }
            assertEquals(2, outputReads)
            assertFalse(output.exists())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun normalAndroidIdentityFallbackStillPublishes() {
        val root = Files.createTempDirectory("staged-normal").toFile()
        try {
            val stage = File(root, ".stage.part").apply { writeText("ours") }
            val output = File(root, "result.pdf")
            publishCheckedStagedOutput(
                stage, output,
                readNioFileKey = { null },
                readAndroidIdentity = { identity }
            )
            assertEquals("ours", output.readText())
            assertFalse(stage.exists())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun helperSkipsSelfSuppressionAndKeepsDistinctFailure() {
        val primary = IllegalStateException("primary")
        val secondary = IllegalStateException("secondary")
        addSuppressedUnlessSame(primary, primary)
        assertTrue(primary.suppressed.isEmpty())
        addSuppressedUnlessSame(primary, secondary)
        assertEquals(1, primary.suppressed.size)
        assertSame(secondary, primary.suppressed[0])
    }
}
