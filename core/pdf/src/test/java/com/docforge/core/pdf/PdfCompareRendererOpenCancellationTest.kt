package com.docforge.core.pdf

import java.io.Closeable
import java.util.concurrent.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test

class PdfCompareRendererOpenCancellationTest {
    private class FakeRenderer(
        private val name: String,
        private val events: MutableList<String>,
        private val closeFailure: Throwable? = null
    ) : Closeable {
        override fun close() {
            events += "close:$name"
            closeFailure?.let { throw it }
        }
    }

    @Test fun cancelledBeforeFirstRendererDoesNotOpen() {
        val cancelled = CancellationException("before left renderer")
        var opens = 0
        val thrown = assertThrows(CancellationException::class.java) {
            openCompareRendererWithPreflight(
                checkCancelled = { throw cancelled },
                openRenderer = { opens++; Any() }
            )
        }
        assertSame(cancelled, thrown)
        assertEquals(0, opens)
    }

    @Test fun successfulRendererOpenChecksCancellationFirst() {
        val events = mutableListOf<String>()
        val renderer = FakeRenderer("left", events)
        val returned = openCompareRendererWithPreflight(
            checkCancelled = { events += "check" },
            openRenderer = { events += "open"; renderer }
        )
        assertSame(renderer, returned)
        assertEquals(listOf("check", "open"), events)
    }

    @Test fun preflightFailurePreservesOriginalException() {
        val failure = IllegalStateException("preflight failed")
        var opens = 0
        val thrown = assertThrows(IllegalStateException::class.java) {
            openCompareRendererWithPreflight(
                checkCancelled = { throw failure },
                openRenderer = { opens++; Any() }
            )
        }
        assertSame(failure, thrown)
        assertEquals(0, opens)
    }

    @Test fun rendererOpenFailurePreservesOriginalException() {
        val failure = IllegalArgumentException("open failed")
        val thrown = assertThrows(IllegalArgumentException::class.java) {
            openCompareRendererWithPreflight(
                checkCancelled = {},
                openRenderer = { throw failure }
            )
        }
        assertSame(failure, thrown)
    }

    @Test fun cancellationBeforeRightRendererClosesLeftWithoutOpeningRight() {
        val events = mutableListOf<String>()
        val cancelled = CancellationException("before right renderer")
        val left = FakeRenderer("left", events)
        var rightOpens = 0
        val thrown = assertThrows(CancellationException::class.java) {
            withOpenedCloseables(
                openFirst = {
                    openCompareRendererWithPreflight(
                        checkCancelled = { events += "check:left" },
                        openRenderer = { events += "open:left"; left }
                    )
                },
                openSecond = {
                    openCompareRendererWithPreflight(
                        checkCancelled = { events += "check:right"; throw cancelled },
                        openRenderer = { rightOpens++; FakeRenderer("right", events) }
                    )
                }
            ) { _, _ -> events += "compare" }
        }
        assertSame(cancelled, thrown)
        assertEquals(0, rightOpens)
        assertEquals(listOf("check:left", "open:left", "check:right", "close:left"), events)
    }

    @Test fun rightRendererOpenFailureClosesLeftAndPreservesPrimary() {
        val events = mutableListOf<String>()
        val failure = IllegalStateException("right open")
        val left = FakeRenderer("left", events)
        val thrown = assertThrows(IllegalStateException::class.java) {
            withOpenedCloseables(
                openFirst = { openCompareRendererWithPreflight({}, { left }) },
                openSecond = { openCompareRendererWithPreflight({}, { throw failure }) }
            ) { _, _ -> events += "compare" }
        }
        assertSame(failure, thrown)
        assertEquals(listOf("close:left"), events)
    }

    @Test fun normalTwoRendererScopeClosesBothInReverseOrder() {
        val events = mutableListOf<String>()
        val result = withOpenedCloseables(
            openFirst = {
                openCompareRendererWithPreflight(
                    checkCancelled = { events += "check:left" },
                    openRenderer = { events += "open:left"; FakeRenderer("left", events) }
                )
            },
            openSecond = {
                openCompareRendererWithPreflight(
                    checkCancelled = { events += "check:right" },
                    openRenderer = { events += "open:right"; FakeRenderer("right", events) }
                )
            }
        ) { _, _ ->
            events += "compare"
            42
        }
        assertEquals(42, result)
        assertEquals(
            listOf("check:left", "open:left", "check:right", "open:right", "compare", "close:right", "close:left"),
            events
        )
    }

    @Test fun rightOpenFailureSuppressesLeftCloseFailure() {
        val events = mutableListOf<String>()
        val failure = IllegalStateException("right open")
        val cleanup = IllegalArgumentException("left close")
        val left = FakeRenderer("left", events, cleanup)
        val thrown = assertThrows(IllegalStateException::class.java) {
            withOpenedCloseables(
                openFirst = { openCompareRendererWithPreflight({}, { left }) },
                openSecond = { openCompareRendererWithPreflight({}, { throw failure }) }
            ) { _, _ -> Unit }
        }
        assertSame(failure, thrown)
        assertSame(cleanup, thrown.suppressed.single())
        assertEquals(listOf("close:left"), events)
    }
}
