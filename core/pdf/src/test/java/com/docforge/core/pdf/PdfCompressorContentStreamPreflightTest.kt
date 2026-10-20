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

class PdfCompressorContentStreamPreflightTest {
    @Test fun cancellationAfterJpegEncodingSkipsStreamOpenAndClosesResources() {
        val events = mutableListOf<String>()
        val cancelled = CancellationException("cancel during JPEG encoding")
        val actual = assertThrows(CancellationException::class.java) {
            Closeable { events += "close-descriptor" }.use {
                Closeable { events += "close-renderer" }.use {
                    Closeable { events += "close-document" }.use {
                        Closeable { events += "close-page" }.use {
                            val bitmap = Closeable { events += "recycle-bitmap" }
                            try {
                                events += "encode-jpeg"
                                openCompressionResourceWithPreflight(
                                    checkCancelled = { events += "check"; throw cancelled },
                                    open = { events += "open-stream"; Closeable {} }
                                ).use { events += "draw" }
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
            listOf("encode-jpeg", "check", "recycle-bitmap", "close-page",
                "close-document", "close-renderer", "close-descriptor"), events
        )
    }

    @Test fun successfulStreamCreationFollowsEncodingAndPreflight() {
        val events = mutableListOf<String>()
        val stream = Closeable { events += "close-stream" }
        events += "encode-jpeg"
        val actual = openCompressionResourceWithPreflight(
            checkCancelled = { events += "check" },
            open = { events += "open-stream"; stream }
        )
        assertSame(stream, actual)
        actual.use { events += "draw" }
        assertEquals(
            listOf("encode-jpeg", "check", "open-stream", "draw", "close-stream"),
            events
        )
    }

    @Test fun streamConstructorFailurePreservesOriginalExceptionAndBitmapCleanup() {
        val events = mutableListOf<String>()
        val failure = IOException("stream constructor failed")
        val actual = assertThrows(IOException::class.java) {
            Closeable { events += "close-document" }.use {
                Closeable { events += "close-page" }.use {
                    val bitmap = Closeable { events += "recycle-bitmap" }
                    try {
                        events += "encode-jpeg"
                        openCompressionResourceWithPreflight(
                            checkCancelled = { events += "check" },
                            open = { events += "open-stream"; throw failure }
                        )
                    } finally {
                        bitmap.close()
                    }
                }
            }
        }
        assertSame(failure, actual)
        assertEquals(
            listOf("encode-jpeg", "check", "open-stream", "recycle-bitmap",
                "close-page", "close-document"), events
        )
    }

    @Test fun drawFailureStillClosesStreamAndRecyclesBitmap() {
        val events = mutableListOf<String>()
        val failure = IllegalStateException("draw failed")
        val actual = assertThrows(IllegalStateException::class.java) {
            Closeable { events += "close-page" }.use {
                val bitmap = Closeable { events += "recycle-bitmap" }
                try {
                    openCompressionResourceWithPreflight(
                        checkCancelled = { events += "check" },
                        open = { events += "open-stream"; Closeable { events += "close-stream" } }
                    ).use { events += "draw"; throw failure }
                } finally {
                    bitmap.close()
                }
            }
        }
        assertSame(failure, actual)
        assertEquals(
            listOf("check", "open-stream", "draw", "close-stream",
                "recycle-bitmap", "close-page"), events
        )
    }

    @Test fun interruptedPreflightPreservesIdentityWithoutOpeningStream() {
        val interrupted = InterruptedException("interrupted after encoding")
        var opened = false
        val actual = assertThrows(InterruptedException::class.java) {
            openCompressionResourceWithPreflight(
                checkCancelled = { throw interrupted },
                open = { opened = true; Closeable {} }
            )
        }
        assertSame(interrupted, actual)
        assertFalse(opened)
    }

    @Test fun cancellationExceptionIdentitySurvivesNestedCleanup() {
        val cancelled = CancellationException("cancel after JPEG")
        var opened = false
        val actual = assertThrows(CancellationException::class.java) {
            Closeable {}.use {
                openCompressionResourceWithPreflight(
                    checkCancelled = { throw cancelled },
                    open = { opened = true; Closeable {} }
                ).use { }
            }
        }
        assertSame(cancelled, actual)
        assertFalse(opened)
    }

    @Test fun everyPageChecksBeforeOpeningItsContentStream() {
        val events = mutableListOf<String>()
        repeat(3) { page ->
            events += "encode-$page"
            openCompressionResourceWithPreflight(
                checkCancelled = { events += "check-$page" },
                open = { events += "open-$page"; Closeable { events += "close-$page" } }
            ).use { events += "draw-$page" }
        }
        assertEquals(
            (0..2).flatMap { page ->
                listOf("encode-$page", "check-$page", "open-$page", "draw-$page", "close-$page")
            },
            events
        )
    }

    @Test fun actualCoroutineCancellationAfterEncodingSkipsStreamOpen() = runBlocking {
        var opened = false
        var recycled = false
        var observed = false
        val worker = launch(start = CoroutineStart.UNDISPATCHED) {
            val checkCancelled = { coroutineContext.ensureActive() }
            val bitmap = Closeable { recycled = true }
            try {
                coroutineContext.cancel()
                openCompressionResourceWithPreflight(checkCancelled) {
                    opened = true
                    Closeable {}
                }.use { }
            } catch (_: CancellationException) {
                observed = true
            } finally {
                bitmap.close()
            }
        }
        worker.join()
        assertTrue(observed)
        assertTrue(recycled)
        assertFalse(opened)
    }
}
