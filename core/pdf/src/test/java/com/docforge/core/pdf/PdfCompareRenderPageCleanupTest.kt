package com.docforge.core.pdf

import java.util.concurrent.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class PdfCompareRenderPageCleanupTest {
    private class FakeBitmap(var recycleFailure: Throwable? = null) {
        var recycleCount = 0

        fun recycle() {
            recycleCount++
            recycleFailure?.let { throw it }
        }
    }

    @Test fun successfulRenderReturnsOwnedBitmapWithoutRecycling() {
        val bitmap = FakeBitmap()
        var renderCount = 0
        val result = renderComparePageWithCleanup(bitmap, FakeBitmap::recycle) {
            assertSame(bitmap, it)
            renderCount++
        }
        assertSame(bitmap, result)
        assertEquals(1, renderCount)
        assertEquals(0, bitmap.recycleCount)
    }

    @Test fun renderFailureRecyclesOnceAndPreservesOriginal() {
        val bitmap = FakeBitmap()
        val renderFailure = IllegalStateException("render failed")
        val actual = assertThrows(IllegalStateException::class.java) {
            renderComparePageWithCleanup(bitmap, FakeBitmap::recycle) { throw renderFailure }
        }
        assertSame(renderFailure, actual)
        assertEquals(1, bitmap.recycleCount)
        assertTrue(actual.suppressed.isEmpty())
    }

    @Test fun renderAndRecycleFailuresPreserveOriginalAndSuppressCleanup() {
        val renderFailure = IllegalStateException("render failed")
        val recycleFailure = IllegalArgumentException("recycle failed")
        val bitmap = FakeBitmap(recycleFailure)
        val actual = assertThrows(IllegalStateException::class.java) {
            renderComparePageWithCleanup(bitmap, FakeBitmap::recycle) { throw renderFailure }
        }
        assertSame(renderFailure, actual)
        assertSame(recycleFailure, actual.suppressed.single())
        assertEquals(1, bitmap.recycleCount)
    }

    @Test fun eraseFailurePreservesOriginalWhenRecycleAlsoFails() {
        val eraseFailure = IllegalArgumentException("erase failed")
        val recycleFailure = IllegalStateException("recycle failed")
        val bitmap = FakeBitmap(recycleFailure)
        val actual = assertThrows(IllegalArgumentException::class.java) {
            renderComparePageWithCleanup(bitmap, FakeBitmap::recycle) { throw eraseFailure }
        }
        assertSame(eraseFailure, actual)
        assertSame(recycleFailure, actual.suppressed.single())
        assertEquals(1, bitmap.recycleCount)
    }

    @Test fun cancellationRemainsPrimaryWhenRecycleFails() {
        val cancellation = CancellationException("cancelled")
        val recycleFailure = IllegalStateException("recycle failed")
        val bitmap = FakeBitmap(recycleFailure)
        val actual = assertThrows(CancellationException::class.java) {
            renderComparePageWithCleanup(bitmap, FakeBitmap::recycle) { throw cancellation }
        }
        assertSame(cancellation, actual)
        assertSame(recycleFailure, actual.suppressed.single())
        assertEquals(1, bitmap.recycleCount)
    }

    @Test fun identicalRenderAndRecycleThrowableDoesNotSelfSuppress() {
        val failure = IllegalStateException("shared throwable")
        val bitmap = FakeBitmap(failure)
        val actual = assertThrows(IllegalStateException::class.java) {
            renderComparePageWithCleanup(bitmap, FakeBitmap::recycle) { throw failure }
        }
        assertSame(failure, actual)
        assertTrue(actual.suppressed.isEmpty())
        assertEquals(1, bitmap.recycleCount)
    }
}
