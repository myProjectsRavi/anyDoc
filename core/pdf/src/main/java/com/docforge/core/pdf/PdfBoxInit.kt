package com.docforge.core.pdf

import android.content.Context
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

/**
 * Small retryable one-time initializer.
 *
 * The success flag is published only after [block] returns successfully. Concurrent callers
 * serialize through the monitor, so no caller can observe an initialized state while the
 * initialization work is still running. A failure leaves the initializer retryable.
 */
internal class RetryableOnceInitializer {
    @Volatile
    private var initialized = false

    fun run(block: () -> Unit) {
        if (initialized) return

        synchronized(this) {
            if (initialized) return
            block()
            initialized = true
        }
    }
}

/**
 * Process-wide guard for PDFBoxResourceLoader.init().
 *
 * Initialization is deliberately marked complete only after PDFBox finishes successfully.
 * This prevents a concurrent PDF operation from racing ahead of the loader and allows a later
 * call to retry if initialization throws.
 */
object PdfBoxInit {
    private val initializer = RetryableOnceInitializer()

    fun ensure(context: Context) {
        initializer.run {
            PDFBoxResourceLoader.init(context.applicationContext)
        }
    }
}
