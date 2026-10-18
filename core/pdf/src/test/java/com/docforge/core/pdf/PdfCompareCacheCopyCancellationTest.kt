package com.docforge.core.pdf

import java.io.Closeable
import java.util.concurrent.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test

class PdfCompareCacheCopyCancellationTest {
    private class Copy(
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

    private fun <T> startCopy(checkCancelled: () -> Unit, copy: () -> T): T {
        checkCompareCacheCopyPreflight(checkCancelled)
        return copy()
    }

    @Test fun cancellationBeforeLeftCopySkipsBothProviderOperations() {
        val events = mutableListOf<String>()
        val cancelled = CancellationException("before left")
        val thrown = assertThrows(CancellationException::class.java) {
            startCopy(
                { events += "check:left"; throw cancelled },
                { events += "copy:left"; Copy("left", events) }
            ).use { _: Copy ->
                startCopy(
                    { events += "check:right" },
                    { events += "copy:right"; Copy("right", events) }
                ).use { }
            }
        }
        assertSame(cancelled, thrown)
        assertEquals(listOf("check:left"), events)
    }

    @Test fun cancellationBetweenCopiesClosesLeftWithoutStartingRight() {
        val events = mutableListOf<String>()
        val left = Copy("left", events)
        val cancelled = CancellationException("before right")
        val thrown = assertThrows(CancellationException::class.java) {
            startCopy(
                { events += "check:left" },
                { events += "copy:left"; left }
            ).use {
                startCopy(
                    { events += "check:right"; throw cancelled },
                    { events += "copy:right"; Copy("right", events) }
                ).use { }
            }
        }
        assertSame(cancelled, thrown)
        assertEquals(1, left.closes)
        assertEquals(listOf("check:left", "copy:left", "check:right", "close:left"), events)
    }

    @Test fun successfulCopiesCheckBeforeEachOperationAndCloseInReverseOrder() {
        val events = mutableListOf<String>()
        val left = Copy("left", events)
        val right = Copy("right", events)
        val result = startCopy(
            { events += "check:left" },
            { events += "copy:left"; left }
        ).use { a ->
            startCopy(
                { events += "check:right" },
                { events += "copy:right"; right }
            ).use { b ->
                assertSame(left, a)
                assertSame(right, b)
                events += "compare"
                42
            }
        }
        assertEquals(42, result)
        assertEquals(1, left.closes)
        assertEquals(1, right.closes)
        assertEquals(
            listOf("check:left", "copy:left", "check:right", "copy:right",
                "compare", "close:right", "close:left"),
            events
        )
    }

    @Test fun nonCancellationPreflightFailureKeepsIdentityAndSkipsCopy() {
        val events = mutableListOf<String>()
        val failure = IllegalStateException("preflight")
        val thrown = assertThrows(IllegalStateException::class.java) {
            startCopy(
                { events += "check"; throw failure },
                { events += "copy"; Copy("left", events) }
            )
        }
        assertSame(failure, thrown)
        assertEquals(listOf("check"), events)
    }

    @Test fun firstCopyFailureSkipsRightAndPreservesIdentity() {
        val events = mutableListOf<String>()
        val failure = IllegalStateException("first copy")
        val thrown = assertThrows(IllegalStateException::class.java) {
            startCopy(
                { events += "check:left" },
                { events += "copy:left"; throw failure }
            ).use { _: Copy ->
                startCopy(
                    { events += "check:right" },
                    { events += "copy:right"; Copy("right", events) }
                )
            }
        }
        assertSame(failure, thrown)
        assertEquals(listOf("check:left", "copy:left"), events)
    }

    @Test fun secondCopyFailureClosesLeftAndPreservesIdentity() {
        val events = mutableListOf<String>()
        val left = Copy("left", events)
        val failure = IllegalStateException("second copy")
        val thrown = assertThrows(IllegalStateException::class.java) {
            startCopy(
                { events += "check:left" },
                { events += "copy:left"; left }
            ).use {
                startCopy(
                    { events += "check:right" },
                    { events += "copy:right"; throw failure }
                )
            }
        }
        assertSame(failure, thrown)
        assertEquals(1, left.closes)
        assertEquals(listOf("check:left", "copy:left", "check:right", "copy:right", "close:left"), events)
    }

    @Test fun cancellationAndLeftCleanupFailurePreserveCancellationAsPrimary() {
        val events = mutableListOf<String>()
        val cleanup = IllegalStateException("left close")
        val left = Copy("left", events, cleanup)
        val cancelled = CancellationException("right preflight")
        val thrown = assertThrows(CancellationException::class.java) {
            startCopy({}, { left }).use {
                startCopy({ throw cancelled }, { Copy("right", events) })
            }
        }
        assertSame(cancelled, thrown)
        assertSame(cleanup, thrown.suppressed.single())
        assertEquals(1, left.closes)
    }

    @Test fun bodyAndBothCloseFailuresKeepOriginalAndSuppressBoth() {
        val events = mutableListOf<String>()
        val leftCleanup = IllegalStateException("left close")
        val rightCleanup = IllegalStateException("right close")
        val failure = IllegalArgumentException("compare")
        val left = Copy("left", events, leftCleanup)
        val right = Copy("right", events, rightCleanup)
        val thrown = assertThrows(IllegalArgumentException::class.java) {
            startCopy({}, { left }).use {
                startCopy({}, { right }).use { throw failure }
            }
        }
        assertSame(failure, thrown)
        assertEquals(2, thrown.suppressed.size)
        assertSame(rightCleanup, thrown.suppressed[0])
        assertSame(leftCleanup, thrown.suppressed[1])
        assertEquals(1, left.closes)
        assertEquals(1, right.closes)
    }

    @Test fun repeatedPreflightsAlwaysRunBeforeTheirCopy() {
        val events = mutableListOf<String>()
        repeat(4) { index ->
            val result = startCopy(
                { events += "check:$index" },
                { events += "copy:$index"; index }
            )
            assertEquals(index, result)
        }
        assertEquals((0 until 4).flatMap { listOf("check:$it", "copy:$it") }, events)
    }
}
