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
import kotlinx.coroutines.runBlocking

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

}
