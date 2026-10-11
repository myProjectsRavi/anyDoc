package com.docforge.app.batch

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/**
 * A published conversion output is not a failed conversion when its optional
 * history record cannot be stored. Keep conversion errors and cancellation
 * visible to the caller, which owns the queue's terminal task transition.
 */
internal suspend fun <T> convertWithBestEffortHistory(
    convert: suspend () -> T,
    recordHistory: suspend (T) -> Unit
): T {
    val outcome = convert()
    currentCoroutineContext().ensureActive()
    try {
        recordHistory(outcome)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        // A history DAO failure must not turn an already published output into
        // FAILED or encourage the user to retry a conversion that succeeded.
        currentCoroutineContext().ensureActive()
    }
    currentCoroutineContext().ensureActive()
    return outcome
}
