package com.docforge.feature.pdftools

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class TypedSignatureRendererTest {
    @Test
    fun boundedTypedSignatureBitmapSize_preservesSafeDimensions() {
        assertEquals(TypedSignatureBitmapSize(132, 72), boundedTypedSignatureBitmapSize(100f, 40f, 16))
    }

    @Test
    fun boundedTypedSignatureBitmapSize_acceptsExactLimits() {
        assertEquals(TypedSignatureBitmapSize(4096, 1024), boundedTypedSignatureBitmapSize(4096f, 1024f, 0))
    }

    @Test
    fun boundedTypedSignatureBitmapSize_rejectsWidthAboveLimit() {
        assertThrows(IllegalArgumentException::class.java) {
            boundedTypedSignatureBitmapSize(4097f, 1024f, 0)
        }
    }

    @Test
    fun boundedTypedSignatureBitmapSize_rejectsHeightAboveLimit() {
        assertThrows(IllegalArgumentException::class.java) {
            boundedTypedSignatureBitmapSize(4096f, 1025f, 0)
        }
    }

    @Test
    fun boundedTypedSignatureBitmapSize_rejectsInvalidPadding() {
        assertThrows(IllegalArgumentException::class.java) {
            boundedTypedSignatureBitmapSize(100f, 40f, 1025)
        }
    }

    @Test
    fun boundedTypedSignatureBitmapSize_rejectsNonFiniteMeasurement() {
        assertThrows(IllegalArgumentException::class.java) {
            boundedTypedSignatureBitmapSize(Float.POSITIVE_INFINITY, 40f, 16)
        }
    }
}
