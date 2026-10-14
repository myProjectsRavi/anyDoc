package com.docforge.core.pdf

import java.io.File
import java.nio.file.Files
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.fail
import org.junit.Test

class PdfOutputNameBoundaryTest {
    private fun reject(block: () -> Unit) {
        try {
            block()
            fail("Expected invalid output name")
        } catch (_: IllegalArgumentException) {
            // Rejected before any filesystem mutation.
        }
    }

    @Test
    fun resolverRejectsTraversalAndControlCharacters() {
        val directory = Files.createTempDirectory("anydoc-name-check").toFile()
        try {
            listOf("../escape", "sub/file", "..\\escape", "/absolute", ".", "..", "", " ", "bad\nname", "bad\u0085name").forEach { name ->
                reject { resolveNonConflictingFile(directory, name, "pdf") }
            }
            listOf("../pdf", "sub/pdf", "..\\pdf", "/absolute", ".", "..", "", " ", "p\u007fdf").forEach { ext ->
                reject { resolveNonConflictingFile(directory, "report", ext) }
            }
            assertEquals(emptyList<String>(), directory.listFiles().orEmpty().map { it.name })
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun safeNamesPreserveNumberedConflictResolution() {
        val directory = Files.createTempDirectory("anydoc-name-safe").toFile()
        try {
            File(directory, "résumé 2026.pdf").writeText("original")
            val next = resolveNonConflictingFile(directory, "résumé 2026", "pdf")
            assertEquals("résumé 2026_1.pdf", next.name)
            assertEquals(directory.canonicalPath, next.parentFile.canonicalPath)
            assertEquals("original", File(directory, "résumé 2026.pdf").readText())
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun invalidLaterBatchNamePreventsAllWritersAndDirectoryCreation() = runBlocking {
        val root = Files.createTempDirectory("anydoc-batch-name").toFile()
        val selected = File(root, "selected")
        var calls = 0
        try {
            reject {
                runBlocking {
                    withStagedOutputFiles(
                        selected,
                        listOf(
                            StagedOutputRequest("safe", "pdf") { stage -> calls++; stage.writeText("safe") },
                            StagedOutputRequest("escape", "../pdf") { stage -> calls++; stage.writeText("unsafe") }
                        )
                    )
                }
            }
            assertEquals(0, calls)
            assertFalse(selected.exists())
            assertFalse(File(root, "escape.pdf").exists())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun safeDuplicateBatchNamesRemainDistinct() = runBlocking {
        val directory = Files.createTempDirectory("anydoc-batch-safe").toFile()
        try {
            val results = withStagedOutputFiles(
                directory,
                listOf(
                    StagedOutputRequest("report", "pdf") { stage -> stage.writeText("first") },
                    StagedOutputRequest("report", "pdf") { stage -> stage.writeText("second") }
                )
            )
            assertEquals(listOf("report.pdf", "report_1.pdf"), results.map { it.outputFile.name })
            assertEquals(listOf("first", "second"), results.map { it.outputFile.readText() })
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun concurrentSameNamePublishersNeverOverwriteEachOther() {
        val directory = Files.createTempDirectory("anydoc-concurrent-publish").toFile()
        val executor = Executors.newFixedThreadPool(8)
        try {
            val count = 72
            val tasks = (0 until count).map { index ->
                Callable {
                    runBlocking {
                        withStagedOutputFile(directory, "report", "pdf") { stage ->
                            stage.writeText("content-$index")
                            index
                        }
                    }
                }
            }
            val futures = executor.invokeAll(tasks, 30, TimeUnit.SECONDS)
            val results = futures.map { it.get(30, TimeUnit.SECONDS) }
            assertEquals(count, results.map { it.outputFile.canonicalPath }.toSet().size)
            assertEquals(count, directory.listFiles().orEmpty().size)
            results.forEach { result ->
                assertEquals("content-${result.value}", result.outputFile.readText())
            }
            assertFalse(directory.listFiles().orEmpty().any { it.name.startsWith(".anydoc_stage_") })
        } finally {
            executor.shutdownNow()
            directory.deleteRecursively()
        }
    }

    @Test
    fun resolverSkipsDanglingSymlinkInsteadOfFollowingItOnWrite() {
        val root = Files.createTempDirectory("anydoc-dangling-resolve").toFile()
        val directory = File(root, "output").apply { mkdirs() }
        val outside = File(root, "outside.pdf")
        val link = File(directory, "report.pdf")
        try {
            Files.createSymbolicLink(link.toPath(), outside.toPath())
            assertFalse(link.exists()) // File.exists follows the dangling link.
            val allocated = resolveNonConflictingFile(directory, "report", "pdf")
            assertEquals("report_1.pdf", allocated.name)
            allocated.writeText("safe")
            assertFalse(outside.exists())
            assertEquals("safe", allocated.readText())
            assertEquals(true, Files.isSymbolicLink(link.toPath()))
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun resolverSkipsNumberedDanglingSymlink() {
        val root = Files.createTempDirectory("anydoc-dangling-numbered").toFile()
        val directory = File(root, "output").apply { mkdirs() }
        val outside = File(root, "outside.pdf")
        val link = File(directory, "report_1.pdf")
        try {
            File(directory, "report.pdf").writeText("original")
            Files.createSymbolicLink(link.toPath(), outside.toPath())
            val allocated = resolveNonConflictingFile(directory, "report", "pdf")
            assertEquals("report_2.pdf", allocated.name)
            assertEquals("original", File(directory, "report.pdf").readText())
            assertFalse(outside.exists())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun stagedOutputSkipsDanglingSymlinkWithoutCreatingExternalFile() = runBlocking {
        val root = Files.createTempDirectory("anydoc-dangling-staged").toFile()
        val directory = File(root, "output").apply { mkdirs() }
        val outside = File(root, "outside.pdf")
        val link = File(directory, "report.pdf")
        try {
            Files.createSymbolicLink(link.toPath(), outside.toPath())
            val result = withStagedOutputFile(directory, "report", "pdf") { stage ->
                stage.writeText("staged")
            }
            assertEquals("report_1.pdf", result.outputFile.name)
            assertEquals("staged", result.outputFile.readText())
            assertFalse(outside.exists())
            assertEquals(true, Files.isSymbolicLink(link.toPath()))
        } finally {
            root.deleteRecursively()
        }
    }

}
