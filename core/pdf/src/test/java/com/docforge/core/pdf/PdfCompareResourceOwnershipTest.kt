package com.docforge.core.pdf

import java.util.concurrent.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.Assert.assertThrows

class PdfCompareResourceOwnershipTest {
    private class FakeRaster {
        var recycleCalls = 0
        fun recycle() { recycleCalls++ }
    }

    @Test
    fun cancellationRecyclesOutputBeforeOwnershipTransfer() {
        val output = FakeRaster()
        val cancellation = CancellationException("cancelled")
        val thrown = assertThrows(CancellationException::class.java) {
            withCleanupOnFailure(output, FakeRaster::recycle) {
                throw cancellation
            }
        }
        assertSame(cancellation, thrown)
        assertEquals(1, output.recycleCalls)
    }

    @Test
    fun successfulDiffTransfersOutputWithoutRecycling() {
        val output = FakeRaster()
        val result = withCleanupOnFailure(output, FakeRaster::recycle) { output to 12.5f }
        assertSame(output, result.first)
        assertEquals(12.5f, result.second, 0f)
        assertEquals(0, output.recycleCalls)
    }

    @Test
    fun unexpectedFailureRecyclesOutput() {
        val output = FakeRaster()
        val failure = IllegalStateException("pixel access failed")
        val thrown = assertThrows(IllegalStateException::class.java) {
            withCleanupOnFailure(output, FakeRaster::recycle) {
                throw failure
            }
        }
        assertSame(failure, thrown)
        assertEquals(1, output.recycleCalls)
    }

    @Test
    fun cleanupFailureDoesNotMaskOriginalFailure() {
        val failure = IllegalStateException("render failed")
        val cleanupFailure = IllegalArgumentException("recycle failed")
        val thrown = assertThrows(IllegalStateException::class.java) {
            withCleanupOnFailure(Any(), { throw cleanupFailure }) {
                throw failure
            }
        }
        assertSame(failure, thrown)
        assertEquals(1, thrown.suppressed.size)
        assertSame(cleanupFailure, thrown.suppressed[0])
        assertTrue(thrown.message!!.contains("render failed"))
    }
}
