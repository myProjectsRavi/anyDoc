package com.docforge.core.storage.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ConversionHistoryEntity::class,
        BatchPresetEntity::class,
        BatchQueueTaskEntity::class,
        DocumentTextIndexEntity::class,
        DocumentTextFts::class
    ],
    version = 4,
    exportSchema = true
)
abstract class DocForgeDatabase : RoomDatabase() {

    abstract fun conversionHistoryDao(): ConversionHistoryDao
    abstract fun batchPresetDao(): BatchPresetDao
    abstract fun batchQueueTaskDao(): BatchQueueTaskDao
    abstract fun documentTextIndexDao(): DocumentTextIndexDao

    companion object {
        @Volatile
        private var instance: DocForgeDatabase? = null

        /** Migration 1→2: add batch_presets table */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `batch_presets` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `createdAtMillis` INTEGER NOT NULL,
                        `tasksJson` TEXT NOT NULL
                    )"""
                )
            }
        }

        /** Migration 2→3: add outputUri + displayName to conversion_history */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE conversion_history ADD COLUMN outputUri TEXT")
                db.execSQL("ALTER TABLE conversion_history ADD COLUMN displayName TEXT")
            }
        }

        /** Migration 3→4: add batch_queue_tasks + document_text_index + FTS tables */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `batch_queue_tasks` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `task_type` TEXT NOT NULL,
                        `input_uris_json` TEXT NOT NULL,
                        `input_summary` TEXT NOT NULL,
                        `output_base_name` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `output_path` TEXT,
                        `output_size_bytes` INTEGER NOT NULL DEFAULT 0,
                        `error_message` TEXT,
                        `created_at_millis` INTEGER NOT NULL
                    )"""
                )
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `document_text_index` (
                        `rowId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `document_name` TEXT NOT NULL,
                        `document_path` TEXT NOT NULL,
                        `text_content` TEXT NOT NULL,
                        `source_operation` TEXT NOT NULL,
                        `page_count` INTEGER NOT NULL DEFAULT 0,
                        `created_at_millis` INTEGER NOT NULL
                    )"""
                )
                db.execSQL(
                    """CREATE VIRTUAL TABLE IF NOT EXISTS `document_text_fts`
                       USING FTS4(`text_content`, `document_name`, content=`document_text_index`)"""
                )
            }
        }

        fun get(context: Context): DocForgeDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    DocForgeDatabase::class.java,
                    "docforge.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build().also { instance = it }
            }
        }
    }
}
