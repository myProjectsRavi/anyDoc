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

class PdfCompressorPageRenderPreflightTest {
    @Test fun cancellationAfterWhiteFillSkipsRenderAndClosesAllOwners() {
        val events = mutableListOf<String>()
        val cancelled = CancellationException("cancel after white fill")
        val actual = assertThrows(CancellationException::class.java) {
            Closeable { events += "close-descriptor" }.use {
                Closeable { events += "close-renderer" }.use {
                    Closeable { events += "close-document" }.use {
                        Closeable { events += "close-page" }.use {
                            val bitmap = Closeable { events += "recycle-bitmap" }
                            try {
                                events += "allocate-bitmap"
                                events += "fill-white"
                                openCompressionResourceWithPreflight(
                                    checkCancelled = { events += "check"; throw cancelled },
                                    open = { events += "render"; Unit }
                                )
                            } finally {
                                bitmap.close()
                            }
                        }
                    }
                }
            }
        }
        assertSame(cancelled, actual)
        assertEquals(
            listOf("allocate-bitmap", "fill-white", "check", "recycle-bitmap",
                "close-page", "close-document", "close-renderer", "close-descriptor"),
            events
        )
    }

    @Test fun successfulPageFillsThenChecksThenRendersAndEncodes() {
        val events = mutableListOf<String>()
        Closeable { events += "close-page" }.use {
            val bitmap = Closeable { events += "recycle-bitmap" }
            try {
                events += "allocate-bitmap"
                events += "fill-white"
                openCompressionResourceWithPreflight(
                    checkCancelled = { events += "check" },
                    open = { events += "render"; Unit }
                )
                events += "encode-jpeg"
            } finally {
                bitmap.close()
            }
        }
        assertEquals(
            listOf("allocate-bitmap", "fill-white", "check", "render", "encode-jpeg",
                "recycle-bitmap", "close-page"), events
        )
    }

    @Test fun rendererIOExceptionRetainsIdentityAndRecyclesBitmap() {
        val events = mutableListOf<String>()
        val original = IOException("render failed")
        val actual = assertThrows(IOException::class.java) {
            Closeable { events += "close-document" }.use {
                Closeable { events += "close-page" }.use {
                    val bitmap = Closeable { events += "recycle-bitmap" }
                    try {
                        events += "fill-white"
                        openCompressionResourceWithPreflight(
                            checkCancelled = { events += "check" },
                            open = { events += "render"; throw original }
                        )
                    } finally {
                        bitmap.close()
                    }
                }
            }
        }
        assertSame(original, actual)
        assertEquals(
            listOf("fill-white", "check", "render", "recycle-bitmap",
                "close-page", "close-document"), events
        )
    }

    @Test fun interruptedPreflightSkipsRenderWithoutChangingException() {
        val original = InterruptedException("interrupted before render")
        var rendered = false
        val actual = assertThrows(InterruptedException::class.java) {
            openCompressionResourceWithPreflight(
                checkCancelled = { throw original },
                open = { rendered = true; Unit }
            )
        }
        assertSame(original, actual)
        assertFalse(rendered)
    }

    @Test fun cancelledPreflightRetainsSuppressedBitmapCleanupException() {
        val cancelled = CancellationException("cancel before render")
        val cleanup = IOException("bitmap cleanup failed")
        var rendered = false
        val actual = assertThrows(CancellationException::class.java) {
            Closeable { throw cleanup }.use {
                openCompressionResourceWithPreflight(
                    checkCancelled = { throw cancelled },
                    open = { rendered = true; Unit }
                )
            }
        }
        assertSame(cancelled, actual)
        assertFalse(rendered)
        assertEquals(1, actual.suppressed.size)
        assertSame(cleanup, actual.suppressed.single())
    }

    @Test fun everyPageHasItsOwnRenderPreflight() {
        val events = mutableListOf<String>()
        repeat(3) { page ->
            events += "fill-$page"
            openCompressionResourceWithPreflight(
                checkCancelled = { events += "check-$page" },
                open = { events += "render-$page"; Unit }
            )
        }
        assertEquals(
            (0..2).flatMap { listOf("fill-$it", "check-$it", "render-$it") },
            events
        )
    }

    @Test fun rasterBudgetDimensionsAndJpegQualityRemainUnchanged() {
        val level = PdfCompressionLevel.MEDIUM
        val budget = pdfCompressionBitmapBudgetBytes(64L * 1024 * 1024)
        val size = boundedPdfRasterSize(612, 792, level.renderDpi, budget)
        var checked = false
        val rendered = openCompressionResourceWithPreflight(
            checkCancelled = { checked = true },
            open = { Triple(size.width, size.height, level.jpegQuality / 100f) }
        )
        assertTrue(checked)
        assertEquals(size.width, rendered.first)
        assertEquals(size.height, rendered.second)
        assertEquals(0.70f, rendered.third, 0.0001f)
        assertTrue(size.width.toLong() * size.height.toLong() * 4L <= budget)
    }

    @Test fun realCoroutineCancellationAfterFillSkipsRenderAndRecycles() = runBlocking {
        var rendered = false
        var recycled = false
        var observed = false
        val worker = launch(start = CoroutineStart.UNDISPATCHED) {
            val checkCancelled = { coroutineContext.ensureActive() }
            val bitmap = Closeable { recycled = true }
            try {
                coroutineContext.cancel()
                openCompressionResourceWithPreflight(checkCancelled) {
                    rendered = true
                    Unit
                }
            } catch (_: CancellationException) {
                observed = true
            } finally {
                bitmap.close()
            }
        }
        worker.join()
        assertTrue(observed)
        assertTrue(recycled)
        assertFalse(rendered)
    }
}
