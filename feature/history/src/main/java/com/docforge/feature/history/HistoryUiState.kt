package com.docforge.feature.history

import androidx.compose.runtime.Immutable
import com.docforge.core.domain.model.ConversionRecord

@Immutable
data class HistoryUiState(
    val records: List<ConversionRecord> = emptyList()
)
