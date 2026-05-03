package com.docforge.app.runtime

import android.content.Context
import com.docforge.core.pdf.PdfBoxInit
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

object EngineWarmup {
    private val started = AtomicBoolean(false)
    private val warmupScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Completes when OpenCV is ready (or failed). Callers can `await()` before using OpenCV. */
    val openCvReady = CompletableDeferred<Boolean>()

    /** Completes when PdfBox is ready. */
    val pdfBoxReady = CompletableDeferred<Boolean>()

    fun preWarm(context: Context) {
        if (!started.compareAndSet(false, true)) return

        val appContext = context.applicationContext
        warmupScope.launch(Dispatchers.IO) {
            val pdfOk = runCatching { PdfBoxInit.ensure(appContext) }.isSuccess
            pdfBoxReady.complete(pdfOk)

            val cvOk = runCatching {
                val openCvClass = Class.forName("org.opencv.android.OpenCVLoader")
                val initMethod = openCvClass.getMethod("initLocal")
                initMethod.invoke(null) as Boolean
            }.getOrDefault(false)
            openCvReady.complete(cvOk)
        }
    }
}
