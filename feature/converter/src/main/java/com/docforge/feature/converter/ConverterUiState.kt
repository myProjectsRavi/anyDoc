package com.docforge.feature.converter

import androidx.compose.runtime.Immutable
import com.docforge.core.pdf.PdfPageSize
import com.docforge.core.ui.model.StableUriRef
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
data class ConverterUiState(
    val selectedUris: ImmutableList<StableUriRef> = persistentListOf(),
    val outputName: String = defaultOutputName(),
    val pageSize: PdfPageSize = PdfPageSize.A4,
    val isConverting: Boolean = false,
    val lastOutputPath: String? = null,
    val lastOutputSizeBytes: Long? = null,
    val statusMessage: String? = null,
    val errorMessage: String? = null
)

private fun defaultOutputName(): String = "docforge_${System.currentTimeMillis()}"
