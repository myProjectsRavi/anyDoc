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

class PdfStagedCleanupOwnershipTest {
    @Test fun writerFailurePreservesSubstitutedNonemptyDirectory() = runBlocking {
        val root = Files.createTempDirectory("anydoc-staged-foreign").toFile()
        val output = File(root, "output").apply { mkdirs() }
        try {
            try {
                withStagedOutputFile(output, "result", "pdf") { staged ->
                    assertTrue(staged.delete())
                    assertTrue(staged.mkdir())
                    File(staged, "foreign.txt").writeText("preserve")
                    throw IllegalStateException("writer failure")
                }
                fail("Expected writer failure")
            } catch (expected: IllegalStateException) {
                assertEquals("writer failure", expected.message)
            }
            val stagingEntries = output.listFiles().orEmpty()
            assertEquals(1, stagingEntries.size)
            assertEquals("preserve", File(stagingEntries.single(), "foreign.txt").readText())
            assertFalse(File(output, "result.pdf").exists())
        } finally { root.deleteRecursively() }
    }

    @Test fun finalCleanupRejectsForeignDirectoryAndRegularReplacement() {
        val root = Files.createTempDirectory("anydoc-staged-cleanup").toFile()
        try {
            val stage = File(root, "stage.part").apply { writeText("owned") }
            val key = Files.readAttributes(stage.toPath(), BasicFileAttributes::class.java, LinkOption.NOFOLLOW_LINKS).fileKey()
            assumeTrue(key != null)
            val replacement = File(root, "replacement.txt").apply { writeText("foreign") }
            assertTrue(stage.delete())
            Files.move(replacement.toPath(), stage.toPath())
            assertFalse(deleteOwnedStagedEntry(stage, key))
            assertEquals("foreign", stage.readText())
            assertTrue(stage.delete())
            assertTrue(stage.mkdir())
            File(stage, "nested.txt").writeText("keep")
            assertFalse(deleteOwnedStagedEntry(stage, key))
            assertEquals("keep", File(stage, "nested.txt").readText())
        } finally { root.deleteRecursively() }
    }

    @Test fun ownedRegularStageIsRemovedAndUnknownIdentityIsPreserved() {
        val root = Files.createTempDirectory("anydoc-staged-owned").toFile()
        try {
            val stage = File(root, "stage.part").apply { writeText("owned") }
            assertFalse(deleteOwnedStagedEntry(stage, null))
            assertTrue(stage.exists())
            val key = Files.readAttributes(stage.toPath(), BasicFileAttributes::class.java, LinkOption.NOFOLLOW_LINKS).fileKey()
            assumeTrue(key != null)
            assertTrue(deleteOwnedStagedEntry(stage, key))
            assertFalse(Files.exists(stage.toPath(), LinkOption.NOFOLLOW_LINKS))
        } finally { root.deleteRecursively() }
    }
}
