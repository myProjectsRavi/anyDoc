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

class PdfCacheCopyPostChannelCancellationTest {
    private class TrackedInput : ByteArrayInputStream(byteArrayOf(1, 2, 3)) {
        var closes = 0
        override fun close() {
            closes++
            super.close()
        }
    }

    private class TrackedChannel(
        private val payload: ByteArray = byteArrayOf(4, 5, 6),
        private val closeFailure: Throwable? = null
    ) : ReadableByteChannel {
        private var position = 0
        private var open = true
        var closes = 0
        var reads = 0

        override fun isOpen(): Boolean = open

        override fun read(dst: ByteBuffer): Int {
            reads++
            if (position >= payload.size) return -1
            val count = minOf(dst.remaining(), payload.size - position)
            dst.put(payload, position, count)
            position += count
            return count
        }

        override fun close() {
            closes++
            open = false
            closeFailure?.let { throw it }
        }
    }

    private fun withOwnedFile(block: (File) -> Unit) {
        val root = Files.createTempDirectory("anydoc-post-channel").toFile()
        val temp = File(root, "copy.tmp").apply { writeText("partial") }
        try {
            ActiveTempFileRegistry.register(temp)
            block(temp)
        } finally {
            if (ActiveTempFileRegistry.isActive(temp)) releaseCacheCopyFile(temp)
            root.deleteRecursively()
        }
    }

    @Test fun cancelledAfterChannelOpenSkipsOutputAndClosesEveryOwnedResource() = withOwnedFile { temp ->
        val input = TrackedInput()
        val channel = TrackedChannel()
        var outputOpened = false
        var cancelled = false
        runBlocking {
            val job = launch(start = CoroutineStart.UNDISPATCHED) {
                try {
                    withCancellationCheckedRegisteredCacheCopy(temp, false) {
                        withCancellationCheckedOpenedCacheInput(input) {
                            coroutineContext.cancel()
                            withCancellationCheckedCacheSourceChannel(channel) { outputOpened = true }
                        }
                    }
                    fail("Expected cancellation")
                } catch (_: CancellationException) {
                    cancelled = true
                }
            }
            job.join()
        }
        assertTrue(cancelled)
        assertFalse(outputOpened)
        assertEquals(0, channel.reads)
        assertEquals(1, channel.closes)
        assertEquals(1, input.closes)
        assertFalse(temp.exists())
        assertFalse(ActiveTempFileRegistry.isActive(temp))
    }

    @Test fun cancellationDoesNotDeleteAnotherRegisteredOwnersTemp() = withOwnedFile { temp ->
        ActiveTempFileRegistry.register(temp)
        val channel = TrackedChannel()
        runBlocking {
            val job = launch(start = CoroutineStart.UNDISPATCHED) {
                try {
                    withCancellationCheckedRegisteredCacheCopy(temp, false) {
                        coroutineContext.cancel()
                        withCancellationCheckedCacheSourceChannel(channel) { fail("Output setup must be skipped") }
                    }
                    fail("Expected cancellation")
                } catch (_: CancellationException) {
                    // The other registration still owns the temporary file.
                }
            }
            job.join()
        }
        assertEquals(1, channel.closes)
        assertTrue(temp.exists())
        assertTrue(ActiveTempFileRegistry.isActive(temp))
        releaseCacheCopyFile(temp)
        assertFalse(temp.exists())
    }

    @Test fun activeChannelReturnsOriginalResultAndCopiesUnchangedBytes() = runBlocking {
        val channel = TrackedChannel(byteArrayOf(10, 11, 12, 13))
        val expected = Any()
        val bytes = ByteBuffer.allocate(8)
        var calls = 0
        val result = withCancellationCheckedCacheSourceChannel(channel) { source ->
            calls++
            assertSame(channel, source)
            assertEquals(4, source.read(bytes))
            expected
        }
        assertSame(expected, result)
        assertEquals(1, calls)
        assertEquals(1, channel.closes)
        assertTrue(bytes.array().copyOf(4).contentEquals(byteArrayOf(10, 11, 12, 13)))
    }

    @Test fun outputSetupFailureRetainsExactExceptionAndReleasesTemp() = withOwnedFile { temp ->
        val channel = TrackedChannel()
        val original = IllegalStateException("output open failed")
        try {
            runBlocking {
                withCancellationCheckedRegisteredCacheCopy(temp, false) {
                    withCancellationCheckedCacheSourceChannel(channel) { throw original }
                }
            }
            fail("Expected output failure")
        } catch (actual: IllegalStateException) {
            assertSame(original, actual)
        }
        assertEquals(1, channel.closes)
        assertFalse(temp.exists())
    }

    @Test fun channelCloseFailureAfterSuccessRetainsExactException() = withOwnedFile { temp ->
        val original = IllegalStateException("channel close failed")
        val channel = TrackedChannel(closeFailure = original)
        try {
            runBlocking {
                withCancellationCheckedRegisteredCacheCopy(temp, false) {
                    withCancellationCheckedCacheSourceChannel(channel) { "copied" }
                }
            }
            fail("Expected close failure")
        } catch (actual: IllegalStateException) {
            assertSame(original, actual)
        }
        assertEquals(1, channel.closes)
        assertFalse(temp.exists())
    }

    @Test fun cancellationRetainsDistinctChannelCloseFailureAsSuppressed() = withOwnedFile { temp ->
        val closeFailure = IllegalStateException("close during cancellation")
        val channel = TrackedChannel(closeFailure = closeFailure)
        var cancelled: CancellationException? = null
        runBlocking {
            val job = launch(start = CoroutineStart.UNDISPATCHED) {
                try {
                    withCancellationCheckedRegisteredCacheCopy(temp, false) {
                        coroutineContext.cancel()
                        withCancellationCheckedCacheSourceChannel(channel) { fail("Output must not open") }
                    }
                } catch (failure: CancellationException) {
                    cancelled = failure
                }
            }
            job.join()
        }
        assertEquals(1, channel.closes)
        assertSame(closeFailure, cancelled!!.suppressed.single())
        assertFalse(temp.exists())
    }

    @Test fun repeatedSuccessfulCallsCloseEachChannelOnce() = runBlocking {
        repeat(3) {
            val channel = TrackedChannel()
            val result = withCancellationCheckedCacheSourceChannel(channel) { it.read(ByteBuffer.allocate(3)) }
            assertEquals(3, result)
            assertEquals(1, channel.closes)
        }
    }
}
