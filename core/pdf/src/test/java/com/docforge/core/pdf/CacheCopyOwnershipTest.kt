package com.docforge.core.pdf

import com.docforge.core.domain.io.ActiveTempFileRegistry
import java.io.File
import java.nio.file.Files
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class CacheCopyOwnershipTest {
    private fun withFile(block: (File) -> Unit) {
        val directory = Files.createTempDirectory("anydoc-cache-owner").toFile()
        val file = File(directory, "docforge_owner.tmp").apply { writeText("owned") }
        try {
            block(file)
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun successfulCopyWithoutLeaseReleasesRegistrationButKeepsFile() = withFile { file ->
        ActiveTempFileRegistry.register(file)
        val result = runBlocking {
            withRegisteredCacheCopy(file, keepRegistered = false) { file }
        }
        assertEquals(file, result)
        assertTrue(file.exists())
        assertFalse(ActiveTempFileRegistry.isActive(file))
    }

    @Test
    fun successfulCopyWithLeaseRemainsProtectedUntilRelease() = withFile { file ->
        ActiveTempFileRegistry.register(file)
        runBlocking { withRegisteredCacheCopy(file, keepRegistered = true) { file } }
        assertTrue(ActiveTempFileRegistry.isActive(file))
        releaseCacheCopyFile(file)
        assertFalse(file.exists())
        assertFalse(ActiveTempFileRegistry.isActive(file))
    }

    @Test
    fun failedCopyWithoutAnotherOwnerDeletesIncompleteFile() = withFile { file ->
        ActiveTempFileRegistry.register(file)
        val original = IllegalStateException("copy failed")
        try {
            runBlocking {
                withRegisteredCacheCopy(file, keepRegistered = false) { throw original }
            }
            fail("Expected original failure")
        } catch (failure: IllegalStateException) {
            assertSame(original, failure)
        }
        assertFalse(file.exists())
        assertFalse(ActiveTempFileRegistry.isActive(file))
    }

    @Test
    fun failedCopyReleasesExactlyOnceAndPreservesOtherOwner() = withFile { file ->
        ActiveTempFileRegistry.register(file)
        ActiveTempFileRegistry.register(file)
        try {
            runBlocking {
                withRegisteredCacheCopy(file, keepRegistered = false) { error("copy failed") }
            }
            fail("Expected failure")
        } catch (_: IllegalStateException) {
            // Expected.
        }
        assertTrue(file.exists())
        assertTrue(ActiveTempFileRegistry.isActive(file))
        releaseCacheCopyFile(file)
        assertFalse(file.exists())
    }

    @Test
    fun failedKeepRegisteredCopyStillReleasesOnlyItsLease() = withFile { file ->
        ActiveTempFileRegistry.register(file)
        ActiveTempFileRegistry.register(file)
        try {
            runBlocking {
                withRegisteredCacheCopy(file, keepRegistered = true) { error("copy failed") }
            }
            fail("Expected failure")
        } catch (_: IllegalStateException) {
            // Expected.
        }
        assertTrue(file.exists())
        assertTrue(ActiveTempFileRegistry.isActive(file))
        releaseCacheCopyFile(file)
        assertFalse(file.exists())
    }

    @Test
    fun successfulCopyWithoutLeaseDoesNotUnregisterAnotherOwner() = withFile { file ->
        ActiveTempFileRegistry.register(file)
        ActiveTempFileRegistry.register(file)
        runBlocking { withRegisteredCacheCopy(file, keepRegistered = false) { file } }
        assertTrue(file.exists())
        assertTrue(ActiveTempFileRegistry.isActive(file))
        releaseCacheCopyFile(file)
        assertFalse(file.exists())
    }

    @Test
    fun scopedSuccessDeletesUnsharedCacheFile() = withFile { file ->
        ActiveTempFileRegistry.register(file)
        assertEquals("owned", withScopedCacheCopy(file) { it.readText() })
        assertFalse(file.exists())
        assertFalse(ActiveTempFileRegistry.isActive(file))
    }

    @Test
    fun scopedSuccessPreservesAnotherOwner() = withFile { file ->
        ActiveTempFileRegistry.register(file)
        ActiveTempFileRegistry.register(file)
        withScopedCacheCopy(file) { assertTrue(it.exists()) }
        assertTrue(file.exists())
        assertTrue(ActiveTempFileRegistry.isActive(file))
        releaseCacheCopyFile(file)
        assertFalse(file.exists())
    }

    @Test
    fun scopedFailurePreservesOriginalErrorAndOtherOwner() = withFile { file ->
        ActiveTempFileRegistry.register(file)
        ActiveTempFileRegistry.register(file)
        val original = IllegalArgumentException("consumer failed")
        try {
            withScopedCacheCopy(file) { throw original }
            fail("Expected failure")
        } catch (failure: IllegalArgumentException) {
            assertSame(original, failure)
        }
        assertTrue(file.exists())
        assertTrue(ActiveTempFileRegistry.isActive(file))
        releaseCacheCopyFile(file)
        assertFalse(file.exists())
    }

    @Test
    fun copyCancellationPreservesCancellationAndOtherOwner() = withFile { file ->
        ActiveTempFileRegistry.register(file)
        ActiveTempFileRegistry.register(file)
        val cancelled = CancellationException("cancelled")
        try {
            runBlocking {
                withRegisteredCacheCopy(file, keepRegistered = false) { throw cancelled }
            }
            fail("Expected cancellation")
        } catch (failure: CancellationException) {
            assertSame(cancelled, failure)
        }
        assertTrue(file.exists())
        assertTrue(ActiveTempFileRegistry.isActive(file))
        releaseCacheCopyFile(file)
        assertFalse(file.exists())
    }

    @Test
    fun scopedCancellationPreservesCancellation() = withFile { file ->
        ActiveTempFileRegistry.register(file)
        val cancelled = CancellationException("cancelled")
        try {
            withScopedCacheCopy(file) { throw cancelled }
            fail("Expected cancellation")
        } catch (failure: CancellationException) {
            assertSame(cancelled, failure)
        }
        assertFalse(file.exists())
        assertFalse(ActiveTempFileRegistry.isActive(file))
    }

    @Test
    fun concurrentOwnerReleaseNeverDeletesBeforeSecondOwnerFinishes() = withFile { file ->
        val executor = Executors.newFixedThreadPool(2)
        try {
            repeat(200) {
                file.writeText("owned")
                ActiveTempFileRegistry.register(file)
                ActiveTempFileRegistry.register(file)
                val firstReleased = CountDownLatch(1)
                val secondFinished = CountDownLatch(1)
                val first = executor.submit {
                    releaseCacheCopyFile(file)
                    firstReleased.countDown()
                    assertTrue(secondFinished.await(5, TimeUnit.SECONDS))
                }
                val second = executor.submit {
                    assertTrue(firstReleased.await(5, TimeUnit.SECONDS))
                    assertTrue(file.exists())
                    assertTrue(ActiveTempFileRegistry.isActive(file))
                    releaseCacheCopyFile(file)
                    secondFinished.countDown()
                }
                first.get(10, TimeUnit.SECONDS)
                second.get(10, TimeUnit.SECONDS)
                assertFalse(file.exists())
            }
        } finally {
            executor.shutdownNow()
        }
    }
}
