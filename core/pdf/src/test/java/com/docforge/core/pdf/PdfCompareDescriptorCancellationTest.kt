package com.docforge.core.pdf

import java.io.Closeable
import java.util.concurrent.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test

class PdfCompareDescriptorCancellationTest {
    private class Descriptor(
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

    @Test fun cancelledBeforeFirstOpenNeverAcquiresEitherDescriptor() {
        val events = mutableListOf<String>()
        val cancelled = CancellationException("before first")
        val thrown = assertThrows(CancellationException::class.java) {
            withOpenedCloseables(
                openFirst = { openCompareDescriptorWithPreflight(
                    { events += "check:left"; throw cancelled },
                    { events += "open:left"; Descriptor("left", events) }
                ) },
                openSecond = { openCompareDescriptorWithPreflight(
                    { events += "check:right" },
                    { events += "open:right"; Descriptor("right", events) }
                ) },
                block = { _: Descriptor, _: Descriptor -> Unit }
            )
        }
        assertSame(cancelled, thrown)
        assertEquals(listOf("check:left"), events)
    }

    @Test fun cancellationBetweenOpensClosesFirstWithoutOpeningSecond() {
        val events = mutableListOf<String>()
        val first = Descriptor("left", events)
        val cancelled = CancellationException("before right")
        val thrown = assertThrows(CancellationException::class.java) {
            withOpenedCloseables(
                openFirst = { openCompareDescriptorWithPreflight(
                    { events += "check:left" },
                    { events += "open:left"; first }
                ) },
                openSecond = { openCompareDescriptorWithPreflight(
                    { events += "check:right"; throw cancelled },
                    { events += "open:right"; Descriptor("right", events) }
                ) },
                block = { _: Descriptor, _: Descriptor -> Unit }
            )
        }
        assertSame(cancelled, thrown)
        assertEquals(1, first.closes)
        assertEquals(listOf("check:left", "open:left", "check:right", "close:left"), events)
    }

    @Test fun normalPathChecksBeforeEachOpenAndClosesInReverseOrder() {
        val events = mutableListOf<String>()
        val left = Descriptor("left", events)
        val right = Descriptor("right", events)
        val answer = withOpenedCloseables(
            openFirst = { openCompareDescriptorWithPreflight(
                { events += "check:left" }, { events += "open:left"; left }
            ) },
            openSecond = { openCompareDescriptorWithPreflight(
                { events += "check:right" }, { events += "open:right"; right }
            ) }
        ) { a, b ->
            assertSame(left, a)
            assertSame(right, b)
            events += "use"
            42
        }
        assertEquals(42, answer)
        assertEquals(1, left.closes)
        assertEquals(1, right.closes)
        assertEquals(
            listOf("check:left", "open:left", "check:right", "open:right", "use", "close:right", "close:left"),
            events
        )
    }

    @Test fun firstOpenFailureSkipsSecondPreflightAndPreservesThrowable() {
        val events = mutableListOf<String>()
        val failure = IllegalStateException("first open")
        val thrown = assertThrows(IllegalStateException::class.java) {
            withOpenedCloseables(
                openFirst = { openCompareDescriptorWithPreflight(
                    { events += "check:left" },
                    { events += "open:left"; throw failure }
                ) },
                openSecond = { openCompareDescriptorWithPreflight(
                    { events += "check:right" },
                    { events += "open:right"; Descriptor("right", events) }
                ) },
                block = { _: Descriptor, _: Descriptor -> Unit }
            )
        }
        assertSame(failure, thrown)
        assertEquals(listOf("check:left", "open:left"), events)
    }

    @Test fun secondOpenFailureClosesFirstAndPreservesThrowable() {
        val events = mutableListOf<String>()
        val first = Descriptor("left", events)
        val failure = IllegalStateException("second open")
        val thrown = assertThrows(IllegalStateException::class.java) {
            withOpenedCloseables(
                openFirst = { openCompareDescriptorWithPreflight(
                    { events += "check:left" }, { events += "open:left"; first }
                ) },
                openSecond = { openCompareDescriptorWithPreflight(
                    { events += "check:right" }, { events += "open:right"; throw failure }
                ) },
                block = { _: Descriptor, _: Descriptor -> Unit }
            )
        }
        assertSame(failure, thrown)
        assertEquals(1, first.closes)
        assertEquals(listOf("check:left", "open:left", "check:right", "open:right", "close:left"), events)
    }

    @Test fun cancellationAndFirstCloseFailurePreservePrimaryAndSuppressCleanup() {
        val events = mutableListOf<String>()
        val cleanup = IllegalStateException("left close")
        val first = Descriptor("left", events, cleanup)
        val cancelled = CancellationException("right preflight")
        val thrown = assertThrows(CancellationException::class.java) {
            withOpenedCloseables(
                openFirst = { openCompareDescriptorWithPreflight({}, { first }) },
                openSecond = { openCompareDescriptorWithPreflight(
                    { throw cancelled }, { Descriptor("right", events) }
                ) },
                block = { _: Descriptor, _: Descriptor -> Unit }
            )
        }
        assertSame(cancelled, thrown)
        assertSame(cleanup, thrown.suppressed.single())
        assertEquals(1, first.closes)
    }

    @Test fun bodyFailureAndBothCloseFailuresKeepPrimaryAndSuppressCleanup() {
        val events = mutableListOf<String>()
        val leftCleanup = IllegalStateException("left close")
        val rightCleanup = IllegalStateException("right close")
        val left = Descriptor("left", events, leftCleanup)
        val right = Descriptor("right", events, rightCleanup)
        val failure = IllegalArgumentException("compare")
        val thrown = assertThrows(IllegalArgumentException::class.java) {
            withOpenedCloseables(
                openFirst = { openCompareDescriptorWithPreflight({}, { left }) },
                openSecond = { openCompareDescriptorWithPreflight({}, { right }) }
            ) { _: Descriptor, _: Descriptor -> throw failure }
        }
        assertSame(failure, thrown)
        assertSame(rightCleanup, thrown.suppressed.single())
        assertSame(leftCleanup, rightCleanup.suppressed.single())
        assertEquals(1, left.closes)
        assertEquals(1, right.closes)
    }

    @Test fun everyInvocationChecksImmediatelyBeforeItsOwnOpen() {
        val events = mutableListOf<String>()
        repeat(3) { index ->
            val actual = openCompareDescriptorWithPreflight(
                checkCancelled = { events += "check:$index" },
                openDescriptor = { events += "open:$index"; index }
            )
            assertEquals(index, actual)
        }
        assertEquals(
            listOf("check:0", "open:0", "check:1", "open:1", "check:2", "open:2"),
            events
        )
    }
}
