package com.docforge.feature.converter

import java.io.StringReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BoundedTextReaderTest {
    @Test fun readsContentBelowLimit() {
        assertEquals("hello", StringReader("hello").readTextBounded(8))
    }

    @Test fun acceptsExactLimit() {
        assertEquals("12345", StringReader("12345").readTextBounded(5))
    }

    @Test fun rejectsOneCharacterOverLimit() {
        assertThrows(IllegalArgumentException::class.java) {
            StringReader("123456").readTextBounded(5)
        }
    }

    @Test fun supportsMaximumIntLimitWithoutOverflow() {
        assertEquals("ok", StringReader("ok").readTextBounded(Int.MAX_VALUE))
    }

    @Test fun rejectsNonPositiveLimit() {
        assertThrows(IllegalArgumentException::class.java) {
            StringReader("").readTextBounded(0)
        }
    }

    @Test fun boundedLinesPreserveEmptyInput() {
        assertEquals(emptyList<String>(), StringReader("").readLinesBounded(8))
    }

    @Test fun boundedLinesDoNotInventTrailingRow() {
        assertEquals(listOf("a"), StringReader("a\n").readLinesBounded(8))
    }

    @Test fun boundedLinesPreserveRealBlankTrailingRow() {
        assertEquals(listOf("a", ""), StringReader("a\n\n").readLinesBounded(8))
    }

    @Test fun boundedLinesRejectOversizeInput() {
        assertThrows(IllegalArgumentException::class.java) {
            StringReader("123456").readLinesBounded(5)
        }
    }
}
