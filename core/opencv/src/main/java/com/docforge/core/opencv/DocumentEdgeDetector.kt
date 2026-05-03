package com.docforge.core.opencv

import android.graphics.Bitmap
import android.graphics.PointF
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max

class DocumentEdgeDetector {
    private val openCvReady: Boolean = OpenCVLoader.initLocal()
    private val maxPerspectiveEdgePx = 2000.0

    fun isAvailable(): Boolean = openCvReady

    fun detectDocumentBounds(frameWidth: Int, frameHeight: Int): DetectedDocument? {
        if (frameWidth <= 0 || frameHeight <= 0) return null
        return fallbackBounds(frameWidth, frameHeight)
    }

    fun detectDocumentBounds(
        lumaBytes: ByteArray,
        frameWidth: Int,
        frameHeight: Int,
        rotationDegrees: Int = 0
    ): DetectedDocument? {
        if (!openCvReady || frameWidth <= 0 || frameHeight <= 0) {
            return fallbackBounds(frameWidth, frameHeight)
        }

        val required = frameWidth * frameHeight
        if (lumaBytes.size < required) {
            return fallbackBounds(frameWidth, frameHeight)
        }

        val grayMat = Mat(frameHeight, frameWidth, CvType.CV_8UC1)
        var rotatedGray: Mat? = null
        return try {
            grayMat.put(0, 0, lumaBytes, 0, required)
            val normalizedRotation = normalizeRotation(rotationDegrees)
            val input = when (normalizedRotation) {
                90 -> {
                    Mat().also { rotated ->
                        Core.rotate(grayMat, rotated, Core.ROTATE_90_CLOCKWISE)
                        rotatedGray = rotated
                    }
                }
                180 -> {
                    Mat().also { rotated ->
                        Core.rotate(grayMat, rotated, Core.ROTATE_180)
                        rotatedGray = rotated
                    }
                }
                270 -> {
                    Mat().also { rotated ->
                        Core.rotate(grayMat, rotated, Core.ROTATE_90_COUNTERCLOCKWISE)
                        rotatedGray = rotated
                    }
                }
                else -> grayMat
            }

            detectFromGray(input) ?: fallbackBounds(input.cols(), input.rows())
        } catch (_: Throwable) {
            fallbackBounds(frameWidth, frameHeight)
        } finally {
            rotatedGray?.release()
            grayMat.release()
        }
    }

    fun detectDocumentBounds(bitmap: Bitmap): DetectedDocument? {
        if (!openCvReady || bitmap.width <= 0 || bitmap.height <= 0) {
            return fallbackBounds(bitmap.width, bitmap.height)
        }

        val rgba = Mat()
        val gray = Mat()
        return try {
            Utils.bitmapToMat(bitmap, rgba)
            Imgproc.cvtColor(rgba, gray, Imgproc.COLOR_RGBA2GRAY)
            detectFromGray(gray) ?: fallbackBounds(bitmap.width, bitmap.height)
        } catch (_: Throwable) {
            fallbackBounds(bitmap.width, bitmap.height)
        } finally {
            gray.release()
            rgba.release()
        }
    }

    fun perspectiveCorrect(bitmap: Bitmap, corners: List<PointF>): Bitmap? {
        if (!openCvReady || corners.size != 4 || bitmap.width <= 0 || bitmap.height <= 0) return null
        val ordered = orderCorners(corners)

        val source = Mat()
        val transformed = Mat()
        val srcPoints = MatOfPoint2f()
        val dstPoints = MatOfPoint2f()
        var perspective: Mat? = null

        return try {
            Utils.bitmapToMat(bitmap, source)

            val topWidth = distance(ordered[0], ordered[1])
            val bottomWidth = distance(ordered[3], ordered[2])
            val leftHeight = distance(ordered[0], ordered[3])
            val rightHeight = distance(ordered[1], ordered[2])

            val rawWidth = max(1.0, max(topWidth, bottomWidth))
            val rawHeight = max(1.0, max(leftHeight, rightHeight))
            val longestEdge = max(rawWidth, rawHeight)
            val downscale = if (longestEdge > maxPerspectiveEdgePx) {
                maxPerspectiveEdgePx / longestEdge
            } else {
                1.0
            }

            val outputWidth = max(1.0, rawWidth * downscale).toInt()
            val outputHeight = max(1.0, rawHeight * downscale).toInt()

            srcPoints.fromArray(
                Point(ordered[0].x.toDouble(), ordered[0].y.toDouble()),
                Point(ordered[1].x.toDouble(), ordered[1].y.toDouble()),
                Point(ordered[2].x.toDouble(), ordered[2].y.toDouble()),
                Point(ordered[3].x.toDouble(), ordered[3].y.toDouble())
            )
            dstPoints.fromArray(
                Point(0.0, 0.0),
                Point((outputWidth - 1).toDouble(), 0.0),
                Point((outputWidth - 1).toDouble(), (outputHeight - 1).toDouble()),
                Point(0.0, (outputHeight - 1).toDouble())
            )

            perspective = Imgproc.getPerspectiveTransform(srcPoints, dstPoints)
            Imgproc.warpPerspective(source, transformed, perspective, Size(outputWidth.toDouble(), outputHeight.toDouble()))

            val outputBitmap = Bitmap.createBitmap(outputWidth, outputHeight, Bitmap.Config.ARGB_8888)
            Utils.matToBitmap(transformed, outputBitmap)
            outputBitmap
        } catch (_: Throwable) {
            null
        } finally {
            perspective?.release()
            dstPoints.release()
            srcPoints.release()
            transformed.release()
            source.release()
        }
    }

    fun autoCorrect(bitmap: Bitmap): Bitmap? {
        val detected = detectDocumentBounds(bitmap) ?: return null
        if (detected.confidence < 0.2f) return null
        return perspectiveCorrect(bitmap, detected.corners)
    }

    fun applyGrayscaleFilter(bitmap: Bitmap): Bitmap? {
        return transformBitmap(bitmap) { sourceRgba, outputRgba ->
            val gray = Mat()
            try {
                Imgproc.cvtColor(sourceRgba, gray, Imgproc.COLOR_RGBA2GRAY)
                Imgproc.cvtColor(gray, outputRgba, Imgproc.COLOR_GRAY2RGBA)
            } finally {
                gray.release()
            }
        }
    }

    /**
     * Applies Otsu's adaptive thresholding for B&W conversion.
     * If [threshold] > 0 it is used as a fixed cutoff; otherwise Otsu picks the optimal value.
     */
    fun applyBlackWhiteFilter(bitmap: Bitmap, threshold: Double = 0.0): Bitmap? {
        return transformBitmap(bitmap) { sourceRgba, outputRgba ->
            val gray = Mat()
            val binary = Mat()
            try {
                Imgproc.cvtColor(sourceRgba, gray, Imgproc.COLOR_RGBA2GRAY)
                if (threshold > 0.0) {
                    Imgproc.threshold(gray, binary, threshold, 255.0, Imgproc.THRESH_BINARY)
                } else {
                    // Otsu automatically determines the best global threshold
                    Imgproc.threshold(gray, binary, 0.0, 255.0, Imgproc.THRESH_BINARY + Imgproc.THRESH_OTSU)
                }
                Imgproc.cvtColor(binary, outputRgba, Imgproc.COLOR_GRAY2RGBA)
            } finally {
                binary.release()
                gray.release()
            }
        }
    }

    fun applyEnhancedFilter(bitmap: Bitmap): Bitmap? {
        return transformBitmap(bitmap) { sourceRgba, outputRgba ->
            val gray = Mat()
            val blurred = Mat()
            val sharpened = Mat()
            try {
                Imgproc.cvtColor(sourceRgba, gray, Imgproc.COLOR_RGBA2GRAY)
                Imgproc.GaussianBlur(gray, blurred, Size(0.0, 0.0), 2.2)
                Core.addWeighted(gray, 1.55, blurred, -0.55, 0.0, sharpened)
                Imgproc.cvtColor(sharpened, outputRgba, Imgproc.COLOR_GRAY2RGBA)
            } finally {
                sharpened.release()
                blurred.release()
                gray.release()
            }
        }
    }

    private fun detectFromGray(grayInput: Mat): DetectedDocument? {
        if (grayInput.empty()) return null

        val blurred = Mat()
        val edges = Mat()
        val hierarchy = Mat()
        val contours = mutableListOf<MatOfPoint>()

        var bestArea = 0.0
        var bestCorners: List<PointF>? = null

        return try {
            Imgproc.GaussianBlur(grayInput, blurred, Size(5.0, 5.0), 0.0)
            Imgproc.Canny(blurred, edges, 75.0, 200.0)
            Imgproc.findContours(edges, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE)

            contours.forEach { contour ->
                val contour2f = MatOfPoint2f(*contour.toArray())
                val approx2f = MatOfPoint2f()
                try {
                    val perimeter = Imgproc.arcLength(contour2f, true)
                    if (perimeter <= 0.0) return@forEach
                    Imgproc.approxPolyDP(contour2f, approx2f, 0.02 * perimeter, true)
                    if (approx2f.total() != 4L) return@forEach

                    val approxPoints = approx2f.toArray()
                    val approxAsMatOfPoint = MatOfPoint(*approxPoints)
                    val isConvex = Imgproc.isContourConvex(approxAsMatOfPoint)
                    val area = abs(Imgproc.contourArea(approx2f))
                    approxAsMatOfPoint.release()
                    if (!isConvex || area <= bestArea) return@forEach

                    val areaRatio = area / (grayInput.cols().toDouble() * grayInput.rows().toDouble())
                    if (areaRatio < 0.15) return@forEach

                    bestArea = area
                    bestCorners = approxPoints.map { point ->
                        PointF(point.x.toFloat(), point.y.toFloat())
                    }
                } finally {
                    approx2f.release()
                    contour2f.release()
                }
            }

            val corners = bestCorners ?: return null
            val orderedCorners = orderCorners(corners)
            val confidence = (bestArea / (grayInput.cols().toDouble() * grayInput.rows().toDouble()))
                .coerceIn(0.0, 1.0)
                .toFloat()
            DetectedDocument(corners = orderedCorners, confidence = confidence)
        } finally {
            contours.forEach { it.release() }
            hierarchy.release()
            edges.release()
            blurred.release()
        }
    }

    private fun orderCorners(points: List<PointF>): List<PointF> {
        require(points.size == 4) { "Exactly 4 corners are required" }
        val sortedByY = points.sortedBy { it.y }
        val top = sortedByY.take(2).sortedBy { it.x }
        val bottom = sortedByY.takeLast(2).sortedBy { it.x }
        return listOf(
            top[0],      // top-left
            top[1],      // top-right
            bottom[1],   // bottom-right
            bottom[0]    // bottom-left
        )
    }

    private fun fallbackBounds(frameWidth: Int, frameHeight: Int): DetectedDocument? {
        if (frameWidth <= 0 || frameHeight <= 0) return null
        val insetX = frameWidth * 0.08f
        val insetY = frameHeight * 0.12f
        return DetectedDocument(
            corners = listOf(
                PointF(insetX, insetY),
                PointF(frameWidth - insetX, insetY),
                PointF(frameWidth - insetX, frameHeight - insetY),
                PointF(insetX, frameHeight - insetY)
            ),
            confidence = 0.25f
        )
    }

    private fun normalizeRotation(rotation: Int): Int {
        val normalized = ((rotation % 360) + 360) % 360
        return when (normalized) {
            90, 180, 270 -> normalized
            else -> 0
        }
    }

    private fun distance(a: PointF, b: PointF): Double {
        return hypot((a.x - b.x).toDouble(), (a.y - b.y).toDouble())
    }

    private inline fun transformBitmap(
        bitmap: Bitmap,
        transform: (sourceRgba: Mat, outputRgba: Mat) -> Unit
    ): Bitmap? {
        if (!openCvReady || bitmap.width <= 0 || bitmap.height <= 0) return null
        val sourceRgba = Mat()
        val outputRgba = Mat()
        return try {
            Utils.bitmapToMat(bitmap, sourceRgba)
            transform(sourceRgba, outputRgba)
            val width = outputRgba.cols().coerceAtLeast(1)
            val height = outputRgba.rows().coerceAtLeast(1)
            val outputBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            Utils.matToBitmap(outputRgba, outputBitmap)
            outputBitmap
        } catch (_: Throwable) {
            null
        } finally {
            outputRgba.release()
            sourceRgba.release()
        }
    }
}

data class DetectedDocument(
    val corners: List<PointF>,
    val confidence: Float
)
