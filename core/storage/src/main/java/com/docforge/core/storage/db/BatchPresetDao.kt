package com.docforge.core.storage.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface BatchPresetDao {
    @Query("SELECT * FROM batch_presets ORDER BY createdAtMillis DESC")
    suspend fun getAll(): List<BatchPresetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(preset: BatchPresetEntity): Long

    @Query("DELETE FROM batch_presets WHERE id = :id")
    suspend fun deleteById(id: Long): Int
}
