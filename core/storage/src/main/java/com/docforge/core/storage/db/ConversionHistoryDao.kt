package com.docforge.core.storage.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversionHistoryDao {

    @Query("SELECT * FROM conversion_history ORDER BY createdAtMillis DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<ConversionHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ConversionHistoryEntity)

    @Query("DELETE FROM conversion_history WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    @Query("DELETE FROM conversion_history")
    suspend fun deleteAll()
}
