package com.docforge.app.runtime

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

object EngineWarmup {
    private val started = AtomicBoolean(false)
    private val warmupScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun preWarm(context: Context) {
        if (!started.compareAndSet(false, true)) return

        val appContext = context.applicationContext
        warmupScope.launch(Dispatchers.IO) {
            runCatching {
                val loaderClass = Class.forName("com.tom_roush.pdfbox.android.PDFBoxResourceLoader")
                val initMethod = loaderClass.getMethod("init", Context::class.java)
                initMethod.invoke(null, appContext)
            }
            runCatching {
                val openCvClass = Class.forName("org.opencv.android.OpenCVLoader")
                val initMethod = openCvClass.getMethod("initLocal")
                initMethod.invoke(null)
            }
        }
    }
}
