package com.docforge.core.storage.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [ConversionHistoryEntity::class, BatchPresetEntity::class],
    version = 3,
    exportSchema = true
)
abstract class DocForgeDatabase : RoomDatabase() {

    abstract fun conversionHistoryDao(): ConversionHistoryDao
    abstract fun batchPresetDao(): BatchPresetDao

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

        fun get(context: Context): DocForgeDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    DocForgeDatabase::class.java,
                    "docforge.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .fallbackToDestructiveMigration()
                    .build().also { instance = it }
            }
        }
    }
}
