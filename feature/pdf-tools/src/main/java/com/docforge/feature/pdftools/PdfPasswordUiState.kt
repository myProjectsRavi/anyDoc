package com.docforge.feature.pdftools

import android.net.Uri

enum class PdfPasswordMode {
    PROTECT,
    UNLOCK
}

data class PdfPasswordUiState(
    val selectedUri: Uri? = null,
    val inputLabel: String? = null,
    val isEncrypted: Boolean? = null,
    val outputName: String = defaultPasswordOutputName(),
    val mode: PdfPasswordMode = PdfPasswordMode.PROTECT,
    val userPasswordInput: String = "",
    val ownerPasswordInput: String = "",
    val unlockPasswordInput: String = "",
    val isProcessing: Boolean = false,
    val lastOutputPath: String? = null,
    val lastOutputSizeBytes: Long? = null,
    val statusMessage: String? = null,
    val errorMessage: String? = null
)

private fun defaultPasswordOutputName(): String = "secured_${System.currentTimeMillis()}"
