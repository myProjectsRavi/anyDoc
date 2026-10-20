package com.docforge.core.pdf

import java.io.File
import java.nio.file.Files
import java.util.concurrent.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class PdfStagedEntryCancellationTest {
    @Test fun cancelledEmptyBatchDoesNotCreateOutputDirectory() = runBlocking {
        val root = Files.createTempDirectory("stage-cancel-empty").toFile()
        val output = File(root, "not-created")
        try {
            var cancelled = false
            val worker = launch(start = CoroutineStart.UNDISPATCHED) {
                coroutineContext.cancel()
                try {
                    withStagedOutputFiles<Unit>(output, emptyList())
                    fail("Expected cancellation before directory setup")
                } catch (_: CancellationException) {
                    cancelled = true
                }
            }
            worker.join()
            assertTrue(cancelled)
            assertFalse(output.exists())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun cancelledNonemptyBatchDoesNotCreateDirectoryOrRunWriter() = runBlocking {
        val root = Files.createTempDirectory("stage-cancel-writer").toFile()
        val output = File(root, "not-created")
        try {
            var cancelled = false
            var wrote = false
            val worker = launch(start = CoroutineStart.UNDISPATCHED) {
                coroutineContext.cancel()
                try {
                    withStagedOutputFiles(
                        output,
                        listOf(StagedOutputRequest("result", "pdf") { stage ->
                            wrote = true
                            stage.writeText("unexpected")
                        })
                    )
                    fail("Expected cancellation before staging")
                } catch (_: CancellationException) {
                    cancelled = true
                }
            }
            worker.join()
            assertTrue(cancelled)
            assertFalse(wrote)
            assertFalse(output.exists())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun cancelledExistingDirectorySkipsIdentityReadersAndLeavesFilesAlone() = runBlocking {
        val root = Files.createTempDirectory("stage-cancel-existing").toFile()
        val existing = File(root, "keep.txt").apply { writeText("untouched") }
        try {
            var cancelled = false
            var identityReads = 0
            val worker = launch(start = CoroutineStart.UNDISPATCHED) {
                coroutineContext.cancel()
                try {
                    withStagedOutputFilesUsingIdentityReaders(
                        root,
                        listOf(StagedOutputRequest("new", "pdf") { stage ->
                            stage.writeText("unexpected")
                        }),
                        readNioFileKey = { identityReads++; null },
                        readAndroidIdentity = { identityReads++; null }
                    )
                    fail("Expected cancellation before staging")
                } catch (_: CancellationException) {
                    cancelled = true
                }
            }
            worker.join()
            assertTrue(cancelled)
            assertEquals(0, identityReads)
            assertEquals("untouched", existing.readText())
            assertEquals(listOf("keep.txt"), root.listFiles().orEmpty().map { it.name })
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun activeEmptyBatchKeepsExistingDirectoryBehavior() = runBlocking {
        val root = Files.createTempDirectory("stage-active-empty").toFile()
        val output = File(root, "created")
        try {
            val results = withStagedOutputFiles<Unit>(output, emptyList())
            assertTrue(results.isEmpty())
            assertTrue(output.isDirectory)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun activeWriterPublishesOutputAndValue() = runBlocking {
        val root = Files.createTempDirectory("stage-active-writer").toFile()
        val output = File(root, "created")
        try {
            val result = withStagedOutputFile(output, "result", "pdf") { stage ->
                stage.writeText("payload")
                42
            }
            assertEquals(42, result.value)
            assertEquals("result.pdf", result.outputFile.name)
            assertEquals("payload", result.outputFile.readText())
            assertEquals(1, output.listFiles().orEmpty().size)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun activeInvalidOutputNameStillFailsBeforeDirectoryCreation() = runBlocking {
        val root = Files.createTempDirectory("stage-invalid-name").toFile()
        val output = File(root, "not-created")
        try {
            try {
                withStagedOutputFiles(
                    output,
                    listOf(StagedOutputRequest("../unsafe", "pdf") { _: File -> Unit })
                )
                fail("Expected invalid name rejection")
            } catch (expected: IllegalArgumentException) {
                assertEquals("Invalid output name.", expected.message)
            }
            assertFalse(output.exists())
        } finally {
            root.deleteRecursively()
        }
    }
}
