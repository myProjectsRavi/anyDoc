package com.docforge.feature.pdftools

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.docforge.core.domain.model.ConversionRecord
import com.docforge.core.domain.repository.HistoryRepository
import com.docforge.core.pdf.PdfFormTool
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PdfFormViewModel(
    private val historyRepository: HistoryRepository,
    private val formTool: PdfFormTool
) : ViewModel() {

    private val _uiState = MutableStateFlow(PdfFormUiState())
    val uiState: StateFlow<PdfFormUiState> = _uiState.asStateFlow()

    fun onInputSelected(uri: Uri, labelHint: String?) {
        _uiState.update {
            it.copy(
                selectedUri = uri,
                inputLabel = labelHint ?: uri.lastPathSegment ?: uri.toString(),
                fields = emptyList(),
                selectedFieldName = null,
                fieldEdits = emptyMap(),
                statusMessage = "Loading form fields...",
                errorMessage = null,
                lastOutputPath = null,
                lastOutputSizeBytes = null
            )
        }

        viewModelScope.launch {
            runCatching { formTool.listFields(uri) }
                .onSuccess { fields ->
                    val initialEdits = fields.associate { field ->
                        field.name to normalizeInitialDraftValue(field)
                    }
                    _uiState.update {
                        it.copy(
                            fields = fields,
                            selectedFieldName = fields.firstOrNull()?.name,
                            fieldEdits = initialEdits,
                            statusMessage = if (fields.isEmpty()) {
                                "No fillable fields found. You can add one below."
                            } else {
                                "Loaded ${fields.size} field(s). Tap a field to edit."
                            },
                            errorMessage = null
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            statusMessage = null,
                            errorMessage = error.message ?: "Unable to load fields"
                        )
                    }
                }
        }
    }

    fun onOutputNameChanged(value: String) = _uiState.update { it.copy(outputName = value) }
    fun onFlattenChanged(value: Boolean) = _uiState.update { it.copy(flattenAfterFill = value) }
    fun onFieldSelected(name: String) = _uiState.update { it.copy(selectedFieldName = name) }
    fun onFieldValueChanged(name: String, value: String) {
        _uiState.update { state ->
            state.copy(fieldEdits = state.fieldEdits.toMutableMap().apply { put(name, value) })
        }
    }
    fun onFieldBooleanChanged(name: String, checked: Boolean) {
        onFieldValueChanged(name, if (checked) "true" else "false")
    }

    fun onBuilderFieldNameChanged(value: String) = _uiState.update { it.copy(builderFieldName = value) }
    fun onBuilderPageChanged(value: String) = _uiState.update { it.copy(builderPage = value) }
    fun onBuilderXChanged(value: String) = _uiState.update { it.copy(builderXRatio = value) }
    fun onBuilderYChanged(value: String) = _uiState.update { it.copy(builderYRatio = value) }
    fun onBuilderWidthChanged(value: String) = _uiState.update { it.copy(builderWidthRatio = value) }
    fun onBuilderHeightChanged(value: String) = _uiState.update { it.copy(builderHeightRatio = value) }
    fun onBuilderDefaultChanged(value: String) = _uiState.update { it.copy(builderDefaultValue = value) }

    fun clearError() = _uiState.update { it.copy(errorMessage = null) }

    fun fillFields() {
        val state = _uiState.value
        val uri = state.selectedUri
        if (uri == null) {
            _uiState.update { it.copy(errorMessage = "Select a PDF first.") }
            return
        }
        if (state.isProcessing) return

        val updates = state.fieldEdits
            .filterKeys { name -> state.fields.any { field -> field.name == name } }
            .mapValues { (_, value) -> value.trim() }
        if (updates.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "No editable fields detected in this PDF.") }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isProcessing = true,
                    statusMessage = "Applying form values...",
                    errorMessage = null,
                    lastOutputPath = null,
                    lastOutputSizeBytes = null
                )
            }

            runCatching {
                formTool.fillFields(
                    inputUri = uri,
                    outputName = state.outputName,
                    values = updates,
                    flatten = state.flattenAfterFill
                )
            }.onSuccess { result ->
                historyRepository.insert(
                    ConversionRecord(
                        sourceLabel = state.inputLabel ?: "Form PDF",
                        outputPath = result.outputFile.absolutePath,
                        operation = "PDF Form Fill",
                        createdAtMillis = System.currentTimeMillis(),
                        inputCount = 1,
                        outputSizeBytes = result.outputSizeBytes
                    )
                )
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        statusMessage = "Updated ${result.updatedFieldCount} field(s)",
                        errorMessage = null,
                        lastOutputPath = result.outputFile.absolutePath,
                        lastOutputSizeBytes = result.outputSizeBytes
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        statusMessage = null,
                        errorMessage = error.message ?: "Form fill failed"
                    )
                }
            }
        }
    }

    fun addTextField() {
        val state = _uiState.value
        val uri = state.selectedUri
        if (uri == null) {
            _uiState.update { it.copy(errorMessage = "Select a PDF first.") }
            return
        }
        if (state.isProcessing) return

        val page = state.builderPage.toIntOrNull()
        val x = state.builderXRatio.toFloatOrNull()
        val y = state.builderYRatio.toFloatOrNull()
        val width = state.builderWidthRatio.toFloatOrNull()
        val height = state.builderHeightRatio.toFloatOrNull()

        if (page == null || x == null || y == null || width == null || height == null) {
            _uiState.update { it.copy(errorMessage = "Builder values are invalid. Use numeric ratios.") }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isProcessing = true,
                    statusMessage = "Adding new text field...",
                    errorMessage = null,
                    lastOutputPath = null,
                    lastOutputSizeBytes = null
                )
            }

            runCatching {
                formTool.addTextField(
                    inputUri = uri,
                    outputName = state.outputName,
                    fieldName = state.builderFieldName,
                    pageOneBased = page,
                    xRatio = x,
                    yRatio = y,
                    widthRatio = width,
                    heightRatio = height,
                    defaultValue = state.builderDefaultValue
                )
            }.onSuccess { result ->
                historyRepository.insert(
                    ConversionRecord(
                        sourceLabel = state.inputLabel ?: "Form PDF",
                        outputPath = result.outputFile.absolutePath,
                        operation = "PDF Form Builder",
                        createdAtMillis = System.currentTimeMillis(),
                        inputCount = 1,
                        outputSizeBytes = result.outputSizeBytes
                    )
                )
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        statusMessage = "Created field '${result.createdFieldName}'",
                        errorMessage = null,
                        lastOutputPath = result.outputFile.absolutePath,
                        lastOutputSizeBytes = result.outputSizeBytes
                    )
                }
                onInputSelected(uri, state.inputLabel)
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        statusMessage = null,
                        errorMessage = error.message ?: "Field creation failed"
                    )
                }
            }
        }
    }

    private fun normalizeInitialDraftValue(field: com.docforge.core.pdf.PdfFormFieldInfo): String {
        if (field.type != com.docforge.core.pdf.PdfFormFieldType.CHECKBOX) {
            return field.value
        }
        val current = field.value.trim()
        val isChecked = current.equals("true", ignoreCase = true) ||
            current.equals("yes", ignoreCase = true) ||
            current.equals("on", ignoreCase = true) ||
            current == "1" ||
            (field.options.any { option -> option.equals(current, ignoreCase = true) })
        return if (isChecked) "true" else "false"
    }
}

class PdfFormViewModelFactory(
    private val historyRepository: HistoryRepository,
    private val formTool: PdfFormTool
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PdfFormViewModel::class.java)) {
            return PdfFormViewModel(historyRepository, formTool) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
