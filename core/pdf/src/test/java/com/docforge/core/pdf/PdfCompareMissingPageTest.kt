package com.docforge.core.pdf

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class PdfCompareMissingPageTest {
    @Test
    fun missingLeftHighlightsRightCopyAndReportsEntirePageChanged() {
        val original = Any()
        val highlighted = Any()
        var invocations = 0
        val result = missingPageDiff(null, original) { source ->
            assertSame(original, source)
            invocations++
            highlighted
        }
        assertSame(highlighted, result!!.first)
        assertEquals(100f, result.second, 0f)
        assertEquals(1, invocations)
    }

    @Test
    fun missingRightHighlightsLeftCopyAndReportsEntirePageChanged() {
        val original = Any()
        val highlighted = Any()
        val result = missingPageDiff(original, null) { source ->
            assertSame(original, source)
            highlighted
        }
        assertSame(highlighted, result!!.first)
        assertEquals(100f, result.second, 0f)
    }

    @Test
    fun noMissingSideDoesNotRunOverlayCallback() {
        var calls = 0
        assertNull(missingPageDiff(Any(), Any()) { source -> calls++; source })
        assertNull(missingPageDiff<Any>(null, null) { source -> calls++; source })
        assertEquals(0, calls)
    }

    @Test
    fun missingPageOverlayIsTranslucentRedRatherThanOpaqueReplacement() {
        val color = MISSING_PAGE_OVERLAY_COLOR
        val alpha = (color ushr 24) and 0xff
        val red = (color ushr 16) and 0xff
        val green = (color ushr 8) and 0xff
        val blue = color and 0xff
        assertTrue(alpha in 1..254)
        assertTrue(red > green)
        assertTrue(red > blue)
    }

    @Test
    fun overlayFailurePropagatesWithoutPublishingAResult() {
        val failure = IllegalStateException("highlight failed")
        try {
            missingPageDiff(null, Any()) { _: Any -> throw failure }
            org.junit.Assert.fail("Expected failure")
        } catch (caught: IllegalStateException) {
            assertSame(failure, caught)
        }
    }
}
