package com.docforge.app.batch

object BatchQueueServiceContract {
    const val ACTION_RUN_QUEUE = "com.docforge.app.batch.action.RUN_QUEUE"
    const val ACTION_CANCEL_QUEUE = "com.docforge.app.batch.action.CANCEL_QUEUE"

    const val EXTRA_TASK_IDS = "extra_task_ids"

    const val NOTIFICATION_CHANNEL_ID = "docforge_batch_queue"
    const val NOTIFICATION_ID = 4242
}
