package com.docforge.core.storage.repository

import com.docforge.core.domain.model.ConversionRecord
import com.docforge.core.domain.repository.HistoryRepository
import com.docforge.core.storage.db.ConversionHistoryDao
import com.docforge.core.storage.db.ConversionHistoryEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LocalHistoryRepository(
    private val dao: ConversionHistoryDao
) : HistoryRepository {

    override fun observeRecent(limit: Int): Flow<List<ConversionRecord>> {
        return dao.observeRecent(limit).map { rows ->
            rows.map { it.toDomain() }
        }
    }

    override suspend fun insert(record: ConversionRecord) {
        dao.insert(record.toEntity())
    }
}

private fun ConversionHistoryEntity.toDomain(): ConversionRecord {
    return ConversionRecord(
        id = id,
        sourceLabel = sourceLabel,
        outputPath = outputPath,
        operation = operation,
        createdAtMillis = createdAtMillis,
        inputCount = inputCount,
        outputSizeBytes = outputSizeBytes
    )
}

private fun ConversionRecord.toEntity(): ConversionHistoryEntity {
    return ConversionHistoryEntity(
        id = id,
        sourceLabel = sourceLabel,
        outputPath = outputPath,
        operation = operation,
        createdAtMillis = createdAtMillis,
        inputCount = inputCount,
        outputSizeBytes = outputSizeBytes
    )
}
