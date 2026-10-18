package com.docforge.core.storage.db;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class DocForgeDatabase_Impl extends DocForgeDatabase {
  private volatile ConversionHistoryDao _conversionHistoryDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(1) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `conversion_history` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `sourceLabel` TEXT NOT NULL, `outputPath` TEXT NOT NULL, `operation` TEXT NOT NULL, `createdAtMillis` INTEGER NOT NULL, `inputCount` INTEGER NOT NULL, `outputSizeBytes` INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '827d205ac23493e30fe59d9ea4f5666a')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `conversion_history`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsConversionHistory = new HashMap<String, TableInfo.Column>(7);
        _columnsConversionHistory.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsConversionHistory.put("sourceLabel", new TableInfo.Column("sourceLabel", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsConversionHistory.put("outputPath", new TableInfo.Column("outputPath", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsConversionHistory.put("operation", new TableInfo.Column("operation", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsConversionHistory.put("createdAtMillis", new TableInfo.Column("createdAtMillis", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsConversionHistory.put("inputCount", new TableInfo.Column("inputCount", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsConversionHistory.put("outputSizeBytes", new TableInfo.Column("outputSizeBytes", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysConversionHistory = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesConversionHistory = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoConversionHistory = new TableInfo("conversion_history", _columnsConversionHistory, _foreignKeysConversionHistory, _indicesConversionHistory);
        final TableInfo _existingConversionHistory = TableInfo.read(db, "conversion_history");
        if (!_infoConversionHistory.equals(_existingConversionHistory)) {
          return new RoomOpenHelper.ValidationResult(false, "conversion_history(com.docforge.core.storage.db.ConversionHistoryEntity).\n"
                  + " Expected:\n" + _infoConversionHistory + "\n"
                  + " Found:\n" + _existingConversionHistory);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "827d205ac23493e30fe59d9ea4f5666a", "ef4e0ca66a39a927b96edff2673e774d");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "conversion_history");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `conversion_history`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(ConversionHistoryDao.class, ConversionHistoryDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public ConversionHistoryDao conversionHistoryDao() {
    if (_conversionHistoryDao != null) {
      return _conversionHistoryDao;
    } else {
      synchronized(this) {
        if(_conversionHistoryDao == null) {
          _conversionHistoryDao = new ConversionHistoryDao_Impl(this);
        }
        return _conversionHistoryDao;
      }
    }
  }
}
