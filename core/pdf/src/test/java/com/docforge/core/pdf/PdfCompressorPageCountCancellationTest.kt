package com.docforge.core.pdf

import java.io.IOException
import java.io.InterruptedIOException
import java.net.SocketTimeoutException
import java.nio.channels.AsynchronousCloseException
import java.nio.channels.ClosedByInterruptException
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

class PdfCompressorPageCountCancellationTest {
    @Test fun successfulPageCountIsReturned() = runBlocking {
        assertEquals(12, pageCountOrZero { 12 })
    }

    @Test fun ordinaryIoFailureReturnsZero() = runBlocking {
        assertEquals(0, pageCountOrZero { throw IOException("bad PDF") })
    }

    @Test fun invalidDocumentFailureReturnsZero() = runBlocking {
        assertEquals(0, pageCountOrZero { throw IllegalArgumentException("invalid PDF") })
    }

    @Test fun originalCancellationIsPropagated() = runBlocking {
        val original = CancellationException("page count cancelled")
        try {
            pageCountOrZero { throw original }
            fail("Expected cancellation")
        } catch (actual: CancellationException) {
            assertSame(original, actual)
        }
    }

    @Test fun alreadyCancelledCoroutineSkipsPageCountProbe() = runBlocking {
        var called = false
        var caught = false
        val worker = launch(start = CoroutineStart.UNDISPATCHED) {
            coroutineContext.cancel()
            try {
                pageCountOrZero { called = true; 3 }
                fail("Expected cancellation before probe")
            } catch (_: CancellationException) {
                caught = true
            }
        }
        worker.join()
        assertTrue(caught)
        assertFalse(called)
    }

    @Test fun cancellationAfterProbeDoesNotReturnStaleCount() = runBlocking {
        var caught = false
        val worker = launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                pageCountOrZero {
                    coroutineContext.cancel()
                    3
                }
                fail("Expected cancellation after probe")
            } catch (_: CancellationException) {
                caught = true
            }
        }
        worker.join()
        assertTrue(caught)
    }

    @Test fun interruptedThreadSignalPreservesIdentity() = runBlocking {
        val original = InterruptedException("provider interrupted")
        try {
            pageCountOrZero { throw original }
            fail("Expected interruption")
        } catch (actual: InterruptedException) {
            assertSame(original, actual)
        }
    }

    @Test fun interruptedIoSignalPreservesIdentity() = runBlocking {
        val original = InterruptedIOException("provider interrupted")
        try {
            pageCountOrZero { throw original }
            fail("Expected interrupted I/O")
        } catch (actual: InterruptedIOException) {
            assertSame(original, actual)
        }
    }

    @Test fun interruptedChannelSignalPreservesIdentity() = runBlocking {
        val original = ClosedByInterruptException()
        try {
            pageCountOrZero { throw original }
            fail("Expected interrupted channel")
        } catch (actual: ClosedByInterruptException) {
            assertSame(original, actual)
        }
    }

    @Test fun socketTimeoutStillReturnsZero() = runBlocking {
        assertEquals(0, pageCountOrZero { throw SocketTimeoutException("provider timeout") })
    }

    @Test fun asynchronousCloseStillReturnsZero() = runBlocking {
        assertEquals(0, pageCountOrZero { throw AsynchronousCloseException() })
    }

    @Test fun cancelledCoroutineWithRecoverableIoFailureDoesNotReturnZero() = runBlocking {
        var cancellationObserved = false
        val worker = launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                pageCountOrZero {
                    coroutineContext.cancel()
                    throw IOException("provider failed during cancellation")
                }
                fail("Expected cancellation, not a zero-page hint")
            } catch (_: CancellationException) {
                cancellationObserved = true
            }
        }
        worker.join()
        assertTrue(cancellationObserved)
    }

    @Test fun cancelledCoroutineWithSocketTimeoutDoesNotReturnZero() = runBlocking {
        var cancellationObserved = false
        val worker = launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                pageCountOrZero {
                    coroutineContext.cancel()
                    throw SocketTimeoutException("provider timed out during cancellation")
                }
                fail("Expected cancellation, not a zero-page hint")
            } catch (_: CancellationException) {
                cancellationObserved = true
            }
        }
        worker.join()
        assertTrue(cancellationObserved)
    }

    @Test fun fatalErrorIsNotSilenced() = runBlocking {
        val original = LinkageError("runtime failure")
        try {
            pageCountOrZero { throw original }
            fail("Expected fatal error")
        } catch (actual: LinkageError) {
            assertSame(original, actual)
        }
    }
}
