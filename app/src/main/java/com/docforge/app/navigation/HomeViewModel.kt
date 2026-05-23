package com.docforge.app.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.Immutable
import com.docforge.core.domain.model.ConversionRecord
import com.docforge.core.domain.repository.HistoryRepository
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class HomeToolId {
    IMAGES_TO_PDF,
    IMAGE_FORMAT,
    AUDIO_FORMAT,
    DOC_TO_PDF,
    TEXT_TO_PDF,
    VIDEO_TO_AUDIO,
    BATCH_QUEUE,
    HISTORY,
    SETTINGS,
    SCANNER,
    PDF_MERGE,
    PDF_SPLIT,
    PDF_SIGN,
    PDF_ANNOTATE,
    PDF_PASSWORD,
    PDF_COMPRESS,
    PDF_TEXT,
    PDF_TO_IMAGES,
    PDF_BATCH_STAMP,
    PDF_OCR,
    PDF_FORM,
    ID_CARD,
    PDF_REDACT
}

data class HomeTool(
    val id: HomeToolId,
    val title: String,
    val subtitle: String,
    val keywords: String,
    val onClick: () -> Unit
)

@Immutable
data class HomeRecentItem(
    val id: Long,
    val title: String,
    val subtitle: String
)

@Immutable
data class HomeUiState(
    val quickActionToolIds: ImmutableList<HomeToolId> = persistentListOf(),
    val recentItems: ImmutableList<HomeRecentItem> = persistentListOf()
)

class HomeViewModel(
    private val historyRepository: HistoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            historyRepository.observeRecent(limit = 40).collect { records ->
                val mapped = records
                    .mapNotNull { record -> operationToToolId(record.operation) }
                    .distinct()
                    .take(4)
                val now = System.currentTimeMillis()
                val recents = records
                    .take(8)
                    .map { record ->
                        HomeRecentItem(
                            id = record.id,
                            title = recentTitle(record),
                            subtitle = "${record.operation} • ${formatRelativeTime(record.createdAtMillis, now)}"
                        )
                    }

                _uiState.update {
                    it.copy(
                        quickActionToolIds = mapped.toPersistentList(),
                        recentItems = recents.toPersistentList()
                    )
                }
            }
        }
    }

    private fun recentTitle(record: ConversionRecord): String {
        val explicitName = record.displayName?.trim().orEmpty()
        if (explicitName.isNotEmpty()) return explicitName

        val outputName = record.outputPath.substringAfterLast('/').trim()
        if (outputName.isNotEmpty()) return outputName

        return record.sourceLabel.trim().ifEmpty { "Conversion #${record.id}" }
    }

    private fun formatRelativeTime(timestampMillis: Long, nowMillis: Long): String {
        val deltaMillis = (nowMillis - timestampMillis).coerceAtLeast(0L)
        val minuteMillis = 60_000L
        val hourMillis = 60 * minuteMillis
        val dayMillis = 24 * hourMillis

        return when {
            deltaMillis < minuteMillis -> "Just now"
            deltaMillis < hourMillis -> "${deltaMillis / minuteMillis}m ago"
            deltaMillis < dayMillis -> "${deltaMillis / hourMillis}h ago"
            deltaMillis < 7 * dayMillis -> "${deltaMillis / dayMillis}d ago"
            else -> "Earlier"
        }
    }

    private fun operationToToolId(operation: String): HomeToolId? {
        return when {
            operation.startsWith("Scan ->") -> HomeToolId.SCANNER
            operation == "Images -> PDF" -> HomeToolId.IMAGES_TO_PDF
            operation.startsWith("Images ->") -> HomeToolId.IMAGE_FORMAT
            operation.startsWith("Audio ->") -> HomeToolId.AUDIO_FORMAT
            operation.startsWith("Video ->") -> HomeToolId.VIDEO_TO_AUDIO
            operation == "PDF Merge" -> HomeToolId.PDF_MERGE
            operation.startsWith("PDF Split") ||
                operation.startsWith("PDF Extract") ||
                operation == "PDF Reorder Pages" ||
                operation == "PDF Delete Pages" ||
                operation == "PDF Rotate Pages" -> HomeToolId.PDF_SPLIT
            operation == "PDF Sign" -> HomeToolId.PDF_SIGN
            operation == "PDF Annotate" -> HomeToolId.PDF_ANNOTATE
            operation == "PDF Protect" || operation == "PDF Unlock" -> HomeToolId.PDF_PASSWORD
            operation == "PDF Compress" -> HomeToolId.PDF_COMPRESS
            operation == "PDF -> TXT" -> HomeToolId.PDF_TEXT
            operation.startsWith("PDF ->") -> HomeToolId.PDF_TO_IMAGES
            operation == "PDF Batch Stamp" -> HomeToolId.PDF_BATCH_STAMP
            operation == "PDF OCR" -> HomeToolId.PDF_OCR
            operation == "PDF Form Fill" || operation == "PDF Form Builder" -> HomeToolId.PDF_FORM
            operation == "ID Card -> PDF" -> HomeToolId.ID_CARD
            operation == "PDF Redact" -> HomeToolId.PDF_REDACT
            operation == "Text -> PDF" -> HomeToolId.TEXT_TO_PDF
            operation.endsWith("-> PDF") -> HomeToolId.DOC_TO_PDF
            else -> null
        }
    }
}

class HomeViewModelFactory(
    private val historyRepository: HistoryRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            return HomeViewModel(historyRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
