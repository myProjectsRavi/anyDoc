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

class PdfCompressorOutputPagePreflightTest {
    @Test fun cancelledAfterRenderSkipsPageCreationAndReleasesOwners() {
        val events = mutableListOf<String>()
        val cancelled = CancellationException("cancelled while rendering")
        val thrown = assertThrows(CancellationException::class.java) {
            Closeable { events += "close-descriptor" }.use {
                Closeable { events += "close-renderer" }.use {
                    Closeable { events += "close-document" }.use {
                        Closeable { events += "close-source-page" }.use {
                            val bitmap = Closeable { events += "recycle-bitmap" }
                            try {
                                events += "render"
                                openCompressionResourceWithPreflight(
                                    checkCancelled = { events += "check"; throw cancelled },
                                    open = { events += "create-output-page"; events += "add-output-page" }
                                )
                            } finally {
                                bitmap.close()
                            }
                        }
                    }
                }
            }
        }
        assertSame(cancelled, thrown)
        assertEquals(listOf("render", "check", "recycle-bitmap", "close-source-page",
            "close-document", "close-renderer", "close-descriptor"), events)
    }

    @Test fun successfulRenderChecksBeforePageAllocationAndJpegEncoding() {
        val events = mutableListOf<String>()
        events += "render"
        val page = openCompressionResourceWithPreflight(
            checkCancelled = { events += "check" },
            open = {
                events += "allocate-page"
                "output-page".also { events += "add-page" }
            }
        )
        events += "encode-jpeg"
        assertEquals("output-page", page)
        assertEquals(listOf("render", "check", "allocate-page", "add-page", "encode-jpeg"), events)
    }

    @Test fun pageConstructionFailureKeepsOriginalExceptionAndClosesBitmap() {
        val failure = IOException("construct page")
        val events = mutableListOf<String>()
        val actual = assertThrows(IOException::class.java) {
            Closeable { events += "close-document" }.use {
                val bitmap = Closeable { events += "recycle-bitmap" }
                try {
                    openCompressionResourceWithPreflight(
                        checkCancelled = { events += "check" },
                        open = { events += "allocate-page"; throw failure }
                    )
                } finally {
                    bitmap.close()
                }
            }
        }
        assertSame(failure, actual)
        assertEquals(listOf("check", "allocate-page", "recycle-bitmap", "close-document"), events)
    }

    @Test fun addPageFailureKeepsOriginalExceptionAndClosesOwners() {
        val failure = IOException("append page")
        val events = mutableListOf<String>()
        val actual = assertThrows(IOException::class.java) {
            Closeable { events += "close-document" }.use {
                Closeable { events += "close-source-page" }.use {
                    val bitmap = Closeable { events += "recycle-bitmap" }
                    try {
                        openCompressionResourceWithPreflight(
                            checkCancelled = { events += "check" },
                            open = {
                                events += "allocate-page"
                                events += "add-page"
                                throw failure
                            }
                        )
                    } finally {
                        bitmap.close()
                    }
                }
            }
        }
        assertSame(failure, actual)
        assertEquals(listOf("check", "allocate-page", "add-page", "recycle-bitmap",
            "close-source-page", "close-document"), events)
    }

    @Test fun cancellationRetainsSuppressedResourceCleanupFailure() {
        val cancelled = CancellationException("cancel after render")
        val cleanup = IOException("close failed")
        var allocated = false
        val actual = assertThrows(CancellationException::class.java) {
            Closeable { throw cleanup }.use {
                openCompressionResourceWithPreflight(
                    checkCancelled = { throw cancelled },
                    open = { allocated = true }
                )
            }
        }
        assertSame(cancelled, actual)
        assertFalse(allocated)
        assertEquals(1, actual.suppressed.size)
        assertSame(cleanup, actual.suppressed.single())
    }

    @Test fun eachRenderedPageHasIndependentPreflight() {
        val events = mutableListOf<String>()
        repeat(3) { i ->
            events += "render-$i"
            openCompressionResourceWithPreflight(
                checkCancelled = { events += "check-$i" },
                open = { events += "allocate-$i"; events += "append-$i" }
            )
        }
        assertEquals((0..2).flatMap { listOf("render-$it", "check-$it", "allocate-$it",
            "append-$it") }, events)
    }

    @Test fun actualCoroutineCancellationAfterRenderSkipsPageMutation() = runBlocking {
        var allocated = false
        var appended = false
        var recycled = false
        var observed = false
        val worker = launch(start = CoroutineStart.UNDISPATCHED) {
            val checkCancelled = { coroutineContext.ensureActive() }
            val bitmap = Closeable { recycled = true }
            try {
                coroutineContext.cancel()
                openCompressionResourceWithPreflight(checkCancelled) {
                    allocated = true
                    appended = true
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
        assertFalse(allocated)
        assertFalse(appended)
    }

    @Test fun pageDimensionsAndJpegQualityRemainUnchangedOnSuccess() {
        val level = PdfCompressionLevel.MEDIUM
        var checks = 0
        val originalWidth = 612f
        val originalHeight = 792f
        val dimensions = openCompressionResourceWithPreflight(
            checkCancelled = { checks++ },
            open = { Pair(originalWidth, originalHeight) }
        )
        assertEquals(1, checks)
        assertEquals(originalWidth, dimensions.first, 0f)
        assertEquals(originalHeight, dimensions.second, 0f)
        assertEquals(0.70f, level.jpegQuality / 100f, 0.0001f)
    }
}