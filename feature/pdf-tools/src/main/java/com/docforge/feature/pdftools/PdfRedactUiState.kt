package com.docforge.feature.pdftools

import android.net.Uri
import androidx.compose.runtime.Immutable

@Immutable
data class PdfRedactUiState(
    val selectedUri: Uri? = null,
    val inputLabel: String? = null,
    val outputName: String = "redacted_pdf",
    val termsText: String = "",
    val caseSensitive: Boolean = false,
    val scrubMetadata: Boolean = true,
    val scrubFormValues: Boolean = true,
    val verifyIrreversible: Boolean = true,
    val autoDetectPii: Boolean = true,
    val isProcessing: Boolean = false,
    val statusMessage: String? = null,
    val errorMessage: String? = null,
    val progressStage: String? = null,
    val progressCurrent: Int = 0,
    val progressTotal: Int = 0,
    val lastOutputPath: String? = null,
    val lastOutputSizeBytes: Long? = null,
    val removedTextOperatorCount: Int? = null,
    val clearedFormFieldCount: Int? = null
)
