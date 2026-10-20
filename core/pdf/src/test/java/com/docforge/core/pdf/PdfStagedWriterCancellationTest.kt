package com.docforge.core.pdf

import java.nio.file.Files
import java.util.concurrent.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class PdfStagedWriterCancellationTest {
    @Test fun cancelledWriterWithMissingStageReportsCancellationFirst() = runBlocking {
        val root = Files.createTempDirectory("stage-writer-missing").toFile()
        try {
            var caughtCancellation = false
            val worker = launch(start = CoroutineStart.UNDISPATCHED) {
                try {
                    withStagedOutputFile(root, "output", "pdf") { stage ->
                        assertTrue(stage.delete())
                        currentCoroutineContext().cancel()
                    }
                    fail("Expected cancellation before staged-file validation")
                } catch (_: CancellationException) {
                    caughtCancellation = true
                }
            }
            worker.join()
            assertTrue(caughtCancellation)
            assertFalse(root.resolve("output.pdf").exists())
            assertTrue(root.listFiles().orEmpty().isEmpty())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun cancelledWriterWithValidStageDoesNotPublish() = runBlocking {
        val root = Files.createTempDirectory("stage-writer-cancel").toFile()
        try {
            var caughtCancellation = false
            val worker = launch(start = CoroutineStart.UNDISPATCHED) {
                try {
                    withStagedOutputFile(root, "output", "pdf") { stage ->
                        stage.writeText("unpublished")
                        currentCoroutineContext().cancel()
                    }
                    fail("Expected cancellation")
                } catch (_: CancellationException) {
                    caughtCancellation = true
                }
            }
            worker.join()
            assertTrue(caughtCancellation)
            assertFalse(root.resolve("output.pdf").exists())
            assertTrue(root.listFiles().orEmpty().isEmpty())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun cancelledFirstWriterNeverRunsSecondWriter() = runBlocking {
        val root = Files.createTempDirectory("stage-writer-second").toFile()
        try {
            var ranSecond = false
            var caughtCancellation = false
            val worker = launch(start = CoroutineStart.UNDISPATCHED) {
                try {
                    withStagedOutputFiles(
                        root,
                        listOf(
                            StagedOutputRequest("first", "pdf") { stage ->
                                stage.writeText("first")
                                currentCoroutineContext().cancel()
                            },
                            StagedOutputRequest("second", "pdf") { stage ->
                                ranSecond = true
                                stage.writeText("second")
                            }
                        )
                    )
                    fail("Expected cancellation")
                } catch (_: CancellationException) {
                    caughtCancellation = true
                }
            }
            worker.join()
            assertTrue(caughtCancellation)
            assertFalse(ranSecond)
            assertTrue(root.listFiles().orEmpty().isEmpty())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun successfulWriterStillPublishesResultAndValue() = runBlocking {
        val root = Files.createTempDirectory("stage-writer-success").toFile()
        try {
            val result = withStagedOutputFile(root, "output", "pdf") { stage ->
                stage.writeText("valid")
                27
            }
            assertEquals(27, result.value)
            assertEquals("output.pdf", result.outputFile.name)
            assertEquals("valid", result.outputFile.readText())
            assertEquals(1, root.listFiles().orEmpty().size)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun writerExceptionPreservesOriginalInstance() = runBlocking {
        val root = Files.createTempDirectory("stage-writer-failure").toFile()
        val expected = IllegalStateException("writer failed")
        try {
            try {
                withStagedOutputFile(root, "output", "pdf") { stage ->
                    stage.writeText("partial")
                    throw expected
                }
                fail("Expected original writer exception")
            } catch (actual: IllegalStateException) {
                assertSame(expected, actual)
            }
            assertTrue(root.listFiles().orEmpty().isEmpty())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun writerFailureAfterCancellationPreservesOriginalException() = runBlocking {
        val root = Files.createTempDirectory("stage-writer-cancel-error").toFile()
        val expected = IllegalArgumentException("writer failed while cancelled")
        try {
            var caughtOriginal = false
            val worker = launch(start = CoroutineStart.UNDISPATCHED) {
                try {
                    withStagedOutputFile(root, "output", "pdf") { stage ->
                        stage.writeText("partial")
                        currentCoroutineContext().cancel()
                        throw expected
                    }
                    fail("Expected writer exception")
                } catch (actual: IllegalArgumentException) {
                    assertSame(expected, actual)
                    caughtOriginal = true
                }
            }
            worker.join()
            assertTrue(caughtOriginal)
            assertTrue(root.listFiles().orEmpty().isEmpty())
        } finally {
            root.deleteRecursively()
        }
    }
}
