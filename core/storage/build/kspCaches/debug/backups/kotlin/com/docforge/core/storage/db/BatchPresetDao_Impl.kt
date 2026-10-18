package com.docforge.core.storage.db

import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.getTotalChangedRows
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import javax.`annotation`.processing.Generated
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.mutableListOf
import kotlin.reflect.KClass

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class BatchPresetDao_Impl(
  __db: RoomDatabase,
) : BatchPresetDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfBatchPresetEntity: EntityInsertAdapter<BatchPresetEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfBatchPresetEntity = object : EntityInsertAdapter<BatchPresetEntity>() {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `batch_presets` (`id`,`name`,`createdAtMillis`,`tasksJson`) VALUES (nullif(?, 0),?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: BatchPresetEntity) {
        statement.bindLong(1, entity.id)
        statement.bindText(2, entity.name)
        statement.bindLong(3, entity.createdAtMillis)
        statement.bindText(4, entity.tasksJson)
      }
    }
  }

  public override suspend fun insert(preset: BatchPresetEntity): Long = performSuspending(__db,
      false, true) { _connection ->
    val _result: Long = __insertAdapterOfBatchPresetEntity.insertAndReturnId(_connection, preset)
    _result
  }

  public override suspend fun getAll(): List<BatchPresetEntity> {
    val _sql: String = "SELECT * FROM batch_presets ORDER BY createdAtMillis DESC"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfCreatedAtMillis: Int = getColumnIndexOrThrow(_stmt, "createdAtMillis")
        val _columnIndexOfTasksJson: Int = getColumnIndexOrThrow(_stmt, "tasksJson")
        val _result: MutableList<BatchPresetEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: BatchPresetEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpCreatedAtMillis: Long
          _tmpCreatedAtMillis = _stmt.getLong(_columnIndexOfCreatedAtMillis)
          val _tmpTasksJson: String
          _tmpTasksJson = _stmt.getText(_columnIndexOfTasksJson)
          _item = BatchPresetEntity(_tmpId,_tmpName,_tmpCreatedAtMillis,_tmpTasksJson)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteById(id: Long): Int {
    val _sql: String = "DELETE FROM batch_presets WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, id)
        _stmt.step()
        getTotalChangedRows(_connection)
      } finally {
        _stmt.close()
      }
    }
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
