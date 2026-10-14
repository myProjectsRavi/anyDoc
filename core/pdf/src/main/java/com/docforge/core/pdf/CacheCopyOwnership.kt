package com.docforge.core.pdf

import com.docforge.core.domain.io.ActiveTempFileRegistry
import java.io.File

internal fun releaseCacheCopyFile(file: File) {
    ActiveTempFileRegistry.unregister(file)
    ActiveTempFileRegistry.deleteIfInactive(file)
}
