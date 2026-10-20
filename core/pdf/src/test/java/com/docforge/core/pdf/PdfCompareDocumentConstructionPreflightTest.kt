package com.docforge.core.pdf

import java.util.concurrent.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test

class PdfCompareDocumentConstructionPreflightTest {
    private class FakeDocument(
        private val events: MutableList<String>,
        private val closeFailure: Throwable? = null
    ) {
        fun close() {
            events += "close"
            closeFailure?.let { throw it }
        }
    }

    @Test fun cancelledBeforeConstructionDoesNotAllocate() {
        val events = mutableListOf<String>()
        val cancelled = CancellationException("before construction")
        val actual = assertThrows(CancellationException::class.java) {
            createComparePdfDocumentWithPreflight(
                checkCancelled = { events += "check"; throw cancelled },
                createDocument = { events += "construct"; FakeDocument(events) }
            )
        }
        assertSame(cancelled, actual)
        assertEquals(listOf("check"), events)
    }

    @Test fun successfulConstructionChecksOnceAndReturnsOriginalDocument() {
        val events = mutableListOf<String>()
        val document = FakeDocument(events)
        val result = createComparePdfDocumentWithPreflight(
            checkCancelled = { events += "check" },
            createDocument = { events += "construct"; document }
        )
        assertSame(document, result)
        assertEquals(listOf("check", "construct"), events)
    }

    @Test fun nonCancellationPreflightFailurePreservesThrowableIdentity() {
        val failure = IllegalStateException("preflight")
        var constructions = 0
        val actual = assertThrows(IllegalStateException::class.java) {
            createComparePdfDocumentWithPreflight(
                checkCancelled = { throw failure },
                createDocument = { constructions++; FakeDocument(mutableListOf()) }
            )
        }
        assertSame(failure, actual)
        assertEquals(0, constructions)
    }

    @Test fun constructionFailurePreservesThrowableIdentity() {
        val events = mutableListOf<String>()
        val failure = OutOfMemoryError("simulated construction failure")
        val actual = assertThrows(OutOfMemoryError::class.java) {
            createComparePdfDocumentWithPreflight(
                checkCancelled = { events += "check" },
                createDocument = { events += "construct"; throw failure }
            )
        }
        assertSame(failure, actual)
        assertEquals(listOf("check", "construct"), events)
    }

    @Test fun cancellationBetweenStagingAndConstructionSkipsAllocation() {
        val events = mutableListOf("stage:opened")
        val cancelled = CancellationException("after staging")
        var cancelledNow = false
        cancelledNow = true
        val actual = assertThrows(CancellationException::class.java) {
            createComparePdfDocumentWithPreflight(
                checkCancelled = {
                    events += "check"
                    if (cancelledNow) throw cancelled
                },
                createDocument = { events += "construct"; FakeDocument(events) }
            )
        }
        assertSame(cancelled, actual)
        assertEquals(listOf("stage:opened", "check"), events)
    }

    @Test fun successfulDocumentStillClosesUnderExistingOwnershipScope() {
        val events = mutableListOf<String>()
        val result = withClosePreservingPrimaryFailure(
            resource = createComparePdfDocumentWithPreflight(
                checkCancelled = { events += "check" },
                createDocument = { events += "construct"; FakeDocument(events) }
            ),
            close = FakeDocument::close
        ) {
            events += "use"
            42
        }
        assertEquals(42, result)
        assertEquals(listOf("check", "construct", "use", "close"), events)
    }

    @Test fun bodyFailureRemainsPrimaryWhenDocumentCloseAlsoFails() {
        val events = mutableListOf<String>()
        val primary = IllegalStateException("render")
        val cleanup = IllegalArgumentException("close")
        val actual = assertThrows(IllegalStateException::class.java) {
            withClosePreservingPrimaryFailure(
                resource = createComparePdfDocumentWithPreflight(
                    checkCancelled = { events += "check" },
                    createDocument = { events += "construct"; FakeDocument(events, cleanup) }
                ),
                close = FakeDocument::close
            ) { _: FakeDocument ->
                events += "use"
                throw primary
            }
        }
        assertSame(primary, actual)
        assertSame(cleanup, actual.suppressed.single())
        assertEquals(listOf("check", "construct", "use", "close"), events)
    }

    @Test fun everyInvocationPreflightsBeforeItsOwnConstruction() {
        val events = mutableListOf<String>()
        repeat(3) { index ->
            createComparePdfDocumentWithPreflight(
                checkCancelled = { events += "check:$index" },
                createDocument = { events += "construct:$index"; FakeDocument(events) }
            )
        }
        assertEquals(
            listOf("check:0", "construct:0", "check:1", "construct:1", "check:2", "construct:2"),
            events
        )
    }
}
