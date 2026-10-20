package com.docforge.core.pdf

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

class PdfCacheCopyEntryPreflightTest {
    @Test fun cancelledEntrySkipsMetadataPreflight() = runBlocking {
        var queried = false
        var cancelled = false
        val worker = launch(start = CoroutineStart.UNDISPATCHED) {
            coroutineContext.cancel()
            try {
                withCacheCopyEntryPreflight {
                    queried = true
                    Unit
                }
                fail("Expected cancellation")
            } catch (_: CancellationException) {
                cancelled = true
            }
        }
        worker.join()
        assertTrue(cancelled)
        assertFalse(queried)
    }

    @Test fun cancellationDuringMetadataPreflightStopsBeforeCacheSetup() = runBlocking {
        var queried = 0
        var reachedSetup = false
        var cancelled = false
        val worker = launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                withCacheCopyEntryPreflight {
                    queried++
                    coroutineContext.cancel()
                    Unit
                }
                reachedSetup = true
            } catch (_: CancellationException) {
                cancelled = true
            }
        }
        worker.join()
        assertEquals(1, queried)
        assertTrue(cancelled)
        assertFalse(reachedSetup)
    }

    @Test fun activeEntryReturnsPreflightResultWithoutChangingIdentity() = runBlocking {
        val expected = Any()
        var queries = 0
        val actual = withCacheCopyEntryPreflight {
            queries++
            expected
        }
        assertSame(expected, actual)
        assertEquals(1, queries)
    }

    @Test fun metadataPreflightFailurePreservesOriginalException() = runBlocking {
        val original = IllegalStateException("metadata unavailable")
        try {
            withCacheCopyEntryPreflight<Unit> { throw original }
            fail("Expected preflight failure")
        } catch (actual: IllegalStateException) {
            assertSame(original, actual)
        }
    }

    @Test fun repeatedActiveEntriesInvokePreflightExactlyOnceEach() = runBlocking {
        val order = mutableListOf<Int>()
        assertEquals(1, withCacheCopyEntryPreflight { order.add(1); 1 })
        assertEquals(2, withCacheCopyEntryPreflight { order.add(2); 2 })
        assertEquals(listOf(1, 2), order)
    }
}
