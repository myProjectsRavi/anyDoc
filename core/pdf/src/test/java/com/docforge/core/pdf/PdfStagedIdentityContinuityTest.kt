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

class PdfStagedIdentityContinuityTest {
    private fun key(file: File): Any? =
        Files.readAttributes(file.toPath(), BasicFileAttributes::class.java, LinkOption.NOFOLLOW_LINKS).fileKey()

    @Test fun writerRegularFileReplacementCannotPublishForeignBytes() {
        val root = Files.createTempDirectory("anydoc-identity-writer").toFile()
        try {
            val foreign = File(root, "foreign.txt").apply { writeText("foreign") }
            var replaced: File? = null
            try {
                runBlocking {
                    withStagedOutputFile(root, "report", "pdf") { stage ->
                        assumeTrue(key(stage) != null)
                        assertTrue(stage.delete())
                        Files.move(foreign.toPath(), stage.toPath())
                        replaced = stage
                    }
                }
                fail("Expected known original staging identity mismatch")
            } catch (_: IllegalArgumentException) {
                assertFalse(File(root, "report.pdf").exists())
                assertEquals("foreign", replaced!!.readText())
            }
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun laterWriterCannotReplaceEarlierPreparedRegularStage() {
        val root = Files.createTempDirectory("anydoc-identity-batch").toFile()
        try {
            val foreign = File(root, "foreign.txt").apply { writeText("foreign") }
            var first: File? = null
            try {
                runBlocking {
                    withStagedOutputFiles(root, listOf(
                        StagedOutputRequest("first", "pdf") { stage ->
                            assumeTrue(key(stage) != null)
                            first = stage
                            stage.writeText("original")
                        },
                        StagedOutputRequest("second", "pdf") { stage ->
                            assertTrue(first!!.delete())
                            Files.move(foreign.toPath(), first!!.toPath())
                            stage.writeText("second")
                        }
                    ))
                }
                fail("Expected earlier staging identity mismatch")
            } catch (_: IllegalArgumentException) {
                assertFalse(File(root, "first.pdf").exists())
                assertFalse(File(root, "second.pdf").exists())
                assertEquals("foreign", first!!.readText())
            }
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun preMoveHookRegularSwapPreservesForeignStage() {
        val root = Files.createTempDirectory("anydoc-identity-move").toFile()
        try {
            val foreign = File(root, "foreign.txt").apply { writeText("foreign") }
            val stage = File(root, ".anydoc_stage_identity.part").apply { writeText("original") }
            val originalKey = key(stage)
            assumeTrue(originalKey != null)
            val final = File(root, "report.pdf")
            try {
                publishCheckedStagedOutput(stage, final, originalKey) {
                    assertTrue(stage.delete())
                    Files.move(foreign.toPath(), stage.toPath())
                }
                fail("Expected pre-move staging identity mismatch")
            } catch (_: IllegalArgumentException) {
                assertFalse(Files.exists(final.toPath(), LinkOption.NOFOLLOW_LINKS))
                assertEquals("foreign", stage.readText())
            }
        } finally {
            root.deleteRecursively()
        }
    }
}
