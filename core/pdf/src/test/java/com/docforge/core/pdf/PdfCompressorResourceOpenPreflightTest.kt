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
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class PdfCompressorResourceOpenPreflightTest {
    @Test fun cancelledPreflightSkipsDescriptorOpenAndPreservesException() {
        val original = CancellationException("cancel before descriptor")
        var opened = false
        try {
            openCompressionResourceWithPreflight(
                checkCancelled = { throw original },
                open = { opened = true; "descriptor" }
            )
            fail("Expected cancellation")
        } catch (actual: CancellationException) {
            assertSame(original, actual)
        }
        assertFalse(opened)
    }

    @Test fun successfulPreflightPrecedesResourceOpen() {
        val events = mutableListOf<String>()
        val token = Any()
        val result = openCompressionResourceWithPreflight(
            checkCancelled = { events.add("preflight") },
            open = { events.add("open"); token }
        )
        assertSame(token, result)
        assertEquals(listOf("preflight", "open"), events)
    }

    @Test fun resourceOpenFailurePreservesOriginalException() {
        val original = IOException("descriptor open failed")
        try {
            openCompressionResourceWithPreflight(
                checkCancelled = {},
                open = { throw original }
            )
            fail("Expected resource open failure")
        } catch (actual: IOException) {
            assertSame(original, actual)
        }
    }

    @Test fun secondPreflightCancellationClosesFirstResourceAndSkipsRenderer() {
        val events = mutableListOf<String>()
        val original = CancellationException("cancel before renderer")
        var checks = 0
        val preflight = {
            checks++
            events.add("check-$checks")
            if (checks == 2) throw original
        }
        try {
            openCompressionResourceWithPreflight(preflight) {
                events.add("descriptor")
                Closeable { events.add("close-descriptor") }
            }.use {
                openCompressionResourceWithPreflight(preflight) {
                    events.add("renderer")
                    Closeable { events.add("close-renderer") }
                }.use {}
            }
            fail("Expected cancellation before renderer")
        } catch (actual: CancellationException) {
            assertSame(original, actual)
        }
        assertEquals(listOf("check-1", "descriptor", "check-2", "close-descriptor"), events)
    }

    @Test fun rendererOpenFailureClosesDescriptorAndPreservesException() {
        val events = mutableListOf<String>()
        val original = IOException("renderer failed")
        try {
            openCompressionResourceWithPreflight({ events.add("check-descriptor") }) {
                events.add("descriptor")
                Closeable { events.add("close-descriptor") }
            }.use {
                openCompressionResourceWithPreflight({ events.add("check-renderer") }) {
                    events.add("renderer")
                    throw original
                }
            }
            fail("Expected renderer failure")
        } catch (actual: IOException) {
            assertSame(original, actual)
        }
        assertEquals(listOf("check-descriptor", "descriptor", "check-renderer", "renderer", "close-descriptor"), events)
    }

    @Test fun realCoroutineCancellationBetweenOpensSkipsRenderer() = runBlocking {
        var rendererOpened = false
        var descriptorClosed = false
        var observed = false
        val worker = launch(start = CoroutineStart.UNDISPATCHED) {
            val checkCancelled = { coroutineContext.ensureActive() }
            try {
                openCompressionResourceWithPreflight(checkCancelled) {
                    Closeable { descriptorClosed = true }
                }.use {
                    coroutineContext.cancel()
                    openCompressionResourceWithPreflight(checkCancelled) {
                        rendererOpened = true
                        Closeable {}
                    }.use {}
                }
                fail("Expected cancellation")
            } catch (_: CancellationException) {
                observed = true
            }
        }
        worker.join()
        assertTrue(observed)
        assertTrue(descriptorClosed)
        assertFalse(rendererOpened)
    }

    @Test fun successfulNestedResourceOpensCloseInReverseOrder() {
        val events = mutableListOf<String>()
        openCompressionResourceWithPreflight({ events.add("check-descriptor") }) {
            events.add("descriptor")
            Closeable { events.add("close-descriptor") }
        }.use {
            openCompressionResourceWithPreflight({ events.add("check-renderer") }) {
                events.add("renderer")
                Closeable { events.add("close-renderer") }
            }.use { events.add("process") }
        }
        assertEquals(
            listOf("check-descriptor", "descriptor", "check-renderer", "renderer", "process", "close-renderer", "close-descriptor"),
            events
        )
    }
}
