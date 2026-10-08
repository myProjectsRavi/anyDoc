package com.docforge.feature.pdftools

import java.io.File
import java.io.IOException
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class SignaturePlacementTemplateStoreTest {
    private val file: File
        get() = File(RuntimeEnvironment.getApplication().filesDir, "pdf_sign_placement_templates.json")

    private val validJson = """{"templates":[{"name":"Old","updatedAtMillis":123,"placements":[{"pageOneBased":1,"xRatio":0.2,"yRatio":0.3,"widthRatio":0.4}]}]}"""

    @Before fun setup() { file.delete() }
    @After fun cleanup() { file.delete() }

    private fun store() = SignaturePlacementTemplateStore(RuntimeEnvironment.getApplication())

    @Test fun validTemplateRemainsReadableAndDeletable() {
        file.writeText(validJson)
        assertEquals("Old", store().listTemplates().single().name)
        store().deleteTemplate("missing")
        assertEquals("Old", store().listTemplates().single().name)
        store().deleteTemplate("Old")
        assertTrue(store().listTemplates().isEmpty())
    }

    @Test fun corruptJsonCannotBeOverwrittenBySave() {
        file.writeText("{broken")
        assertThrows(IOException::class.java) {
            store().saveTemplate(
                "New",
                listOf(PdfSignaturePlacementUi(1, 0.2f, 0.3f, 0.4f))
            )
        }
        assertEquals("{broken", file.readText())
    }

    @Test fun malformedJsonCannotBeOverwrittenByDelete() {
        file.writeText("{broken")
        assertThrows(IOException::class.java) { store().deleteTemplate("Old") }
        assertEquals("{broken", file.readText())
    }

    @Test fun blankExistingFileCannotBeOverwritten() {
        file.writeText("")
        assertThrows(IOException::class.java) { store().deleteTemplate("Old") }
        assertEquals("", file.readText())
    }

    @Test fun missingTemplateArrayCannotBeOverwritten() {
        file.writeText("{}")
        assertThrows(IOException::class.java) { store().deleteTemplate("Old") }
        assertEquals("{}", file.readText())
    }

    @Test fun invalidPlacementCannotBeSilentlyDropped() {
        val broken = """{"templates":[{"name":"Old","updatedAtMillis":123,"placements":[{"pageOneBased":0,"xRatio":0.2,"yRatio":0.3,"widthRatio":0.4}]}]}"""
        file.writeText(broken)
        assertThrows(IOException::class.java) { store().deleteTemplate("Old") }
        assertEquals(broken, file.readText())
    }
}
