package com.docforge.core.pdf

import com.docforge.core.domain.io.ActiveTempFileRegistry
import java.io.ByteArrayInputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.channels.ReadableByteChannel
import java.nio.file.Files
import java.util.concurrent.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class PdfCacheCopyCompletionCancellationTest {
    private class TrackedInput(
        private val onClose: () -> Unit = {},
        private val closeFailure: Throwable? = null
    ) : ByteArrayInputStream(byteArrayOf(1, 2, 3, 4)) {
        var closes = 0
        override fun close() {
            closes++
            onClose()
            closeFailure?.let { throw it }
            super.close()
        }
    }

    private class TrackedChannel(private val onClose: () -> Unit) : ReadableByteChannel {
        var closes = 0
        private var open = true
        override fun isOpen() = open
        override fun read(dst: ByteBuffer): Int = -1
        override fun close() {
            closes++
            open = false
            onClose()
        }
    }

    private fun withOwnedFile(block: (File) -> Unit) {
        val directory = Files.createTempDirectory("anydoc-copy-completion").toFile()
        val file = File(directory, "copy.tmp").apply { writeText("partial") }
        try {
            ActiveTempFileRegistry.register(file)
            block(file)
        } finally {
            if (ActiveTempFileRegistry.isActive(file)) releaseCacheCopyFile(file)
            directory.deleteRecursively()
        }
    }

    @Test fun cancellationDuringProviderCloseDeletesUnsharedTemp() = withOwnedFile { file ->
        var cancelled = false
        val inputCloseCounts = mutableListOf<Int>()
        runBlocking {
            val job = launch(start = CoroutineStart.UNDISPATCHED) {
                val input = TrackedInput(onClose = { coroutineContext.cancel() })
                try {
                    withCancellationCheckedRegisteredCacheCopy(file, false) {
                        withCancellationCheckedOpenedCacheInput(input) { source ->
                            file.writeBytes(source.readBytes())
                        }
                        withCacheCopyCompletionPreflight(file)
                    }
                    fail("Cancelled copy must not report success")
                } catch (_: CancellationException) {
                    cancelled = true
                } finally {
                    inputCloseCounts += input.closes
                }
            }
            job.join()
        }
        assertTrue(cancelled)
        assertEquals(listOf(1), inputCloseCounts)
        assertFalse(file.exists())
        assertFalse(ActiveTempFileRegistry.isActive(file))
    }

    @Test fun cancellationDuringChannelCloseAlsoReleasesTemp() = withOwnedFile { file ->
        var cancelled = false
        var channelCloses = 0
        runBlocking {
            val job = launch(start = CoroutineStart.UNDISPATCHED) {
                val channel = TrackedChannel { coroutineContext.cancel() }
                try {
                    withCancellationCheckedRegisteredCacheCopy(file, false) {
                        withCancellationCheckedCacheSourceChannel(channel) { /* copied */ }
                        withCacheCopyCompletionPreflight(file)
                    }
                    fail("Cancelled copy must not report success")
                } catch (_: CancellationException) {
                    cancelled = true
                } finally {
                    channelCloses = channel.closes
                }
            }
            job.join()
        }
        assertTrue(cancelled)
        assertEquals(1, channelCloses)
        assertFalse(file.exists())
    }

    @Test fun cancellationPreservesOtherRegisteredOwner() = withOwnedFile { file ->
        ActiveTempFileRegistry.register(file)
        runBlocking {
            val job = launch(start = CoroutineStart.UNDISPATCHED) {
                try {
                    withCancellationCheckedRegisteredCacheCopy(file, false) {
                        coroutineContext.cancel()
                        withCacheCopyCompletionPreflight(file)
                    }
                    fail("Expected cancellation")
                } catch (_: CancellationException) {
                    // The other owner still retains the file.
                }
            }
            job.join()
        }
        assertTrue(file.exists())
        assertTrue(ActiveTempFileRegistry.isActive(file))
        releaseCacheCopyFile(file)
        assertFalse(file.exists())
    }

    @Test fun cancelledKeepRegisteredCopyReleasesFailedLease() = withOwnedFile { file ->
        runBlocking {
            val job = launch(start = CoroutineStart.UNDISPATCHED) {
                try {
                    withCancellationCheckedRegisteredCacheCopy(file, true) {
                        coroutineContext.cancel()
                        withCacheCopyCompletionPreflight(file)
                    }
                    fail("Expected cancellation")
                } catch (_: CancellationException) {
                    // keepRegistered applies only to successful copies.
                }
            }
            job.join()
        }
        assertFalse(file.exists())
        assertFalse(ActiveTempFileRegistry.isActive(file))
    }

    @Test fun activeCompletionReturnsExactFileAndUnchangedBytes() = withOwnedFile { file ->
        val bytes = byteArrayOf(10, 11, 12, 13)
        val result = runBlocking {
            withCancellationCheckedRegisteredCacheCopy(file, false) {
                file.writeBytes(bytes)
                withCacheCopyCompletionPreflight(file)
            }
        }
        assertSame(file, result)
        assertTrue(file.readBytes().contentEquals(bytes))
        assertFalse(ActiveTempFileRegistry.isActive(file))
    }

    @Test fun activeKeepRegisteredCompletionRetainsLeaseUntilRelease() = withOwnedFile { file ->
        val result = runBlocking {
            withCancellationCheckedRegisteredCacheCopy(file, true) {
                withCacheCopyCompletionPreflight(file)
            }
        }
        assertSame(file, result)
        assertTrue(ActiveTempFileRegistry.isActive(file))
        releaseCacheCopyFile(file)
        assertFalse(file.exists())
    }

    @Test fun providerCloseFailurePreservesOriginalException() = withOwnedFile { file ->
        val original = IllegalStateException("provider close failed")
        val input = TrackedInput(closeFailure = original)
        try {
            runBlocking {
                withCancellationCheckedRegisteredCacheCopy(file, false) {
                    withCancellationCheckedOpenedCacheInput(input) { it.readBytes() }
                    withCacheCopyCompletionPreflight(file)
                }
            }
            fail("Expected original close failure")
        } catch (actual: IllegalStateException) {
            assertSame(original, actual)
        }
        assertEquals(1, input.closes)
        assertFalse(file.exists())
    }

    @Test fun copyFailurePreservesOriginalException() = withOwnedFile { file ->
        val original = IllegalArgumentException("copy failed")
        try {
            runBlocking {
                withCancellationCheckedRegisteredCacheCopy(file, false) {
                    val failingCopy: () -> Unit = { throw original }
                    failingCopy()
                    withCacheCopyCompletionPreflight(file)
                }
            }
            fail("Expected original copy failure")
        } catch (actual: IllegalArgumentException) {
            assertSame(original, actual)
        }
        assertFalse(file.exists())
    }

    @Test fun repeatedActiveCompletionReturnsSameFileIdentity() = withOwnedFile { file ->
        repeat(3) {
            val result = runBlocking {
                withCancellationCheckedRegisteredCacheCopy(file, true) {
                    withCacheCopyCompletionPreflight(file)
                }
            }
            assertSame(file, result)
            assertTrue(file.exists())
            assertTrue(ActiveTempFileRegistry.isActive(file))
        }
        releaseCacheCopyFile(file)
        assertFalse(file.exists())
    }
}
