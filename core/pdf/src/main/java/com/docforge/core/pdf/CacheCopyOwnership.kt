package com.docforge.core.pdf

import com.docforge.core.domain.io.ActiveTempFileRegistry
import java.io.File
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/** Releases one owner and deletes only when the registry reports no owners. */
internal fun releaseCacheCopyFile(file: File) {
    ActiveTempFileRegistry.unregister(file)
    ActiveTempFileRegistry.deleteIfInactive(file)
}

/** Releases the copy's registration exactly once, including on failure. */
internal suspend inline fun <T> withRegisteredCacheCopy(
    file: File,
    keepRegistered: Boolean,
    copy: () -> T
): T {
    var succeeded = false
    try {
        val result = copy()
        succeeded = true
        return result
    } catch (failure: Throwable) {
        try {
            releaseCacheCopyFile(file)
        } catch (cleanupFailure: Throwable) {
            if (cleanupFailure !== failure) failure.addSuppressed(cleanupFailure)
        }
        throw failure
    } finally {
        if (succeeded && !keepRegistered) {
            ActiveTempFileRegistry.unregister(file)
        }
    }
}

/** Keeps the original block failure if temporary-file cleanup also fails. */
internal inline fun <T> withScopedCacheCopy(file: File, block: (File) -> T): T {
    var succeeded = false
    try {
        val result = block(file)
        succeeded = true
        return result
    } catch (failure: Throwable) {
        try {
            releaseCacheCopyFile(file)
        } catch (cleanupFailure: Throwable) {
            if (cleanupFailure !== failure) failure.addSuppressed(cleanupFailure)
        }
        throw failure
    } finally {
        if (succeeded) releaseCacheCopyFile(file)
    }
}

/** Checks cancellation inside the ownership scope so a skipped callback still releases the copy. */
internal suspend inline fun <T> withCancellationCheckedScopedCacheCopy(
    file: File,
    block: (File) -> T
): T = withScopedCacheCopy(file) {
    currentCoroutineContext().ensureActive()
    block(it)
}

/** Avoid provider acquisition after cancellation while registered cleanup owns the temp file. */
internal suspend inline fun <T> withCancellationCheckedRegisteredCacheCopy(
    file: File,
    keepRegistered: Boolean,
    acquireAndCopy: () -> T
): T = withRegisteredCacheCopy(file, keepRegistered) {
    currentCoroutineContext().ensureActive()
    acquireAndCopy()
}
