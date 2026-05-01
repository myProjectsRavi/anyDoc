package com.docforge.feature.pdftools

import android.net.Uri
import androidx.compose.runtime.Immutable

@Immutable
data class PdfSplitUiState(
    val selectedUri: Uri? = null,
    val inputLabel: String? = null,
    val pageCount: Int? = null,
    val outputBaseName: String = defaultOutputName(),
    val startPageInput: String = "1",
    val endPageInput: String = "1",
    val splitEveryNInput: String = "10",
    val extractPagesInput: String = "",
    val reorderPagesInput: String = "",
    val deletePagesInput: String = "",
    val rotatePagesInput: String = "",
    val rotateDegrees: Int = 90,
    val isProcessing: Boolean = false,
    val lastOutputPaths: List<String> = emptyList(),
    val lastOutputSizeBytes: Long? = null,
    val statusMessage: String? = null,
    val errorMessage: String? = null
)

private fun defaultOutputName(): String = "split_${System.currentTimeMillis()}"
