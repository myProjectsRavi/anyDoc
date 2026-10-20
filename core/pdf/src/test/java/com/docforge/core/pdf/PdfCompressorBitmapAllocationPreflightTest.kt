package com.docforge.core.pdf

import java.io.Closeable
import java.io.IOException
import java.util.concurrent.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.cancel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class PdfCompressorBitmapAllocationPreflightTest {
    @Test fun cancellationAfterRasterSizingSkipsAllocationAndClosesOwners() {
        val events = mutableListOf<String>()
        val original = CancellationException("cancel after raster sizing")
        val actual = assertThrows(CancellationException::class.java) {
            Closeable { events += "close-descriptor" }.use {
                Closeable { events += "close-renderer" }.use {
                    Closeable { events += "close-document" }.use {
                        Closeable { events += "close-page" }.use {
                            val size = boundedPdfRasterSize(612, 792, 120, 8L * 1024 * 1024)
                            assertTrue(size.width > 0)
                            events += "size"
                            openCompressionResourceWithPreflight(
                                checkCancelled = { events += "check"; throw original },
                                open = { events += "allocate"; Closeable {} }
                            ).use {}
                        }
                    }
                }
            }
        }
        assertSame(original, actual)
        assertEquals(
            listOf("size", "check", "close-page", "close-document",
                "close-renderer", "close-descriptor"),
            events
        )
    }

    @Test fun successfulAllocationFollowsSizingAndPreflightAndRecycles() {
        val events = mutableListOf<String>()
        val bitmap = Closeable { events += "recycle-bitmap" }
        Closeable { events += "close-page" }.use {
            val size = boundedPdfRasterSize(612, 792, 96, 8L * 1024 * 1024)
            events += "size"
            val actual = openCompressionResourceWithPreflight(
                checkCancelled = { events += "check" },
                open = { events += "allocate"; bitmap }
            )
            assertSame(bitmap, actual)
            assertTrue(size.width > 0 && size.height > 0)
            actual.use { events += "render" }
        }
        assertEquals(
            listOf("size", "check", "allocate", "render", "recycle-bitmap", "close-page"),
            events
        )
    }

    @Test fun allocationFailurePreservesIdentityAndClosesOwners() {
        val events = mutableListOf<String>()
        val original = IOException("bitmap allocation failed")
        val actual = assertThrows(IOException::class.java) {
            Closeable { events += "close-document" }.use {
                Closeable { events += "close-page" }.use {
                    events += "size"
                    openCompressionResourceWithPreflight(
                        checkCancelled = { events += "check" },
                        open = { events += "allocate"; throw original }
                    )
                }
            }
        }
        assertSame(original, actual)
        assertEquals(
            listOf("size", "check", "allocate", "close-page", "close-document"),
            events
        )
    }

    @Test fun preflightInterruptionPreservesOriginalExceptionAndSkipsAllocation() {
        val original = InterruptedException("interrupted before bitmap")
        var allocated = false
        val actual = assertThrows(InterruptedException::class.java) {
            openCompressionResourceWithPreflight(
                checkCancelled = { throw original },
                open = { allocated = true; Any() }
            )
        }
        assertSame(original, actual)
        assertFalse(allocated)
    }

    @Test fun renderFailureRecyclesBitmapAndClosesPage() {
        val events = mutableListOf<String>()
        val original = IllegalStateException("render failed")
        val actual = assertThrows(IllegalStateException::class.java) {
            Closeable { events += "close-page" }.use {
                openCompressionResourceWithPreflight(
                    checkCancelled = { events += "check" },
                    open = { events += "allocate"; Closeable { events += "recycle-bitmap" } }
                ).use {
                    events += "render"
                    throw original
                }
            }
        }
        assertSame(original, actual)
        assertEquals(
            listOf("check", "allocate", "render", "recycle-bitmap", "close-page"),
            events
        )
    }

    @Test fun everyPageChecksCancellationBeforeItsOwnAllocation() {
        val events = mutableListOf<String>()
        repeat(3) { page ->
            val token = Any()
            val result = openCompressionResourceWithPreflight(
                checkCancelled = { events += "check-$page" },
                open = { events += "allocate-$page"; token }
            )
            assertSame(token, result)
        }
        assertEquals(
            (0..2).flatMap { listOf("check-$it", "allocate-$it") },
            events
        )
    }

    @Test fun bitmapBudgetAndRasterSizeRemainUnchanged() {
        val budget = pdfCompressionBitmapBudgetBytes(64L * 1024 * 1024)
        val size = boundedPdfRasterSize(4000, 3000, 150, budget)
        var checked = false
        val result = openCompressionResourceWithPreflight(
            checkCancelled = { checked = true },
            open = { size }
        )
        assertTrue(checked)
        assertSame(size, result)
        assertTrue(size.width.toLong() * size.height.toLong() * 4L <= budget)
    }

    @Test fun realCoroutineCancellationAfterSizingSkipsAllocation() = runBlocking {
        var allocated = false
        var pageClosed = false
        var observed = false
        val worker = launch(start = CoroutineStart.UNDISPATCHED) {
            val checkCancelled = { coroutineContext.ensureActive() }
            Closeable { pageClosed = true }.use {
                boundedPdfRasterSize(612, 792, 120, 8L * 1024 * 1024)
                coroutineContext.cancel()
                try {
                    openCompressionResourceWithPreflight(checkCancelled) {
                        allocated = true
                        Any()
                    }
                } catch (_: CancellationException) {
                    observed = true
                }
            }
        }
        worker.join()
        assertTrue(observed)
        assertTrue(pageClosed)
        assertFalse(allocated)
    }
}
