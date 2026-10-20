package com.docforge.core.pdf

import java.io.IOException
import java.util.concurrent.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.fail
import org.junit.Test

class PdfCacheMetadataCancellationTest {
    @Test fun queryCancellationPropagatesWithoutDescriptorFallback() {
        val cancelled = CancellationException("query cancelled")
        var descriptorCalled = false
        try {
            knownInputSizeFromProbes(
                querySize = { throw cancelled },
                descriptorSize = { descriptorCalled = true; 42L }
            )
            fail("Expected cancellation")
        } catch (actual: CancellationException) {
            assertSame(cancelled, actual)
        }
        assertFalse(descriptorCalled)
    }

    @Test fun descriptorCancellationPropagatesWithoutMasking() {
        val cancelled = CancellationException("descriptor cancelled")
        try {
            knownInputSizeFromProbes(querySize = { null }, descriptorSize = { throw cancelled })
            fail("Expected cancellation")
        } catch (actual: CancellationException) {
            assertSame(cancelled, actual)
        }
    }

    @Test fun queryInterruptionPropagatesWithoutDescriptorFallback() {
        val interrupted = InterruptedException("query interrupted")
        var descriptorCalled = false
        try {
            knownInputSizeFromProbes(
                querySize = { throw interrupted },
                descriptorSize = { descriptorCalled = true; 42L }
            )
            fail("Expected interruption")
        } catch (actual: InterruptedException) {
            assertSame(interrupted, actual)
        }
        assertFalse(descriptorCalled)
    }

    @Test fun descriptorInterruptionPreservesOriginalException() {
        val interrupted = InterruptedException("descriptor interrupted")
        try {
            knownInputSizeFromProbes(
                querySize = { null },
                descriptorSize = { throw interrupted }
            )
            fail("Expected interruption")
        } catch (actual: InterruptedException) {
            assertSame(interrupted, actual)
        }
    }

    @Test fun directMetadataProbeInterruptionIsNotSilenced() {
        val interrupted = InterruptedException("provider interrupted")
        try {
            metadataProbeOrNull<Long> { throw interrupted }
            fail("Expected interruption")
        } catch (actual: InterruptedException) {
            assertSame(interrupted, actual)
        }
    }

    @Test fun interruptedIoQueryPropagatesWithoutDescriptorFallback() {
        val interrupted = java.io.InterruptedIOException("query interrupted")
        var descriptorCalled = false
        try {
            knownInputSizeFromProbes(
                querySize = { throw interrupted },
                descriptorSize = { descriptorCalled = true; 42L }
            )
            fail("Expected interrupted I/O")
        } catch (actual: java.io.InterruptedIOException) {
            assertSame(interrupted, actual)
        }
        assertFalse(descriptorCalled)
    }

    @Test fun interruptedIoDescriptorPreservesOriginalException() {
        val interrupted = java.io.InterruptedIOException("descriptor interrupted")
        try {
            knownInputSizeFromProbes(querySize = { -1L }, descriptorSize = { throw interrupted })
            fail("Expected interrupted I/O")
        } catch (actual: java.io.InterruptedIOException) {
            assertSame(interrupted, actual)
        }
    }

    @Test fun directMetadataProbeInterruptedIoPropagates() {
        val interrupted = java.io.InterruptedIOException("probe interrupted")
        try {
            metadataProbeOrNull<Long> { throw interrupted }
            fail("Expected interrupted I/O")
        } catch (actual: java.io.InterruptedIOException) {
            assertSame(interrupted, actual)
        }
    }

    @Test fun closedByInterruptQueryPropagatesWithoutDescriptorFallback() {
        val interrupted = java.nio.channels.ClosedByInterruptException()
        var descriptorCalled = false
        try {
            knownInputSizeFromProbes(
                querySize = { throw interrupted },
                descriptorSize = { descriptorCalled = true; 42L }
            )
            fail("Expected interrupted channel")
        } catch (actual: java.nio.channels.ClosedByInterruptException) {
            assertSame(interrupted, actual)
        }
        assertFalse(descriptorCalled)
    }

    @Test fun closedByInterruptDescriptorPreservesOriginalException() {
        val interrupted = java.nio.channels.ClosedByInterruptException()
        try {
            knownInputSizeFromProbes(querySize = { -1L }, descriptorSize = { throw interrupted })
            fail("Expected interrupted channel")
        } catch (actual: java.nio.channels.ClosedByInterruptException) {
            assertSame(interrupted, actual)
        }
    }

    @Test fun directMetadataProbeClosedByInterruptPropagates() {
        val interrupted = java.nio.channels.ClosedByInterruptException()
        try {
            metadataProbeOrNull<Long> { throw interrupted }
            fail("Expected interrupted channel")
        } catch (actual: java.nio.channels.ClosedByInterruptException) {
            assertSame(interrupted, actual)
        }
    }

    @Test fun asynchronousCloseQueryStillFallsBackToDescriptor() {
        assertEquals(42L, knownInputSizeFromProbes(
            querySize = { throw java.nio.channels.AsynchronousCloseException() },
            descriptorSize = { 42L }
        ))
    }

    @Test fun asynchronousCloseDescriptorStillReturnsUnknownSize() {
        assertNull(knownInputSizeFromProbes(
            querySize = { null },
            descriptorSize = { throw java.nio.channels.AsynchronousCloseException() }
        ))
    }

    @Test fun socketTimeoutInQueryFallsBackToDescriptor() {
        assertEquals(42L, knownInputSizeFromProbes(
            querySize = { throw java.net.SocketTimeoutException("query timed out") },
            descriptorSize = { 42L }
        ))
    }

    @Test fun socketTimeoutInDescriptorReturnsUnknownSize() {
        assertNull(knownInputSizeFromProbes(
            querySize = { null },
            descriptorSize = { throw java.net.SocketTimeoutException("descriptor timed out") }
        ))
    }

    @Test fun ordinaryQueryIOExceptionStillFallsBackToDescriptor() {
        assertEquals(42L, knownInputSizeFromProbes(
            querySize = { throw IOException("query metadata unavailable") },
            descriptorSize = { 42L }
        ))
    }

    @Test fun ordinaryDescriptorIOExceptionStillReturnsUnknownSize() {
        assertNull(knownInputSizeFromProbes(
            querySize = { null },
            descriptorSize = { throw IOException("descriptor metadata unavailable") }
        ))
    }

    @Test fun fatalDescriptorErrorPreservesIdentity() {
        val fatal = LinkageError("descriptor provider failure")
        try {
            knownInputSizeFromProbes(querySize = { null }, descriptorSize = { throw fatal })
            fail("Expected fatal error")
        } catch (actual: LinkageError) {
            assertSame(fatal, actual)
        }
    }

    @Test fun ordinaryQueryFailureFallsBackToDescriptor() {
        assertEquals(42L, knownInputSizeFromProbes(
            querySize = { throw SecurityException("metadata unavailable") },
            descriptorSize = { 42L }
        ))
    }

    @Test fun ordinaryDescriptorFailureReturnsUnknownSize() {
        assertNull(knownInputSizeFromProbes(
            querySize = { null },
            descriptorSize = { throw IOException("metadata unavailable") }
        ))
    }

    @Test fun validQuerySizeSkipsDescriptor() {
        var descriptorCalled = false
        assertEquals(19L, knownInputSizeFromProbes(
            querySize = { 19L },
            descriptorSize = { descriptorCalled = true; 21L }
        ))
        assertFalse(descriptorCalled)
    }

    @Test fun negativeQuerySizeFallsBackToDescriptor() {
        assertEquals(24L, knownInputSizeFromProbes(querySize = { -1L }, descriptorSize = { 24L }))
    }

    @Test fun negativeDescriptorSizeIsUnknown() {
        assertNull(knownInputSizeFromProbes(querySize = { null }, descriptorSize = { -1L }))
    }

    @Test fun fatalProviderErrorIsNotSilenced() {
        val failure = LinkageError("provider linkage failure")
        try {
            knownInputSizeFromProbes(querySize = { throw failure }, descriptorSize = { 100L })
            fail("Expected fatal error")
        } catch (actual: LinkageError) {
            assertSame(failure, actual)
        }
    }
}
