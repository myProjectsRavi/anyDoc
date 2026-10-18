package com.docforge.core.pdf

import java.util.concurrent.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class PdfComparePageBitmapCleanupTest {
    private data class FakeBitmap(val name: String, val failure: Throwable? = null)

    private fun recycle(bitmap: FakeBitmap, log: MutableList<String>) {
        log += bitmap.name
        bitmap.failure?.let { throw it }
    }

    @Test fun successReturnsValueAndRecyclesAll() {
        val log = mutableListOf<String>()
        val result = withComparePageBitmapCleanup(
            resources = { listOf(FakeBitmap("left"), FakeBitmap("right"), FakeBitmap("diff")) },
            recycle = { recycle(it, log) }
        ) { 42 }
        assertEquals(42, result)
        assertEquals(listOf("left", "right", "diff"), log)
    }

    @Test fun firstRecycleFailureStillAttemptsAllResources() {
        val error = IllegalStateException("left")
        val log = mutableListOf<String>()
        val thrown = assertThrows(IllegalStateException::class.java) {
            withComparePageBitmapCleanup(
                resources = { listOf(FakeBitmap("left", error), FakeBitmap("right"), FakeBitmap("diff")) },
                recycle = { recycle(it, log) }
            ) { Unit }
        }
        assertSame(error, thrown)
        assertEquals(listOf("left", "right", "diff"), log)
    }

    @Test fun multipleRecycleFailuresKeepFirstAndSuppressSecond() {
        val first = IllegalStateException("first")
        val second = IllegalArgumentException("second")
        val log = mutableListOf<String>()
        val thrown = assertThrows(IllegalStateException::class.java) {
            withComparePageBitmapCleanup(
                resources = { listOf(FakeBitmap("left", first), FakeBitmap("right", second), FakeBitmap("diff")) },
                recycle = { recycle(it, log) }
            ) { Unit }
        }
        assertSame(first, thrown)
        assertSame(second, thrown.suppressed.single())
        assertEquals(listOf("left", "right", "diff"), log)
    }

    @Test fun pageFailureRemainsPrimaryAndCleanupContinues() {
        val primary = IllegalStateException("page")
        val cleanup = IllegalArgumentException("recycle")
        val log = mutableListOf<String>()
        val thrown = assertThrows(IllegalStateException::class.java) {
            withComparePageBitmapCleanup(
                resources = { listOf(FakeBitmap("left", cleanup), FakeBitmap("right"), FakeBitmap("diff")) },
                recycle = { recycle(it, log) }
            ) { throw primary }
        }
        assertSame(primary, thrown)
        assertSame(cleanup, thrown.suppressed.single())
        assertEquals(listOf("left", "right", "diff"), log)
    }

    @Test fun identicalPrimaryAndRecycleFailureDoesNotSelfSuppress() {
        val shared = IllegalStateException("shared")
        val thrown = assertThrows(IllegalStateException::class.java) {
            withComparePageBitmapCleanup(
                resources = { listOf(FakeBitmap("left", shared), FakeBitmap("right")) },
                recycle = { it.failure?.let { error -> throw error } }
            ) { throw shared }
        }
        assertSame(shared, thrown)
        assertTrue(thrown.suppressed.isEmpty())
    }

    @Test fun cancellationPreservedAndNullResourcesIgnored() {
        val cancellation = CancellationException("cancelled")
        val cleanup = IllegalStateException("recycle")
        val log = mutableListOf<String>()
        val thrown = assertThrows(CancellationException::class.java) {
            withComparePageBitmapCleanup(
                resources = { listOf(null, FakeBitmap("right", cleanup), null, FakeBitmap("diff")) },
                recycle = { bitmap: FakeBitmap -> recycle(bitmap, log) }
            ) { throw cancellation }
        }
        assertSame(cancellation, thrown)
        assertSame(cleanup, thrown.suppressed.single())
        assertEquals(listOf("right", "diff"), log)
    }
}
