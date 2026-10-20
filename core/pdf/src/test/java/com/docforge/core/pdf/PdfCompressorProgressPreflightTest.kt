package com.docforge.core.pdf

import java.util.concurrent.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.cancel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test

class PdfCompressorProgressPreflightTest {
    @Test fun cancellationSkipsProgressCallback() {
        val cancelled = CancellationException("cancelled after page work")
        var callbacks = 0
        val thrown = assertThrows(CancellationException::class.java) {
            notifyCompressionProgressWithPreflight(
                checkCancelled = { throw cancelled },
                completed = 1,
                total = 3,
                onProgress = { _, _ -> callbacks++ }
            )
        }
        assertSame(cancelled, thrown)
        assertEquals(0, callbacks)
    }

    @Test fun activeProgressChecksBeforeCallbackAndPreservesArguments() {
        val events = mutableListOf<String>()
        notifyCompressionProgressWithPreflight(
            checkCancelled = { events += "check" },
            completed = 2,
            total = 5,
            onProgress = { completed, total -> events += "progress:$completed/$total" }
        )
        assertEquals(listOf("check", "progress:2/5"), events)
    }

    @Test fun absentCallbackDoesNotRunUnnecessaryPreflight() {
        var checks = 0
        notifyCompressionProgressWithPreflight(
            checkCancelled = { checks++ },
            completed = 1,
            total = 1,
            onProgress = null
        )
        assertEquals(0, checks)
    }

    @Test fun callbackFailurePreservesOriginalException() {
        val failure = IllegalStateException("ui callback failed")
        val thrown = assertThrows(IllegalStateException::class.java) {
            notifyCompressionProgressWithPreflight(
                checkCancelled = {},
                completed = 1,
                total = 1,
                onProgress = { _, _ -> throw failure }
            )
        }
        assertSame(failure, thrown)
    }

    @Test fun multiplePagesPreserveProgressOrder() {
        val events = mutableListOf<Pair<Int, Int>>()
        repeat(3) { index ->
            notifyCompressionProgressWithPreflight(
                checkCancelled = {},
                completed = index + 1,
                total = 3,
                onProgress = { completed, total -> events += completed to total }
            )
        }
        assertEquals(listOf(1 to 3, 2 to 3, 3 to 3), events)
    }

    @Test fun actualCoroutineCancellationSkipsCallback() = runBlocking {
        var callbacks = 0
        var caught = false
        val job = launch(start = CoroutineStart.UNDISPATCHED) {
            coroutineContext.cancel()
            try {
                notifyCompressionProgressWithPreflight(
                    checkCancelled = { coroutineContext.ensureActive() },
                    completed = 1,
                    total = 2,
                    onProgress = { _, _ -> callbacks++ }
                )
            } catch (_: CancellationException) {
                caught = true
            }
        }
        job.join()
        assertEquals(true, caught)
        assertEquals(0, callbacks)
    }
}
