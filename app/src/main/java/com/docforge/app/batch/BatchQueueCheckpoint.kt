package com.docforge.app.batch

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/** A cancelled coroutine must never be reported as a persistence failure. */
internal suspend fun persistBatchQueueCheckpoint(
    persist: suspend () -> Unit,
    onFailure: suspend (Exception) -> Unit
): Boolean = try {
    currentCoroutineContext().ensureActive()
    persist()
    true
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (error: Exception) {
    onFailure(error)
    false
}


/**
 * A failed per-task terminal write must release its admitted run. Otherwise the
 * worker returns while the queue still reports isProcessing=true indefinitely.
 * The second write is best effort and cannot replace a newer run's UI state.
 */
internal suspend fun persistBatchTaskTerminalCheckpoint(
    persist: suspend () -> Unit,
    taskIds: Set<Long>,
    runId: Long,
    failureMessage: String
): Boolean = persistBatchQueueCheckpoint(
    persist = persist,
    onFailure = {
        if (BatchQueueRuntimeStore.failRunIfCurrentWithSnapshot(
                taskIds, runId, failureMessage
            ) != null
        ) {
            persistBatchQueueCheckpoint(
                persist = persist,
                onFailure = { /* Retain the original run failure in runtime state. */ }
            )
        }
    }
)
