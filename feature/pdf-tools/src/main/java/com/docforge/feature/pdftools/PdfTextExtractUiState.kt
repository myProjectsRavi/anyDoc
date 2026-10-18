package com.docforge.feature.pdftools

import android.net.Uri

data class PdfTextExtractUiState(
    val selectedUri: Uri? = null,
    val inputLabel: String? = null,
    val pageCount: Int? = null,
    val outputName: String = defaultOutputName(),
    val isProcessing: Boolean = false,
    val lastOutputPath: String? = null,
    val lastOutputSizeBytes: Long? = null,
    val lastExtractedChars: Int? = null,
    val statusMessage: String? = null,
    val errorMessage: String? = null
)

private fun defaultOutputName(): String = "pdf_text_${System.currentTimeMillis()}"
