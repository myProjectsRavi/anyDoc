package com.docforge.feature.pdftools

import android.net.Uri

data class PdfBatchStampUiState(
    val selectedUris: List<Uri> = emptyList(),
    val inputLabels: List<String> = emptyList(),
    val outputBaseName: String = "batch_stamped",
    val watermarkText: String = "CONFIDENTIAL",
    val enableWatermark: Boolean = true,
    val enableBates: Boolean = true,
    val batesPrefix: String = "EXHIBIT-",
    val batesStart: String = "1",
    val batesPadding: Int = 4,
    val isProcessing: Boolean = false,
    val statusMessage: String? = null,
    val errorMessage: String? = null,
    val lastOutputPaths: List<String> = emptyList(),
    val lastOutputSizeBytes: Long? = null
)
