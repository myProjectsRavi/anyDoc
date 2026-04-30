package com.docforge.feature.converter

import android.net.Uri

data class AudioFormatUiState(
    val selectedUri: Uri? = null,
    val inputLabel: String? = null,
    val sourceMimeType: String? = null,
    val durationMs: Long? = null,
    val outputBaseName: String = defaultAudioFormatOutputName(),
    val outputFormat: AudioConvertOutputFormat = AudioConvertOutputFormat.M4A_AAC,
    val isProcessing: Boolean = false,
    val lastOutputPath: String? = null,
    val lastOutputSizeBytes: Long? = null,
    val statusMessage: String? = null,
    val errorMessage: String? = null
)

private fun defaultAudioFormatOutputName(): String = "audio_conv_${System.currentTimeMillis()}"
