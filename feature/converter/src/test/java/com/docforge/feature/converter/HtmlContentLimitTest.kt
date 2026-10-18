package com.docforge.feature.converter

import org.junit.Assert.assertThrows
import org.junit.Test

class HtmlContentLimitTest {
    @Test fun acceptsContentBelowLimit() {
        requireHtmlContentWithinLimit("<p>ok</p>", 16)
    }

    @Test fun acceptsExactLimit() {
        requireHtmlContentWithinLimit("12345", 5)
    }

    @Test fun rejectsOneCharacterOverLimit() {
        assertThrows(IllegalArgumentException::class.java) {
            requireHtmlContentWithinLimit("123456", 5)
        }
    }

    @Test fun rejectsNonPositiveLimit() {
        assertThrows(IllegalArgumentException::class.java) {
            requireHtmlContentWithinLimit("", 0)
        }
    }
}
