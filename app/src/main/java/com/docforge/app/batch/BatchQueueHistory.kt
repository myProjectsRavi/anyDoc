package com.docforge.app.batch

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/**
 * A published conversion output is not a failed conversion when its optional
 * history record cannot be stored. Keep conversion errors and cancellation
 * visible to the caller. onPublished commits success before optional history
 * I/O, so cancellation cannot downgrade an existing output.
 */
internal suspend fun <T> convertWithBestEffortHistory(
    convert: suspend () -> T,
    recordHistory: suspend (T) -> Unit,
    onPublished: (T) -> Unit = {}
): T {
    val outcome = convert()
    // Commit the published result before history I/O or cancellation checks.
    onPublished(outcome)
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
