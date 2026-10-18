package com.docforge.feature.pdftools

import android.net.Uri
import androidx.compose.runtime.Immutable
import com.docforge.core.pdf.PdfAnnotationCommand
import com.docforge.core.pdf.PdfAnnotationType

@Immutable
data class PdfAnnotateUiState(
    val selectedUri: Uri? = null,
    val inputLabel: String? = null,
    val pageCount: Int? = null,
    val outputName: String = defaultAnnotateOutputName(),
    val selectedType: PdfAnnotationType = PdfAnnotationType.HIGHLIGHT,
    val pageInput: String = "1",
    val xRatioInput: String = "0.10",
    val yRatioInput: String = "0.10",
    val widthRatioInput: String = "0.30",
    val heightRatioInput: String = "0.10",
    val textInput: String = "",
    val pendingAnnotations: List<PdfAnnotationCommand> = emptyList(),
    val isProcessing: Boolean = false,
    val lastOutputPath: String? = null,
    val lastOutputSizeBytes: Long? = null,
    val statusMessage: String? = null,
    val errorMessage: String? = null
)

private fun defaultAnnotateOutputName(): String = "annotated_${System.currentTimeMillis()}"
