package com.docforge.core.pdf

import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class PdfStagedPreparedCleanupFailureTest {
    private val identity = StagedAndroidIdentity(41L, 59L)

    @Test fun distinctCleanupErrorIsSuppressedOnLaterWriterFailure() = runBlocking {
        val root = Files.createTempDirectory("prepared-cleanup-distinct").toFile()
        val writer = IllegalStateException("later writer")
        val cleanup = IllegalArgumentException("identity reader")
        var first: File? = null
        var second: File? = null
        try {
            try {
                withStagedOutputFilesUsingIdentityReaders(root, listOf(
                    StagedOutputRequest<Int>("first", "pdf") { file -> first = file; file.writeText("first"); 1 },
                    StagedOutputRequest<Int>("second", "pdf") { file -> second = file; throw writer }
                ), { null }, { file ->
                    if (file == first && second != null) throw cleanup
                    identity
                })
                fail("Expected writer failure")
            } catch (actual: IllegalStateException) {
                assertSame(writer, actual)
                assertEquals(1, actual.suppressed.size)
                assertSame(cleanup, actual.suppressed.single())
            }
            assertTrue(first!!.exists())
            assertFalse(second!!.exists())
            assertFalse(root.resolve("first.pdf").exists())
        } finally { root.deleteRecursively() }
    }

    @Test fun sameThrowableCannotSuppressItself() = runBlocking {
        val root = Files.createTempDirectory("prepared-cleanup-same").toFile()
        val writer = IllegalStateException("same throwable")
        var first: File? = null
        var second: File? = null
        try {
            try {
                withStagedOutputFilesUsingIdentityReaders(root, listOf(
                    StagedOutputRequest<Int>("first", "pdf") { file -> first = file; file.writeText("first"); 1 },
                    StagedOutputRequest<Int>("second", "pdf") { file -> second = file; throw writer }
                ), { null }, { file ->
                    if (file == first && second != null) throw writer
                    identity
                })
                fail("Expected writer failure")
            } catch (actual: IllegalStateException) {
                assertSame(writer, actual)
                assertTrue(actual.suppressed.isEmpty())
            }
            assertTrue(first!!.exists())
            assertFalse(second!!.exists())
        } finally { root.deleteRecursively() }
    }

    @Test fun multipleCleanupFailuresArePreservedInOrder() = runBlocking {
        val root = Files.createTempDirectory("prepared-cleanup-multiple").toFile()
        val writer = IllegalStateException("third writer")
        val firstError = IllegalArgumentException("first cleanup")
        val secondError = IllegalArgumentException("second cleanup")
        var first: File? = null
        var second: File? = null
        var third: File? = null
        try {
            try {
                withStagedOutputFilesUsingIdentityReaders(root, listOf(
                    StagedOutputRequest<Int>("first", "pdf") { file -> first = file; file.writeText("first"); 1 },
                    StagedOutputRequest<Int>("second", "pdf") { file -> second = file; file.writeText("second"); 2 },
                    StagedOutputRequest<Int>("third", "pdf") { file -> third = file; throw writer }
                ), { null }, { file ->
                    if (third != null && file == first) throw firstError
                    if (third != null && file == second) throw secondError
                    identity
                })
                fail("Expected writer failure")
            } catch (actual: IllegalStateException) {
                assertSame(writer, actual)
                assertEquals(2, actual.suppressed.size)
                assertSame(firstError, actual.suppressed[0])
                assertSame(secondError, actual.suppressed[1])
            }
            assertTrue(first!!.exists())
            assertTrue(second!!.exists())
            assertFalse(third!!.exists())
        } finally { root.deleteRecursively() }
    }

    @Test fun identityMismatchFailsClosed() = runBlocking {
        val root = Files.createTempDirectory("prepared-cleanup-mismatch").toFile()
        val writer = IllegalStateException("later writer")
        var first: File? = null
        var second: File? = null
        try {
            try {
                withStagedOutputFilesUsingIdentityReaders(root, listOf(
                    StagedOutputRequest<Int>("first", "pdf") { file -> first = file; file.writeText("first"); 1 },
                    StagedOutputRequest<Int>("second", "pdf") { file -> second = file; throw writer }
                ), { null }, { file ->
                    if (file == first && second != null) StagedAndroidIdentity(41L, 60L) else identity
                })
                fail("Expected writer failure")
            } catch (actual: IllegalStateException) {
                assertSame(writer, actual)
                assertTrue(actual.suppressed.isEmpty())
            }
            assertTrue(first!!.exists())
            assertFalse(second!!.exists())
        } finally { root.deleteRecursively() }
    }

    @Test fun successfulBatchPublishesOnlyFinalOutputs() = runBlocking {
        val root = Files.createTempDirectory("prepared-cleanup-success").toFile()
        try {
            val result = withStagedOutputFilesUsingIdentityReaders(root, listOf(
                StagedOutputRequest("first", "pdf") { file -> file.writeText("first"); 1 },
                StagedOutputRequest("second", "pdf") { file -> file.writeText("second"); 2 }
            ), { null }, { identity })
            assertEquals(listOf(1, 2), result.map { it.value })
            assertEquals(setOf("first.pdf", "second.pdf"), root.listFiles().orEmpty().map { it.name }.toSet())
            assertEquals("first", root.resolve("first.pdf").readText())
            assertEquals("second", root.resolve("second.pdf").readText())
        } finally { root.deleteRecursively() }
    }
}
