package com.docforge.app.batch

import kotlinx.coroutines.CancellationException

/** Report service-launch failures without crashing the cancel action. */
internal fun launchBatchQueueCancellation(
    launch: () -> Unit,
    onFailure: (Exception) -> Unit
): Boolean = try {
    launch()
    true
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (error: Exception) {
    onFailure(error)
    false
}
