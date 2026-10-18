package com.docforge.core.pdf

import java.io.File
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.BasicFileAttributes
import java.util.concurrent.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Assume.assumeTrue
import org.junit.Test

/** End-to-end JVM simulations of Android identity when the NIO file key is absent. */
class PdfStagedNullNioTransactionTest {
    private fun hostKey(file: File): Any? = Files.readAttributes(
        file.toPath(), BasicFileAttributes::class.java, LinkOption.NOFOLLOW_LINKS
    ).fileKey()

    private fun identityReader(): (File) -> StagedAndroidIdentity? {
        val ids = mutableMapOf<Any, Long>()
        return { file ->
            val key = hostKey(file)
            if (key == null) null else StagedAndroidIdentity(13L, ids.getOrPut(key) { ids.size.toLong() + 1L })
        }
    }

    private fun requireHostKeys(root: File) {
        val probe = File(root, "probe").apply { writeText("probe") }
        assumeTrue("Host file keys needed to simulate stable Android inode identity", hostKey(probe) != null)
        assertTrue(probe.delete())
    }

    private fun stages(root: File) = root.listFiles().orEmpty().filter { it.name.startsWith(".anydoc_stage_") }

    @Test fun nullNioBatchPublicationFailureRollsBackBothOutputs() = runBlocking {
        val root = Files.createTempDirectory("null-nio-batch").toFile()
        try {
            requireHostKeys(root)
            val delegate = identityReader()
            var injected = false
            var firstWasPublished = false
            val reader: (File) -> StagedAndroidIdentity? = { file ->
                if (file.name == "second.pdf" && !injected) {
                    firstWasPublished = File(root, "first.pdf").exists()
                    injected = true
                    throw IllegalStateException("simulated second publication failure")
                }
                delegate(file)
            }
            try {
                withStagedOutputFilesUsingIdentityReaders(
                    root,
                    listOf(
                        StagedOutputRequest("first", "pdf") { stage -> stage.writeText("one"); 1 },
                        StagedOutputRequest("second", "pdf") { stage -> stage.writeText("two"); 2 }
                    ),
                    readNioFileKey = { null },
                    readAndroidIdentity = reader
                )
                fail("Expected second publication failure")
            } catch (expected: IllegalStateException) {
                assertEquals("simulated second publication failure", expected.message)
            }
            assertTrue("First output must be published before injected failure", firstWasPublished)
            assertFalse(File(root, "first.pdf").exists())
            assertFalse(File(root, "second.pdf").exists())
            assertTrue(stages(root).isEmpty())
        } finally { root.deleteRecursively() }
    }

    @Test fun nullNioCancellationCleansEarlierAndCurrentOwnedStages() = runBlocking {
        val root = Files.createTempDirectory("null-nio-cancel").toFile()
        try {
            requireHostKeys(root)
            val created = mutableListOf<File>()
            try {
                withStagedOutputFilesUsingIdentityReaders(
                    root,
                    listOf(
                        StagedOutputRequest("first", "pdf") { stage ->
                            created += stage
                            stage.writeText("one")
                            1
                        },
                        StagedOutputRequest("second", "pdf") { stage ->
                            created += stage
                            stage.writeText("two")
                            throw CancellationException("cancel second writer")
                        }
                    ),
                    readNioFileKey = { null },
                    readAndroidIdentity = identityReader()
                )
                fail("Expected cancellation")
            } catch (expected: CancellationException) {
                assertEquals("cancel second writer", expected.message)
            }
            assertEquals(2, created.size)
            assertTrue(created.none { it.exists() })
            assertTrue(stages(root).isEmpty())
            assertFalse(File(root, "first.pdf").exists())
            assertFalse(File(root, "second.pdf").exists())
        } finally { root.deleteRecursively() }
    }

    @Test fun nullNioCancellationPreservesSubstitutedForeignStage() = runBlocking {
        val root = Files.createTempDirectory("null-nio-cancel-foreign").toFile()
        try {
            requireHostKeys(root)
            val foreign = File(root, "foreign").apply { writeText("foreign payload") }
            var firstStage: File? = null
            var secondStage: File? = null
            try {
                withStagedOutputFilesUsingIdentityReaders(
                    root,
                    listOf(
                        StagedOutputRequest("first", "pdf") { stage ->
                            firstStage = stage
                            stage.writeText("ours")
                            1
                        },
                        StagedOutputRequest("second", "pdf") { stage ->
                            secondStage = stage
                            stage.writeText("ours too")
                            Files.move(foreign.toPath(), requireNotNull(firstStage).toPath(), StandardCopyOption.REPLACE_EXISTING)
                            throw CancellationException("cancel after foreign substitution")
                        }
                    ),
                    readNioFileKey = { null },
                    readAndroidIdentity = identityReader()
                )
                fail("Expected cancellation")
            } catch (expected: CancellationException) {
                assertEquals("cancel after foreign substitution", expected.message)
            }
            assertEquals("foreign payload", requireNotNull(firstStage).readText())
            assertFalse(requireNotNull(secondStage).exists())
            assertEquals(1, stages(root).size)
            assertFalse(File(root, "first.pdf").exists())
            assertFalse(File(root, "second.pdf").exists())
        } finally { root.deleteRecursively() }
    }

    @Test fun nullNioNormalPublicationPreservesNumberedCollision() = runBlocking {
        val root = Files.createTempDirectory("null-nio-collision").toFile()
        try {
            requireHostKeys(root)
            File(root, "report.pdf").writeText("existing")
            val result = withStagedOutputFilesUsingIdentityReaders(
                root,
                listOf(StagedOutputRequest("report", "pdf") { stage -> stage.writeText("new"); 7 }),
                readNioFileKey = { null },
                readAndroidIdentity = identityReader()
            ).single()
            assertEquals("report_1.pdf", result.outputFile.name)
            assertEquals("new", result.outputFile.readText())
            assertEquals("existing", File(root, "report.pdf").readText())
            assertTrue(stages(root).isEmpty())
        } finally { root.deleteRecursively() }
    }
}
