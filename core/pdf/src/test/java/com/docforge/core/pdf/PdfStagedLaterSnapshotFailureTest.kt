package com.docforge.core.pdf

import java.io.File
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.attribute.BasicFileAttributes
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Assume.assumeTrue
import org.junit.Test

class PdfStagedLaterSnapshotFailureTest {
    @Test fun laterIdentityCaptureFailureCleansEarlierOwnedStage() = runBlocking {
        val root = Files.createTempDirectory("later-snapshot-failure").toFile()
        try {
            val probe = File(root, "probe").apply { writeText("probe") }
            assumeTrue(Files.readAttributes(probe.toPath(), BasicFileAttributes::class.java, LinkOption.NOFOLLOW_LINKS).fileKey() != null)
            probe.delete()
            var first: File? = null
            var second: File? = null
            var reads = 0
            try {
                withStagedOutputFilesUsingIdentityReaders(
                    root,
                    listOf(
                        StagedOutputRequest("first", "pdf") { stage -> first = stage; stage.writeText("owned"); 1 },
                        StagedOutputRequest("second", "pdf") { stage -> stage.writeText("unexpected"); 2 }
                    ),
                    readNioFileKey = { stage ->
                        reads++
                        if (reads == 2) {
                            second = stage
                            throw IllegalStateException("later snapshot failure")
                        }
                        Files.readAttributes(stage.toPath(), BasicFileAttributes::class.java, LinkOption.NOFOLLOW_LINKS).fileKey()
                    },
                    readAndroidIdentity = { null }
                )
                fail("Expected identity snapshot failure")
            } catch (expected: IllegalStateException) {
                assertEquals("later snapshot failure", expected.message)
            }
            assertFalse(requireNotNull(first).exists())
            assertTrue(requireNotNull(second).exists())
            assertFalse(File(root, "first.pdf").exists())
            assertFalse(File(root, "second.pdf").exists())
        } finally { root.deleteRecursively() }
    }
}
