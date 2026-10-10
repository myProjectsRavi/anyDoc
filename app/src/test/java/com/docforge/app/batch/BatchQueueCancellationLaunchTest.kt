package com.docforge.app.batch

import kotlinx.coroutines.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class BatchQueueCancellationLaunchTest {
    @Test fun successfulLaunchDoesNotReportFailure() {
        var calls = 0
        val launched = launchBatchQueueCancellation(
            launch = { calls++ },
            onFailure = { fail("Unexpected error: $it") }
        )
        assertTrue(launched)
        assertEquals(1, calls)
    }

    @Test fun rejectedServiceStartReportsFailure() {
        val errors = mutableListOf<Exception>()
        val launched = launchBatchQueueCancellation(
            launch = { throw IllegalStateException("background start blocked") },
            onFailure = { errors += it }
        )
        assertFalse(launched)
        assertEquals(1, errors.size)
        assertEquals("background start blocked", errors.single().message)
    }

    @Test fun securityFailureReportsFailure() {
        val errors = mutableListOf<Exception>()
        val launched = launchBatchQueueCancellation(
            launch = { throw SecurityException("not allowed") },
            onFailure = { errors += it }
        )
        assertFalse(launched)
        assertEquals("not allowed", errors.single().message)
    }

    @Test fun cancellationPropagatesWithoutReportingFailure() {
        var errors = 0
        try {
            launchBatchQueueCancellation(
                launch = { throw CancellationException("cancelled") },
                onFailure = { errors++ }
            )
            fail("Expected cancellation")
        } catch (expected: CancellationException) {
            assertEquals("cancelled", expected.message)
        }
        assertEquals(0, errors)
    }
}
