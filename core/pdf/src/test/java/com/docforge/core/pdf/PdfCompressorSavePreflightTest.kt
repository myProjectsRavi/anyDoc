package com.docforge.core.pdf

import java.io.IOException
import java.util.concurrent.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class PdfCompressorSavePreflightTest {
    @Test fun cancellationBeforeSaveSkipsSerializationAndPreservesIdentity() {
        val original = CancellationException("cancel before compressed save")
        var saveCalled = false
        try {
            saveCompressedPdfWithPreflight(
                checkCancelled = { throw original },
                save = { saveCalled = true }
            )
            fail("Expected cancellation")
        } catch (actual: CancellationException) {
            assertSame(original, actual)
        }
        assertFalse(saveCalled)
    }

    @Test fun successfulSaveRunsAfterPreflightAndReturnsResult() {
        val events = mutableListOf<String>()
        val result = saveCompressedPdfWithPreflight(
            checkCancelled = { events.add("preflight") },
            save = { events.add("save"); 42 }
        )
        assertEquals(42, result)
        assertEquals(listOf("preflight", "save"), events)
    }

    @Test fun saveFailurePreservesOriginalException() {
        val original = IOException("save failed")
        try {
            saveCompressedPdfWithPreflight(
                checkCancelled = {},
                save = { throw original }
            )
            fail("Expected save failure")
        } catch (actual: IOException) {
            assertSame(original, actual)
        }
    }

    @Test fun preflightFailurePreservesOriginalException() {
        val original = IllegalStateException("preflight failed")
        var saveCalled = false
        try {
            saveCompressedPdfWithPreflight(
                checkCancelled = { throw original },
                save = { saveCalled = true }
            )
            fail("Expected preflight failure")
        } catch (actual: IllegalStateException) {
            assertSame(original, actual)
        }
        assertFalse(saveCalled)
    }

    @Test fun cancelledCoroutineAfterFinalProgressSkipsSave() = runBlocking {
        var observedCancellation = false
        var saved = false
        val worker = launch(start = CoroutineStart.UNDISPATCHED) {
            val checkCancelled = { coroutineContext.ensureActive() }
            coroutineContext.cancel()
            try {
                saveCompressedPdfWithPreflight(checkCancelled) { saved = true }
                fail("Expected cancellation after progress callback")
            } catch (_: CancellationException) {
                observedCancellation = true
            }
        }
        worker.join()
        assertTrue(observedCancellation)
        assertFalse(saved)
    }
}
