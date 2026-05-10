package com.docforge.core.storage.db

import androidx.room.ColumnInfo
import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

/**
 * Persists the batch queue across process kills.
 *
 * Sprint 4 feature — Batch Queue Persistence.
 */
@Entity(tableName = "batch_queue_tasks")
data class BatchQueueTaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    @ColumnInfo(name = "task_type")
    val taskType: String,

    @ColumnInfo(name = "input_uris_json")
    val inputUrisJson: String,

    @ColumnInfo(name = "input_summary")
    val inputSummary: String,

    @ColumnInfo(name = "output_base_name")
    val outputBaseName: String,

    @ColumnInfo(name = "status")
    val status: String,

    @ColumnInfo(name = "output_path")
    val outputPath: String? = null,

    @ColumnInfo(name = "output_size_bytes")
    val outputSizeBytes: Long = 0L,

    @ColumnInfo(name = "error_message")
    val errorMessage: String? = null,

    @ColumnInfo(name = "created_at_millis")
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Dao
interface BatchQueueTaskDao {

    @Query("SELECT * FROM batch_queue_tasks ORDER BY created_at_millis ASC")
    suspend fun getAll(): List<BatchQueueTaskEntity>

    /** Paging 3 source for large task queues. */
    @Query("SELECT * FROM batch_queue_tasks ORDER BY created_at_millis ASC")
    fun observePaged(): PagingSource<Int, BatchQueueTaskEntity>

    @Query("SELECT * FROM batch_queue_tasks WHERE status = :status ORDER BY created_at_millis ASC")
    suspend fun getByStatus(status: String): List<BatchQueueTaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tasks: List<BatchQueueTaskEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: BatchQueueTaskEntity): Long

    @Query("UPDATE batch_queue_tasks SET status = :status, error_message = :errorMessage WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String, errorMessage: String? = null)

    @Query("UPDATE batch_queue_tasks SET status = :status, output_path = :outputPath, output_size_bytes = :sizeBytes WHERE id = :id")
    suspend fun markSuccess(id: Long, status: String, outputPath: String, sizeBytes: Long)

    @Query("DELETE FROM batch_queue_tasks")
    suspend fun deleteAll()

    @Query("DELETE FROM batch_queue_tasks WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM batch_queue_tasks WHERE status IN ('SUCCESS', 'FAILED', 'CANCELED')")
    suspend fun deleteCompleted()
}
