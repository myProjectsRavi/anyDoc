package com.docforge.core.pdf

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream
import java.util.concurrent.CancellationException
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test

class PdfCompareSerializationCancellationTest {
    private class TrackingOutputStream : OutputStream() {
        val bytes = ByteArrayOutputStream()
        val chunks = mutableListOf<Int>()
        override fun write(value: Int) { bytes.write(value) }
        override fun write(input: ByteArray, offset: Int, length: Int) {
            chunks += length
            bytes.write(input, offset, length)
        }
    }

    @Test fun largeWritesAreChunkedWithoutChangingBytes() {
        val input = ByteArray(140_000) { (it % 251).toByte() }
        val sink = TrackingOutputStream()
        var checks = 0
        CancellationCheckingOutputStream(sink, { checks++ }).write(input)
        assertArrayEquals(input, sink.bytes.toByteArray())
        assertEquals(listOf(65536, 65536, 8928), sink.chunks)
        assertEquals(3, checks)
    }

    @Test fun nonZeroOffsetAndShortWritePreserveExactSlice() {
        val sink = TrackingOutputStream()
        CancellationCheckingOutputStream(sink, {}).write(byteArrayOf(0, 1, 2, 3, 4, 5), 2, 3)
        assertArrayEquals(byteArrayOf(2, 3, 4), sink.bytes.toByteArray())
        assertEquals(listOf(3), sink.chunks)
    }

    @Test fun cancellationBeforeFirstChunkDoesNotWrite() {
        val failure = CancellationException("cancelled")
        val sink = TrackingOutputStream()
        val stream = CancellationCheckingOutputStream(sink, { throw failure }, chunkSize = 4)
        assertSame(failure, assertThrows(CancellationException::class.java) { stream.write(ByteArray(10)) })
        assertEquals(0, sink.bytes.size())
    }

    @Test fun cancellationBetweenChunksStopsFurtherWrites() {
        val failure = CancellationException("cancelled")
        val sink = TrackingOutputStream()
        var checks = 0
        val stream = CancellationCheckingOutputStream(sink, { if (++checks == 2) throw failure }, chunkSize = 4)
        assertSame(failure, assertThrows(CancellationException::class.java) {
            stream.write(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9))
        })
        assertArrayEquals(byteArrayOf(1, 2, 3, 4), sink.bytes.toByteArray())
        assertEquals(2, checks)
    }

    @Test fun singleByteAndEmptyWritesCheckCancellation() {
        val failure = CancellationException("cancelled")
        val sink = TrackingOutputStream()
        val stream = CancellationCheckingOutputStream(sink, { throw failure })
        assertSame(failure, assertThrows(CancellationException::class.java) { stream.write(123) })
        assertSame(failure, assertThrows(CancellationException::class.java) { stream.write(ByteArray(0)) })
        assertEquals(0, sink.bytes.size())
    }

    @Test fun invalidRangesFailBeforeWriting() {
        val sink = TrackingOutputStream()
        val stream = CancellationCheckingOutputStream(sink, {})
        val input = byteArrayOf(1, 2, 3)
        assertThrows(IndexOutOfBoundsException::class.java) { stream.write(input, -1, 1) }
        assertThrows(IndexOutOfBoundsException::class.java) { stream.write(input, 1, -1) }
        assertThrows(IndexOutOfBoundsException::class.java) { stream.write(input, 2, 2) }
        assertEquals(0, sink.bytes.size())
    }

    @Test fun downstreamIOExceptionIsNotMasked() {
        val failure = IOException("disk full")
        val sink = object : OutputStream() {
            override fun write(value: Int) { throw failure }
            override fun write(bytes: ByteArray, offset: Int, length: Int) { throw failure }
        }
        val stream = CancellationCheckingOutputStream(sink, {})
        assertSame(failure, assertThrows(IOException::class.java) { stream.write(ByteArray(10)) })
    }

    @Test fun flushChecksCancellationAndCloseStillReleasesDelegate() {
        var flushes = 0
        var closes = 0
        val sink = object : OutputStream() {
            override fun write(value: Int) = Unit
            override fun flush() { flushes++ }
            override fun close() { closes++ }
        }
        var checks = 0
        val stream = CancellationCheckingOutputStream(sink, { checks++ })
        stream.flush()
        stream.close()
        assertEquals(1, checks)
        assertEquals(1, flushes)
        assertEquals(1, closes)
    }
}
