package com.docforge.feature.pdftools

import androidx.compose.runtime.Immutable
import com.docforge.core.ui.model.StableUriRef
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
data class PdfBatchStampUiState(
    val selectedUris: ImmutableList<StableUriRef> = persistentListOf(),
    val inputLabels: ImmutableList<String> = persistentListOf(),
    val outputBaseName: String = "batch_stamped",
    val watermarkText: String = "CONFIDENTIAL",
    val enableWatermark: Boolean = true,
    val enableBates: Boolean = true,
    val batesPrefix: String = "EXHIBIT-",
    val batesStart: String = "1",
    val batesPadding: Int = 4,
    val isProcessing: Boolean = false,
    val statusMessage: String? = null,
    val errorMessage: String? = null,
    val lastOutputPaths: ImmutableList<String> = persistentListOf(),
    val lastOutputSizeBytes: Long? = null
)
