package com.docforge.core.pdf

import java.io.Closeable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.fail
import org.junit.Test

class PdfCompareDescriptorLifecycleTest {
    private class Tracked : Closeable {
        var closes = 0
        override fun close() { closes++ }
    }

    @Test fun secondOpenFailureClosesFirstAndPreservesError() {
        val first = Tracked()
        val failure = IllegalStateException("second open failed")
        try {
            withOpenedCloseables(
                openFirst = { first },
                openSecond = { throw failure },
                block = { _: Tracked, _: Tracked -> fail("block should not run") }
            )
            fail("Expected second-open failure")
        } catch (actual: IllegalStateException) {
            assertSame(failure, actual)
        }
        assertEquals(1, first.closes)
    }

    @Test fun firstOpenFailureDoesNotOpenSecond() {
        val failure = IllegalStateException("first open failed")
        var secondOpens = 0
        try {
            withOpenedCloseables(
                openFirst = { throw failure },
                openSecond = { secondOpens++; Tracked() },
                block = { _: Tracked, _: Tracked -> Unit }
            )
            fail("Expected first-open failure")
        } catch (actual: IllegalStateException) {
            assertSame(failure, actual)
        }
        assertEquals(0, secondOpens)
    }

    @Test fun blockFailureClosesBothOnceAndPreservesError() {
        val first = Tracked()
        val second = Tracked()
        val failure = IllegalStateException("render failed")
        try {
            withOpenedCloseables({ first }, { second }) { _, _ -> throw failure }
            fail("Expected block failure")
        } catch (actual: IllegalStateException) {
            assertSame(failure, actual)
        }
        assertEquals(1, first.closes)
        assertEquals(1, second.closes)
    }

    @Test fun successClosesBothExactlyOnce() {
        val first = Tracked()
        val second = Tracked()
        val answer = withOpenedCloseables({ first }, { second }) { _, _ -> "ok" }
        assertEquals("ok", answer)
        assertEquals(1, first.closes)
        assertEquals(1, second.closes)
    }
}
