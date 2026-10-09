package com.docforge.app.batch

import kotlinx.coroutines.CancellationException

/** A cancelled coroutine must never be reported as a persistence failure. */
internal suspend fun persistBatchQueueCheckpoint(
    persist: suspend () -> Unit,
    onFailure: suspend (Exception) -> Unit
): Boolean = try {
    persist()
    true
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (error: Exception) {
    onFailure(error)
    false
}
