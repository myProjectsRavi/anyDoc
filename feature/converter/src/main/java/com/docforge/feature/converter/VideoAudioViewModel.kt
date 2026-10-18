package com.docforge.feature.converter

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.docforge.core.domain.model.ConversionRecord
import com.docforge.core.domain.repository.HistoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class VideoAudioViewModel(
    private val historyRepository: HistoryRepository,
    private val videoAudioExtractor: VideoAudioExtractor
) : ViewModel() {

    private val _uiState = MutableStateFlow(VideoAudioUiState())
    val uiState: StateFlow<VideoAudioUiState> = _uiState.asStateFlow()

    fun onInputSelected(uri: Uri, labelHint: String?) {
        _uiState.update {
            it.copy(
                selectedUri = uri,
                inputLabel = labelHint ?: uri.lastPathSegment ?: uri.toString(),
                sourceMimeType = null,
                durationMs = null,
                isProcessing = false,
                lastOutputPath = null,
                lastOutputSizeBytes = null,
                statusMessage = "Inspecting audio track...",
                errorMessage = null
            )
        }

        viewModelScope.launch {
            runCatching { videoAudioExtractor.inspectAudioTrack(uri) }
                .onSuccess { info ->
                    _uiState.update {
                        it.copy(
                            sourceMimeType = info.mimeType,
                            durationMs = info.durationMs,
                            statusMessage = "Audio track ready (${info.mimeType})",
                            errorMessage = null
                        )
                    }
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(
                            sourceMimeType = null,
                            durationMs = null,
                            statusMessage = null,
                            errorMessage = err.message ?: "Failed to inspect audio track"
                        )
                    }
                }
        }
    }

    fun onOutputBaseNameChanged(value: String) {
        _uiState.update { it.copy(outputBaseName = value) }
    }

    fun onOutputFormatChanged(format: AudioOutputFormat) {
        _uiState.update { it.copy(outputFormat = format) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun extractAudio() {
        val state = _uiState.value
        val uri = state.selectedUri
        if (uri == null) {
            _uiState.update { it.copy(errorMessage = "Select one video first.") }
            return
        }
        if (state.isProcessing) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isProcessing = true,
                    statusMessage = "Extracting audio...",
                    errorMessage = null,
                    lastOutputPath = null,
                    lastOutputSizeBytes = null
                )
            }

            runCatching {
                videoAudioExtractor.extractAudio(
                    inputUri = uri,
                    outputBaseName = state.outputBaseName,
                    outputFormat = state.outputFormat
                )
            }.onSuccess { result ->
                historyRepository.insert(
                    ConversionRecord(
                        sourceLabel = state.inputLabel ?: "Video",
                        outputPath = result.outputFile.absolutePath,
                        operation = "Video -> ${result.outputFormat.name}",
                        createdAtMillis = System.currentTimeMillis(),
                        inputCount = 1,
                        outputSizeBytes = result.outputSizeBytes
                    )
                )

                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        sourceMimeType = result.sourceMimeType,
                        durationMs = result.durationMs,
                        lastOutputPath = result.outputFile.absolutePath,
                        lastOutputSizeBytes = result.outputSizeBytes,
                        statusMessage = "Audio extracted as ${result.outputFormat.name}",
                        errorMessage = null
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        statusMessage = null,
                        errorMessage = err.message ?: "Audio extraction failed"
                    )
                }
            }
        }
    }
}

class VideoAudioViewModelFactory(
    private val historyRepository: HistoryRepository,
    private val videoAudioExtractor: VideoAudioExtractor
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(VideoAudioViewModel::class.java)) {
            return VideoAudioViewModel(historyRepository, videoAudioExtractor) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
