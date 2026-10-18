package com.docforge.core.storage.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversion_history")
data class ConversionHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sourceLabel: String,
    val outputPath: String,
    val outputUri: String? = null,
    val displayName: String? = null,
    val operation: String,
    val createdAtMillis: Long,
    val inputCount: Int,
    val outputSizeBytes: Long
)
