package com.docforge.core.pdf

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class PdfRedactionPagingTest {
    @Test
    fun scanPagesUntil_readsEachPageInOrderWhenNoMatchExists() {
        val visited = mutableListOf<Int>()

        val result = scanPagesUntil(
            pageCount = 4,
            readPage = { page -> visited += page; "page-$page" },
            stopWhen = { false }
        )

        assertNull(result)
        assertEquals(listOf(0, 1, 2, 3), visited)
    }

    @Test
    fun scanPagesUntil_stopsImmediatelyAtFirstMatch() {
        val visited = mutableListOf<Int>()

        val result = scanPagesUntil(
            pageCount = 5,
            readPage = { page -> visited += page; "page-$page" },
            stopWhen = { it == "page-2" }
        )

        assertEquals("page-2", result)
        assertEquals(listOf(0, 1, 2), visited)
    }

    @Test
    fun scanPagesUntil_handlesEmptyDocumentWithoutReading() {
        var reads = 0

        val result = scanPagesUntil(
            pageCount = 0,
            readPage = { reads += 1; it },
            stopWhen = { true }
        )

        assertNull(result)
        assertEquals(0, reads)
    }

    @Test
    fun scanPagesUntil_rejectsNegativePageCount() {
        assertThrows(IllegalArgumentException::class.java) {
            scanPagesUntil(
                pageCount = -1,
                readPage = { it },
                stopWhen = { false }
            )
        }
    }
}
