package com.docforge.app.batch

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.fail
import org.junit.Test

class BatchQueueHistoryTest {
    @Test fun historyWriteFailurePreservesPublishedConversionOutcome() = runBlocking {
        val published = "/files/converted.pdf"
        val result = convertWithBestEffortHistory(
            convert = { published },
            recordHistory = { throw IllegalStateException("history database unavailable") }
        )
        assertEquals(published, result)
    }

    @Test fun successfulHistoryWriteReceivesConversionResult() = runBlocking {
        val published = "/files/converted.pdf"
        var saved: String? = null
        val result = convertWithBestEffortHistory(
            convert = { published },
            recordHistory = { saved = it }
        )
        assertEquals(published, result)
        assertEquals(published, saved)
    }

    @Test fun conversionFailureIsNotSilenced() = runBlocking {
        val failure = IllegalStateException("converter failed")
        try {
            convertWithBestEffortHistory<String>(
                convert = { throw failure },
                recordHistory = { fail("History must not be written") }
            )
            fail("Expected conversion failure")
        } catch (expected: IllegalStateException) {
            assertSame(failure, expected)
        }
    }

    @Test fun historyCancellationIsNotSilenced() = runBlocking {
        try {
            convertWithBestEffortHistory(
                convert = { "/files/converted.pdf" },
                recordHistory = { throw CancellationException("cancelled") }
            )
            fail("Expected cancellation")
        } catch (expected: CancellationException) {
            assertEquals("cancelled", expected.message)
        }
    }
}
