package com.docforge.feature.converter

import android.content.Context
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.webkit.WebView
import android.webkit.WebViewClient
import com.docforge.core.domain.settings.DocForgeOutputBucket
import com.docforge.core.domain.settings.DocForgeSettingsStore
import com.docforge.core.pdf.PdfCreationResult
import com.docforge.core.pdf.resolveNonConflictingFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume
import kotlin.math.ceil

/**
 * Converts HTML content (from a file URI or raw string) to PDF
 * using Android's [WebView] rendered onto [PdfDocument] pages.
 *
 * Sprint 4 feature — HTML → PDF.
 *
 * Must be called from the **main thread** for WebView creation.
 * The suspend function switches to Main internally.
 */
class HtmlPdfConverter(
    private val context: Context
) {

    data class HtmlPdfConversionResult(
        val pdfResult: PdfCreationResult,
        val inputType: String = "HTML"
    )

    companion object {
        /** A4 at 72 DPI */
        private const val PAGE_WIDTH = 595
        private const val PAGE_HEIGHT = 842
    }

    /**
     * Loads HTML from [inputUri] and produces a PDF.
     */
    suspend fun convertToPdf(
        inputUri: Uri,
        outputName: String
    ): HtmlPdfConversionResult {
        val htmlContent = withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(inputUri)?.bufferedReader()?.use { it.readText() }
                ?: error("Unable to read HTML file.")
        }
        return convertHtmlStringToPdf(htmlContent, outputName)
    }

    /**
     * Renders raw HTML string to PDF by drawing a WebView onto PdfDocument pages.
     */
    suspend fun convertHtmlStringToPdf(
        htmlContent: String,
        outputName: String
    ): HtmlPdfConversionResult = withContext(Dispatchers.Main) {
        require(htmlContent.isNotBlank()) { "HTML content is empty." }

        val outputDir = DocForgeSettingsStore.resolveOutputDirectory(
            context = context,
            bucket = DocForgeOutputBucket.DOCUMENTS
        )
        val sanitized = outputName.ifBlank { "html_${System.currentTimeMillis()}" }
            .replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val outputFile = resolveNonConflictingFile(outputDir, sanitized, "pdf")

        val webView = WebView(context).apply {
            settings.javaScriptEnabled = false
            settings.allowFileAccess = false
        }

        try {
            // Wait for WebView to finish loading
            suspendCancellableCoroutine { cont ->
                webView.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        cont.resume(Unit)
                    }
                }
                webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
            }

            // Let WebView finish layout
            delay(200)

            // Measure content
            webView.measure(
                android.view.View.MeasureSpec.makeMeasureSpec(PAGE_WIDTH, android.view.View.MeasureSpec.EXACTLY),
                android.view.View.MeasureSpec.makeMeasureSpec(0, android.view.View.MeasureSpec.UNSPECIFIED)
            )
            webView.layout(0, 0, PAGE_WIDTH, webView.measuredHeight)

            val totalHeight = webView.measuredHeight.coerceAtLeast(1)
            val pageCount = ceil(totalHeight.toFloat() / PAGE_HEIGHT).toInt().coerceAtLeast(1)

            val pdfDocument = PdfDocument()
            try {
                for (i in 0 until pageCount) {
                    val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, i + 1).create()
                    val page = pdfDocument.startPage(pageInfo)
                    val canvas = page.canvas
                    canvas.translate(0f, -(i * PAGE_HEIGHT).toFloat())
                    webView.draw(canvas)
                    pdfDocument.finishPage(page)
                }

                withContext(Dispatchers.IO) {
                    FileOutputStream(outputFile).use { out ->
                        pdfDocument.writeTo(out)
                    }
                }
            } finally {
                pdfDocument.close()
            }

            HtmlPdfConversionResult(
                pdfResult = PdfCreationResult(
                    outputFile = outputFile,
                    pageCount = pageCount,
                    outputSizeBytes = outputFile.length()
                )
            )
        } finally {
            webView.destroy()
        }
    }
}
