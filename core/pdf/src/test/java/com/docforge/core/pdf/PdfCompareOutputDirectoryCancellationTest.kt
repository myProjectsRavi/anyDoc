package com.docforge.core.pdf

import java.io.Closeable
import java.io.File
import java.util.concurrent.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test

class PdfCompareOutputDirectoryCancellationTest {
    private class Renderer(
        private val name: String,
        private val events: MutableList<String>,
        private val closeFailure: Throwable? = null
    ) : Closeable {
        var closes = 0
        override fun close() {
            closes++
            events += "close:$name"
            closeFailure?.let { throw it }
        }
    }

    @Test fun cancellationSkipsOutputDirectoryResolution() {
        val events = mutableListOf<String>()
        val cancelled = CancellationException("cancel")
        val thrown = assertThrows(CancellationException::class.java) {
            resolveCompareOutputDirectoryWithPreflight(
                { events += "check"; throw cancelled },
                { events += "resolve"; File("unused") }
            )
        }
        assertSame(cancelled, thrown)
        assertEquals(listOf("check"), events)
    }

    @Test fun normalPathChecksFirstAndPreservesDirectory() {
        val events = mutableListOf<String>()
        val directory = File("configured")
        val result = resolveCompareOutputDirectoryWithPreflight(
            { events += "check" }, { events += "resolve"; directory }
        )
        assertSame(directory, result)
        assertEquals(listOf("check", "resolve"), events)
    }

    @Test fun nonCancellationPreflightFailurePreservesIdentity() {
        val failure = IllegalStateException("preflight")
        var called = false
        val thrown = assertThrows(IllegalStateException::class.java) {
            resolveCompareOutputDirectoryWithPreflight(
                { throw failure }, { called = true; File("unused") }
            )
        }
        assertSame(failure, thrown)
        assertEquals(false, called)
    }

    @Test fun resolverFailurePreservesIdentity() {
        val failure = IllegalArgumentException("resolver")
        val thrown = assertThrows(IllegalArgumentException::class.java) {
            resolveCompareOutputDirectoryWithPreflight(
                {}, { throw failure }
            )
        }
        assertSame(failure, thrown)
    }

    @Test fun cancellationAfterAcquiringRenderersClosesBothWithoutResolution() {
        val events = mutableListOf<String>()
        val left = Renderer("left", events)
        val right = Renderer("right", events)
        val cancelled = CancellationException("after renderers")
        val thrown = assertThrows(CancellationException::class.java) {
            withOpenedCloseables(
                openFirst = { events += "open:left"; left },
                openSecond = { events += "open:right"; right }
            ) { _: Renderer, _: Renderer ->
                resolveCompareOutputDirectoryWithPreflight(
                    { events += "check"; throw cancelled },
                    { events += "resolve"; File("unused") }
                )
            }
        }
        assertSame(cancelled, thrown)
        assertEquals(1, left.closes)
        assertEquals(1, right.closes)
        assertEquals(listOf("open:left", "open:right", "check", "close:right", "close:left"), events)
    }

    @Test fun cancellationWithBothCloseFailuresSuppressesBoth() {
        val events = mutableListOf<String>()
        val leftClose = IllegalStateException("left close")
        val rightClose = IllegalStateException("right close")
        val left = Renderer("left", events, leftClose)
        val right = Renderer("right", events, rightClose)
        val cancelled = CancellationException("cancel")
        val thrown = assertThrows(CancellationException::class.java) {
            withOpenedCloseables(
                openFirst = { left }, openSecond = { right }
            ) { _: Renderer, _: Renderer ->
                resolveCompareOutputDirectoryWithPreflight(
                    { throw cancelled }, { File("unused") }
                )
            }
        }
        assertSame(cancelled, thrown)
        assertEquals(2, thrown.suppressed.size)
        assertSame(rightClose, thrown.suppressed[0])
        assertSame(leftClose, thrown.suppressed[1])
        assertEquals(1, left.closes)
        assertEquals(1, right.closes)
    }

    @Test fun repeatedInvocationsCheckBeforeEachResolution() {
        val events = mutableListOf<String>()
        repeat(4) { index ->
            val result = resolveCompareOutputDirectoryWithPreflight(
                { events += "check:$index" },
                { events += "resolve:$index"; index }
            )
            assertEquals(index, result)
        }
        assertEquals((0 until 4).flatMap { listOf("check:$it", "resolve:$it") }, events)
    }
}
