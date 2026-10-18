package com.docforge.core.storage.db

import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
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
public class DocumentTextIndexDao_Impl(
  __db: RoomDatabase,
) : DocumentTextIndexDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfDocumentTextIndexEntity: EntityInsertAdapter<DocumentTextIndexEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfDocumentTextIndexEntity = object :
        EntityInsertAdapter<DocumentTextIndexEntity>() {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `document_text_index` (`rowId`,`document_name`,`document_path`,`text_content`,`source_operation`,`page_count`,`created_at_millis`) VALUES (nullif(?, 0),?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: DocumentTextIndexEntity) {
        statement.bindLong(1, entity.rowId)
        statement.bindText(2, entity.documentName)
        statement.bindText(3, entity.documentPath)
        statement.bindText(4, entity.textContent)
        statement.bindText(5, entity.sourceOperation)
        statement.bindLong(6, entity.pageCount.toLong())
        statement.bindLong(7, entity.createdAtMillis)
      }
    }
  }

  public override suspend fun insert(entity: DocumentTextIndexEntity): Long =
      performSuspending(__db, false, true) { _connection ->
    val _result: Long = __insertAdapterOfDocumentTextIndexEntity.insertAndReturnId(_connection,
        entity)
    _result
  }

  public override suspend fun search(query: String, limit: Int): List<DocumentSearchResult> {
    val _sql: String = """
        |
        |        SELECT 
        |            dti.rowId,
        |            dti.document_name AS documentName,
        |            dti.document_path AS documentPath,
        |            dti.source_operation AS sourceOperation,
        |            dti.page_count AS pageCount,
        |            dti.created_at_millis AS createdAtMillis,
        |            snippet(document_text_fts, '**', '**', '...', 0, 40) AS snippet
        |        FROM document_text_fts
        |        JOIN document_text_index dti ON document_text_fts.rowid = dti.rowId
        |        WHERE document_text_fts MATCH ?
        |        ORDER BY dti.created_at_millis DESC
        |        LIMIT ?
        |    
        """.trimMargin()
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, query)
        _argIndex = 2
        _stmt.bindLong(_argIndex, limit.toLong())
        val _columnIndexOfRowId: Int = 0
        val _columnIndexOfDocumentName: Int = 1
        val _columnIndexOfDocumentPath: Int = 2
        val _columnIndexOfSourceOperation: Int = 3
        val _columnIndexOfPageCount: Int = 4
        val _columnIndexOfCreatedAtMillis: Int = 5
        val _columnIndexOfSnippet: Int = 6
        val _result: MutableList<DocumentSearchResult> = mutableListOf()
        while (_stmt.step()) {
          val _item: DocumentSearchResult
          val _tmpRowId: Long
          _tmpRowId = _stmt.getLong(_columnIndexOfRowId)
          val _tmpDocumentName: String
          _tmpDocumentName = _stmt.getText(_columnIndexOfDocumentName)
          val _tmpDocumentPath: String
          _tmpDocumentPath = _stmt.getText(_columnIndexOfDocumentPath)
          val _tmpSourceOperation: String
          _tmpSourceOperation = _stmt.getText(_columnIndexOfSourceOperation)
          val _tmpPageCount: Int
          _tmpPageCount = _stmt.getLong(_columnIndexOfPageCount).toInt()
          val _tmpCreatedAtMillis: Long
          _tmpCreatedAtMillis = _stmt.getLong(_columnIndexOfCreatedAtMillis)
          val _tmpSnippet: String
          _tmpSnippet = _stmt.getText(_columnIndexOfSnippet)
          _item =
              DocumentSearchResult(_tmpRowId,_tmpDocumentName,_tmpDocumentPath,_tmpSourceOperation,_tmpPageCount,_tmpCreatedAtMillis,_tmpSnippet)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun count(): Int {
    val _sql: String = "SELECT COUNT(*) FROM document_text_index"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _result: Int
        if (_stmt.step()) {
          val _tmp: Int
          _tmp = _stmt.getLong(0).toInt()
          _result = _tmp
        } else {
          _result = 0
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteByPath(documentPath: String) {
    val _sql: String = "DELETE FROM document_text_index WHERE document_path = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, documentPath)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteAll() {
    val _sql: String = "DELETE FROM document_text_index"
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
