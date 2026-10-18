package com.docforge.core.storage.db

import androidx.room.InvalidationTracker
import androidx.room.RoomOpenDelegate
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.room.util.FtsTableInfo
import androidx.room.util.TableInfo
import androidx.room.util.dropFtsSyncTriggers
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import javax.`annotation`.processing.Generated
import kotlin.Lazy
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.MutableList
import kotlin.collections.MutableMap
import kotlin.collections.MutableSet
import kotlin.collections.Set
import kotlin.collections.mutableListOf
import kotlin.collections.mutableMapOf
import kotlin.collections.mutableSetOf
import kotlin.reflect.KClass
import androidx.room.util.FtsTableInfo.Companion.read as ftsTableInfoRead
import androidx.room.util.TableInfo.Companion.read as tableInfoRead

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class DocForgeDatabase_Impl : DocForgeDatabase() {
  private val _conversionHistoryDao: Lazy<ConversionHistoryDao> = lazy {
    ConversionHistoryDao_Impl(this)
  }

  private val _batchPresetDao: Lazy<BatchPresetDao> = lazy {
    BatchPresetDao_Impl(this)
  }

  private val _batchQueueTaskDao: Lazy<BatchQueueTaskDao> = lazy {
    BatchQueueTaskDao_Impl(this)
  }

  private val _documentTextIndexDao: Lazy<DocumentTextIndexDao> = lazy {
    DocumentTextIndexDao_Impl(this)
  }

  protected override fun createOpenDelegate(): RoomOpenDelegate {
    val _openDelegate: RoomOpenDelegate = object : RoomOpenDelegate(4,
        "578fd66b22f64588adbdb01acbb02c2e", "278748747210562873a66c87eadd6eac") {
      public override fun createAllTables(connection: SQLiteConnection) {
        connection.execSQL("CREATE TABLE IF NOT EXISTS `conversion_history` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `sourceLabel` TEXT NOT NULL, `outputPath` TEXT NOT NULL, `outputUri` TEXT, `displayName` TEXT, `operation` TEXT NOT NULL, `createdAtMillis` INTEGER NOT NULL, `inputCount` INTEGER NOT NULL, `outputSizeBytes` INTEGER NOT NULL)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `batch_presets` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `createdAtMillis` INTEGER NOT NULL, `tasksJson` TEXT NOT NULL)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `batch_queue_tasks` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `task_type` TEXT NOT NULL, `input_uris_json` TEXT NOT NULL, `input_summary` TEXT NOT NULL, `output_base_name` TEXT NOT NULL, `status` TEXT NOT NULL, `output_path` TEXT, `output_size_bytes` INTEGER NOT NULL, `error_message` TEXT, `created_at_millis` INTEGER NOT NULL)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `document_text_index` (`rowId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `document_name` TEXT NOT NULL, `document_path` TEXT NOT NULL, `text_content` TEXT NOT NULL, `source_operation` TEXT NOT NULL, `page_count` INTEGER NOT NULL, `created_at_millis` INTEGER NOT NULL)")
        connection.execSQL("CREATE VIRTUAL TABLE IF NOT EXISTS `document_text_fts` USING FTS4(`text_content` TEXT NOT NULL, `document_name` TEXT NOT NULL, content=`document_text_index`)")
        connection.execSQL("CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_document_text_fts_BEFORE_UPDATE BEFORE UPDATE ON `document_text_index` BEGIN DELETE FROM `document_text_fts` WHERE `docid`=OLD.`rowid`; END")
        connection.execSQL("CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_document_text_fts_BEFORE_DELETE BEFORE DELETE ON `document_text_index` BEGIN DELETE FROM `document_text_fts` WHERE `docid`=OLD.`rowid`; END")
        connection.execSQL("CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_document_text_fts_AFTER_UPDATE AFTER UPDATE ON `document_text_index` BEGIN INSERT INTO `document_text_fts`(`docid`, `text_content`, `document_name`) VALUES (NEW.`rowid`, NEW.`text_content`, NEW.`document_name`); END")
        connection.execSQL("CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_document_text_fts_AFTER_INSERT AFTER INSERT ON `document_text_index` BEGIN INSERT INTO `document_text_fts`(`docid`, `text_content`, `document_name`) VALUES (NEW.`rowid`, NEW.`text_content`, NEW.`document_name`); END")
        connection.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
        connection.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '578fd66b22f64588adbdb01acbb02c2e')")
      }

      public override fun dropAllTables(connection: SQLiteConnection) {
        connection.execSQL("DROP TABLE IF EXISTS `conversion_history`")
        connection.execSQL("DROP TABLE IF EXISTS `batch_presets`")
        connection.execSQL("DROP TABLE IF EXISTS `batch_queue_tasks`")
        connection.execSQL("DROP TABLE IF EXISTS `document_text_index`")
        connection.execSQL("DROP TABLE IF EXISTS `document_text_fts`")
      }

      public override fun onCreate(connection: SQLiteConnection) {
      }

      public override fun onOpen(connection: SQLiteConnection) {
        internalInitInvalidationTracker(connection)
      }

      public override fun onPreMigrate(connection: SQLiteConnection) {
        dropFtsSyncTriggers(connection)
      }

      public override fun onPostMigrate(connection: SQLiteConnection) {
        connection.execSQL("CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_document_text_fts_BEFORE_UPDATE BEFORE UPDATE ON `document_text_index` BEGIN DELETE FROM `document_text_fts` WHERE `docid`=OLD.`rowid`; END")
        connection.execSQL("CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_document_text_fts_BEFORE_DELETE BEFORE DELETE ON `document_text_index` BEGIN DELETE FROM `document_text_fts` WHERE `docid`=OLD.`rowid`; END")
        connection.execSQL("CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_document_text_fts_AFTER_UPDATE AFTER UPDATE ON `document_text_index` BEGIN INSERT INTO `document_text_fts`(`docid`, `text_content`, `document_name`) VALUES (NEW.`rowid`, NEW.`text_content`, NEW.`document_name`); END")
        connection.execSQL("CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_document_text_fts_AFTER_INSERT AFTER INSERT ON `document_text_index` BEGIN INSERT INTO `document_text_fts`(`docid`, `text_content`, `document_name`) VALUES (NEW.`rowid`, NEW.`text_content`, NEW.`document_name`); END")
      }

      public override fun onValidateSchema(connection: SQLiteConnection):
          RoomOpenDelegate.ValidationResult {
        val _columnsConversionHistory: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsConversionHistory.put("id", TableInfo.Column("id", "INTEGER", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsConversionHistory.put("sourceLabel", TableInfo.Column("sourceLabel", "TEXT", true,
            0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsConversionHistory.put("outputPath", TableInfo.Column("outputPath", "TEXT", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsConversionHistory.put("outputUri", TableInfo.Column("outputUri", "TEXT", false, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsConversionHistory.put("displayName", TableInfo.Column("displayName", "TEXT", false,
            0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsConversionHistory.put("operation", TableInfo.Column("operation", "TEXT", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsConversionHistory.put("createdAtMillis", TableInfo.Column("createdAtMillis",
            "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsConversionHistory.put("inputCount", TableInfo.Column("inputCount", "INTEGER", true,
            0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsConversionHistory.put("outputSizeBytes", TableInfo.Column("outputSizeBytes",
            "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysConversionHistory: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesConversionHistory: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoConversionHistory: TableInfo = TableInfo("conversion_history",
            _columnsConversionHistory, _foreignKeysConversionHistory, _indicesConversionHistory)
        val _existingConversionHistory: TableInfo = tableInfoRead(connection, "conversion_history")
        if (!_infoConversionHistory.equals(_existingConversionHistory)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |conversion_history(com.docforge.core.storage.db.ConversionHistoryEntity).
              | Expected:
              |""".trimMargin() + _infoConversionHistory + """
              |
              | Found:
              |""".trimMargin() + _existingConversionHistory)
        }
        val _columnsBatchPresets: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsBatchPresets.put("id", TableInfo.Column("id", "INTEGER", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsBatchPresets.put("name", TableInfo.Column("name", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsBatchPresets.put("createdAtMillis", TableInfo.Column("createdAtMillis", "INTEGER",
            true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsBatchPresets.put("tasksJson", TableInfo.Column("tasksJson", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysBatchPresets: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesBatchPresets: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoBatchPresets: TableInfo = TableInfo("batch_presets", _columnsBatchPresets,
            _foreignKeysBatchPresets, _indicesBatchPresets)
        val _existingBatchPresets: TableInfo = tableInfoRead(connection, "batch_presets")
        if (!_infoBatchPresets.equals(_existingBatchPresets)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |batch_presets(com.docforge.core.storage.db.BatchPresetEntity).
              | Expected:
              |""".trimMargin() + _infoBatchPresets + """
              |
              | Found:
              |""".trimMargin() + _existingBatchPresets)
        }
        val _columnsBatchQueueTasks: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsBatchQueueTasks.put("id", TableInfo.Column("id", "INTEGER", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsBatchQueueTasks.put("task_type", TableInfo.Column("task_type", "TEXT", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsBatchQueueTasks.put("input_uris_json", TableInfo.Column("input_uris_json", "TEXT",
            true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsBatchQueueTasks.put("input_summary", TableInfo.Column("input_summary", "TEXT", true,
            0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsBatchQueueTasks.put("output_base_name", TableInfo.Column("output_base_name", "TEXT",
            true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsBatchQueueTasks.put("status", TableInfo.Column("status", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsBatchQueueTasks.put("output_path", TableInfo.Column("output_path", "TEXT", false, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsBatchQueueTasks.put("output_size_bytes", TableInfo.Column("output_size_bytes",
            "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsBatchQueueTasks.put("error_message", TableInfo.Column("error_message", "TEXT",
            false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsBatchQueueTasks.put("created_at_millis", TableInfo.Column("created_at_millis",
            "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysBatchQueueTasks: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesBatchQueueTasks: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoBatchQueueTasks: TableInfo = TableInfo("batch_queue_tasks",
            _columnsBatchQueueTasks, _foreignKeysBatchQueueTasks, _indicesBatchQueueTasks)
        val _existingBatchQueueTasks: TableInfo = tableInfoRead(connection, "batch_queue_tasks")
        if (!_infoBatchQueueTasks.equals(_existingBatchQueueTasks)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |batch_queue_tasks(com.docforge.core.storage.db.BatchQueueTaskEntity).
              | Expected:
              |""".trimMargin() + _infoBatchQueueTasks + """
              |
              | Found:
              |""".trimMargin() + _existingBatchQueueTasks)
        }
        val _columnsDocumentTextIndex: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsDocumentTextIndex.put("rowId", TableInfo.Column("rowId", "INTEGER", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsDocumentTextIndex.put("document_name", TableInfo.Column("document_name", "TEXT",
            true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsDocumentTextIndex.put("document_path", TableInfo.Column("document_path", "TEXT",
            true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsDocumentTextIndex.put("text_content", TableInfo.Column("text_content", "TEXT", true,
            0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsDocumentTextIndex.put("source_operation", TableInfo.Column("source_operation",
            "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsDocumentTextIndex.put("page_count", TableInfo.Column("page_count", "INTEGER", true,
            0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsDocumentTextIndex.put("created_at_millis", TableInfo.Column("created_at_millis",
            "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysDocumentTextIndex: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesDocumentTextIndex: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoDocumentTextIndex: TableInfo = TableInfo("document_text_index",
            _columnsDocumentTextIndex, _foreignKeysDocumentTextIndex, _indicesDocumentTextIndex)
        val _existingDocumentTextIndex: TableInfo = tableInfoRead(connection, "document_text_index")
        if (!_infoDocumentTextIndex.equals(_existingDocumentTextIndex)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |document_text_index(com.docforge.core.storage.db.DocumentTextIndexEntity).
              | Expected:
              |""".trimMargin() + _infoDocumentTextIndex + """
              |
              | Found:
              |""".trimMargin() + _existingDocumentTextIndex)
        }
        val _columnsDocumentTextFts: MutableSet<String> = mutableSetOf()
        _columnsDocumentTextFts.add("text_content")
        _columnsDocumentTextFts.add("document_name")
        val _infoDocumentTextFts: FtsTableInfo = FtsTableInfo("document_text_fts",
            _columnsDocumentTextFts,
            "CREATE VIRTUAL TABLE IF NOT EXISTS `document_text_fts` USING FTS4(`text_content` TEXT NOT NULL, `document_name` TEXT NOT NULL, content=`document_text_index`)")
        val _existingDocumentTextFts: FtsTableInfo = ftsTableInfoRead(connection,
            "document_text_fts")
        if (!_infoDocumentTextFts.equals(_existingDocumentTextFts)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |document_text_fts(com.docforge.core.storage.db.DocumentTextFts).
              | Expected:
              |""".trimMargin() + _infoDocumentTextFts + """
              |
              | Found:
              |""".trimMargin() + _existingDocumentTextFts)
        }
        return RoomOpenDelegate.ValidationResult(true, null)
      }
    }
    return _openDelegate
  }

  protected override fun createInvalidationTracker(): InvalidationTracker {
    val _shadowTablesMap: MutableMap<String, String> = mutableMapOf()
    _shadowTablesMap.put("document_text_fts", "document_text_index")
    val _viewTables: MutableMap<String, Set<String>> = mutableMapOf()
    return InvalidationTracker(this, _shadowTablesMap, _viewTables, "conversion_history",
        "batch_presets", "batch_queue_tasks", "document_text_index", "document_text_fts")
  }

  public override fun clearAllTables() {
    super.performClear(false, "conversion_history", "batch_presets", "batch_queue_tasks",
        "document_text_index", "document_text_fts")
  }

  protected override fun getRequiredTypeConverterClasses(): Map<KClass<*>, List<KClass<*>>> {
    val _typeConvertersMap: MutableMap<KClass<*>, List<KClass<*>>> = mutableMapOf()
    _typeConvertersMap.put(ConversionHistoryDao::class,
        ConversionHistoryDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(BatchPresetDao::class, BatchPresetDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(BatchQueueTaskDao::class, BatchQueueTaskDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(DocumentTextIndexDao::class,
        DocumentTextIndexDao_Impl.getRequiredConverters())
    return _typeConvertersMap
  }

  public override fun getRequiredAutoMigrationSpecClasses(): Set<KClass<out AutoMigrationSpec>> {
    val _autoMigrationSpecsSet: MutableSet<KClass<out AutoMigrationSpec>> = mutableSetOf()
    return _autoMigrationSpecsSet
  }

  public override
      fun createAutoMigrations(autoMigrationSpecs: Map<KClass<out AutoMigrationSpec>, AutoMigrationSpec>):
      List<Migration> {
    val _autoMigrations: MutableList<Migration> = mutableListOf()
    return _autoMigrations
  }

  public override fun conversionHistoryDao(): ConversionHistoryDao = _conversionHistoryDao.value

  public override fun batchPresetDao(): BatchPresetDao = _batchPresetDao.value

  public override fun batchQueueTaskDao(): BatchQueueTaskDao = _batchQueueTaskDao.value

  public override fun documentTextIndexDao(): DocumentTextIndexDao = _documentTextIndexDao.value
}
