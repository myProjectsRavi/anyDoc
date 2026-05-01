package com.docforge.feature.converter

import androidx.compose.runtime.Immutable
import com.docforge.core.ui.model.StableUriRef
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
data class ImageFormatUiState(
    val selectedUris: ImmutableList<StableUriRef> = persistentListOf(),
    val outputBaseName: String = defaultOutputName(),
    val outputFormat: ImageOutputFormat = ImageOutputFormat.JPG,
    val qualityInput: String = "90",
    val scaleInput: String = "1.0",
    val isConverting: Boolean = false,
    val lastOutputPaths: ImmutableList<String> = persistentListOf(),
    val lastOutputSizeBytes: Long? = null,
    val statusMessage: String? = null,
    val errorMessage: String? = null
)

private fun defaultOutputName(): String = "img_${System.currentTimeMillis()}"
