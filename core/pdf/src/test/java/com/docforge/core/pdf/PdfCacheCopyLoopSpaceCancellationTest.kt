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

class PdfCacheCopyLoopSpaceCancellationTest {
    @Test fun cancelledIterationSkipsFreeSpaceQuery() = runBlocking {
        var queried = false
        var cancelled = false
        val worker = launch(start = CoroutineStart.UNDISPATCHED) {
            coroutineContext.cancel()
            try {
                withCacheCopySpaceCheck {
                    queried = true
                    true
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

    @Test fun cancellationDuringFreeSpaceQueryTakesPrecedence() = runBlocking {
        var cancelled = false
        var queried = 0
        val worker = launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                withCacheCopySpaceCheck {
                    queried++
                    coroutineContext.cancel()
                    false
                }
                fail("Expected cancellation")
            } catch (_: CancellationException) {
                cancelled = true
            }
        }
        worker.join()
        assertEquals(1, queried)
        assertTrue(cancelled)
    }

    @Test fun activeSpaceCheckPreservesBothBooleanOutcomes() = runBlocking {
        assertTrue(withCacheCopySpaceCheck { true })
        assertFalse(withCacheCopySpaceCheck { false })
    }

    @Test fun activeSpaceCheckPreservesOriginalFailure() = runBlocking {
        val original = IllegalStateException("space unavailable")
        try {
            withCacheCopySpaceCheck<Boolean> { throw original }
            fail("Expected space query failure")
        } catch (actual: IllegalStateException) {
            assertSame(original, actual)
        }
    }

    @Test fun cancellationBetweenIterationsSkipsSecondQuery() = runBlocking {
        var queries = 0
        var cancelled = false
        val worker = launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                withCacheCopySpaceCheck { queries++; true }
                coroutineContext.cancel()
                withCacheCopySpaceCheck { queries++; true }
                fail("Expected cancellation")
            } catch (_: CancellationException) {
                cancelled = true
            }
        }
        worker.join()
        assertTrue(cancelled)
        assertEquals(1, queries)
    }

    @Test fun activeSpaceChecksKeepQueryOrder() = runBlocking {
        val checks = mutableListOf<String>()
        assertTrue(withCacheCopySpaceCheck { checks += "before"; true })
        assertTrue(withCacheCopySpaceCheck { checks += "after"; true })
        assertEquals(listOf("before", "after"), checks)
    }

    @Test fun cancellationDuringTransferPreventsNormalReturnEvenAtEof() = runBlocking {
        var cancelled = false
        var transferred = 0
        val worker = launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                copyChunkWithCancellation {
                    transferred++
                    coroutineContext.cancel()
                    0L
                }
                fail("Expected cancellation")
            } catch (_: CancellationException) {
                cancelled = true
            }
        }
        worker.join()
        assertTrue(cancelled)
        assertEquals(1, transferred)
    }

    @Test fun activeTransferPreservesTransferredByteCount() = runBlocking {
        assertEquals(42L, copyChunkWithCancellation { 42L })
    }

    @Test fun transferFailurePreservesOriginalException() = runBlocking {
        val original = IllegalStateException("read failed")
        try {
            copyChunkWithCancellation { throw original }
            fail("Expected transfer failure")
        } catch (actual: IllegalStateException) {
            assertSame(original, actual)
        }
    }
}
