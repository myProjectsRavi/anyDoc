package com.docforge.app.batch

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class BatchQueueForegroundService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var processingJob: Job? = null

    private val dependencies by lazy { AppDependencies(applicationContext) }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            BatchQueueServiceContract.ACTION_CANCEL_QUEUE -> {
                processingJob?.cancel()
                return START_NOT_STICKY
            }

            BatchQueueServiceContract.ACTION_RUN_QUEUE -> {
                if (processingJob?.isActive == true) {
                    return START_STICKY
                }

                val taskIds = intent.getLongArrayExtra(BatchQueueServiceContract.EXTRA_TASK_IDS)
                    ?.toList()
                    ?.distinct()
                    .orEmpty()

                if (taskIds.isEmpty()) {
                    BatchQueueRuntimeStore.failProcessing("No queued tasks to run.")
                    stopSelf(startId)
                    return START_NOT_STICKY
                }

                createNotificationChannelIfNeeded()
                startForeground(
                    BatchQueueServiceContract.NOTIFICATION_ID,
                    buildProgressNotification(
                        title = "DocForge batch queue",
                        text = "Starting queue...",
                        processed = 0,
                        total = taskIds.size,
                        ongoing = true
                    )
                )

                processingJob = serviceScope.launch {
                    processQueue(taskIds)
                    stopSelf(startId)
                }

                return START_STICKY
            }

            else -> return START_NOT_STICKY
        }
    }

    override fun onDestroy() {
        processingJob?.cancel()
        serviceScope.coroutineContext.cancel()
        super.onDestroy()
    }

    private suspend fun processQueue(taskIds: List<Long>) {
        val allowedTaskIds = taskIds.toSet()
        if (allowedTaskIds.isEmpty()) {
            BatchQueueRuntimeStore.failProcessing("No queued tasks available.")
            stopForeground(STOP_FOREGROUND_REMOVE)
            return
        }

        val total = allowedTaskIds.size
        try {
            while (true) {
                val task = BatchQueueRuntimeStore.startNextQueuedTask(allowedTaskIds, total) ?: break
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
                        outputSizeBytes = outcome.outputSizeBytes
                    )
                } catch (cancelled: CancellationException) {
                    BatchQueueRuntimeStore.markTaskCanceled(task.id)
                    throw cancelled
                } catch (error: Throwable) {
                    BatchQueueRuntimeStore.markTaskFailure(
                        taskId = task.id,
                        errorMessage = error.message ?: "Task failed"
                    )
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
            BatchQueueRuntimeStore.finishProcessing(summary)
            showCompletionNotification(summary)
        } catch (_: CancellationException) {
            BatchQueueRuntimeStore.markRemainingQueuedAsCanceled()
            val canceledState = BatchQueueRuntimeStore.state.value
            val summary = "Batch queue canceled: ${canceledState.successCount} success, ${canceledState.failureCount} failed"
            BatchQueueRuntimeStore.finishProcessing(summary)
            showCompletionNotification(summary)
        } finally {
            stopForeground(STOP_FOREGROUND_REMOVE)
        }
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
        NotificationManagerCompat.from(this).notify(
            BatchQueueServiceContract.NOTIFICATION_ID,
            buildProgressNotification(
                title = "DocForge batch queue",
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
            .setContentTitle("DocForge batch queue")
            .setContentText(summary)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(this).notify(
            BatchQueueServiceContract.NOTIFICATION_ID + 1,
            notification
        )
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
            "DocForge Batch Queue",
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
