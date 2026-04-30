package com.docforge.feature.pdftools

import android.net.Uri
import com.docforge.core.pdf.PdfPageImageFormat

data class PdfPageImageUiState(
    val selectedUri: Uri? = null,
    val inputLabel: String? = null,
    val pageCount: Int? = null,
    val outputBaseName: String = defaultOutputName(),
    val imageFormat: PdfPageImageFormat = PdfPageImageFormat.JPG,
    val zipBundleOutput: Boolean = false,
    val jpegQualityInput: String = "90",
    val scaleInput: String = "1.0",
    val isProcessing: Boolean = false,
    val lastOutputPaths: List<String> = emptyList(),
    val lastOutputSizeBytes: Long? = null,
    val statusMessage: String? = null,
    val errorMessage: String? = null
)

private fun defaultOutputName(): String = "pdf_pages_${System.currentTimeMillis()}"
