package com.docforge.core.storage.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "batch_presets")
data class BatchPresetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val createdAtMillis: Long,
    /** JSON-encoded task list */
    val tasksJson: String
)
