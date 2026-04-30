package com.docforge.feature.pdftools

import android.net.Uri

data class PdfSavedSignatureSlotUi(
    val slot: Int,
    val exists: Boolean = false,
    val updatedAtMillis: Long? = null
)

data class PdfSignaturePlacementUi(
    val pageOneBased: Int,
    val xRatio: Float,
    val yRatio: Float,
    val widthRatio: Float
)

data class PdfPlacementTemplateUi(
    val name: String,
    val placementCount: Int,
    val updatedAtMillis: Long
)

data class PdfSignUiState(
    val selectedUri: Uri? = null,
    val inputLabel: String? = null,
    val pageCount: Int? = null,
    val outputName: String = defaultOutputName(),
    val targetPageInput: String = "1",
    val xRatioInput: String = "0.72",
    val yRatioInput: String = "0.82",
    val widthRatioInput: String = "0.24",
    val templateNameInput: String = "",
    val placements: List<PdfSignaturePlacementUi> = emptyList(),
    val placementTemplates: List<PdfPlacementTemplateUi> = emptyList(),
    val isProcessing: Boolean = false,
    val savedSignatureSlots: List<PdfSavedSignatureSlotUi> = (1..SavedSignatureStore.MAX_SLOTS)
        .map { slot -> PdfSavedSignatureSlotUi(slot = slot, exists = false, updatedAtMillis = null) },
    val lastOutputPath: String? = null,
    val lastOutputSizeBytes: Long? = null,
    val statusMessage: String? = null,
    val errorMessage: String? = null
)

private fun defaultOutputName(): String = "signed_${System.currentTimeMillis()}"
