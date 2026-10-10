package com.docforge.app.batch

import android.net.Uri
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BatchQueueAtomicDispatchTest {
    @Before fun setUp() = reset()
    @After fun tearDown() = reset()

    private fun reset() {
        BatchQueueRuntimeStore.finishProcessing("Stopped")
        assertTrue(BatchQueueRuntimeStore.clearQueue().isSuccess)
    }

    private fun enqueue(index: Int): Long {
        assertTrue(BatchQueueRuntimeStore.addTask(
            BatchTaskType.PDF_COMPRESS,
            listOf(Uri.parse("content://provider/dispatch-$index.pdf")),
            emptyList()
        ).isSuccess)
        return BatchQueueRuntimeStore.state.value.tasks.last().id
    }

    private fun begin() {
        assertTrue(runBlocking { BatchQueueRuntimeStore.beginProcessing() }.isSuccess)
    }

    @Test fun inactiveQueueCannotDispatch() {
        val id = enqueue(1)
        val before = BatchQueueRuntimeStore.state.value
        assertNull(BatchQueueRuntimeStore.startNextQueuedTask(setOf(id), 1))
        assertEquals(before, BatchQueueRuntimeStore.state.value)
    }

    @Test fun claimedTaskIsCommittedRunning() {
        val id = enqueue(2)
        begin()
        val claimed = BatchQueueRuntimeStore.startNextQueuedTask(setOf(id), 1)
        assertNotNull(claimed)
        assertEquals(id, claimed!!.id)
        assertEquals(BatchTaskStatus.RUNNING, claimed.status)
        assertEquals(BatchTaskStatus.RUNNING, BatchQueueRuntimeStore.state.value.tasks.single().status)
        assertEquals("Running 1/1: ${claimed.type.title}", BatchQueueRuntimeStore.state.value.statusMessage)
    }

    @Test fun duplicateClaimReturnsNullWithoutMutation() {
        val id = enqueue(3)
        begin()
        assertNotNull(BatchQueueRuntimeStore.startNextQueuedTask(setOf(id), 1))
        val after = BatchQueueRuntimeStore.state.value
        assertNull(BatchQueueRuntimeStore.startNextQueuedTask(setOf(id), 1))
        assertEquals(after, BatchQueueRuntimeStore.state.value)
    }

    @Test fun ineligibleTaskIdsDoNotClaimOtherTasks() {
        val first = enqueue(4)
        val second = enqueue(5)
        begin()
        val claimed = BatchQueueRuntimeStore.startNextQueuedTask(setOf(second), 2)
        assertEquals(second, claimed?.id)
        assertEquals(BatchTaskStatus.QUEUED, BatchQueueRuntimeStore.state.value.tasks.first { it.id == first }.status)
    }

    @Test fun stoppedQueueDoesNotDispatchRemainingTasks() {
        val first = enqueue(6)
        val second = enqueue(7)
        begin()
        assertNotNull(BatchQueueRuntimeStore.startNextQueuedTask(setOf(first), 2))
        BatchQueueRuntimeStore.finishProcessing("Stopped")
        val before = BatchQueueRuntimeStore.state.value
        assertFalse(before.isProcessing)
        assertNull(BatchQueueRuntimeStore.startNextQueuedTask(setOf(second), 2))
        assertEquals(before, BatchQueueRuntimeStore.state.value)
    }

    @Test fun emptyEligibilitySetDoesNotChangeState() {
        enqueue(8)
        begin()
        val before = BatchQueueRuntimeStore.state.value
        assertNull(BatchQueueRuntimeStore.startNextQueuedTask(emptySet(), 1))
        assertEquals(before, BatchQueueRuntimeStore.state.value)
    }

    @Test fun concurrentClaimsOfOneTaskSucceedExactlyOnce() {
        val pool = Executors.newFixedThreadPool(2)
        try {
            repeat(100) { index ->
                reset()
                val id = enqueue(1000 + index)
                begin()
                val gate = CountDownLatch(1)
                val a = pool.submit<BatchQueueTask?> { gate.await(); BatchQueueRuntimeStore.startNextQueuedTask(setOf(id), 1) }
                val b = pool.submit<BatchQueueTask?> { gate.await(); BatchQueueRuntimeStore.startNextQueuedTask(setOf(id), 1) }
                gate.countDown()
                val claims = listOf(a.get(10, TimeUnit.SECONDS), b.get(10, TimeUnit.SECONDS))
                assertEquals(1, claims.count { it != null })
                assertEquals(id, claims.filterNotNull().single().id)
                assertEquals(BatchTaskStatus.RUNNING, BatchQueueRuntimeStore.state.value.tasks.single().status)
            }
        } finally {
            pool.shutdownNow()
        }
    }

    @Test fun concurrentClaimsOfTwoTasksReturnDistinctCommittedTasks() {
        val pool = Executors.newFixedThreadPool(2)
        try {
            repeat(100) { index ->
                reset()
                val first = enqueue(2000 + index * 2)
                val second = enqueue(2001 + index * 2)
                begin()
                val gate = CountDownLatch(1)
                val a = pool.submit<BatchQueueTask?> { gate.await(); BatchQueueRuntimeStore.startNextQueuedTask(setOf(first, second), 2) }
                val b = pool.submit<BatchQueueTask?> { gate.await(); BatchQueueRuntimeStore.startNextQueuedTask(setOf(first, second), 2) }
                gate.countDown()
                val claims = listOf(a.get(10, TimeUnit.SECONDS), b.get(10, TimeUnit.SECONDS)).filterNotNull()
                assertEquals(setOf(first, second), claims.map { it.id }.toSet())
                assertTrue(claims.all { it.status == BatchTaskStatus.RUNNING })
                assertTrue(BatchQueueRuntimeStore.state.value.tasks.all { it.status == BatchTaskStatus.RUNNING })
            }
        } finally {
            pool.shutdownNow()
        }
    }

    @Test fun concurrentStopAndClaimCannotDispatchAfterStopWins() {
        val pool = Executors.newFixedThreadPool(2)
        try {
            repeat(100) { index ->
                reset()
                val id = enqueue(3000 + index)
                begin()
                val gate = CountDownLatch(1)
                val stop = pool.submit { gate.await(); BatchQueueRuntimeStore.finishProcessing("Stopped") }
                val claim = pool.submit<BatchQueueTask?> {
                    gate.await()
                    BatchQueueRuntimeStore.startNextQueuedTask(setOf(id), 1)
                }
                gate.countDown()
                stop.get(10, TimeUnit.SECONDS)
                val result = claim.get(10, TimeUnit.SECONDS)
                val state = BatchQueueRuntimeStore.state.value
                assertFalse(state.isProcessing)
                assertEquals(
                    if (result == null) BatchTaskStatus.QUEUED else BatchTaskStatus.RUNNING,
                    state.tasks.single().status
                )
                assertEquals(id, state.tasks.single().id)
            }
        } finally {
            pool.shutdownNow()
        }
    }
}
