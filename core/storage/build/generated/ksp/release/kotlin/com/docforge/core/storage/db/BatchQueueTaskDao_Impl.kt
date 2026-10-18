package com.docforge.core.storage.db

import androidx.paging.PagingSource
import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.RoomRawQuery
import androidx.room.paging.LimitOffsetPagingSource
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import javax.`annotation`.processing.Generated
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.mutableListOf
import kotlin.reflect.KClass

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class BatchQueueTaskDao_Impl(
  __db: RoomDatabase,
) : BatchQueueTaskDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfBatchQueueTaskEntity: EntityInsertAdapter<BatchQueueTaskEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfBatchQueueTaskEntity = object :
        EntityInsertAdapter<BatchQueueTaskEntity>() {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `batch_queue_tasks` (`id`,`task_type`,`input_uris_json`,`input_summary`,`output_base_name`,`status`,`output_path`,`output_size_bytes`,`error_message`,`created_at_millis`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: BatchQueueTaskEntity) {
        statement.bindLong(1, entity.id)
        statement.bindText(2, entity.taskType)
        statement.bindText(3, entity.inputUrisJson)
        statement.bindText(4, entity.inputSummary)
        statement.bindText(5, entity.outputBaseName)
        statement.bindText(6, entity.status)
        val _tmpOutputPath: String? = entity.outputPath
        if (_tmpOutputPath == null) {
          statement.bindNull(7)
        } else {
          statement.bindText(7, _tmpOutputPath)
        }
        statement.bindLong(8, entity.outputSizeBytes)
        val _tmpErrorMessage: String? = entity.errorMessage
        if (_tmpErrorMessage == null) {
          statement.bindNull(9)
        } else {
          statement.bindText(9, _tmpErrorMessage)
        }
        statement.bindLong(10, entity.createdAtMillis)
      }
    }
  }

  public override suspend fun insertAll(tasks: List<BatchQueueTaskEntity>): Unit =
      performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfBatchQueueTaskEntity.insert(_connection, tasks)
  }

  public override suspend fun insert(task: BatchQueueTaskEntity): Long = performSuspending(__db,
      false, true) { _connection ->
    val _result: Long = __insertAdapterOfBatchQueueTaskEntity.insertAndReturnId(_connection, task)
    _result
  }

  public override suspend fun getAll(): List<BatchQueueTaskEntity> {
    val _sql: String = "SELECT * FROM batch_queue_tasks ORDER BY created_at_millis ASC"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfTaskType: Int = getColumnIndexOrThrow(_stmt, "task_type")
        val _columnIndexOfInputUrisJson: Int = getColumnIndexOrThrow(_stmt, "input_uris_json")
        val _columnIndexOfInputSummary: Int = getColumnIndexOrThrow(_stmt, "input_summary")
        val _columnIndexOfOutputBaseName: Int = getColumnIndexOrThrow(_stmt, "output_base_name")
        val _columnIndexOfStatus: Int = getColumnIndexOrThrow(_stmt, "status")
        val _columnIndexOfOutputPath: Int = getColumnIndexOrThrow(_stmt, "output_path")
        val _columnIndexOfOutputSizeBytes: Int = getColumnIndexOrThrow(_stmt, "output_size_bytes")
        val _columnIndexOfErrorMessage: Int = getColumnIndexOrThrow(_stmt, "error_message")
        val _columnIndexOfCreatedAtMillis: Int = getColumnIndexOrThrow(_stmt, "created_at_millis")
        val _result: MutableList<BatchQueueTaskEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: BatchQueueTaskEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpTaskType: String
          _tmpTaskType = _stmt.getText(_columnIndexOfTaskType)
          val _tmpInputUrisJson: String
          _tmpInputUrisJson = _stmt.getText(_columnIndexOfInputUrisJson)
          val _tmpInputSummary: String
          _tmpInputSummary = _stmt.getText(_columnIndexOfInputSummary)
          val _tmpOutputBaseName: String
          _tmpOutputBaseName = _stmt.getText(_columnIndexOfOutputBaseName)
          val _tmpStatus: String
          _tmpStatus = _stmt.getText(_columnIndexOfStatus)
          val _tmpOutputPath: String?
          if (_stmt.isNull(_columnIndexOfOutputPath)) {
            _tmpOutputPath = null
          } else {
            _tmpOutputPath = _stmt.getText(_columnIndexOfOutputPath)
          }
          val _tmpOutputSizeBytes: Long
          _tmpOutputSizeBytes = _stmt.getLong(_columnIndexOfOutputSizeBytes)
          val _tmpErrorMessage: String?
          if (_stmt.isNull(_columnIndexOfErrorMessage)) {
            _tmpErrorMessage = null
          } else {
            _tmpErrorMessage = _stmt.getText(_columnIndexOfErrorMessage)
          }
          val _tmpCreatedAtMillis: Long
          _tmpCreatedAtMillis = _stmt.getLong(_columnIndexOfCreatedAtMillis)
          _item =
              BatchQueueTaskEntity(_tmpId,_tmpTaskType,_tmpInputUrisJson,_tmpInputSummary,_tmpOutputBaseName,_tmpStatus,_tmpOutputPath,_tmpOutputSizeBytes,_tmpErrorMessage,_tmpCreatedAtMillis)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observePaged(): PagingSource<Int, BatchQueueTaskEntity> {
    val _sql: String = "SELECT * FROM batch_queue_tasks ORDER BY created_at_millis ASC"
    val _rawQuery: RoomRawQuery = RoomRawQuery(_sql)
    return object : LimitOffsetPagingSource<BatchQueueTaskEntity>(_rawQuery, __db,
        "batch_queue_tasks") {
      protected override suspend fun convertRows(limitOffsetQuery: RoomRawQuery, itemCount: Int):
          List<BatchQueueTaskEntity> = performSuspending(__db, true, false) { _connection ->
        val _stmt: SQLiteStatement = _connection.prepare(limitOffsetQuery.sql)
        limitOffsetQuery.getBindingFunction().invoke(_stmt)
        try {
          val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
          val _columnIndexOfTaskType: Int = getColumnIndexOrThrow(_stmt, "task_type")
          val _columnIndexOfInputUrisJson: Int = getColumnIndexOrThrow(_stmt, "input_uris_json")
          val _columnIndexOfInputSummary: Int = getColumnIndexOrThrow(_stmt, "input_summary")
          val _columnIndexOfOutputBaseName: Int = getColumnIndexOrThrow(_stmt, "output_base_name")
          val _columnIndexOfStatus: Int = getColumnIndexOrThrow(_stmt, "status")
          val _columnIndexOfOutputPath: Int = getColumnIndexOrThrow(_stmt, "output_path")
          val _columnIndexOfOutputSizeBytes: Int = getColumnIndexOrThrow(_stmt, "output_size_bytes")
          val _columnIndexOfErrorMessage: Int = getColumnIndexOrThrow(_stmt, "error_message")
          val _columnIndexOfCreatedAtMillis: Int = getColumnIndexOrThrow(_stmt, "created_at_millis")
          val _result: MutableList<BatchQueueTaskEntity> = mutableListOf()
          while (_stmt.step()) {
            val _item: BatchQueueTaskEntity
            val _tmpId: Long
            _tmpId = _stmt.getLong(_columnIndexOfId)
            val _tmpTaskType: String
            _tmpTaskType = _stmt.getText(_columnIndexOfTaskType)
            val _tmpInputUrisJson: String
            _tmpInputUrisJson = _stmt.getText(_columnIndexOfInputUrisJson)
            val _tmpInputSummary: String
            _tmpInputSummary = _stmt.getText(_columnIndexOfInputSummary)
            val _tmpOutputBaseName: String
            _tmpOutputBaseName = _stmt.getText(_columnIndexOfOutputBaseName)
            val _tmpStatus: String
            _tmpStatus = _stmt.getText(_columnIndexOfStatus)
            val _tmpOutputPath: String?
            if (_stmt.isNull(_columnIndexOfOutputPath)) {
              _tmpOutputPath = null
            } else {
              _tmpOutputPath = _stmt.getText(_columnIndexOfOutputPath)
            }
            val _tmpOutputSizeBytes: Long
            _tmpOutputSizeBytes = _stmt.getLong(_columnIndexOfOutputSizeBytes)
            val _tmpErrorMessage: String?
            if (_stmt.isNull(_columnIndexOfErrorMessage)) {
              _tmpErrorMessage = null
            } else {
              _tmpErrorMessage = _stmt.getText(_columnIndexOfErrorMessage)
            }
            val _tmpCreatedAtMillis: Long
            _tmpCreatedAtMillis = _stmt.getLong(_columnIndexOfCreatedAtMillis)
            _item =
                BatchQueueTaskEntity(_tmpId,_tmpTaskType,_tmpInputUrisJson,_tmpInputSummary,_tmpOutputBaseName,_tmpStatus,_tmpOutputPath,_tmpOutputSizeBytes,_tmpErrorMessage,_tmpCreatedAtMillis)
            _result.add(_item)
          }
          _result
        } finally {
          _stmt.close()
        }
      }
    }
  }

  public override suspend fun getByStatus(status: String): List<BatchQueueTaskEntity> {
    val _sql: String =
        "SELECT * FROM batch_queue_tasks WHERE status = ? ORDER BY created_at_millis ASC"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, status)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfTaskType: Int = getColumnIndexOrThrow(_stmt, "task_type")
        val _columnIndexOfInputUrisJson: Int = getColumnIndexOrThrow(_stmt, "input_uris_json")
        val _columnIndexOfInputSummary: Int = getColumnIndexOrThrow(_stmt, "input_summary")
        val _columnIndexOfOutputBaseName: Int = getColumnIndexOrThrow(_stmt, "output_base_name")
        val _columnIndexOfStatus: Int = getColumnIndexOrThrow(_stmt, "status")
        val _columnIndexOfOutputPath: Int = getColumnIndexOrThrow(_stmt, "output_path")
        val _columnIndexOfOutputSizeBytes: Int = getColumnIndexOrThrow(_stmt, "output_size_bytes")
        val _columnIndexOfErrorMessage: Int = getColumnIndexOrThrow(_stmt, "error_message")
        val _columnIndexOfCreatedAtMillis: Int = getColumnIndexOrThrow(_stmt, "created_at_millis")
        val _result: MutableList<BatchQueueTaskEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: BatchQueueTaskEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpTaskType: String
          _tmpTaskType = _stmt.getText(_columnIndexOfTaskType)
          val _tmpInputUrisJson: String
          _tmpInputUrisJson = _stmt.getText(_columnIndexOfInputUrisJson)
          val _tmpInputSummary: String
          _tmpInputSummary = _stmt.getText(_columnIndexOfInputSummary)
          val _tmpOutputBaseName: String
          _tmpOutputBaseName = _stmt.getText(_columnIndexOfOutputBaseName)
          val _tmpStatus: String
          _tmpStatus = _stmt.getText(_columnIndexOfStatus)
          val _tmpOutputPath: String?
          if (_stmt.isNull(_columnIndexOfOutputPath)) {
            _tmpOutputPath = null
          } else {
            _tmpOutputPath = _stmt.getText(_columnIndexOfOutputPath)
          }
          val _tmpOutputSizeBytes: Long
          _tmpOutputSizeBytes = _stmt.getLong(_columnIndexOfOutputSizeBytes)
          val _tmpErrorMessage: String?
          if (_stmt.isNull(_columnIndexOfErrorMessage)) {
            _tmpErrorMessage = null
          } else {
            _tmpErrorMessage = _stmt.getText(_columnIndexOfErrorMessage)
          }
          val _tmpCreatedAtMillis: Long
          _tmpCreatedAtMillis = _stmt.getLong(_columnIndexOfCreatedAtMillis)
          _item =
              BatchQueueTaskEntity(_tmpId,_tmpTaskType,_tmpInputUrisJson,_tmpInputSummary,_tmpOutputBaseName,_tmpStatus,_tmpOutputPath,_tmpOutputSizeBytes,_tmpErrorMessage,_tmpCreatedAtMillis)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun updateStatus(
    id: Long,
    status: String,
    errorMessage: String?,
  ) {
    val _sql: String = "UPDATE batch_queue_tasks SET status = ?, error_message = ? WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, status)
        _argIndex = 2
        if (errorMessage == null) {
          _stmt.bindNull(_argIndex)
        } else {
          _stmt.bindText(_argIndex, errorMessage)
        }
        _argIndex = 3
        _stmt.bindLong(_argIndex, id)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun markSuccess(
    id: Long,
    status: String,
    outputPath: String,
    sizeBytes: Long,
  ) {
    val _sql: String =
        "UPDATE batch_queue_tasks SET status = ?, output_path = ?, output_size_bytes = ? WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, status)
        _argIndex = 2
        _stmt.bindText(_argIndex, outputPath)
        _argIndex = 3
        _stmt.bindLong(_argIndex, sizeBytes)
        _argIndex = 4
        _stmt.bindLong(_argIndex, id)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteAll() {
    val _sql: String = "DELETE FROM batch_queue_tasks"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteById(id: Long) {
    val _sql: String = "DELETE FROM batch_queue_tasks WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, id)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteCompleted() {
    val _sql: String =
        "DELETE FROM batch_queue_tasks WHERE status IN ('SUCCESS', 'FAILED', 'CANCELED')"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
