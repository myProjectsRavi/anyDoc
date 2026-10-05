package com.docforge.core.pdf

import com.docforge.core.domain.io.ActiveTempFileRegistry
import java.nio.file.Files
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class PdfCoreSafetyTest {

    @Test
    fun retryableOnceInitializerRetriesAfterFailureAndLatchesOnlySuccess() {
        val initializer = RetryableOnceInitializer()
        val attempts = AtomicInteger(0)

        try {
            initializer.run {
                attempts.incrementAndGet()
                error("synthetic initialization failure")
            }
            fail("Expected the synthetic failure to propagate")
        } catch (_: IllegalStateException) {
            // Expected: a failed initialization must not be latched as success.
        }

        initializer.run {
            attempts.incrementAndGet()
        }
        initializer.run {
            attempts.incrementAndGet()
        }

        assertEquals(2, attempts.get())
    }

    @Test
    fun retryableOnceInitializerRunsOnlyOnceAcrossConcurrentCallers() {
        val initializer = RetryableOnceInitializer()
        val calls = AtomicInteger(0)
        val startGate = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(8)

        try {
            val futures = (0 until 24).map {
                executor.submit {
                    assertTrue(startGate.await(5, TimeUnit.SECONDS))
                    initializer.run {
                        calls.incrementAndGet()
                        Thread.sleep(20)
                    }
                }
            }

            startGate.countDown()
            futures.forEach { it.get(5, TimeUnit.SECONDS) }

            assertEquals(1, calls.get())
        } finally {
            executor.shutdownNow()
        }
    }

    @Test
    fun activeTempFileRegistryTracksOnlyCurrentLeases() {
        val directory = Files.createTempDirectory("anydoc-temp-registry-test").toFile()
        val file = directory.resolve("docforge_active.tmp")
        file.writeText("active")

        try {
            assertFalse(ActiveTempFileRegistry.isActive(file))

            ActiveTempFileRegistry.register(file)
            assertTrue(ActiveTempFileRegistry.isActive(file))

            ActiveTempFileRegistry.unregister(file)
            assertFalse(ActiveTempFileRegistry.isActive(file))
        } finally {
            ActiveTempFileRegistry.unregister(file)
            directory.deleteRecursively()
        }
    }

    @Test
    fun resolveNonConflictingFileNeverDeletesExistingOutput() {
        val directory = Files.createTempDirectory("anydoc-output-test").toFile()
        try {
            val original = directory.resolve("audio.mp3")
            original.writeText("keep-me")

            val resolved = resolveNonConflictingFile(
                directory = directory,
                baseName = "audio",
                extension = "mp3"
            )

            assertEquals("audio_1.mp3", resolved.name)
            assertTrue(original.exists())
            assertEquals("keep-me", original.readText())
            assertFalse(resolved.exists())
        } finally {
            directory.deleteRecursively()
        }
    }
    @Test
    fun stagedOutputPublishesOnlyAfterSuccessfulBlockAndPreservesExistingFile() = runBlocking {
        val directory = Files.createTempDirectory("anydoc-staged-output-test").toFile()
        try {
            val existing = directory.resolve("compressed.pdf")
            existing.writeText("existing")

            val result = withStagedOutputFile(
                directory = directory,
                baseName = "compressed",
                extension = "pdf"
            ) { staged ->
                staged.writeText("complete")
                7
            }

            assertEquals(7, result.value)
            assertEquals("compressed_1.pdf", result.outputFile.name)
            assertEquals("existing", existing.readText())
            assertEquals("complete", result.outputFile.readText())
            assertTrue(directory.listFiles().orEmpty().none { it.name.startsWith(".anydoc_stage_") })
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun stagedOutputDeletesPartialFileWhenOperationFails() = runBlocking {
        val directory = Files.createTempDirectory("anydoc-staged-failure-test").toFile()
        try {
            try {
                withStagedOutputFile(
                    directory = directory,
                    baseName = "compressed",
                    extension = "pdf"
                ) { staged ->
                    staged.writeText("partial")
                    error("synthetic conversion failure")
                }
                fail("Expected staged operation failure to propagate")
            } catch (_: IllegalStateException) {
                // Expected.
            }

            assertFalse(directory.resolve("compressed.pdf").exists())
            assertTrue(directory.listFiles().orEmpty().none { it.name.startsWith(".anydoc_stage_") })
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun stagedOutputSetPublishesNothingUntilEveryWriterSucceeds() = runBlocking {
        val directory = Files.createTempDirectory("anydoc-staged-set-test").toFile()
        try {
            val existing = directory.resolve("page_1.pdf")
            existing.writeText("existing")

            val results = withStagedOutputFiles(
                directory = directory,
                requests = listOf(
                    StagedOutputRequest(
                        baseName = "page_1",
                        extension = "pdf"
                    ) { staged ->
                        staged.writeText("one")
                        1
                    },
                    StagedOutputRequest(
                        baseName = "page_2",
                        extension = "pdf"
                    ) { staged ->
                        assertFalse(directory.resolve("page_1_1.pdf").exists())
                        staged.writeText("two")
                        2
                    }
                )
            )

            assertEquals(listOf("page_1_1.pdf", "page_2.pdf"), results.map { it.outputFile.name })
            assertEquals(listOf(1, 2), results.map { it.value })
            assertEquals("existing", existing.readText())
            assertEquals("one", results[0].outputFile.readText())
            assertEquals("two", results[1].outputFile.readText())
            assertTrue(directory.listFiles().orEmpty().none { it.name.startsWith(".anydoc_stage_") })
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun stagedOutputSetDeletesEveryPartialWhenLaterWriterFails() = runBlocking {
        val directory = Files.createTempDirectory("anydoc-staged-set-failure-test").toFile()
        try {
            try {
                withStagedOutputFiles(
                    directory = directory,
                    requests = listOf(
                        StagedOutputRequest(
                            baseName = "part_1",
                            extension = "pdf"
                        ) { staged ->
                            staged.writeText("complete-first")
                            Unit
                        },
                        StagedOutputRequest(
                            baseName = "part_2",
                            extension = "pdf"
                        ) { staged ->
                            assertFalse(directory.resolve("part_1.pdf").exists())
                            staged.writeText("partial-second")
                            error("synthetic later-writer failure")
                        }
                    )
                )
                fail("Expected later writer failure to propagate")
            } catch (_: IllegalStateException) {
                // Expected.
            }

            assertFalse(directory.resolve("part_1.pdf").exists())
            assertFalse(directory.resolve("part_2.pdf").exists())
            assertTrue(directory.listFiles().orEmpty().none { it.name.startsWith(".anydoc_stage_") })
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun cancelledCacheCopyChunkDoesNotInvokeTransfer() = runBlocking {
        val transfers = AtomicInteger(0)
        val cancelledJob = Job().apply { cancel() }

        try {
            withContext(cancelledJob) {
                copyChunkWithCancellation {
                    transfers.incrementAndGet()
                    1L
                }
            }
            fail("Expected cancellation before transfer")
        } catch (_: CancellationException) {
            // Expected.
        }

        assertEquals(0, transfers.get())
    }

    @Test
    fun diffRowsStopBeforeProcessingNextRowAfterCancellation() {
        val checks = AtomicInteger(0)
        val processedRows = mutableListOf<Int>()

        try {
            forEachDiffRow(
                height = 8,
                checkCancelled = {
                    if (checks.incrementAndGet() == 4) {
                        throw CancellationException("synthetic compare cancellation")
                    }
                }
            ) { row ->
                processedRows += row
            }
            fail("Expected row-boundary cancellation")
        } catch (_: CancellationException) {
            // Expected.
        }

        assertEquals(listOf(0, 1, 2), processedRows)
        assertEquals(4, checks.get())
    }

    @Test
    fun largeInputCachePreflightRequiresWorkingSpaceAndReserve() {
        val inputBytes = 600L * 1024L * 1024L
        val expected = (2L * inputBytes) + (32L * 1024L * 1024L)

        assertEquals(expected, requiredCacheBytesForKnownInput(inputBytes))
    }

    @Test
    fun largeInputCachePreflightSaturatesInsteadOfOverflowing() {
        assertEquals(Long.MAX_VALUE, requiredCacheBytesForKnownInput(Long.MAX_VALUE))
    }

    @Test
    fun idCardDecodeBudget_scalesForLowNormalAndHighHeap() {
        assertEquals(591, IdCardDecodeBudget.maxLongEdge(32L * 1024L * 1024L))
        assertEquals(1182, IdCardDecodeBudget.maxLongEdge(128L * 1024L * 1024L))
        assertEquals(1800, IdCardDecodeBudget.maxLongEdge(512L * 1024L * 1024L))
    }

}
