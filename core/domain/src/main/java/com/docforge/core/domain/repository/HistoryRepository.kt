package com.docforge.core.domain.repository

import com.docforge.core.domain.model.ConversionRecord
import kotlinx.coroutines.flow.Flow

interface HistoryRepository {
    fun observeRecent(limit: Int = 20): Flow<List<ConversionRecord>>
    suspend fun insert(record: ConversionRecord)
}
