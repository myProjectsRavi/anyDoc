package com.docforge.app.batch

import android.app.Service
import org.junit.Assert.assertEquals
import org.junit.Test

class BatchQueueForegroundServicePolicyTest {

    @Test
    fun restartMode_isNonSticky_untilDurableQueueRecoveryExists() {
        assertEquals(Service.START_NOT_STICKY, batchQueueRestartMode())
    }
}
