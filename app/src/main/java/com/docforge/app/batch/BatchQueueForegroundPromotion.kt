package com.docforge.app.batch

import kotlinx.coroutines.CancellationException

/**
 * Android may reject foreground promotion after startForegroundService() has already returned.
 * Keep the error handling outside the framework API so it can be tested deterministically.
 */
internal inline fun attemptBatchForegroundPromotion(
    promote: () -> Unit,
    onFailure: (Exception) -> Unit
): Boolean = try {
    promote()
    true
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (error: Exception) {
    onFailure(error)
    false
}
