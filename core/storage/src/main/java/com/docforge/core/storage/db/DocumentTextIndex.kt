package com.docforge.core.storage.db

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

/**
 * FTS4 virtual table for full-text search over OCR'd / extracted document text.
 *
 * When a user runs OCR or text extraction on a PDF, the extracted text is saved here
 * alongside a reference to the source document. The user can then search across all
 * their processed documents by keyword — instantly finding which scanned document
 * contains a given word.
 *
 * Gemini 3.1 Pro feedback — "God Tier" feature: Full-Text Search (FTS).
 */
@Entity(tableName = "document_text_index")
data class DocumentTextIndexEntity(
    @PrimaryKey(autoGenerate = true)
    val rowId: Long = 0L,

    /** Display name of the source document (e.g., "scan_20260503_143022.pdf"). */
    @ColumnInfo(name = "document_name")
    val documentName: String,

    /** Absolute path or URI string of the source document. */
    @ColumnInfo(name = "document_path")
    val documentPath: String,

    /** The full extracted / OCR'd text content. */
    @ColumnInfo(name = "text_content")
    val textContent: String,

    /** Which operation produced this text: "ocr", "text_extract", "translation". */
    @ColumnInfo(name = "source_operation")
    val sourceOperation: String,

    /** Number of pages in the source document. */
    @ColumnInfo(name = "page_count")
    val pageCount: Int = 0,

    @ColumnInfo(name = "created_at_millis")
    val createdAtMillis: Long = System.currentTimeMillis()
)

/**
 * FTS4 shadow table for fast keyword matching.
 * Room generates the virtual table from this entity.
 */
@Fts4(contentEntity = DocumentTextIndexEntity::class)
@Entity(tableName = "document_text_fts")
data class DocumentTextFts(
    /** Indexed text content — matches [DocumentTextIndexEntity.textContent]. */
    @ColumnInfo(name = "text_content")
    val textContent: String,

    /** Indexed document name for name-based search. */
    @ColumnInfo(name = "document_name")
    val documentName: String
)

data class DocumentSearchResult(
    val rowId: Long,
    val documentName: String,
    val documentPath: String,
    val sourceOperation: String,
    val pageCount: Int,
    val createdAtMillis: Long,
    /** Snippet of matching text with keyword highlighted. */
    val snippet: String
)

@Dao
interface DocumentTextIndexDao {

    /**
     * Full-text search across all indexed documents.
     * Returns matching rows with a text snippet around the match.
     */
    @Query("""
        SELECT 
            dti.rowId,
            dti.document_name AS documentName,
            dti.document_path AS documentPath,
            dti.source_operation AS sourceOperation,
            dti.page_count AS pageCount,
            dti.created_at_millis AS createdAtMillis,
            snippet(document_text_fts, '**', '**', '...', 0, 40) AS snippet
        FROM document_text_fts
        JOIN document_text_index dti ON document_text_fts.rowid = dti.rowId
        WHERE document_text_fts MATCH :query
        ORDER BY dti.created_at_millis DESC
        LIMIT :limit
    """)
    suspend fun search(query: String, limit: Int): List<DocumentSearchResult>

    /** Index a new document's text content. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: DocumentTextIndexEntity): Long

    /** Remove all indexed text for a given document path. */
    @Query("DELETE FROM document_text_index WHERE document_path = :documentPath")
    suspend fun deleteByPath(documentPath: String)

    /** Get total number of indexed documents. */
    @Query("SELECT COUNT(*) FROM document_text_index")
    suspend fun count(): Int

    /** Delete all indexed content. */
    @Query("DELETE FROM document_text_index")
    suspend fun deleteAll()
}
