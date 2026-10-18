package com.docforge.core.pdf

import java.io.Closeable
import java.io.IOException
import java.util.concurrent.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test

class PdfCompareSerializationPreflightTest {
    private class Stream(private val events: MutableList<String>, private val failClose: Throwable? = null) : Closeable {
        override fun close() {
            events += "close"
            failClose?.let { throw it }
        }
    }

    @Test fun cancelledBeforeOpenNeverOpensOrSerializes() {
        val events = mutableListOf<String>()
        val cancelled = CancellationException("before open")
        val actual = assertThrows(CancellationException::class.java) {
            writeComparePdfWithPreflight(
                checkCancelled = { events += "check"; throw cancelled },
                openStream = { events += "open"; Stream(events) },
                write = { _: Stream -> events += "write" }
            )
        }
        assertSame(cancelled, actual)
        assertEquals(listOf("check"), events)
    }

    @Test fun normalSerializationChecksTwiceAndClosesOnce() {
        val events = mutableListOf<String>()
        val result = writeComparePdfWithPreflight(
            checkCancelled = { events += "check" },
            openStream = { events += "open"; Stream(events) },
            write = { _: Stream -> events += "write"; 42 }
        )
        assertEquals(42, result)
        assertEquals(listOf("check", "open", "check", "write", "close"), events)
    }

    @Test fun cancelledAfterOpenClosesWithoutSerializing() {
        val events = mutableListOf<String>()
        val cancelled = CancellationException("before serialization")
        var checks = 0
        val actual = assertThrows(CancellationException::class.java) {
            writeComparePdfWithPreflight(
                checkCancelled = { events += "check"; if (++checks == 2) throw cancelled },
                openStream = { events += "open"; Stream(events) },
                write = { _: Stream -> events += "write" }
            )
        }
        assertSame(cancelled, actual)
        assertEquals(listOf("check", "open", "check", "close"), events)
    }

    @Test fun openFailurePreservesOriginalAndDoesNotWrite() {
        val events = mutableListOf<String>()
        val failure = IOException("open")
        val actual = assertThrows(IOException::class.java) {
            writeComparePdfWithPreflight(
                checkCancelled = { events += "check" },
                openStream = { events += "open"; throw failure },
                write = { _: Stream -> events += "write" }
            )
        }
        assertSame(failure, actual)
        assertEquals(listOf("check", "open"), events)
    }

    @Test fun writeFailurePreservesOriginalAndCloses() {
        val events = mutableListOf<String>()
        val failure = IOException("write")
        val actual = assertThrows(IOException::class.java) {
            writeComparePdfWithPreflight(
                checkCancelled = { events += "check" },
                openStream = { events += "open"; Stream(events) },
                write = { _: Stream -> events += "write"; throw failure }
            )
        }
        assertSame(failure, actual)
        assertEquals(listOf("check", "open", "check", "write", "close"), events)
    }

    @Test fun closeFailureIsSuppressedOnSerializationFailure() {
        val events = mutableListOf<String>()
        val failure = IOException("write")
        val cleanup = IOException("close")
        val actual = assertThrows(IOException::class.java) {
            writeComparePdfWithPreflight(
                checkCancelled = { events += "check" },
                openStream = { events += "open"; Stream(events, cleanup) },
                write = { _: Stream -> events += "write"; throw failure }
            )
        }
        assertSame(failure, actual)
        assertSame(cleanup, actual.suppressed.single())
        assertEquals(listOf("check", "open", "check", "write", "close"), events)
    }

    @Test fun closeFailureAfterSuccessIsPropagated() {
        val events = mutableListOf<String>()
        val cleanup = IOException("close")
        val actual = assertThrows(IOException::class.java) {
            writeComparePdfWithPreflight(
                checkCancelled = { events += "check" },
                openStream = { events += "open"; Stream(events, cleanup) },
                write = { _: Stream -> events += "write" }
            )
        }
        assertSame(cleanup, actual)
        assertEquals(listOf("check", "open", "check", "write", "close"), events)
    }

    @Test fun everyInvocationChecksTwiceAndClosesItsOwnStream() {
        val events = mutableListOf<String>()
        repeat(3) { index ->
            writeComparePdfWithPreflight(
                checkCancelled = { events += "check:$index" },
                openStream = { events += "open:$index"; Stream(events) },
                write = { _: Stream -> events += "write:$index" }
            )
        }
        assertEquals(6, events.count { it.startsWith("check:") })
        assertEquals(3, events.count { it.startsWith("write:") })
        assertEquals(3, events.count { it == "close" })
    }

    @Test fun cancellationAfterLastOutputPageSkipsStreamCreation() {
        val events = mutableListOf("page:finished")
        val cancelled = CancellationException("after page")
        val actual = assertThrows(CancellationException::class.java) {
            writeComparePdfWithPreflight(
                checkCancelled = { events += "check"; throw cancelled },
                openStream = { events += "open"; Stream(events) },
                write = { _: Stream -> events += "write" }
            )
        }
        assertSame(cancelled, actual)
        assertEquals(listOf("page:finished", "check"), events)
    }
}
