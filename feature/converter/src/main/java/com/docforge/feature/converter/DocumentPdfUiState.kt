package com.docforge.feature.converter

import android.net.Uri

data class DocumentPdfUiState(
    val selectedUri: Uri? = null,
    val inputLabel: String? = null,
    val inputType: DocumentInputType = DocumentInputType.UNKNOWN,
    val outputName: String = defaultDocumentOutputName(),
    val isProcessing: Boolean = false,
    val lastOutputPath: String? = null,
    val lastOutputSizeBytes: Long? = null,
    val lastPageCount: Int? = null,
    val lastLineCount: Int? = null,
    val statusMessage: String? = null,
    val errorMessage: String? = null
)

private fun defaultDocumentOutputName(): String = "doc_${System.currentTimeMillis()}"
