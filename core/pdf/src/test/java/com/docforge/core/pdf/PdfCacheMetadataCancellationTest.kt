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
