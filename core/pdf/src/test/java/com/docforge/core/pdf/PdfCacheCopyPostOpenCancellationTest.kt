package com.docforge.core.pdf

import com.docforge.core.domain.io.ActiveTempFileRegistry
import java.io.ByteArrayInputStream
import java.io.File
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

class PdfCacheCopyPostOpenCancellationTest {
    private class TrackedInput(
        bytes: ByteArray = byteArrayOf(1, 2, 3),
        private val closeFailure: Throwable? = null
    ) : ByteArrayInputStream(bytes) {
        var closes = 0
        override fun close() {
            closes++
            if (closeFailure != null) throw closeFailure
            super.close()
        }
    }

    private fun withOwnedFile(block: (File) -> Unit) {
        val root = Files.createTempDirectory("anydoc-post-open").toFile()
        val temp = File(root, "copy.tmp").apply { writeText("partial") }
        try {
            ActiveTempFileRegistry.register(temp)
            block(temp)
        } finally {
            if (ActiveTempFileRegistry.isActive(temp)) releaseCacheCopyFile(temp)
            root.deleteRecursively()
        }
    }

    @Test fun cancellationDuringProviderOpenSkipsChannelSetupAndClosesInput() = withOwnedFile { temp ->
        val input = TrackedInput()
        var startedSetup = false
        var cancelled = false
        runBlocking {
            val job = launch(start = CoroutineStart.UNDISPATCHED) {
                try {
                    withCancellationCheckedRegisteredCacheCopy(temp, false) {
                        coroutineContext.cancel()
                        withCancellationCheckedOpenedCacheInput(input) {
                            startedSetup = true
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
        assertFalse(startedSetup)
        assertEquals(1, input.closes)
        assertFalse(temp.exists())
        assertFalse(ActiveTempFileRegistry.isActive(temp))
    }

    @Test fun cancellationKeepsAnotherRegisteredOwner() = withOwnedFile { temp ->
        ActiveTempFileRegistry.register(temp)
        val input = TrackedInput()
        var startedSetup = false
        runBlocking {
            val job = launch(start = CoroutineStart.UNDISPATCHED) {
                coroutineContext.cancel()
                try {
                    withCancellationCheckedOpenedCacheInput(input) { startedSetup = true }
                    fail("Expected cancellation")
                } catch (_: CancellationException) {
                    // Cancellation must still close the provider stream.
                }
            }
            job.join()
        }
        assertFalse(startedSetup)
        assertEquals(1, input.closes)
        assertTrue(temp.exists())
        assertTrue(ActiveTempFileRegistry.isActive(temp))
        releaseCacheCopyFile(temp)
        assertTrue(temp.exists())
        releaseCacheCopyFile(temp)
        assertFalse(temp.exists())
    }

    @Test fun activeInputRunsSetupOnceAndReturnsSameResult() = runBlocking {
        val input = TrackedInput()
        val expected = Any()
        var calls = 0
        val result = withCancellationCheckedOpenedCacheInput(input) {
            calls++
            assertSame(input, it)
            expected
        }
        assertSame(expected, result)
        assertEquals(1, calls)
        assertEquals(1, input.closes)
    }

    @Test fun activeInputPreservesBytesAndSuccessfulTempOwnership() = withOwnedFile { temp ->
        val input = TrackedInput(byteArrayOf(4, 5, 6, 7))
        val bytes = runBlocking {
            withCancellationCheckedRegisteredCacheCopy(temp, false) {
                withCancellationCheckedOpenedCacheInput(input) { it.readBytes() }
            }
        }
        assertTrue(bytes.contentEquals(byteArrayOf(4, 5, 6, 7)))
        assertEquals(1, input.closes)
        assertTrue(temp.exists())
        assertFalse(ActiveTempFileRegistry.isActive(temp))
    }

    @Test fun setupFailurePreservesOriginalErrorAndClosesInput() = withOwnedFile { temp ->
        val input = TrackedInput()
        val original = IllegalStateException("channel setup failed")
        try {
            runBlocking {
                withCancellationCheckedRegisteredCacheCopy(temp, false) {
                    withCancellationCheckedOpenedCacheInput(input) { throw original }
                }
            }
            fail("Expected setup failure")
        } catch (actual: IllegalStateException) {
            assertSame(original, actual)
        }
        assertEquals(1, input.closes)
        assertFalse(temp.exists())
    }

    @Test fun closeFailureAfterSuccessfulSetupIsPropagated() = withOwnedFile { temp ->
        val original = IllegalStateException("provider close failed")
        val input = TrackedInput(closeFailure = original)
        try {
            runBlocking {
                withCancellationCheckedRegisteredCacheCopy(temp, false) {
                    withCancellationCheckedOpenedCacheInput(input) { "copied" }
                }
            }
            fail("Expected close failure")
        } catch (actual: IllegalStateException) {
            assertSame(original, actual)
        }
        assertEquals(1, input.closes)
        assertFalse(temp.exists())
    }

    @Test fun keepRegisteredSuccessLeavesTempOwnedUntilRelease() = withOwnedFile { temp ->
        val input = TrackedInput()
        val result = runBlocking {
            withCancellationCheckedRegisteredCacheCopy(temp, true) {
                withCancellationCheckedOpenedCacheInput(input) { "copied" }
            }
        }
        assertEquals("copied", result)
        assertEquals(1, input.closes)
        assertTrue(ActiveTempFileRegistry.isActive(temp))
        releaseCacheCopyFile(temp)
        assertFalse(temp.exists())
    }
}
