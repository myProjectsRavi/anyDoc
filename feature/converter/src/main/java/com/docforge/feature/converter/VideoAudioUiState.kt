package com.docforge.feature.converter

import android.net.Uri

data class VideoAudioUiState(
    val selectedUri: Uri? = null,
    val inputLabel: String? = null,
    val sourceMimeType: String? = null,
    val durationMs: Long? = null,
    val outputBaseName: String = defaultAudioOutputName(),
    val outputFormat: AudioOutputFormat = AudioOutputFormat.M4A,
    val isProcessing: Boolean = false,
    val lastOutputPath: String? = null,
    val lastOutputSizeBytes: Long? = null,
    val statusMessage: String? = null,
    val errorMessage: String? = null
)

private fun defaultAudioOutputName(): String = "audio_${System.currentTimeMillis()}"
