package com.docforge.core.pdf

import java.util.concurrent.CancellationException
import kotlin.math.abs
import kotlin.random.Random
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test

class PdfCompareRowChunkTest {
    @Test fun rowChunkBoundariesPreserveCancellationCheckpoints() {
        for (width in listOf(0, 1, 1024, 1025, 4096, 4097, 8193)) {
            val checks = mutableListOf<Int>()
            val chunks = mutableListOf<Pair<Int, Int>>()
            var processed = 0
            forEachDiffChunk(width, { checks += processed }) { start, count ->
                assertEquals(processed, start)
                assertEquals(true, count in 1..COMPARE_DIFF_ROW_CHUNK_PIXELS)
                chunks += start to count
                processed += count
            }
            assertEquals(width, processed)
            assertEquals((0 until width step 4096).toList(), checks)
            assertEquals((width + COMPARE_DIFF_ROW_CHUNK_PIXELS - 1) / COMPARE_DIFF_ROW_CHUNK_PIXELS, chunks.size)
        }
    }

    @Test fun cancellationBeforeFirstChunkDoesNotProcessPixels() {
        val cancelled = CancellationException("stop")
        var visited = 0
        val caught = assertThrows(CancellationException::class.java) {
            forEachDiffChunk(9000, { throw cancelled }) { _, count -> visited += count }
        }
        assertSame(cancelled, caught)
        assertEquals(0, visited)
    }

    @Test fun cancellationAtNext4096BoundaryStopsAfterFourChunks() {
        val cancelled = CancellationException("stop")
        var checks = 0
        var visited = 0
        val caught = assertThrows(CancellationException::class.java) {
            forEachDiffChunk(9000, {
                if (++checks == 2) throw cancelled
            }) { _, count -> visited += count }
        }
        assertSame(cancelled, caught)
        assertEquals(2, checks)
        assertEquals(4096, visited)
    }

    @Test fun exactThresholdAndAlphaSemanticsArePreserved() {
        val left = intArrayOf(0x000A141E, 0xFF0A141E.toInt(), 0x7F0A141E, 0xFF112233.toInt())
        val right = intArrayOf(0xFF28141E.toInt(), 0x000A141E, 0xFF29141E.toInt(), 0x00112233)
        val output = IntArray(4)
        val changed = compareDiffArgbChunk(left, right, output, 4, 30)
        assertEquals(1, changed)
        assertEquals(0xC8FF3232.toInt(), output[2])
        assertEquals(output[0], output[1])
        assertEquals(output[1] != 0, true)
        assertEquals(output[3], referencePixel(left[3], right[3], 30).first)
    }

    @Test fun deterministicArgbChunksMatchOriginalPixelMath() {
        val random = Random(109)
        val left = IntArray(COMPARE_DIFF_ROW_CHUNK_PIXELS)
        val right = IntArray(COMPARE_DIFF_ROW_CHUNK_PIXELS)
        val actual = IntArray(COMPARE_DIFF_ROW_CHUNK_PIXELS)
        val expected = IntArray(COMPARE_DIFF_ROW_CHUNK_PIXELS)
        for (threshold in listOf(0, 1, 29, 30, 31, 254, 255)) {
            repeat(100) {
                val count = random.nextInt(1, COMPARE_DIFF_ROW_CHUNK_PIXELS + 1)
                for (index in 0 until count) {
                    left[index] = random.nextInt()
                    right[index] = random.nextInt()
                }
                var expectedChanges = 0
                for (index in 0 until count) {
                    val (color, different) = referencePixel(left[index], right[index], threshold)
                    expected[index] = color
                    if (different) expectedChanges++
                }
                assertEquals(expectedChanges, compareDiffArgbChunk(left, right, actual, count, threshold))
                assertArrayEquals(expected.copyOf(count), actual.copyOf(count))
            }
        }
    }

    @Test fun unequalPageWhitePaddingUsesExistingColorSemantics() {
        val white = 0xFFFFFFFF.toInt()
        val left = intArrayOf(0xFF123456.toInt(), white, white)
        val right = intArrayOf(white, white, 0xFF123456.toInt())
        val output = IntArray(3)
        assertEquals(2, compareDiffArgbChunk(left, right, output, 3, 30))
        assertEquals(0xC8FF3232.toInt(), output[0])
        assertEquals(0xC8FF3232.toInt(), output[2])
    }

    private fun referencePixel(left: Int, right: Int, threshold: Int): Pair<Int, Boolean> {
        fun red(value: Int) = (value shr 16) and 255
        fun green(value: Int) = (value shr 8) and 255
        fun blue(value: Int) = value and 255
        val changed = abs(red(left) - red(right)) > threshold ||
            abs(green(left) - green(right)) > threshold ||
            abs(blue(left) - blue(right)) > threshold
        return if (changed) 0xC8FF3232.toInt() to true else {
            val grey = (red(left) * 0.3f + green(left) * 0.59f + blue(left) * 0.11f).toInt()
            (0xFF000000.toInt() or (grey shl 16) or (grey shl 8) or grey) to false
        }
    }
}
