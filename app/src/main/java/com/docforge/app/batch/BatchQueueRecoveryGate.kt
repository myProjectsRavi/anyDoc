package com.docforge.app.batch

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect

/** Prevent queue persistence after failed recovery. */
internal suspend fun collectBatchQueueAfterRecovery(
    recover: suspend () -> List<BatchQueueTask>,
    restore: (List<BatchQueueTask>) -> Unit,
    states: Flow<BatchQueueUiState>,
    onRecoveryFailure: (Exception) -> Unit,
    onState: suspend (BatchQueueUiState) -> Unit
) {
    val recovered = try {
        recover()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        onRecoveryFailure(error)
        return
    }
    restore(recovered)
    states.collect { onState(it) }
}
