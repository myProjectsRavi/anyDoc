package com.docforge.core.pdf

import android.content.Context
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Singleton guard for PDFBoxResourceLoader.init().
 * PDFBoxResourceLoader.init() is idempotent but has internal synchronization cost.
 * This ensures it is called exactly once per process, on the IO thread via EngineWarmup.
 */
object PdfBoxInit {
    private val initialized = AtomicBoolean(false)

    fun ensure(context: Context) {
        if (initialized.compareAndSet(false, true)) {
            PDFBoxResourceLoader.init(context.applicationContext)
        }
    }
}
