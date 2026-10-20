package com.docforge.core.pdf

import java.io.File
import java.io.IOException
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

class PdfCompressorDirectoryPreflightTest {
    @Test fun cancellationDuringInitializationSkipsDirectoryResolution() = runBlocking {
        var resolved = false
        var observed = false
        val worker = launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                initializePdfBoxAndResolveCompressionDirectory(
                    initialize = { coroutineContext.cancel() },
                    resolve = { resolved = true; File("unexpected") }
                )
                fail("Expected cancellation before resolving output directory")
            } catch (_: CancellationException) {
                observed = true
            }
        }
        worker.join()
        assertTrue(observed)
        assertFalse(resolved)
    }

    @Test fun successfulInitializationPrecedesResolutionAndPreservesFileIdentity() = runBlocking {
        val events = mutableListOf<String>()
        val expected = File("output")
        val actual = initializePdfBoxAndResolveCompressionDirectory(
            initialize = { events.add("initialize") },
            resolve = { events.add("resolve"); expected }
        )
        assertSame(expected, actual)
        assertEquals(listOf("initialize", "resolve"), events)
    }

    @Test fun initializationFailurePreservesIdentityAndSkipsResolution() = runBlocking {
        val original = IllegalStateException("PDFBox init failed")
        var resolved = false
        try {
            initializePdfBoxAndResolveCompressionDirectory(
                initialize = { throw original },
                resolve = { resolved = true; File("unexpected") }
            )
            fail("Expected original initialization failure")
        } catch (actual: IllegalStateException) {
            assertSame(original, actual)
        }
        assertFalse(resolved)
    }

    @Test fun directoryResolutionFailurePreservesOriginalIdentity() = runBlocking {
        val original = IOException("output directory unavailable")
        try {
            initializePdfBoxAndResolveCompressionDirectory(
                initialize = {},
                resolve = { throw original }
            )
            fail("Expected original resolution failure")
        } catch (actual: IOException) {
            assertSame(original, actual)
        }
    }

    @Test fun repeatedSuccessfulCallsPreserveOrderingAndResults() = runBlocking {
        val events = mutableListOf<String>()
        repeat(3) { index ->
            val result = initializePdfBoxAndResolveCompressionDirectory(
                initialize = { events.add("initialize-$index") },
                resolve = { events.add("resolve-$index"); index }
            )
            assertEquals(index, result)
        }
        assertEquals((0..2).flatMap { listOf("initialize-$it", "resolve-$it") }, events)
    }

    @Test fun alreadyCancelledCoroutineCannotReturnResolvedDirectory() = runBlocking {
        var observed = false
        var resolved = false
        val worker = launch(start = CoroutineStart.UNDISPATCHED) {
            coroutineContext.cancel()
            try {
                initializePdfBoxAndResolveCompressionDirectory(
                    initialize = {},
                    resolve = { resolved = true; File("unexpected") }
                )
                fail("Expected cancellation, not a resolved directory")
            } catch (_: CancellationException) {
                observed = true
            }
        }
        worker.join()
        assertTrue(observed)
        assertFalse(resolved)
    }
}
