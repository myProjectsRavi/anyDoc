package com.docforge.feature.converter

import android.net.Uri

data class ImageFormatUiState(
    val selectedUris: List<Uri> = emptyList(),
    val outputBaseName: String = defaultOutputName(),
    val outputFormat: ImageOutputFormat = ImageOutputFormat.JPG,
    val qualityInput: String = "90",
    val scaleInput: String = "1.0",
    val isConverting: Boolean = false,
    val lastOutputPaths: List<String> = emptyList(),
    val lastOutputSizeBytes: Long? = null,
    val statusMessage: String? = null,
    val errorMessage: String? = null
)

private fun defaultOutputName(): String = "img_${System.currentTimeMillis()}"
