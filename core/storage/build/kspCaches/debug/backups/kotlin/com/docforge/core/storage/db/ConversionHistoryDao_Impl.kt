package com.docforge.core.storage.db

import androidx.paging.PagingSource
import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.RoomRawQuery
import androidx.room.coroutines.createFlow
import androidx.room.paging.LimitOffsetPagingSource
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.getTotalChangedRows
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
import kotlinx.coroutines.flow.Flow

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class ConversionHistoryDao_Impl(
  __db: RoomDatabase,
) : ConversionHistoryDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfConversionHistoryEntity: EntityInsertAdapter<ConversionHistoryEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfConversionHistoryEntity = object :
        EntityInsertAdapter<ConversionHistoryEntity>() {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `conversion_history` (`id`,`sourceLabel`,`outputPath`,`outputUri`,`displayName`,`operation`,`createdAtMillis`,`inputCount`,`outputSizeBytes`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: ConversionHistoryEntity) {
        statement.bindLong(1, entity.id)
        statement.bindText(2, entity.sourceLabel)
        statement.bindText(3, entity.outputPath)
        val _tmpOutputUri: String? = entity.outputUri
        if (_tmpOutputUri == null) {
          statement.bindNull(4)
        } else {
          statement.bindText(4, _tmpOutputUri)
        }
        val _tmpDisplayName: String? = entity.displayName
        if (_tmpDisplayName == null) {
          statement.bindNull(5)
        } else {
          statement.bindText(5, _tmpDisplayName)
        }
        statement.bindText(6, entity.operation)
        statement.bindLong(7, entity.createdAtMillis)
        statement.bindLong(8, entity.inputCount.toLong())
        statement.bindLong(9, entity.outputSizeBytes)
      }
    }
  }

  public override suspend fun insert(item: ConversionHistoryEntity): Unit = performSuspending(__db,
      false, true) { _connection ->
    __insertAdapterOfConversionHistoryEntity.insert(_connection, item)
  }

  public override fun observeRecent(limit: Int): Flow<List<ConversionHistoryEntity>> {
    val _sql: String = "SELECT * FROM conversion_history ORDER BY createdAtMillis DESC LIMIT ?"
    return createFlow(__db, false, arrayOf("conversion_history")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, limit.toLong())
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfSourceLabel: Int = getColumnIndexOrThrow(_stmt, "sourceLabel")
        val _columnIndexOfOutputPath: Int = getColumnIndexOrThrow(_stmt, "outputPath")
        val _columnIndexOfOutputUri: Int = getColumnIndexOrThrow(_stmt, "outputUri")
        val _columnIndexOfDisplayName: Int = getColumnIndexOrThrow(_stmt, "displayName")
        val _columnIndexOfOperation: Int = getColumnIndexOrThrow(_stmt, "operation")
        val _columnIndexOfCreatedAtMillis: Int = getColumnIndexOrThrow(_stmt, "createdAtMillis")
        val _columnIndexOfInputCount: Int = getColumnIndexOrThrow(_stmt, "inputCount")
        val _columnIndexOfOutputSizeBytes: Int = getColumnIndexOrThrow(_stmt, "outputSizeBytes")
        val _result: MutableList<ConversionHistoryEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: ConversionHistoryEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpSourceLabel: String
          _tmpSourceLabel = _stmt.getText(_columnIndexOfSourceLabel)
          val _tmpOutputPath: String
          _tmpOutputPath = _stmt.getText(_columnIndexOfOutputPath)
          val _tmpOutputUri: String?
          if (_stmt.isNull(_columnIndexOfOutputUri)) {
            _tmpOutputUri = null
          } else {
            _tmpOutputUri = _stmt.getText(_columnIndexOfOutputUri)
          }
          val _tmpDisplayName: String?
          if (_stmt.isNull(_columnIndexOfDisplayName)) {
            _tmpDisplayName = null
          } else {
            _tmpDisplayName = _stmt.getText(_columnIndexOfDisplayName)
          }
          val _tmpOperation: String
          _tmpOperation = _stmt.getText(_columnIndexOfOperation)
          val _tmpCreatedAtMillis: Long
          _tmpCreatedAtMillis = _stmt.getLong(_columnIndexOfCreatedAtMillis)
          val _tmpInputCount: Int
          _tmpInputCount = _stmt.getLong(_columnIndexOfInputCount).toInt()
          val _tmpOutputSizeBytes: Long
          _tmpOutputSizeBytes = _stmt.getLong(_columnIndexOfOutputSizeBytes)
          _item =
              ConversionHistoryEntity(_tmpId,_tmpSourceLabel,_tmpOutputPath,_tmpOutputUri,_tmpDisplayName,_tmpOperation,_tmpCreatedAtMillis,_tmpInputCount,_tmpOutputSizeBytes)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observePaged(): PagingSource<Int, ConversionHistoryEntity> {
    val _sql: String = "SELECT * FROM conversion_history ORDER BY createdAtMillis DESC"
    val _rawQuery: RoomRawQuery = RoomRawQuery(_sql)
    return object : LimitOffsetPagingSource<ConversionHistoryEntity>(_rawQuery, __db,
        "conversion_history") {
      protected override suspend fun convertRows(limitOffsetQuery: RoomRawQuery, itemCount: Int):
          List<ConversionHistoryEntity> = performSuspending(__db, true, false) { _connection ->
        val _stmt: SQLiteStatement = _connection.prepare(limitOffsetQuery.sql)
        limitOffsetQuery.getBindingFunction().invoke(_stmt)
        try {
          val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
          val _columnIndexOfSourceLabel: Int = getColumnIndexOrThrow(_stmt, "sourceLabel")
          val _columnIndexOfOutputPath: Int = getColumnIndexOrThrow(_stmt, "outputPath")
          val _columnIndexOfOutputUri: Int = getColumnIndexOrThrow(_stmt, "outputUri")
          val _columnIndexOfDisplayName: Int = getColumnIndexOrThrow(_stmt, "displayName")
          val _columnIndexOfOperation: Int = getColumnIndexOrThrow(_stmt, "operation")
          val _columnIndexOfCreatedAtMillis: Int = getColumnIndexOrThrow(_stmt, "createdAtMillis")
          val _columnIndexOfInputCount: Int = getColumnIndexOrThrow(_stmt, "inputCount")
          val _columnIndexOfOutputSizeBytes: Int = getColumnIndexOrThrow(_stmt, "outputSizeBytes")
          val _result: MutableList<ConversionHistoryEntity> = mutableListOf()
          while (_stmt.step()) {
            val _item: ConversionHistoryEntity
            val _tmpId: Long
            _tmpId = _stmt.getLong(_columnIndexOfId)
            val _tmpSourceLabel: String
            _tmpSourceLabel = _stmt.getText(_columnIndexOfSourceLabel)
            val _tmpOutputPath: String
            _tmpOutputPath = _stmt.getText(_columnIndexOfOutputPath)
            val _tmpOutputUri: String?
            if (_stmt.isNull(_columnIndexOfOutputUri)) {
              _tmpOutputUri = null
            } else {
              _tmpOutputUri = _stmt.getText(_columnIndexOfOutputUri)
            }
            val _tmpDisplayName: String?
            if (_stmt.isNull(_columnIndexOfDisplayName)) {
              _tmpDisplayName = null
            } else {
              _tmpDisplayName = _stmt.getText(_columnIndexOfDisplayName)
            }
            val _tmpOperation: String
            _tmpOperation = _stmt.getText(_columnIndexOfOperation)
            val _tmpCreatedAtMillis: Long
            _tmpCreatedAtMillis = _stmt.getLong(_columnIndexOfCreatedAtMillis)
            val _tmpInputCount: Int
            _tmpInputCount = _stmt.getLong(_columnIndexOfInputCount).toInt()
            val _tmpOutputSizeBytes: Long
            _tmpOutputSizeBytes = _stmt.getLong(_columnIndexOfOutputSizeBytes)
            _item =
                ConversionHistoryEntity(_tmpId,_tmpSourceLabel,_tmpOutputPath,_tmpOutputUri,_tmpDisplayName,_tmpOperation,_tmpCreatedAtMillis,_tmpInputCount,_tmpOutputSizeBytes)
            _result.add(_item)
          }
          _result
        } finally {
          _stmt.close()
        }
      }
    }
  }

  public override suspend fun deleteById(id: Long): Int {
    val _sql: String = "DELETE FROM conversion_history WHERE id = ?"
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

  public override suspend fun deleteAll() {
    val _sql: String = "DELETE FROM conversion_history"
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
