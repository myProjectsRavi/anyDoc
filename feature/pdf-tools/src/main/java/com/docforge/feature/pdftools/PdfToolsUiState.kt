package com.docforge.feature.pdftools

import androidx.compose.runtime.Immutable
import com.docforge.core.pdf.PdfMergePageSizeMode
import com.docforge.core.ui.model.StableUriRef
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
data class PdfToolsUiState(
    val selectedUris: ImmutableList<StableUriRef> = persistentListOf(),
    val outputName: String = defaultOutputName(),
    val mergeTitle: String = "",
    val mergeAuthor: String = "",
    val mergeSubject: String = "",
    val addBookmarks: Boolean = true,
    val mergePageSizeMode: PdfMergePageSizeMode = PdfMergePageSizeMode.KEEP_SOURCE,
    val isMerging: Boolean = false,
    val lastOutputPath: String? = null,
    val lastOutputSizeBytes: Long? = null,
    val statusMessage: String? = null,
    val errorMessage: String? = null
)

private fun defaultOutputName(): String = "merged_${System.currentTimeMillis()}"
