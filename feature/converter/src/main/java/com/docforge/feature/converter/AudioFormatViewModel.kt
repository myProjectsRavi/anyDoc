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

class AudioFormatViewModel(
    private val historyRepository: HistoryRepository,
    private val audioFormatConverter: AudioFormatConverter
) : ViewModel() {

    private val _uiState = MutableStateFlow(AudioFormatUiState())
    val uiState: StateFlow<AudioFormatUiState> = _uiState.asStateFlow()

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
            runCatching { audioFormatConverter.inspectAudioTrack(uri) }
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

    fun onOutputFormatChanged(format: AudioConvertOutputFormat) {
        _uiState.update { it.copy(outputFormat = format) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun convertAudio() {
        val state = _uiState.value
        val uri = state.selectedUri
        if (uri == null) {
            _uiState.update { it.copy(errorMessage = "Select one audio file first.") }
            return
        }
        if (state.isProcessing) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isProcessing = true,
                    statusMessage = "Converting audio...",
                    errorMessage = null,
                    lastOutputPath = null,
                    lastOutputSizeBytes = null
                )
            }

            runCatching {
                audioFormatConverter.convert(
                    inputUri = uri,
                    outputBaseName = state.outputBaseName,
                    outputFormat = state.outputFormat
                )
            }.onSuccess { result ->
                historyRepository.insert(
                    ConversionRecord(
                        sourceLabel = state.inputLabel ?: "Audio",
                        outputPath = result.outputFile.absolutePath,
                        operation = "Audio -> ${result.outputFormat.name}",
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
                        statusMessage = "Converted audio to ${result.outputFormat.name}",
                        errorMessage = null
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        statusMessage = null,
                        errorMessage = err.message ?: "Audio conversion failed"
                    )
                }
            }
        }
    }
}

class AudioFormatViewModelFactory(
    private val historyRepository: HistoryRepository,
    private val audioFormatConverter: AudioFormatConverter
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AudioFormatViewModel::class.java)) {
            return AudioFormatViewModel(historyRepository, audioFormatConverter) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
