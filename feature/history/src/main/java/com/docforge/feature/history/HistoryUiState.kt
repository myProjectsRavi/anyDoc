package com.docforge.feature.history

import com.docforge.core.domain.model.ConversionRecord

data class HistoryUiState(
    val records: List<ConversionRecord> = emptyList()
)
