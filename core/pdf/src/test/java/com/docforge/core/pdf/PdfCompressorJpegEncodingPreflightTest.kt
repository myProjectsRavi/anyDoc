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

class PdfCompressorJpegEncodingPreflightTest {
    @Test fun cancellationAfterRenderingSkipsEncodingAndClosesResources() {
        val events = mutableListOf<String>()
        val cancelled = CancellationException("cancel after render")
        val actual = assertThrows(CancellationException::class.java) {
            Closeable { events += "close-descriptor" }.use {
                Closeable { events += "close-renderer" }.use {
                    Closeable { events += "close-document" }.use {
                        Closeable { events += "close-page" }.use {
                            val bitmap = Closeable { events += "recycle-bitmap" }
                            try {
                                events += "render"
                                openCompressionResourceWithPreflight(
                                    checkCancelled = { events += "check"; throw cancelled },
                                    open = { events += "encode"; Any() }
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
            listOf("render", "check", "recycle-bitmap", "close-page",
                "close-document", "close-renderer", "close-descriptor"),
            events
        )
    }

    @Test fun successfulEncodingFollowsRenderingAndPreflightBeforeDrawing() {
        val events = mutableListOf<String>()
        val encoded = Any()
        Closeable { events += "close-page" }.use {
            val bitmap = Closeable { events += "recycle-bitmap" }
            try {
                events += "render"
                events += "add-page"
                val result = openCompressionResourceWithPreflight(
                    checkCancelled = { events += "check" },
                    open = { events += "encode"; encoded }
                )
                assertSame(encoded, result)
                events += "draw"
            } finally {
                bitmap.close()
            }
        }
        assertEquals(
            listOf("render", "add-page", "check", "encode", "draw",
                "recycle-bitmap", "close-page"),
            events
        )
    }

    @Test fun encoderIOExceptionPreservesIdentityAndRecyclesBitmap() {
        val events = mutableListOf<String>()
        val original = IOException("JPEG encoder failed")
        val actual = assertThrows(IOException::class.java) {
            Closeable { events += "close-document" }.use {
                Closeable { events += "close-page" }.use {
                    val bitmap = Closeable { events += "recycle-bitmap" }
                    try {
                        events += "render"
                        openCompressionResourceWithPreflight(
                            checkCancelled = { events += "check" },
                            open = { events += "encode"; throw original }
                        )
                    } finally {
                        bitmap.close()
                    }
                }
            }
        }
        assertSame(original, actual)
        assertEquals(
            listOf("render", "check", "encode", "recycle-bitmap",
                "close-page", "close-document"),
            events
        )
    }

    @Test fun interruptedPreflightPreservesIdentityAndSkipsEncoding() {
        val interrupted = InterruptedException("interrupted before JPEG")
        var encoded = false
        val actual = assertThrows(InterruptedException::class.java) {
            openCompressionResourceWithPreflight(
                checkCancelled = { throw interrupted },
                open = { encoded = true; Any() }
            )
        }
        assertSame(interrupted, actual)
        assertFalse(encoded)
    }

    @Test fun drawFailureAfterEncodingStillRecyclesAndCloses() {
        val events = mutableListOf<String>()
        val failure = IllegalStateException("draw failed")
        val actual = assertThrows(IllegalStateException::class.java) {
            Closeable { events += "close-document" }.use {
                Closeable { events += "close-page" }.use {
                    val bitmap = Closeable { events += "recycle-bitmap" }
                    try {
                        openCompressionResourceWithPreflight(
                            checkCancelled = { events += "check" },
                            open = { events += "encode"; Any() }
                        )
                        events += "draw"
                        throw failure
                    } finally {
                        bitmap.close()
                    }
                }
            }
        }
        assertSame(failure, actual)
        assertEquals(
            listOf("check", "encode", "draw", "recycle-bitmap",
                "close-page", "close-document"),
            events
        )
    }

    @Test fun everyPageChecksBeforeItsOwnJpegEncoding() {
        val events = mutableListOf<String>()
        repeat(3) { page ->
            events += "render-$page"
            val encoded = Any()
            val actual = openCompressionResourceWithPreflight(
                checkCancelled = { events += "check-$page" },
                open = { events += "encode-$page"; encoded }
            )
            assertSame(encoded, actual)
        }
        assertEquals(
            (0..2).flatMap { listOf("render-$it", "check-$it", "encode-$it") },
            events
        )
    }

    @Test fun jpegQualityAndRasterDimensionsRemainUnchanged() {
        val level = PdfCompressionLevel.MEDIUM
        val budget = pdfCompressionBitmapBudgetBytes(64L * 1024 * 1024)
        val dimensions = boundedPdfRasterSize(612, 792, level.renderDpi, budget)
        val quality = level.jpegQuality / 100f
        var checked = false
        val result = openCompressionResourceWithPreflight(
            checkCancelled = { checked = true },
            open = { Triple(dimensions.width, dimensions.height, quality) }
        )
        assertTrue(checked)
        assertEquals(dimensions.width, result.first)
        assertEquals(dimensions.height, result.second)
        assertEquals(0.70f, result.third, 0.0001f)
        assertTrue(dimensions.width.toLong() * dimensions.height.toLong() * 4L <= budget)
    }

    @Test fun realCoroutineCancellationAfterRenderSkipsEncoderAndRecycles() = runBlocking {
        var encoded = false
        var recycled = false
        var observed = false
        val worker = launch(start = CoroutineStart.UNDISPATCHED) {
            val checkCancelled = { coroutineContext.ensureActive() }
            val bitmap = Closeable { recycled = true }
            try {
                coroutineContext.cancel()
                openCompressionResourceWithPreflight(checkCancelled) {
                    encoded = true
                    Any()
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
        assertFalse(encoded)
    }
}
