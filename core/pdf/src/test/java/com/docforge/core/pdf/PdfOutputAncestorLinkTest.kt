package com.docforge.core.pdf

import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.fail
import org.junit.Test

/** Lexical ancestor checks must run before file allocation or a writer callback. */
class PdfOutputAncestorLinkTest {
    private fun rejects(block: () -> Unit) {
        try {
            block()
            fail("Expected ancestor symbolic link to be rejected")
        } catch (_: IllegalArgumentException) {
            // Rejected before any output is created.
        }
    }

    @Test
    fun resolverRejectsSymlinkedAncestorWithoutAllocating() {
        val root = Files.createTempDirectory("anydoc-ancestor-resolve").toFile()
        try {
            val target = File(root, "target").apply { mkdirs() }
            val realOutput = File(target, "output").apply { mkdirs() }
            val link = File(root, "redirect")
            Files.createSymbolicLink(link.toPath(), target.toPath())
            rejects { resolveNonConflictingFile(File(link, "output"), "report", "pdf") }
            assertEquals(emptyList<String>(), realOutput.listFiles().orEmpty().map { it.name })
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun stagedWriterRejectsRelativeAncestorLinkBeforeInvokingCallback() = runBlocking {
        val root = Files.createTempDirectory("anydoc-ancestor-staged").toFile()
        try {
            val target = File(root, "target").apply { mkdirs() }
            val realOutput = File(target, "output").apply { mkdirs() }
            val link = File(root, "redirect")
            Files.createSymbolicLink(link.toPath(), target.toPath().fileName)
            var writes = 0
            rejects {
                runBlocking {
                    withStagedOutputFile(File(link, "output"), "report", "pdf") { staged ->
                        writes++
                        staged.writeText("unsafe")
                    }
                }
            }
            assertEquals(0, writes)
            assertEquals(emptyList<String>(), realOutput.listFiles().orEmpty().map { it.name })
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun regularNestedDirectoryPreservesCollisionNumbering() = runBlocking {
        val root = Files.createTempDirectory("anydoc-ancestor-normal").toFile()
        try {
            val directory = File(root, "parent/output").apply { mkdirs() }
            File(directory, "report.pdf").writeText("existing")
            val result = withStagedOutputFile(directory, "report", "pdf") { it.writeText("new") }
            assertEquals("report_1.pdf", result.outputFile.name)
            assertEquals("new", result.outputFile.readText())
            assertEquals("existing", File(directory, "report.pdf").readText())
            assertFalse(Files.isSymbolicLink(directory.toPath()))
        } finally {
            root.deleteRecursively()
        }
    }
}
