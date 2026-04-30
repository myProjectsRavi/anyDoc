package com.docforge.feature.pdftools

import android.net.Uri

data class PdfOcrUiState(
    val selectedUri: Uri? = null,
    val inputLabel: String? = null,
    val outputName: String = "ocr_output",
    val createSearchablePdf: Boolean = true,
    val isProcessing: Boolean = false,
    val statusMessage: String? = null,
    val errorMessage: String? = null,
    val progressStage: String? = null,
    val progressCurrent: Int = 0,
    val progressTotal: Int = 0,
    val textOutputPath: String? = null,
    val searchablePdfPath: String? = null,
    val extractedTextPreview: String? = null,
    val extractedChars: Int? = null,
    val lineCount: Int? = null
)
