package com.docforge.core.pdf

import org.junit.Assert.assertEquals
import org.junit.Test

class PdfCompareScaleTest {
    @Test
    fun nanScaleUsesDefaultInsteadOfCollapsingPagesToOnePixel() {
        assertEquals(1.5f, normalizedCompareRenderScale(Float.NaN), 0f)
        assertEquals(892, (595f * normalizedCompareRenderScale(Float.NaN)).toInt())
    }

    @Test
    fun infiniteScalesRetainExistingClampBehavior() {
        assertEquals(0.5f, normalizedCompareRenderScale(Float.NEGATIVE_INFINITY), 0f)
        assertEquals(4f, normalizedCompareRenderScale(Float.POSITIVE_INFINITY), 0f)
    }

    @Test
    fun finiteScalesRetainExistingBoundsAndPrecision() {
        assertEquals(0.5f, normalizedCompareRenderScale(-10f), 0f)
        assertEquals(0.5f, normalizedCompareRenderScale(0.5f), 0f)
        assertEquals(1.5f, normalizedCompareRenderScale(1.5f), 0f)
        assertEquals(2.25f, normalizedCompareRenderScale(2.25f), 0f)
        assertEquals(4f, normalizedCompareRenderScale(4f), 0f)
        assertEquals(4f, normalizedCompareRenderScale(100f), 0f)
    }
}
