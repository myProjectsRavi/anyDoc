package com.docforge.app.share

import android.content.Intent
import android.net.Uri
import android.os.Build
import com.docforge.app.navigation.Routes
import java.util.Locale

data class ShareLaunchRequest(
    val targetRoute: String,
    val uris: List<Uri>,
    val mimeType: String?
)

object ShareIntentRouter {
    fun fromIntent(intent: Intent?): ShareLaunchRequest? {
        if (intent == null) return null
        val action = intent.action ?: return null
        if (action != Intent.ACTION_SEND && action != Intent.ACTION_SEND_MULTIPLE) return null

        val uris = extractUris(intent).distinct()
        if (uris.isEmpty()) return null

        val mimeType = intent.type?.lowercase(Locale.US)
        val route = chooseRoute(mimeType = mimeType, uris = uris)
        return ShareLaunchRequest(
            targetRoute = route,
            uris = uris,
            mimeType = mimeType
        )
    }

    private fun chooseRoute(mimeType: String?, uris: List<Uri>): String {
        val normalized = mimeType.orEmpty().lowercase(Locale.US)

        if (normalized.startsWith("image/")) return Routes.IMAGE_FORMAT
        if (normalized.startsWith("video/")) return Routes.VIDEO_TO_AUDIO
        if (normalized.startsWith("audio/")) return Routes.AUDIO_FORMAT
        if (normalized == "application/pdf") return Routes.PDF_MERGE
        if (isDocumentMime(normalized)) return Routes.DOC_TO_PDF

        val extension = uris.firstOrNull()
            ?.toString()
            ?.substringAfterLast('.', missingDelimiterValue = "")
            ?.lowercase(Locale.US)
            .orEmpty()

        return when (extension) {
            "pdf" -> Routes.PDF_MERGE
            "jpg", "jpeg", "png", "webp", "heic", "heif", "bmp", "tif", "tiff" -> Routes.IMAGE_FORMAT
            "mp4", "mov", "mkv", "webm", "3gp", "avi" -> Routes.VIDEO_TO_AUDIO
            "mp3", "m4a", "wav", "flac", "aac", "ogg", "opus" -> Routes.AUDIO_FORMAT
            "doc", "docx", "rtf", "txt", "csv" -> Routes.DOC_TO_PDF
            else -> Routes.HOME
        }
    }

    private fun isDocumentMime(mimeType: String): Boolean {
        return mimeType in setOf(
            "text/plain",
            "text/csv",
            "text/rtf",
            "application/rtf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        )
    }

    private fun extractUris(intent: Intent): List<Uri> {
        return when (intent.action) {
            Intent.ACTION_SEND -> listOfNotNull(readSingleUri(intent))
            Intent.ACTION_SEND_MULTIPLE -> readMultipleUris(intent)
            else -> emptyList()
        }
    }

    private fun readSingleUri(intent: Intent): Uri? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(Intent.EXTRA_STREAM)
        }
    }

    private fun readMultipleUris(intent: Intent): List<Uri> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java).orEmpty()
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM).orEmpty()
        }
    }
}
