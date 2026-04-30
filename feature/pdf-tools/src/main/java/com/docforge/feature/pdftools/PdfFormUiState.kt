package com.docforge.feature.pdftools

import android.net.Uri
import com.docforge.core.pdf.PdfFormFieldInfo

data class PdfFormUiState(
    val selectedUri: Uri? = null,
    val inputLabel: String? = null,
    val fields: List<PdfFormFieldInfo> = emptyList(),
    val selectedFieldName: String? = null,
    val fieldEdits: Map<String, String> = emptyMap(),
    val outputName: String = "form_filled",
    val flattenAfterFill: Boolean = false,
    val builderFieldName: String = "new_field",
    val builderPage: String = "1",
    val builderXRatio: String = "0.1",
    val builderYRatio: String = "0.1",
    val builderWidthRatio: String = "0.5",
    val builderHeightRatio: String = "0.08",
    val builderDefaultValue: String = "",
    val isProcessing: Boolean = false,
    val statusMessage: String? = null,
    val errorMessage: String? = null,
    val lastOutputPath: String? = null,
    val lastOutputSizeBytes: Long? = null
)
