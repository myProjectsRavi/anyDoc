package com.docforge.app.batch

import android.net.Uri

enum class BatchTaskType(
    val title: String,
    val description: String,
    val mimeFilter: String,
    val allowsMultipleInputs: Boolean,
    val minInputCount: Int,
    val defaultOutputPrefix: String
) {
    IMAGES_TO_PDF(
        title = "Images -> PDF",
        description = "Convert one or more images into one PDF",
        mimeFilter = "image/*",
        allowsMultipleInputs = true,
        minInputCount = 1,
        defaultOutputPrefix = "batch_images_pdf"
    ),
    IMAGE_TO_JPG(
        title = "Images -> JPG",
        description = "Batch convert images to JPG",
        mimeFilter = "image/*",
        allowsMultipleInputs = true,
        minInputCount = 1,
        defaultOutputPrefix = "batch_images_jpg"
    ),
    PDF_MERGE(
        title = "PDF Merge",
        description = "Merge multiple PDFs into one file",
        mimeFilter = "application/pdf",
        allowsMultipleInputs = true,
        minInputCount = 2,
        defaultOutputPrefix = "batch_pdf_merge"
    ),
    PDF_COMPRESS(
        title = "PDF Compress",
        description = "Compress one PDF with medium preset",
        mimeFilter = "application/pdf",
        allowsMultipleInputs = false,
        minInputCount = 1,
        defaultOutputPrefix = "batch_pdf_compress"
    ),
    DOC_TO_PDF(
        title = "Document -> PDF",
        description = "Convert DOCX/RTF/CSV/TXT to PDF",
        mimeFilter = "*/*",
        allowsMultipleInputs = false,
        minInputCount = 1,
        defaultOutputPrefix = "batch_doc_pdf"
    ),
    VIDEO_TO_AUDIO(
        title = "Video -> M4A",
        description = "Extract audio from one video",
        mimeFilter = "video/*",
        allowsMultipleInputs = false,
        minInputCount = 1,
        defaultOutputPrefix = "batch_video_audio"
    );

    fun validateInputCount(count: Int): String? {
        if (count < minInputCount) {
            return "${title} needs at least $minInputCount input file(s)."
        }
        if (!allowsMultipleInputs && count > 1) {
            return "${title} accepts one input file at a time."
        }
        return null
    }
}

enum class BatchTaskStatus {
    QUEUED,
    RUNNING,
    SUCCESS,
    FAILED,
    CANCELED
}

data class BatchQueueTask(
    val id: Long,
    val type: BatchTaskType,
    val inputUris: List<Uri>,
    val inputSummary: String,
    val outputBaseName: String,
    val status: BatchTaskStatus = BatchTaskStatus.QUEUED,
    val outputPath: String? = null,
    val outputSizeBytes: Long? = null,
    val errorMessage: String? = null
)

data class BatchQueuePresetTask(
    val type: BatchTaskType,
    val inputUris: List<String>,
    val inputSummary: String,
    val outputBaseName: String
)

data class BatchQueuePreset(
    val id: Long,
    val name: String,
    val createdAtMillis: Long,
    val tasks: List<BatchQueuePresetTask>
)

data class BatchQueueUiState(
    val tasks: List<BatchQueueTask> = emptyList(),
    val isProcessing: Boolean = false,
    val processedCount: Int = 0,
    val successCount: Int = 0,
    val failureCount: Int = 0,
    val statusMessage: String? = null,
    val errorMessage: String? = null
)
