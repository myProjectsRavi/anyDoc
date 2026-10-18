package com.docforge.app.batch

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class BatchQueueSnapshotWriterTest {
    @Test
    fun blockedWriterCapturesLatestSnapshotOnlyAfterLock() = runBlocking {
        val mutex = Mutex()
        var current = listOf("old")
        val persisted = mutableListOf<List<String>>()
        mutex.lock()
        try {
            val writer = launch(start = CoroutineStart.UNDISPATCHED) {
                persistLatestSnapshot(mutex, { current.toList() }) { persisted += it }
            }
            assertFalse(writer.isCompleted)
            current = listOf("new")
            mutex.unlock()
            writer.join()
            assertEquals(listOf(listOf("new")), persisted)
        } finally {
            if (mutex.isLocked) mutex.unlock()
        }
    }

    @Test
    fun queuedSecondWriterReadsStateAfterFirstWriterFinishes() = runBlocking {
        val mutex = Mutex()
        val firstEntered = CompletableDeferred<Unit>()
        val releaseFirst = CompletableDeferred<Unit>()
        var current = listOf("first")
        val persisted = mutableListOf<List<String>>()

        val first = launch(start = CoroutineStart.UNDISPATCHED) {
            persistLatestSnapshot(mutex, { current.toList() }) {
                persisted += it
                firstEntered.complete(Unit)
                releaseFirst.await()
            }
        }
        firstEntered.await()
        val second = launch(start = CoroutineStart.UNDISPATCHED) {
            persistLatestSnapshot(mutex, { current.toList() }) { persisted += it }
        }
        assertFalse(second.isCompleted)
        current = listOf("latest")
        releaseFirst.complete(Unit)
        first.join()
        second.join()
        assertEquals(listOf(listOf("first"), listOf("latest")), persisted)
    }

    @Test
    fun canceledWaitingWriterNeverCapturesOrPersists() = runBlocking {
        val mutex = Mutex(locked = true)
        var captures = 0
        var writes = 0
        val writer = launch(start = CoroutineStart.UNDISPATCHED) {
            persistLatestSnapshot(mutex, { captures++; listOf(1) }) { writes++ }
        }
        writer.cancelAndJoin()
        mutex.unlock()
        assertEquals(0, captures)
        assertEquals(0, writes)
    }

    @Test
    fun snapshotProviderFailurePreventsWriteAndReleasesMutex() = runBlocking {
        val mutex = Mutex()
        var writes = 0
        try {
            persistLatestSnapshot<Int>(mutex, { throw IllegalStateException("read failed") }) {
                writes++
            }
            fail("Expected provider failure")
        } catch (expected: IllegalStateException) {
            assertEquals("read failed", expected.message)
        }
        assertEquals(0, writes)
        assertFalse(mutex.isLocked)
    }

    @Test
    fun persistenceFailurePropagatesAndReleasesMutex() = runBlocking {
        val mutex = Mutex()
        try {
            persistLatestSnapshot(mutex, { listOf(1) }) {
                throw IllegalStateException("write failed")
            }
            fail("Expected write failure")
        } catch (expected: IllegalStateException) {
            assertEquals("write failed", expected.message)
        }
        assertFalse(mutex.isLocked)
    }

    @Test
    fun cancellationFromPersistenceIsNotSwallowed() = runBlocking {
        val mutex = Mutex()
        try {
            persistLatestSnapshot(mutex, { listOf(1) }) {
                throw CancellationException("write canceled")
            }
            fail("Expected cancellation")
        } catch (expected: CancellationException) {
            assertEquals("write canceled", expected.message)
        }
        assertFalse(mutex.isLocked)
    }
}
