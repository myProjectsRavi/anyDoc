package com.docforge.core.storage.db;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class ConversionHistoryDao_Impl implements ConversionHistoryDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<ConversionHistoryEntity> __insertionAdapterOfConversionHistoryEntity;

  public ConversionHistoryDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfConversionHistoryEntity = new EntityInsertionAdapter<ConversionHistoryEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `conversion_history` (`id`,`sourceLabel`,`outputPath`,`operation`,`createdAtMillis`,`inputCount`,`outputSizeBytes`) VALUES (nullif(?, 0),?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ConversionHistoryEntity entity) {
        statement.bindLong(1, entity.getId());
        if (entity.getSourceLabel() == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.getSourceLabel());
        }
        if (entity.getOutputPath() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getOutputPath());
        }
        if (entity.getOperation() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getOperation());
        }
        statement.bindLong(5, entity.getCreatedAtMillis());
        statement.bindLong(6, entity.getInputCount());
        statement.bindLong(7, entity.getOutputSizeBytes());
      }
    };
  }

  @Override
  public Object insert(final ConversionHistoryEntity item,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfConversionHistoryEntity.insert(item);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<ConversionHistoryEntity>> observeRecent(final int limit) {
    final String _sql = "SELECT * FROM conversion_history ORDER BY createdAtMillis DESC LIMIT ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, limit);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"conversion_history"}, new Callable<List<ConversionHistoryEntity>>() {
      @Override
      @NonNull
      public List<ConversionHistoryEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfSourceLabel = CursorUtil.getColumnIndexOrThrow(_cursor, "sourceLabel");
          final int _cursorIndexOfOutputPath = CursorUtil.getColumnIndexOrThrow(_cursor, "outputPath");
          final int _cursorIndexOfOperation = CursorUtil.getColumnIndexOrThrow(_cursor, "operation");
          final int _cursorIndexOfCreatedAtMillis = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAtMillis");
          final int _cursorIndexOfInputCount = CursorUtil.getColumnIndexOrThrow(_cursor, "inputCount");
          final int _cursorIndexOfOutputSizeBytes = CursorUtil.getColumnIndexOrThrow(_cursor, "outputSizeBytes");
          final List<ConversionHistoryEntity> _result = new ArrayList<ConversionHistoryEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ConversionHistoryEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpSourceLabel;
            if (_cursor.isNull(_cursorIndexOfSourceLabel)) {
              _tmpSourceLabel = null;
            } else {
              _tmpSourceLabel = _cursor.getString(_cursorIndexOfSourceLabel);
            }
            final String _tmpOutputPath;
            if (_cursor.isNull(_cursorIndexOfOutputPath)) {
              _tmpOutputPath = null;
            } else {
              _tmpOutputPath = _cursor.getString(_cursorIndexOfOutputPath);
            }
            final String _tmpOperation;
            if (_cursor.isNull(_cursorIndexOfOperation)) {
              _tmpOperation = null;
            } else {
              _tmpOperation = _cursor.getString(_cursorIndexOfOperation);
            }
            final long _tmpCreatedAtMillis;
            _tmpCreatedAtMillis = _cursor.getLong(_cursorIndexOfCreatedAtMillis);
            final int _tmpInputCount;
            _tmpInputCount = _cursor.getInt(_cursorIndexOfInputCount);
            final long _tmpOutputSizeBytes;
            _tmpOutputSizeBytes = _cursor.getLong(_cursorIndexOfOutputSizeBytes);
            _item = new ConversionHistoryEntity(_tmpId,_tmpSourceLabel,_tmpOutputPath,_tmpOperation,_tmpCreatedAtMillis,_tmpInputCount,_tmpOutputSizeBytes);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
