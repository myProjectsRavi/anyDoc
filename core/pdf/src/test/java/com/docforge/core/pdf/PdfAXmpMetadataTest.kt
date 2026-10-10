package com.docforge.core.pdf

import java.io.ByteArrayInputStream
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Document

class PdfAXmpMetadataTest {

    private fun parseXmp(xml: String): Document =
        DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        }.newDocumentBuilder().parse(ByteArrayInputStream(xml.toByteArray(Charsets.UTF_8)))

    @Test
    fun specialCharactersAndUnicode_roundTripAcrossAllFields() {
        val title = "A & B <Draft> \"final\" 'v2' नमस्ते 🌍"
        val author = "O'Reilly & <Team>"
        val producer = "DocForge > tools & utilities"
        val xml = buildPdfAXmpMetadataXml(title, author, producer)
        val document = parseXmp(xml)

        val entries = document.getElementsByTagName("rdf:li")
        assertEquals(2, entries.length)
        assertEquals(title, entries.item(0).textContent)
        assertEquals(author, entries.item(1).textContent)
        assertEquals(producer, document.getElementsByTagName("pdf:Producer").item(0).textContent)
        assertTrue(xml.contains("&amp;"))
        assertTrue(xml.contains("&lt;"))
        assertTrue(xml.contains("&gt;"))
        assertTrue(xml.contains("&quot;"))
        assertTrue(xml.contains("&apos;"))
    }

    @Test
    fun invalidXmlCharactersAndLoneSurrogates_areReplacedDeterministically() {
        val title = "A\u0000B\u000BC\uD800 D\uDC00 E\uFFFE F\uFFFF"
        val expected = "A\uFFFDB\uFFFDC\uFFFD D\uFFFD E\uFFFD F\uFFFD"
        val xml = buildPdfAXmpMetadataXml(title, "", "")
        assertEquals(expected, parseXmp(xml).getElementsByTagName("rdf:li").item(0).textContent)
        assertFalse(xml.contains('\u0000'))
        assertFalse(xml.contains('\u000B'))
    }

    @Test
    fun carriageReturnsTabsAndNewlines_preserveMetadataValues() {
        val title = "\tfirst\rsecond\nthird"
        val xml = buildPdfAXmpMetadataXml(title, "", "")
        assertEquals(title, parseXmp(xml).getElementsByTagName("rdf:li").item(0).textContent)
        assertTrue(xml.contains("&#xD;"))
    }

    @Test
    fun injectedMarkup_remainsTextAndCannotCreateNewXmpNodes() {
        val title = "</rdf:li></rdf:Alt></dc:title><rdf:Description>attack</rdf:Description>"
        val document = parseXmp(buildPdfAXmpMetadataXml(title, "author", "producer"))
        assertEquals(1, document.getElementsByTagName("rdf:Description").length)
        assertEquals(title, document.getElementsByTagName("rdf:li").item(0).textContent)
    }

    @Test
    fun pdfAIdentifierFields_remainUnchanged() {
        val document = parseXmp(buildPdfAXmpMetadataXml("title", "author", "producer"))
        assertEquals("1", document.getElementsByTagName("pdfaid:part").item(0).textContent)
        assertEquals("B", document.getElementsByTagName("pdfaid:conformance").item(0).textContent)
    }

    @Test
    fun escapingIsDeterministicForElementText() {
        assertEquals("A &amp; &lt;B&gt; &quot;C&quot; &apos;D&apos;", escapePdfAXmpText("A & <B> \"C\" 'D'"))
        assertEquals("🌍", escapePdfAXmpText("🌍"))
    }
}
