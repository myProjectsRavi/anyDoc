package com.docforge.core.storage.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ConversionHistoryEntity::class],
    version = 1,
    exportSchema = true
)
abstract class DocForgeDatabase : RoomDatabase() {

    abstract fun conversionHistoryDao(): ConversionHistoryDao

    companion object {
        @Volatile
        private var instance: DocForgeDatabase? = null

        fun get(context: Context): DocForgeDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    DocForgeDatabase::class.java,
                    "docforge.db"
                )
                    .fallbackToDestructiveMigration()
                    .build().also { instance = it }
            }
        }
    }
}
