package com.docforge.core.pdf

import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class PdfStagedWriterCleanupSelfSuppressionTest {
    private val identity = StagedAndroidIdentity(71L, 83L)

    @Test fun sameThrowableFromWriterAndCleanupPreservesWriterFailure() = runBlocking {
        val root = Files.createTempDirectory("staged-writer-same").toFile()
        val primary = IllegalStateException("writer and cleanup")
        var identityReads = 0
        try {
            try {
                withStagedOutputFilesUsingIdentityReaders(
                    root,
                    listOf(StagedOutputRequest("output", "pdf") { stage ->
                        stage.writeText("partial")
                        throw primary
                    }),
                    readNioFileKey = { null },
                    readAndroidIdentity = {
                        identityReads++
                        if (identityReads == 2) throw primary
                        identity
                    }
                )
                fail("Expected writer failure")
            } catch (actual: IllegalStateException) {
                assertSame(primary, actual)
                assertTrue(actual.suppressed.isEmpty())
            }
            assertEquals(2, identityReads)
            assertFalse(root.resolve("output.pdf").exists())
            // Identity verification failed: the stage must be preserved, not deleted blindly.
            assertEquals(1, root.listFiles().orEmpty().size)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun distinctCleanupFailureIsSuppressedOnWriterFailure() = runBlocking {
        val root = Files.createTempDirectory("staged-writer-distinct").toFile()
        val primary = IllegalStateException("writer")
        val cleanup = IllegalArgumentException("identity reader")
        var identityReads = 0
        try {
            try {
                withStagedOutputFilesUsingIdentityReaders(
                    root,
                    listOf(StagedOutputRequest("output", "pdf") { stage ->
                        stage.writeText("partial")
                        throw primary
                    }),
                    readNioFileKey = { null },
                    readAndroidIdentity = {
                        identityReads++
                        if (identityReads == 2) throw cleanup
                        identity
                    }
                )
                fail("Expected writer failure")
            } catch (actual: IllegalStateException) {
                assertSame(primary, actual)
                assertEquals(1, actual.suppressed.size)
                assertSame(cleanup, actual.suppressed[0])
            }
            assertEquals(2, identityReads)
            assertFalse(root.resolve("output.pdf").exists())
            assertEquals(1, root.listFiles().orEmpty().size)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun successfulOwnedStageCleanupKeepsOriginalWriterFailure() = runBlocking {
        val root = Files.createTempDirectory("staged-writer-cleanup").toFile()
        val primary = IllegalStateException("writer")
        try {
            try {
                withStagedOutputFilesUsingIdentityReaders(
                    root,
                    listOf(StagedOutputRequest("output", "pdf") { stage ->
                        stage.writeText("partial")
                        throw primary
                    }),
                    readNioFileKey = { null },
                    readAndroidIdentity = { identity }
                )
                fail("Expected writer failure")
            } catch (actual: IllegalStateException) {
                assertSame(primary, actual)
                assertTrue(actual.suppressed.isEmpty())
            }
            assertTrue(root.listFiles().orEmpty().isEmpty())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun normalStagedPublicationStillWorksWithAndroidIdentity() = runBlocking {
        val root = Files.createTempDirectory("staged-writer-publish").toFile()
        try {
            val results = withStagedOutputFilesUsingIdentityReaders(
                root,
                listOf(StagedOutputRequest("output", "pdf") { stage ->
                    stage.writeText("complete")
                    42
                }),
                readNioFileKey = { null },
                readAndroidIdentity = { identity }
            )
            assertEquals(1, results.size)
            assertEquals(42, results.single().value)
            assertEquals("complete", results.single().outputFile.readText())
            assertEquals(1, root.listFiles().orEmpty().size)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun failedFirstWriterDoesNotRunNextWriter() = runBlocking {
        val root = Files.createTempDirectory("staged-writer-batch").toFile()
        val primary = IllegalStateException("first writer")
        var secondRan = false
        try {
            try {
                withStagedOutputFilesUsingIdentityReaders(
                    root,
                    listOf(
                        StagedOutputRequest("first", "pdf") { stage ->
                            stage.writeText("partial")
                            throw primary
                        },
                        StagedOutputRequest("second", "pdf") { stage ->
                            secondRan = true
                            stage.writeText("unexpected")
                        }
                    ),
                    readNioFileKey = { null },
                    readAndroidIdentity = { identity }
                )
                fail("Expected first writer failure")
            } catch (actual: IllegalStateException) {
                assertSame(primary, actual)
            }
            assertFalse(secondRan)
            assertTrue(root.listFiles().orEmpty().isEmpty())
        } finally {
            root.deleteRecursively()
        }
    }
}
