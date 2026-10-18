package com.docforge.core.storage.repository;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010 \n\u0000\n\u0002\u0010\b\n\u0000\u0018\u00002\u00020\u0001B\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\u0016\u0010\u0005\u001a\u00020\u00062\u0006\u0010\u0007\u001a\u00020\bH\u0096@\u00a2\u0006\u0002\u0010\tJ\u001c\u0010\n\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\b0\f0\u000b2\u0006\u0010\r\u001a\u00020\u000eH\u0016R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u000f"}, d2 = {"Lcom/docforge/core/storage/repository/LocalHistoryRepository;", "Lcom/docforge/core/domain/repository/HistoryRepository;", "dao", "Lcom/docforge/core/storage/db/ConversionHistoryDao;", "(Lcom/docforge/core/storage/db/ConversionHistoryDao;)V", "insert", "", "record", "Lcom/docforge/core/domain/model/ConversionRecord;", "(Lcom/docforge/core/domain/model/ConversionRecord;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "observeRecent", "Lkotlinx/coroutines/flow/Flow;", "", "limit", "", "storage_debug"})
public final class LocalHistoryRepository implements com.docforge.core.domain.repository.HistoryRepository {
    @org.jetbrains.annotations.NotNull()
    private final com.docforge.core.storage.db.ConversionHistoryDao dao = null;
    
    public LocalHistoryRepository(@org.jetbrains.annotations.NotNull()
    com.docforge.core.storage.db.ConversionHistoryDao dao) {
        super();
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.NotNull()
    public kotlinx.coroutines.flow.Flow<java.util.List<com.docforge.core.domain.model.ConversionRecord>> observeRecent(int limit) {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.Nullable()
    public java.lang.Object insert(@org.jetbrains.annotations.NotNull()
    com.docforge.core.domain.model.ConversionRecord record, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
}