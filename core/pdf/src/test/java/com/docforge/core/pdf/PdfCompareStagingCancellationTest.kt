package com.docforge.core.pdf

import java.io.Closeable
import java.io.File
import java.util.concurrent.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test

class PdfCompareStagingCancellationTest {
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

    @Test fun cancellationPreventsStagingSetup() {
        val events = mutableListOf<String>()
        val cancelled = CancellationException("cancelled before staging")
        val thrown = assertThrows(CancellationException::class.java) {
            checkCompareStagingPreflight { events += "check"; throw cancelled }
            events += "stage"
        }
        assertSame(cancelled, thrown)
        assertEquals(listOf("check"), events)
    }

    @Test fun successfulPreflightRunsBeforeStaging() {
        val events = mutableListOf<String>()
        checkCompareStagingPreflight { events += "check" }
        events += "stage"
        assertEquals(listOf("check", "stage"), events)
    }

    @Test fun nonCancellationPreflightFailurePreservesIdentity() {
        val failure = IllegalStateException("preflight")
        var staged = false
        val thrown = assertThrows(IllegalStateException::class.java) {
            checkCompareStagingPreflight { throw failure }
            staged = true
        }
        assertSame(failure, thrown)
        assertEquals(false, staged)
    }

    @Test fun successfulPreflightDoesNotMaskStagingFailure() {
        val failure = IllegalArgumentException("stage")
        val thrown = assertThrows(IllegalArgumentException::class.java) {
            checkCompareStagingPreflight {}
            throw failure
        }
        assertSame(failure, thrown)
    }

    @Test fun cancellationAfterOutputDirectoryResolutionSkipsStaging() {
        val events = mutableListOf<String>()
        val cancelled = CancellationException("cancelled after directory")
        val dir = resolveCompareOutputDirectoryWithPreflight(
            { events += "resolve-check" },
            { events += "resolve"; File("configured") }
        )
        assertEquals(File("configured"), dir)
        val thrown = assertThrows(CancellationException::class.java) {
            checkCompareStagingPreflight { events += "staging-check"; throw cancelled }
            events += "stage"
        }
        assertSame(cancelled, thrown)
        assertEquals(listOf("resolve-check", "resolve", "staging-check"), events)
    }

    @Test fun cancellationBeforeStagingClosesBothRenderers() {
        val events = mutableListOf<String>()
        val left = Renderer("left", events)
        val right = Renderer("right", events)
        val cancelled = CancellationException("before staging")
        val thrown = assertThrows(CancellationException::class.java) {
            withOpenedCloseables(
                openFirst = { events += "open:left"; left },
                openSecond = { events += "open:right"; right }
            ) { _: Renderer, _: Renderer ->
                checkCompareStagingPreflight { events += "check"; throw cancelled }
                events += "stage"
            }
        }
        assertSame(cancelled, thrown)
        assertEquals(1, left.closes)
        assertEquals(1, right.closes)
        assertEquals(listOf("open:left", "open:right", "check", "close:right", "close:left"), events)
    }

    @Test fun cancellationKeepsCleanupFailuresSuppressed() {
        val events = mutableListOf<String>()
        val leftClose = IllegalStateException("left close")
        val rightClose = IllegalStateException("right close")
        val left = Renderer("left", events, leftClose)
        val right = Renderer("right", events, rightClose)
        val cancelled = CancellationException("before staging")
        val thrown = assertThrows(CancellationException::class.java) {
            withOpenedCloseables(
                openFirst = { left }, openSecond = { right }
            ) { _: Renderer, _: Renderer ->
                checkCompareStagingPreflight { throw cancelled }
            }
        }
        assertSame(cancelled, thrown)
        assertEquals(2, thrown.suppressed.size)
        assertSame(rightClose, thrown.suppressed[0])
        assertSame(leftClose, thrown.suppressed[1])
        assertEquals(1, left.closes)
        assertEquals(1, right.closes)
    }

    @Test fun repeatedStagingRequestsCheckEachTime() {
        val events = mutableListOf<String>()
        repeat(4) { index ->
            checkCompareStagingPreflight { events += "check:$index" }
            events += "stage:$index"
        }
        assertEquals((0 until 4).flatMap { listOf("check:$it", "stage:$it") }, events)
    }
}
