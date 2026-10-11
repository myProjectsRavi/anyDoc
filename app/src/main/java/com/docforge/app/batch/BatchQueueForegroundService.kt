package com.docforge.app.batch

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.docforge.app.AppDependencies
import com.docforge.core.domain.model.ConversionRecord
import com.docforge.core.domain.settings.DocForgeSettingsStore
import com.docforge.core.pdf.PdfCompressionLevel
import com.docforge.core.pdf.PdfCreationOptions
import com.docforge.core.pdf.PdfPageSize
import com.docforge.feature.converter.AudioOutputFormat
import com.docforge.feature.converter.ImageOutputFormat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BatchQueueForegroundService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var processingJob: Job? = null
    @Volatile private var processingTaskIds: Set<Long> = emptySet()
    @Volatile private var processingRunId: Long = 0L

    private val dependencies by lazy { (application as com.docforge.app.DocForgeApp).dependencies }
    private val persistenceStore by lazy {
        BatchQueuePersistenceStore(
            com.docforge.core.storage.db.DocForgeDatabase.get(applicationContext).batchQueueTaskDao()
        )
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannelIfNeeded()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            BatchQueueServiceContract.ACTION_CANCEL_QUEUE -> {
                val requested = intent.getLongArrayExtra(BatchQueueServiceContract.EXTRA_TASK_IDS)
                    ?.toSet().orEmpty()
                val requestedRunId = intent.getLongExtra(BatchQueueServiceContract.EXTRA_RUN_ID, 0L)
                if (requestedRunId > 0L && requestedRunId == processingRunId &&
                    requested.isNotEmpty() && requested == processingTaskIds &&
                    processingJob?.isCompleted == false
                ) {
                    processingJob?.cancel()
                    // Its ATOMIC start ensures the worker enters cancellation cleanup.
                    return START_NOT_STICKY
                }
                if (BatchQueueRuntimeStore.cancelPendingRun(requested, requestedRunId)) {
                    // No matching worker is attached. Persist before ending this service start.
                    serviceScope.launch(start = CoroutineStart.UNDISPATCHED) {
                        try {
                            withContext(NonCancellable) {
                                persistTerminalCheckpoint(
                                    "Batch cancellation completed, but the queue could not be saved."
                                )
                            }
                        } finally {
                            if (!BatchQueueRuntimeStore.state.value.isProcessing) {
                                stopForeground(STOP_FOREGROUND_REMOVE)
                            }
                            stopSelf(startId)
                        }
                    }
                }
                // Ignore stale cancel intents; never remove a newer run's notification.
                if (!BatchQueueRuntimeStore.state.value.isProcessing &&
                    processingJob?.isCompleted != false
                ) {
                    stopSelf(startId)
                }
                return START_NOT_STICKY
            }

            BatchQueueServiceContract.ACTION_RUN_QUEUE -> {
                val taskIds = intent.getLongArrayExtra(BatchQueueServiceContract.EXTRA_TASK_IDS)
                    ?.toList()
                    ?.distinct()
                    .orEmpty()

                if (taskIds.isEmpty()) {
                    // Malformed/stale intents cannot fail an unrelated admitted run.
                    if (!BatchQueueRuntimeStore.state.value.isProcessing) {
                        stopForeground(STOP_FOREGROUND_REMOVE)
                    }
                    stopSelf(startId)
                    return START_NOT_STICKY
                }

                val runIds = taskIds.toSet()
                val runId = intent.getLongExtra(BatchQueueServiceContract.EXTRA_RUN_ID, 0L)
                if (!BatchQueueRuntimeStore.isCurrentRun(runIds, runId)) {
                    if (!BatchQueueRuntimeStore.state.value.isProcessing) {
                        stopForeground(STOP_FOREGROUND_REMOVE)
                    }
                    stopSelf(startId)
                    return START_NOT_STICKY
                }

                val previous = processingJob?.takeUnless { it.isCompleted }
                if (previous != null && processingTaskIds == runIds && processingRunId == runId) {
                    return batchQueueRestartMode()
                }

                // Only a validated new RUN needs foreground promotion. Late CANCEL/RUN
                // intents must not overwrite the notification of a newer active batch.
                val promoted = attemptBatchForegroundPromotion(
                    promote = {
                        startForeground(
                            BatchQueueServiceContract.NOTIFICATION_ID,
                            buildProgressNotification(
                                title = "AnyDoc batch queue",
                                text = "Preparing...",
                                processed = 0,
                                total = taskIds.size,
                                ongoing = true
                            )
                        )
                    },
                    onFailure = { error ->
                        val failureMessage = "Unable to start foreground batch service: " +
                            (error.message ?: error.javaClass.simpleName)
                        val failedSnapshot = BatchQueueRuntimeStore.failRunIfCurrentWithSnapshot(
                            runIds, runId, failureMessage
                        )
                        if (failedSnapshot != null) {
                            // The OS may kill a started service that fails to become foreground
                            // within its deadline. Persist on the application scope, not the
                            // service scope that onDestroy() cancels.
                            (application as com.docforge.app.DocForgeApp).launchBatchQueueRecovery {
                                persistTerminalCheckpoint(
                                    "Foreground start failed, and the queue could not be saved.",
                                    expectedState = failedSnapshot
                                )
                            }
                        }
                        // Release the failed foreground-service start without waiting for I/O.
                        // stopSelf(startId) cannot stop a service with a newer start ID.
                        stopSelf(startId)
                    }
                )
                if (!promoted) return START_NOT_STICKY

                processingTaskIds = runIds
                processingRunId = runId
                notifyProgress(
                    text = "Starting queue...",
                    processed = 0,
                    total = taskIds.size
                )

                processingJob = serviceScope.launch(start = CoroutineStart.ATOMIC) {
                    try {
                        // A previous run can be finishing its terminal checkpoint even after
                        // isProcessing becomes false. Do not drop the newly admitted RUN intent.
                        previous?.join()
                        processQueue(taskIds, runId)
                    } catch (_: CancellationException) {
                        // Covers cancellation while waiting for an older worker, before this
                        // run ever reaches processQueue's own cancellation handler.
                        withContext(NonCancellable) {
                            if (BatchQueueRuntimeStore.cancelPendingRun(runIds, runId)) {
                                persistTerminalCheckpoint(
                                    "Batch cancellation completed, but the queue could not be saved."
                                )
                            }
                        }
                    } finally {
                        stopSelf(startId)
                    }
                }

                return batchQueueRestartMode()
            }

            else -> {
                if (!BatchQueueRuntimeStore.state.value.isProcessing) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf(startId)
                }
                return START_NOT_STICKY
            }
        }
    }

    override fun onDestroy() {
        processingJob?.cancel()
        serviceScope.coroutineContext.cancel()
        super.onDestroy()
    }

    private suspend fun processQueue(taskIds: List<Long>, runId: Long) {
        val allowedTaskIds = taskIds.toSet()
        if (allowedTaskIds.isEmpty()) {
            BatchQueueRuntimeStore.failProcessing("No queued tasks available.")
            stopForeground(STOP_FOREGROUND_REMOVE)
            return
        }

        val total = allowedTaskIds.size
        try {
            currentCoroutineContext().ensureActive()
            if (!BatchQueueRuntimeStore.isCurrentRun(allowedTaskIds, runId)) return
            while (true) {
                val task = BatchQueueRuntimeStore.startNextQueuedTask(allowedTaskIds, total, runId) ?: break
                if (!persistRunningCheckpoint(task.id, allowedTaskIds, runId)) {
                    return
                }
                notifyProgress(
                    text = "Running ${BatchQueueRuntimeStore.state.value.processedCount + 1}/$total: ${task.type.title}",
                    processed = BatchQueueRuntimeStore.state.value.processedCount,
                    total = total
                )

                try {
                    val outcome = executeTask(task)
                    dependencies.historyRepository.insert(
                        ConversionRecord(
                            sourceLabel = outcome.sourceLabel,
                            outputPath = outcome.outputPath,
                            operation = outcome.operation,
                            createdAtMillis = System.currentTimeMillis(),
                            inputCount = outcome.inputCount,
                            outputSizeBytes = outcome.outputSizeBytes
                        )
                    )
                    BatchQueueRuntimeStore.markTaskSuccess(
                        taskId = task.id,
                        outputPath = outcome.outputPath,
                        outputSizeBytes = outcome.outputSizeBytes,
                        expectedRunId = runId
                    )
                    if (!persistBatchTaskTerminalCheckpoint(
                            persist = { persistQueueSnapshot() },
                            taskIds = allowedTaskIds,
                            runId = runId,
                            failureMessage = "Task completed, but its queue state could not be saved. Review the output before retrying."
                        )
                    ) {
                        return
                    }
                } catch (cancelled: CancellationException) {
                    BatchQueueRuntimeStore.markTaskCanceled(task.id, runId)
                    // Cancellation is already active; use a non-cancellable cleanup context to persist the
                    // canceled task once, then propagate the original cancellation.
                    withContext(NonCancellable) {
                        persistBatchQueueCheckpoint(
                            persist = { persistQueueSnapshot() },
                            onFailure = { /* Best effort: terminal checkpoint will retry. */ }
                        )
                    }
                    throw cancelled
                } catch (error: Throwable) {
                    BatchQueueRuntimeStore.markTaskFailure(
                        taskId = task.id,
                        errorMessage = error.message ?: "Task failed",
                        expectedRunId = runId
                    )
                    if (!persistBatchTaskTerminalCheckpoint(
                            persist = { persistQueueSnapshot() },
                            taskIds = allowedTaskIds,
                            runId = runId,
                            failureMessage = "Task failed, but its queue state could not be saved. Review the queue before retrying."
                        )
                    ) {
                        return
                    }
                }

                val state = BatchQueueRuntimeStore.state.value
                notifyProgress(
                    text = "Processed ${state.processedCount}/$total",
                    processed = state.processedCount,
                    total = total
                )
            }

            val finalState = BatchQueueRuntimeStore.state.value
            val summary = "Batch complete: ${finalState.successCount} success, ${finalState.failureCount} failed"
            if (BatchQueueRuntimeStore.finishRunIfCurrent(allowedTaskIds, runId, summary)) {
                persistTerminalCheckpoint(
                    "Batch finished, but the final queue state could not be saved. Review outputs before retrying."
                )
                showCompletionNotification(summary)
            }
        } catch (_: CancellationException) {
            // The queue is intentionally terminal on cancellation; persist that terminal state
            // outside the cancelled job context rather than misclassifying it as a disk failure.
            withContext(NonCancellable) {
                // CAS before touching tasks prevents a late callback from affecting a later run.
                if (BatchQueueRuntimeStore.cancelPendingRun(allowedTaskIds, runId)) {
                    persistTerminalCheckpoint(
                        "Batch cancellation completed, but the final queue state could not be saved."
                    )
                    showCompletionNotification("Batch queue canceled")
                }
            }
        } finally {
            // An older worker can complete after a newer run has attached.
            if (processingRunId == runId && processingTaskIds == allowedTaskIds) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            }
        }
    }

    private suspend fun persistRunningCheckpoint(
        taskId: Long,
        taskIds: Set<Long>,
        runId: Long
    ): Boolean =
        persistBatchQueueCheckpoint(
            persist = { persistQueueSnapshot() },
            onFailure = { error ->
                val message = error.message ?: "Unable to save the running queue state."
                if (BatchQueueRuntimeStore.isCurrentRun(taskIds, runId)) {
                    BatchQueueRuntimeStore.markTaskFailure(
                        taskId = taskId,
                        errorMessage = "Task was not run because queue state could not be saved: $message",
                        expectedRunId = runId
                    )
                    BatchQueueRuntimeStore.failRunIfCurrent(
                        taskIds,
                        runId,
                        "Batch stopped before running task #$taskId because its queue state could not be saved."
                    )
                    // Keep the first error visible if recovery persistence also fails.
                    persistBatchQueueCheckpoint(
                        persist = { persistQueueSnapshot() },
                        onFailure = { /* Best effort */ }
                    )
                }
            }
        )

    private suspend fun persistTerminalCheckpoint(
        failureMessage: String,
        expectedState: BatchQueueUiState = BatchQueueRuntimeStore.state.value
    ): Boolean =
        persistBatchQueueCheckpoint(
            persist = { persistQueueSnapshot() },
            onFailure = {
                BatchQueueRuntimeStore.reportPersistenceFailureIfIdle(failureMessage, expectedState)
            }
        )

    private suspend fun persistQueueSnapshot() {
        persistenceStore.replaceSnapshot { BatchQueueRuntimeStore.state.value.tasks }
    }

    private suspend fun executeTask(task: BatchQueueTask): BatchExecutionOutcome {
        return when (task.type) {
            BatchTaskType.IMAGES_TO_PDF -> {
                val result = dependencies.pdfCreator.createPdfFromImages(
                    imageUris = task.inputUris,
                    outputName = task.outputBaseName,
                    options = PdfCreationOptions(pageSize = preferredPdfPageSize())
                )
                BatchExecutionOutcome(
                    operation = "Batch: Images -> PDF",
                    sourceLabel = task.inputSummary,
                    inputCount = task.inputUris.size,
                    outputPath = result.outputFile.absolutePath,
                    outputSizeBytes = result.outputSizeBytes
                )
            }

            BatchTaskType.IMAGE_TO_JPG -> {
                val result = dependencies.imageFormatConverter.convertBatch(
                    inputUris = task.inputUris,
                    outputBaseName = task.outputBaseName,
                    outputFormat = ImageOutputFormat.JPG,
                    quality = preferredImageQuality(),
                    scaleFactor = 1.0f
                )
                val firstOutput = result.outputFiles.firstOrNull()?.absolutePath.orEmpty()
                BatchExecutionOutcome(
                    operation = "Batch: Images -> JPG",
                    sourceLabel = task.inputSummary,
                    inputCount = task.inputUris.size,
                    outputPath = firstOutput,
                    outputSizeBytes = result.outputSizeBytes
                )
            }

            BatchTaskType.PDF_MERGE -> {
                val result = dependencies.pdfMerger.merge(
                    inputUris = task.inputUris,
                    outputName = task.outputBaseName
                )
                BatchExecutionOutcome(
                    operation = "Batch: PDF Merge",
                    sourceLabel = task.inputSummary,
                    inputCount = task.inputUris.size,
                    outputPath = result.outputFile.absolutePath,
                    outputSizeBytes = result.outputSizeBytes
                )
            }

            BatchTaskType.PDF_COMPRESS -> {
                val input = task.inputUris.first()
                val result = dependencies.pdfCompressor.compress(
                    inputUri = input,
                    outputName = task.outputBaseName,
                    level = preferredPdfCompressionLevel()
                )
                BatchExecutionOutcome(
                    operation = "Batch: PDF Compress",
                    sourceLabel = task.inputSummary,
                    inputCount = 1,
                    outputPath = result.outputFile.absolutePath,
                    outputSizeBytes = result.outputSizeBytes
                )
            }

            BatchTaskType.DOC_TO_PDF -> {
                val input = task.inputUris.first()
                val result = dependencies.documentPdfConverter.convertToPdf(
                    inputUri = input,
                    inputNameHint = null,
                    outputName = task.outputBaseName
                )
                BatchExecutionOutcome(
                    operation = "Batch: Document -> PDF",
                    sourceLabel = task.inputSummary,
                    inputCount = 1,
                    outputPath = result.pdfResult.outputFile.absolutePath,
                    outputSizeBytes = result.pdfResult.outputSizeBytes
                )
            }

            BatchTaskType.VIDEO_TO_AUDIO -> {
                val input = task.inputUris.first()
                val result = dependencies.videoAudioExtractor.extractAudio(
                    inputUri = input,
                    outputBaseName = task.outputBaseName,
                    outputFormat = AudioOutputFormat.M4A
                )
                BatchExecutionOutcome(
                    operation = "Batch: Video -> M4A",
                    sourceLabel = task.inputSummary,
                    inputCount = 1,
                    outputPath = result.outputFile.absolutePath,
                    outputSizeBytes = result.outputSizeBytes
                )
            }
        }
    }

    private fun preferredPdfPageSize(): PdfPageSize {
        val stored = DocForgeSettingsStore.readPdfPageSizeName(this)
        return PdfPageSize.entries.firstOrNull { it.name == stored } ?: PdfPageSize.A4
    }

    private fun preferredPdfCompressionLevel(): PdfCompressionLevel {
        val stored = DocForgeSettingsStore.readPdfCompressionName(this)
        return PdfCompressionLevel.entries.firstOrNull { it.name == stored } ?: PdfCompressionLevel.MEDIUM
    }

    private fun preferredImageQuality(): Int {
        return DocForgeSettingsStore.readDefaultImageQuality(this)
    }

    private fun notifyProgress(text: String, processed: Int, total: Int) {
        notifySafely(
            notificationId = BatchQueueServiceContract.NOTIFICATION_ID,
            notification = buildProgressNotification(
                title = "AnyDoc batch queue",
                text = text,
                processed = processed,
                total = total,
                ongoing = true
            )
        )
    }

    private fun showCompletionNotification(summary: String) {
        val notification = NotificationCompat.Builder(this, BatchQueueServiceContract.NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_agenda)
            .setContentTitle("AnyDoc batch queue")
            .setContentText(summary)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notifySafely(
            notificationId = BatchQueueServiceContract.NOTIFICATION_ID + 1,
            notification = notification
        )
    }

    @SuppressLint("MissingPermission")
    private fun notifySafely(notificationId: Int, notification: Notification) {
        if (!canPostNotifications()) return
        runCatching {
            NotificationManagerCompat.from(this).notify(notificationId, notification)
        }
    }

    private fun canPostNotifications(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun buildProgressNotification(
        title: String,
        text: String,
        processed: Int,
        total: Int,
        ongoing: Boolean
    ): Notification {
        val safeTotal = total.coerceAtLeast(1)
        val safeProcessed = processed.coerceIn(0, safeTotal)

        return NotificationCompat.Builder(this, BatchQueueServiceContract.NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_agenda)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(ongoing)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setProgress(safeTotal, safeProcessed, false)
            .build()
    }

    private fun createNotificationChannelIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            BatchQueueServiceContract.NOTIFICATION_CHANNEL_ID,
            "AnyDoc Batch Queue",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Progress for queued offline conversion tasks"
        }
        manager.createNotificationChannel(channel)
    }
}

private data class BatchExecutionOutcome(
    val operation: String,
    val sourceLabel: String,
    val inputCount: Int,
    val outputPath: String,
    val outputSizeBytes: Long
)

/**
 * The executable batch queue currently lives in process-local memory.
 * Until durable queue recovery is wired, Android must not recreate this service after process death
 * without the original task-id intent because that could present a foreground service with no queue.
 */
internal fun batchQueueRestartMode(): Int = Service.START_NOT_STICKY
