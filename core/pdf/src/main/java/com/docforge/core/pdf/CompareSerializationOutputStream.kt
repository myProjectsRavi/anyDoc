package com.docforge.core.pdf

import java.io.OutputStream

/** Keep PDF serialization cancellation responsive without changing the emitted bytes. */
internal const val MAX_COMPARE_SERIALIZATION_CHUNK_BYTES = 64 * 1024

/** The caller owns [delegate]; checks run before each bounded write and flush. */
internal class CancellationCheckingOutputStream(
    private val delegate: OutputStream,
    private val checkCancelled: () -> Unit,
    private val chunkSize: Int = MAX_COMPARE_SERIALIZATION_CHUNK_BYTES
) : OutputStream() {
    init {
        require(chunkSize in 1..MAX_COMPARE_SERIALIZATION_CHUNK_BYTES) { "Invalid PDF serialization chunk size." }
    }

    override fun write(value: Int) {
        checkCancelled()
        delegate.write(value)
    }

    override fun write(bytes: ByteArray, offset: Int, length: Int) {
        if (offset < 0 || length < 0 || offset > bytes.size - length) throw IndexOutOfBoundsException()
        if (length == 0) {
            checkCancelled()
            return
        }
        var position = offset
        val end = offset + length
        while (position < end) {
            checkCancelled()
            val count = minOf(chunkSize, end - position)
            delegate.write(bytes, position, count)
            position += count
        }
    }

    override fun flush() {
        checkCancelled()
        delegate.flush()
    }

    override fun close() {
        delegate.close()
    }
}
