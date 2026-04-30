package com.docforge.feature.pdftools

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.docforge.core.domain.model.ConversionRecord
import com.docforge.core.domain.repository.HistoryRepository
import com.docforge.core.pdf.PdfSigner
import com.docforge.core.pdf.PdfSignaturePlacement
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PdfSignViewModel(
    private val historyRepository: HistoryRepository,
    private val pdfSigner: PdfSigner,
    private val savedSignatureStore: SavedSignatureStore,
    private val placementTemplateStore: SignaturePlacementTemplateStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(PdfSignUiState())
    val uiState: StateFlow<PdfSignUiState> = _uiState.asStateFlow()

    init {
        refreshSavedSignatureSlots()
        refreshPlacementTemplates()
    }

    fun onInputSelected(uri: Uri, labelHint: String?) {
        _uiState.update {
            it.copy(
                selectedUri = uri,
                inputLabel = labelHint ?: uri.lastPathSegment ?: uri.toString(),
                pageCount = null,
                templateNameInput = "",
                placements = emptyList(),
                statusMessage = "Loading PDF details...",
                errorMessage = null,
                lastOutputPath = null,
                lastOutputSizeBytes = null
            )
        }

        viewModelScope.launch {
            runCatching { pdfSigner.getPageCount(uri) }
                .onSuccess { count ->
                    _uiState.update {
                        it.copy(
                            pageCount = count,
                            targetPageInput = "1",
                            statusMessage = "Loaded PDF ($count page(s))",
                            errorMessage = null
                        )
                    }
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(
                            pageCount = null,
                            statusMessage = null,
                            errorMessage = err.message ?: "Failed to read PDF"
                        )
                    }
                }
        }
    }

    fun onOutputNameChanged(value: String) {
        _uiState.update { it.copy(outputName = value) }
    }

    fun onTargetPageChanged(value: String) {
        _uiState.update { it.copy(targetPageInput = value) }
    }

    fun onXRatioChanged(value: String) {
        _uiState.update { it.copy(xRatioInput = value) }
    }

    fun onYRatioChanged(value: String) {
        _uiState.update { it.copy(yRatioInput = value) }
    }

    fun onWidthRatioChanged(value: String) {
        _uiState.update { it.copy(widthRatioInput = value) }
    }

    fun onTemplateNameChanged(value: String) {
        _uiState.update { it.copy(templateNameInput = value) }
    }

    fun addPlacementFromInputs() {
        val state = _uiState.value
        val placement = parsePlacementFromInputs(state)
        if (placement == null) {
            _uiState.update { it.copy(errorMessage = "Enter valid page/X/Y/width values before adding placement.") }
            return
        }
        val limit = 24
        if (state.placements.size >= limit) {
            _uiState.update { it.copy(errorMessage = "Placement limit reached ($limit).") }
            return
        }

        _uiState.update {
            it.copy(
                placements = it.placements + placement,
                statusMessage = "Added placement #${it.placements.size + 1}",
                errorMessage = null
            )
        }
    }

    fun applyPlacementToAllPagesFromInputs() {
        val state = _uiState.value
        val basePlacement = parsePlacementFromInputs(state)
        if (basePlacement == null) {
            _uiState.update { it.copy(errorMessage = "Enter valid page/X/Y/width values before applying to all pages.") }
            return
        }
        val pageCount = state.pageCount
        if (pageCount == null || pageCount <= 0) {
            _uiState.update { it.copy(errorMessage = "Load a PDF first to apply placements across pages.") }
            return
        }
        val limit = 120
        if (pageCount > limit) {
            _uiState.update { it.copy(errorMessage = "For safety, apply-to-all supports up to $limit pages per action.") }
            return
        }

        val baseByPage = (1..pageCount).associateWith { page ->
            basePlacement.copy(pageOneBased = page)
        }
        val carryForward = state.placements
            .filter { placement -> placement.pageOneBased !in 1..pageCount }
        val updated = buildList {
            addAll(carryForward)
            addAll((1..pageCount).map { page -> baseByPage.getValue(page) })
        }
        _uiState.update {
            it.copy(
                placements = updated,
                statusMessage = "Applied placement template to $pageCount page(s)",
                errorMessage = null
            )
        }
    }

    fun removePlacementAt(index: Int) {
        val state = _uiState.value
        if (index !in state.placements.indices) return
        _uiState.update {
            val updated = it.placements.toMutableList()
            updated.removeAt(index)
            it.copy(
                placements = updated,
                statusMessage = "Removed placement ${index + 1}",
                errorMessage = null
            )
        }
    }

    fun clearPlacements() {
        if (_uiState.value.placements.isEmpty()) return
        _uiState.update {
            it.copy(
                placements = emptyList(),
                statusMessage = "Cleared all placements",
                errorMessage = null
            )
        }
    }

    fun savePlacementTemplate() {
        val state = _uiState.value
        val name = state.templateNameInput.trim()
        if (name.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Template name is required.") }
            return
        }

        val placements = if (state.placements.isNotEmpty()) {
            state.placements
        } else {
            parsePlacementFromInputs(state)?.let { listOf(it) }.orEmpty()
        }
        if (placements.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Add at least one placement before saving template.") }
            return
        }

        runCatching {
            placementTemplateStore.saveTemplate(name, placements)
        }.onSuccess {
            refreshPlacementTemplates()
            _uiState.update {
                it.copy(
                    statusMessage = "Saved template \"$name\" (${placements.size} placement(s))",
                    errorMessage = null
                )
            }
        }.onFailure { err ->
            _uiState.update { it.copy(errorMessage = err.message ?: "Failed to save placement template.") }
        }
    }

    fun loadPlacementTemplate(name: String) {
        val placements = runCatching { placementTemplateStore.loadTemplate(name) }
            .getOrNull()
            .orEmpty()
        if (placements.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Template \"$name\" has no placements or does not exist.") }
            return
        }

        val first = placements.first()
        _uiState.update {
            it.copy(
                templateNameInput = name,
                placements = placements,
                targetPageInput = first.pageOneBased.toString(),
                xRatioInput = formatRatio(first.xRatio),
                yRatioInput = formatRatio(first.yRatio),
                widthRatioInput = formatRatio(first.widthRatio),
                statusMessage = "Loaded template \"$name\" (${placements.size} placement(s))",
                errorMessage = null
            )
        }
    }

    fun deletePlacementTemplate(name: String) {
        runCatching {
            placementTemplateStore.deleteTemplate(name)
        }.onSuccess {
            refreshPlacementTemplates()
            _uiState.update {
                it.copy(
                    statusMessage = "Deleted template \"$name\"",
                    errorMessage = null
                )
            }
        }.onFailure { err ->
            _uiState.update { it.copy(errorMessage = err.message ?: "Failed to delete placement template.") }
        }
    }

    fun loadPlacementIntoInputs(index: Int) {
        val placement = _uiState.value.placements.getOrNull(index) ?: return
        _uiState.update {
            it.copy(
                targetPageInput = placement.pageOneBased.toString(),
                xRatioInput = formatRatio(placement.xRatio),
                yRatioInput = formatRatio(placement.yRatio),
                widthRatioInput = formatRatio(placement.widthRatio),
                statusMessage = "Loaded placement ${index + 1} into editor",
                errorMessage = null
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun setError(message: String) {
        _uiState.update { it.copy(errorMessage = message) }
    }

    fun saveSignatureToSlot(slot: Int, signatureBitmap: Bitmap) {
        if (_uiState.value.isProcessing) {
            signatureBitmap.recycle()
            return
        }
        viewModelScope.launch {
            runCatching {
                savedSignatureStore.save(slot, signatureBitmap)
            }.onSuccess {
                refreshSavedSignatureSlots()
                _uiState.update { it.copy(statusMessage = "Saved signature to slot $slot", errorMessage = null) }
            }.onFailure { err ->
                _uiState.update { it.copy(errorMessage = err.message ?: "Failed to save signature slot.") }
            }
            if (!signatureBitmap.isRecycled) {
                signatureBitmap.recycle()
            }
        }
    }

    fun deleteSignatureSlot(slot: Int) {
        if (_uiState.value.isProcessing) return
        runCatching {
            savedSignatureStore.delete(slot)
        }.onSuccess {
            refreshSavedSignatureSlots()
            _uiState.update { it.copy(statusMessage = "Deleted signature slot $slot", errorMessage = null) }
        }.onFailure { err ->
            _uiState.update { it.copy(errorMessage = err.message ?: "Failed to delete signature slot.") }
        }
    }

    fun signWithSavedSignature(slot: Int) {
        val state = _uiState.value
        if (state.isProcessing) return
        val signatureBitmap = runCatching { savedSignatureStore.load(slot) }.getOrNull()
        if (signatureBitmap == null) {
            _uiState.update { it.copy(errorMessage = "No saved signature found in slot $slot.") }
            return
        }
        signPdf(signatureBitmap)
    }

    fun signPdf(signatureBitmap: Bitmap) {
        val state = _uiState.value
        val uri = state.selectedUri
        if (uri == null) {
            _uiState.update { it.copy(errorMessage = "Select one PDF first.") }
            return
        }
        if (state.isProcessing) return

        val placementsToApply = if (state.placements.isNotEmpty()) {
            state.placements.map { placement ->
                PdfSignaturePlacement(
                    targetPageOneBased = placement.pageOneBased,
                    xRatio = placement.xRatio,
                    yRatio = placement.yRatio,
                    widthRatio = placement.widthRatio
                )
            }
        } else {
            val parsedPlacement = parsePlacementFromInputs(state)
            if (parsedPlacement == null) {
                _uiState.update { it.copy(errorMessage = "Enter valid numeric signature position values.") }
                return
            }
            listOf(
                PdfSignaturePlacement(
                    targetPageOneBased = parsedPlacement.pageOneBased,
                    xRatio = parsedPlacement.xRatio,
                    yRatio = parsedPlacement.yRatio,
                    widthRatio = parsedPlacement.widthRatio
                )
            )
        }
        val pageCount = state.pageCount
        if (pageCount != null && placementsToApply.any { it.targetPageOneBased !in 1..pageCount }) {
            _uiState.update { it.copy(errorMessage = "One or more placements reference pages outside this PDF.") }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isProcessing = true,
                    statusMessage = "Applying ${placementsToApply.size} signature placement(s)...",
                    errorMessage = null,
                    lastOutputPath = null,
                    lastOutputSizeBytes = null
                )
            }

            try {
                runCatching {
                    pdfSigner.signMultiple(
                        inputUri = uri,
                        outputName = state.outputName,
                        signatureBitmap = signatureBitmap,
                        placements = placementsToApply
                    )
                }.onSuccess { result ->
                    val operation = if (placementsToApply.size > 1) "PDF Sign Multi" else "PDF Sign"
                    historyRepository.insert(
                        ConversionRecord(
                            sourceLabel = "Signed ${placementsToApply.size} placement(s)",
                            outputPath = result.outputFile.absolutePath,
                            operation = operation,
                            createdAtMillis = System.currentTimeMillis(),
                            inputCount = placementsToApply.size,
                            outputSizeBytes = result.outputSizeBytes
                        )
                    )
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            lastOutputPath = result.outputFile.absolutePath,
                            lastOutputSizeBytes = result.outputSizeBytes,
                            statusMessage = "Signed PDF saved with ${placementsToApply.size} placement(s)",
                            errorMessage = null
                        )
                    }
                }.onFailure { err ->
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            statusMessage = null,
                            errorMessage = err.message ?: "Signing failed"
                        )
                    }
                }
            } finally {
                if (!signatureBitmap.isRecycled) {
                    signatureBitmap.recycle()
                }
            }
        }
    }

    private fun refreshSavedSignatureSlots() {
        val slots = savedSignatureStore.listSlots()
            .map { slot ->
                PdfSavedSignatureSlotUi(
                    slot = slot.slot,
                    exists = slot.exists,
                    updatedAtMillis = slot.updatedAtMillis
                )
            }
        _uiState.update { it.copy(savedSignatureSlots = slots) }
    }

    private fun refreshPlacementTemplates() {
        val templates = placementTemplateStore.listTemplates()
            .map { template ->
                PdfPlacementTemplateUi(
                    name = template.name,
                    placementCount = template.placementCount,
                    updatedAtMillis = template.updatedAtMillis
                )
            }
        _uiState.update { it.copy(placementTemplates = templates) }
    }

    private fun parsePlacementFromInputs(state: PdfSignUiState): PdfSignaturePlacementUi? {
        val page = state.targetPageInput.toIntOrNull() ?: return null
        val x = state.xRatioInput.toFloatOrNull() ?: return null
        val y = state.yRatioInput.toFloatOrNull() ?: return null
        val width = state.widthRatioInput.toFloatOrNull() ?: return null
        if (page <= 0) return null
        if (x !in 0f..1f || y !in 0f..1f) return null
        if (width <= 0f) return null

        return PdfSignaturePlacementUi(
            pageOneBased = page,
            xRatio = x.coerceIn(0f, 1f),
            yRatio = y.coerceIn(0f, 1f),
            widthRatio = width.coerceIn(0.1f, 0.8f)
        )
    }

    private fun formatRatio(value: Float): String {
        return "%.3f".format(java.util.Locale.US, value)
    }
}

class PdfSignViewModelFactory(
    private val historyRepository: HistoryRepository,
    private val pdfSigner: PdfSigner,
    private val savedSignatureStore: SavedSignatureStore,
    private val placementTemplateStore: SignaturePlacementTemplateStore
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PdfSignViewModel::class.java)) {
            return PdfSignViewModel(
                historyRepository = historyRepository,
                pdfSigner = pdfSigner,
                savedSignatureStore = savedSignatureStore,
                placementTemplateStore = placementTemplateStore
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
