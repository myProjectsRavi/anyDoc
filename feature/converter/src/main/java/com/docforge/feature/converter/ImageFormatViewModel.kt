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

class ImageFormatViewModel(
    private val historyRepository: HistoryRepository,
    private val imageFormatConverter: ImageFormatConverter,
    defaultQuality: Int = 90
) : ViewModel() {

    private val _uiState = MutableStateFlow(ImageFormatUiState(qualityInput = defaultQuality.coerceIn(10, 100).toString()))
    val uiState: StateFlow<ImageFormatUiState> = _uiState.asStateFlow()

    fun onImagesSelected(uris: List<Uri>) {
        _uiState.update {
            it.copy(
                selectedUris = uris,
                statusMessage = if (uris.isNotEmpty()) "${uris.size} image(s) selected" else null,
                errorMessage = null,
                lastOutputPaths = emptyList(),
                lastOutputSizeBytes = null
            )
        }
    }

    fun onOutputBaseNameChanged(value: String) {
        _uiState.update { it.copy(outputBaseName = value) }
    }

    fun onOutputFormatChanged(format: ImageOutputFormat) {
        _uiState.update { it.copy(outputFormat = format) }
    }

    fun onQualityChanged(value: String) {
        _uiState.update { it.copy(qualityInput = value) }
    }

    fun onScaleChanged(value: String) {
        _uiState.update { it.copy(scaleInput = value) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun convert() {
        val state = _uiState.value
        if (state.selectedUris.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Select at least one image first.") }
            return
        }
        if (state.isConverting) return

        val quality = state.qualityInput.toIntOrNull()
        val scale = state.scaleInput.toFloatOrNull()
        if (quality == null || scale == null) {
            _uiState.update { it.copy(errorMessage = "Enter numeric quality and scale values.") }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isConverting = true,
                    statusMessage = "Converting images...",
                    errorMessage = null,
                    lastOutputPaths = emptyList(),
                    lastOutputSizeBytes = null
                )
            }

            runCatching {
                imageFormatConverter.convertBatch(
                    inputUris = state.selectedUris,
                    outputBaseName = state.outputBaseName,
                    outputFormat = state.outputFormat,
                    quality = quality,
                    scaleFactor = scale
                )
            }.onSuccess { result ->
                val firstOutput = result.outputFiles.firstOrNull()?.absolutePath.orEmpty()
                historyRepository.insert(
                    ConversionRecord(
                        sourceLabel = "Images (${state.selectedUris.size})",
                        outputPath = firstOutput,
                        operation = "Images -> ${state.outputFormat.name}",
                        createdAtMillis = System.currentTimeMillis(),
                        inputCount = state.selectedUris.size,
                        outputSizeBytes = result.outputSizeBytes
                    )
                )

                _uiState.update {
                    it.copy(
                        isConverting = false,
                        lastOutputPaths = result.outputFiles.map { file -> file.absolutePath },
                        lastOutputSizeBytes = result.outputSizeBytes,
                        statusMessage = "Converted ${result.outputFiles.size} image(s)",
                        errorMessage = null
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isConverting = false,
                        statusMessage = null,
                        errorMessage = err.message ?: "Image conversion failed"
                    )
                }
            }
        }
    }
}

class ImageFormatViewModelFactory(
    private val historyRepository: HistoryRepository,
    private val imageFormatConverter: ImageFormatConverter,
    private val defaultQuality: Int = 90
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ImageFormatViewModel::class.java)) {
            return ImageFormatViewModel(
                historyRepository = historyRepository,
                imageFormatConverter = imageFormatConverter,
                defaultQuality = defaultQuality
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
