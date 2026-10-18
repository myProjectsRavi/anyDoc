package com.docforge.core.pdf

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class PdfCompareChunkPaddingTest {
    private val white = 0xFFFFFFFF.toInt()
    private val stale = 0xFF123456.toInt()

    @Test fun fullChunkDoesNotOverwriteFreshPixels() {
        val pixels = IntArray(COMPARE_DIFF_ROW_CHUNK_PIXELS) { it * 173 + 1 }
        val expected = pixels.copyOf()
        fillCompareMissingChunkPixels(pixels, pixels.size, pixels.size)
        assertArrayEquals(expected, pixels)
    }

    @Test fun partiallyCoveredChunkKeepsSourcePrefixAndWhitesTail() {
        val pixels = intArrayOf(0xFF001122.toInt(), 0xFF334455.toInt(), stale, stale, stale)
        fillCompareMissingChunkPixels(pixels, 2, pixels.size)
        assertArrayEquals(
            intArrayOf(0xFF001122.toInt(), 0xFF334455.toInt(), white, white, white),
            pixels
        )
    }

    @Test fun entirelyMissingRowOrColumnBecomesWhite() {
        val pixels = IntArray(17) { stale }
        fillCompareMissingChunkPixels(pixels, 0, pixels.size)
        assertArrayEquals(IntArray(17) { white }, pixels)
    }

    @Test fun zeroLengthChunkLeavesBufferUntouched() {
        val pixels = intArrayOf(stale, stale, stale)
        fillCompareMissingChunkPixels(pixels, 0, 0)
        assertArrayEquals(intArrayOf(stale, stale, stale), pixels)
    }

    @Test fun reusedBufferCannotLeakPreviousRowsTail() {
        val pixels = IntArray(8) { stale }
        for (i in 0 until 8) pixels[i] = 0xFF000000.toInt() or i
        fillCompareMissingChunkPixels(pixels, 8, 8)
        for (i in 0 until 3) pixels[i] = 0xFF445500.toInt() or i
        fillCompareMissingChunkPixels(pixels, 3, 8)
        assertArrayEquals(
            intArrayOf(0xFF445500.toInt(), 0xFF445501.toInt(), 0xFF445502.toInt(),
                white, white, white, white, white),
            pixels
        )
    }

    @Test fun partialLastChunkAndFullCoverageBoundaries() {
        for (count in listOf(1, 2, 1023, COMPARE_DIFF_ROW_CHUNK_PIXELS)) {
            for (copied in listOf(0, count / 2, count).distinct()) {
                val pixels = IntArray(COMPARE_DIFF_ROW_CHUNK_PIXELS) { stale }
                fillCompareMissingChunkPixels(pixels, copied, count)
                for (index in pixels.indices) {
                    assertEquals("count=$count copied=$copied index=$index",
                        if (index in copied until count) white else stale, pixels[index])
                }
            }
        }
    }

    @Test fun missingTailProducesOriginalWhitePixelComparison() {
        val left = intArrayOf(0xFF000000.toInt(), stale, stale)
        val right = intArrayOf(0xFF000000.toInt(), white, white)
        fillCompareMissingChunkPixels(left, 1, 3)
        val output = IntArray(3)
        assertEquals(0, compareDiffArgbChunk(left, right, output, 3, 30))
        assertEquals(output[0] != 0, true)
    }
}
