package com.docforge.app.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.docforge.app.settings.AppSettings
import com.docforge.app.settings.AppSettingsRepository
import com.docforge.core.pdf.PdfCompressionLevel
import com.docforge.core.pdf.PdfPageSize
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val defaultPdfPageSize: PdfPageSize = PdfPageSize.A4,
    val defaultPdfCompressionLevel: PdfCompressionLevel = PdfCompressionLevel.MEDIUM,
    val defaultImageQualityInput: String = "90",
    val documentsFolderNameInput: String = "DocForge",
    val imagesFolderNameInput: String = "DocForge",
    val audioFolderNameInput: String = "DocForge",
    val statusMessage: String? = null,
    val errorMessage: String? = null
)

class SettingsViewModel(
    private val settingsRepository: AppSettingsRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(settingsRepository.currentSettings().toUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _uiState.update { current ->
                    settings.toUiState(
                        statusMessage = current.statusMessage,
                        errorMessage = current.errorMessage
                    )
                }
            }
        }
    }

    fun onPdfPageSizeChanged(size: PdfPageSize) {
        settingsRepository.setDefaultPdfPageSizeName(size.name)
        _uiState.update { it.copy(statusMessage = "Default PDF page size set to ${size.name}.", errorMessage = null) }
    }

    fun onPdfCompressionChanged(level: PdfCompressionLevel) {
        settingsRepository.setDefaultPdfCompressionName(level.name)
        _uiState.update { it.copy(statusMessage = "Default PDF compression set to ${level.name}.", errorMessage = null) }
    }

    fun onDefaultImageQualityChanged(value: String) {
        _uiState.update { it.copy(defaultImageQualityInput = value, statusMessage = null, errorMessage = null) }
    }

    fun applyDefaultImageQuality() {
        val quality = uiState.value.defaultImageQualityInput.toIntOrNull()
        if (quality == null) {
            _uiState.update { it.copy(errorMessage = "Image quality must be a number between 10 and 100.") }
            return
        }
        settingsRepository.setDefaultImageQuality(quality)
        _uiState.update {
            it.copy(
                defaultImageQualityInput = quality.coerceIn(10, 100).toString(),
                statusMessage = "Default image quality updated.",
                errorMessage = null
            )
        }
    }

    fun onDocumentsFolderNameChanged(value: String) {
        _uiState.update { it.copy(documentsFolderNameInput = value, statusMessage = null, errorMessage = null) }
    }

    fun onImagesFolderNameChanged(value: String) {
        _uiState.update { it.copy(imagesFolderNameInput = value, statusMessage = null, errorMessage = null) }
    }

    fun onAudioFolderNameChanged(value: String) {
        _uiState.update { it.copy(audioFolderNameInput = value, statusMessage = null, errorMessage = null) }
    }

    fun saveOutputFolders() {
        settingsRepository.setDocumentsFolderName(uiState.value.documentsFolderNameInput)
        settingsRepository.setImagesFolderName(uiState.value.imagesFolderNameInput)
        settingsRepository.setAudioFolderName(uiState.value.audioFolderNameInput)
        _uiState.update { it.copy(statusMessage = "Default output folders saved.", errorMessage = null) }
    }

    fun clearMessages() {
        _uiState.update { it.copy(statusMessage = null, errorMessage = null) }
    }
}

class SettingsViewModelFactory(
    private val settingsRepository: AppSettingsRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            return SettingsViewModel(settingsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

private fun AppSettings.toUiState(
    statusMessage: String? = null,
    errorMessage: String? = null
): SettingsUiState {
    val pageSize = PdfPageSize.entries.firstOrNull { it.name == defaultPdfPageSizeName } ?: PdfPageSize.A4
    val compression = PdfCompressionLevel.entries.firstOrNull { it.name == defaultPdfCompressionName }
        ?: PdfCompressionLevel.MEDIUM

    return SettingsUiState(
        defaultPdfPageSize = pageSize,
        defaultPdfCompressionLevel = compression,
        defaultImageQualityInput = defaultImageQuality.toString(),
        documentsFolderNameInput = documentsFolderName,
        imagesFolderNameInput = imagesFolderName,
        audioFolderNameInput = audioFolderName,
        statusMessage = statusMessage,
        errorMessage = errorMessage
    )
}
