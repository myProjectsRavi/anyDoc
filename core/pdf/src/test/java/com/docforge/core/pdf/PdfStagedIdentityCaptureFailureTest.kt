package com.docforge.core.pdf

import java.io.File
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.attribute.BasicFileAttributes
import java.util.concurrent.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Assume.assumeTrue
import org.junit.Test

class PdfStagedIdentityCaptureFailureTest {
    private fun key(file: File): Any? = Files.readAttributes(
        file.toPath(), BasicFileAttributes::class.java, LinkOption.NOFOLLOW_LINKS
    ).fileKey()

    private fun androidReader(): (File) -> StagedAndroidIdentity? {
        val ids = mutableMapOf<Any, Long>()
        return { file ->
            val k = key(file)
            if (k == null) null else StagedAndroidIdentity(19L, ids.getOrPut(k) { ids.size.toLong() + 1L })
        }
    }

    @Test fun unsupportedNioReaderUsesAndroidFallbackForPublication() = runBlocking {
        val root = Files.createTempDirectory("snapshot-fallback").toFile()
        try {
            val probe = File(root, "probe").apply { writeText("probe") }
            assumeTrue(key(probe) != null)
            assertTrue(probe.delete())
            val result = withStagedOutputFilesUsingIdentityReaders(
                root, listOf(StagedOutputRequest("output", "pdf") { stage -> stage.writeText("owned"); 5 }),
                readNioFileKey = { throw UnsupportedOperationException("no NIO identity") },
                readAndroidIdentity = androidReader()
            ).single()
            assertEquals("owned", result.outputFile.readText())
            assertEquals(5, result.value)
            assertTrue(root.listFiles().orEmpty().none { it.name.startsWith(".anydoc_stage_") })
        } finally { root.deleteRecursively() }
    }

    @Test fun unsupportedNioReaderCancellationCleansOwnedStage() = runBlocking {
        val root = Files.createTempDirectory("snapshot-cancel").toFile()
        try {
            val probe = File(root, "probe").apply { writeText("probe") }
            assumeTrue(key(probe) != null)
            assertTrue(probe.delete())
            var staged: File? = null
            try {
                withStagedOutputFilesUsingIdentityReaders(
                    root, listOf(StagedOutputRequest<Int>("output", "pdf") { stage ->
                        staged = stage
                        stage.writeText("owned")
                        throw CancellationException("cancel")
                    }),
                    readNioFileKey = { throw UnsupportedOperationException("no NIO identity") },
                    readAndroidIdentity = androidReader()
                )
                fail("Expected cancellation")
            } catch (expected: CancellationException) {
                assertEquals("cancel", expected.message)
            }
            assertFalse(requireNotNull(staged).exists())
            assertFalse(File(root, "output.pdf").exists())
        } finally { root.deleteRecursively() }
    }

    @Test fun unexpectedReaderFailureStopsBeforeWriterWithoutUnownedDelete() = runBlocking {
        val root = Files.createTempDirectory("snapshot-fail").toFile()
        try {
            var staged: File? = null
            var writerCalled = false
            try {
                withStagedOutputFilesUsingIdentityReaders(
                    root, listOf(StagedOutputRequest("output", "pdf") { writerCalled = true; 1 }),
                    readNioFileKey = { file -> staged = file; throw IllegalStateException("snapshot failure") },
                    readAndroidIdentity = { null }
                )
                fail("Expected snapshot failure")
            } catch (expected: IllegalStateException) {
                assertEquals("snapshot failure", expected.message)
            }
            assertFalse(writerCalled)
            assertFalse(File(root, "output.pdf").exists())
            assertTrue("Unknown original identity fails closed", requireNotNull(staged).exists())
        } finally { root.deleteRecursively() }
    }
}
