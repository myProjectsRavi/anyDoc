package com.docforge.feature.pdftools

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import java.io.ByteArrayOutputStream

/**
 * Generates typed (font-based) signatures as PNG bitmaps
 * using calligraphy-style system fonts.
 *
 * Sprint 5 feature — Typed Signature (Font-Based Calligraphy).
 */
class TypedSignatureRenderer {

    data class TypedSignatureResult(
        val bitmap: Bitmap,
        val pngBytes: ByteArray
    ) {
        override fun equals(other: Any?): Boolean = other is TypedSignatureResult && pngBytes.contentEquals(other.pngBytes)
        override fun hashCode(): Int = pngBytes.contentHashCode()
    }

    enum class SignatureStyle(val fontFamily: String, val typefaceStyle: Int) {
        SERIF_ITALIC("serif", Typeface.BOLD_ITALIC),
        SANS_ITALIC("sans-serif", Typeface.ITALIC),
        CURSIVE("cursive", Typeface.NORMAL),
        MONOSPACE_BOLD("monospace", Typeface.BOLD),
        SERIF_BOLD("serif", Typeface.BOLD)
    }

    /**
     * Renders [name] in the specified [style] at [fontSize].
     * Returns a transparent-background PNG bitmap.
     *
     * @param name      the text to render as a signature.
     * @param style     which font style to use.
     * @param fontSize  text size in pixels (default 64).
     * @param inkColor  color of the text (default dark navy blue).
     * @param padding   padding around the text in pixels.
     */
    fun render(
        name: String,
        style: SignatureStyle = SignatureStyle.CURSIVE,
        fontSize: Float = 64f,
        inkColor: Int = Color.rgb(20, 20, 80),
        padding: Int = 16
    ): TypedSignatureResult {
        require(name.isNotBlank()) { "Signature name cannot be blank." }

        val typeface = Typeface.create(style.fontFamily, style.typefaceStyle)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            this.textSize = fontSize.coerceIn(24f, 200f)
            this.color = inkColor
        }

        val textWidth = paint.measureText(name)
        val metrics = paint.fontMetrics
        val textHeight = metrics.descent - metrics.ascent

        val bitmapWidth = (textWidth + padding * 2).toInt().coerceAtLeast(1)
        val bitmapHeight = (textHeight + padding * 2).toInt().coerceAtLeast(1)

        val bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        // Transparent background — no drawColor

        val x = padding.toFloat()
        val y = padding - metrics.ascent
        canvas.drawText(name, x, y, paint)

        val pngStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, pngStream)

        return TypedSignatureResult(
            bitmap = bitmap,
            pngBytes = pngStream.toByteArray()
        )
    }

    /**
     * Returns all available signature styles with preview text.
     */
    fun previewAllStyles(name: String, fontSize: Float = 48f): List<Pair<SignatureStyle, Bitmap>> {
        return SignatureStyle.entries.map { style ->
            style to render(name, style, fontSize).bitmap
        }
    }
}
