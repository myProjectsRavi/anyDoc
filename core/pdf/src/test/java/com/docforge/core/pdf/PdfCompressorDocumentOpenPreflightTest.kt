package com.docforge.core.pdf

import java.io.Closeable
import java.io.IOException
import java.util.concurrent.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class PdfCompressorDocumentOpenPreflightTest {
    @Test fun cancellationAfterPageCountSkipsDocumentAndClosesInputs() {
        val events = mutableListOf<String>()
        val cancelled = CancellationException("after page count")
        val actual = assertThrows(CancellationException::class.java) {
            Closeable { events += "close-descriptor" }.use {
                Closeable { events += "close-renderer" }.use {
                    events += "page-count"
                    require(1 > 0)
                    openCompressionResourceWithPreflight(
                        checkCancelled = { events += "check"; throw cancelled },
                        open = { events += "construct"; Closeable {} }
                    ).use {}
                }
            }
        }
        assertSame(cancelled, actual)
        assertEquals(
            listOf("page-count", "check", "close-renderer", "close-descriptor"),
            events
        )
    }

    @Test fun emptyPageValidationPrecedesPreflightAndDocumentConstruction() {
        val events = mutableListOf<String>()
        val actual = assertThrows(IllegalArgumentException::class.java) {
            Closeable { events += "close-descriptor" }.use {
                Closeable { events += "close-renderer" }.use {
                    val pageCount = 0
                    require(pageCount > 0) { "Input PDF has no pages." }
                    openCompressionResourceWithPreflight(
                        checkCancelled = { events += "check" },
                        open = { events += "construct"; Closeable {} }
                    ).use {}
                }
            }
        }
        assertEquals("Input PDF has no pages.", actual.message)
        assertEquals(listOf("close-renderer", "close-descriptor"), events)
    }

    @Test fun successfulConstructionPreservesIdentityAndReverseCleanupOrder() {
        val events = mutableListOf<String>()
        val document = Closeable { events += "close-document" }
        Closeable { events += "close-descriptor" }.use {
            Closeable { events += "close-renderer" }.use {
                events += "page-count"
                val actual = openCompressionResourceWithPreflight(
                    checkCancelled = { events += "check" },
                    open = { events += "construct"; document }
                )
                assertSame(document, actual)
                actual.use { events += "process" }
            }
        }
        assertEquals(
            listOf("page-count", "check", "construct", "process",
                "close-document", "close-renderer", "close-descriptor"),
            events
        )
    }

    @Test fun documentConstructorFailurePreservesIdentityAndClosesInputs() {
        val events = mutableListOf<String>()
        val failure = IOException("document construction failed")
        val actual = assertThrows(IOException::class.java) {
            Closeable { events += "close-descriptor" }.use {
                Closeable { events += "close-renderer" }.use {
                    openCompressionResourceWithPreflight(
                        checkCancelled = { events += "check" },
                        open = { events += "construct"; throw failure }
                    )
                }
            }
        }
        assertSame(failure, actual)
        assertEquals(
            listOf("check", "construct", "close-renderer", "close-descriptor"),
            events
        )
    }

    @Test fun preflightFailurePreservesOriginalCancellationIdentity() {
        val original = CancellationException("cancelled before allocation")
        var allocated = false
        val actual = assertThrows(CancellationException::class.java) {
            openCompressionResourceWithPreflight(
                checkCancelled = { throw original },
                open = { allocated = true; Closeable {} }
            )
        }
        assertSame(original, actual)
        assertFalse(allocated)
    }

    @Test fun processingFailurePreservesIdentityAndClosesAllResources() {
        val events = mutableListOf<String>()
        val original = IllegalStateException("page processing failed")
        val actual = assertThrows(IllegalStateException::class.java) {
            Closeable { events += "close-descriptor" }.use {
                Closeable { events += "close-renderer" }.use {
                    openCompressionResourceWithPreflight(
                        checkCancelled = { events += "check" },
                        open = { events += "construct"; Closeable { events += "close-document" } }
                    ).use {
                        events += "process"
                        throw original
                    }
                }
            }
        }
        assertSame(original, actual)
        assertEquals(
            listOf("check", "construct", "process",
                "close-document", "close-renderer", "close-descriptor"),
            events
        )
    }

    @Test fun repeatedSuccessChecksBeforeEachAllocation() {
        val events = mutableListOf<String>()
        repeat(3) { index ->
            val token = Any()
            val result = openCompressionResourceWithPreflight(
                checkCancelled = { events += "check-$index" },
                open = { events += "construct-$index"; token }
            )
            assertSame(token, result)
        }
        assertEquals(
            (0..2).flatMap { listOf("check-$it", "construct-$it") },
            events
        )
        assertTrue(events.isNotEmpty())
    }
}
