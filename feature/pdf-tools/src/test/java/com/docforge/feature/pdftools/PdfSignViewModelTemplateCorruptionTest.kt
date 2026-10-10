package com.docforge.feature.pdftools

import com.docforge.core.domain.model.ConversionRecord
import com.docforge.core.domain.repository.HistoryRepository
import com.docforge.core.pdf.PdfSigner
import java.io.File
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class PdfSignViewModelTemplateCorruptionTest {
    private val file: File
        get() = File(RuntimeEnvironment.getApplication().filesDir, "pdf_sign_placement_templates.json")

    @Before fun setup() { file.delete() }
    @After fun cleanup() { file.delete() }

    private fun viewModel(): PdfSignViewModel {
        val context = RuntimeEnvironment.getApplication()
        val history = object : HistoryRepository {
            override fun observeRecent(limit: Int): Flow<List<ConversionRecord>> = flowOf(emptyList())
            override suspend fun insert(record: ConversionRecord) = Unit
            override suspend fun deleteById(id: Long) = Unit
            override suspend fun deleteAll() = Unit
        }
        return PdfSignViewModel(
            historyRepository = history,
            pdfSigner = PdfSigner(context),
            savedSignatureStore = SavedSignatureStore(context),
            placementTemplateStore = SignaturePlacementTemplateStore(context)
        )
    }

    @Test fun corruptSavedTemplatesDoNotCrashViewModelInitialization() {
        file.writeText("{broken")
        val state = viewModel().uiState.value
        assertTrue(state.placementTemplates.isEmpty())
        assertTrue(state.errorMessage.orEmpty().contains("corrupt", ignoreCase = true))
        assertEquals("{broken", file.readText())
    }

    @Test fun loadingCorruptTemplateReportsStorageErrorInsteadOfMissingTemplate() {
        file.writeText("{broken")
        val model = viewModel()
        model.loadPlacementTemplate("Old")
        assertTrue(model.uiState.value.errorMessage.orEmpty().contains("corrupt", ignoreCase = true))
        assertEquals("{broken", file.readText())
    }

    @Test fun validSavedTemplatesRemainVisibleAndLoadable() {
        file.writeText(
            """{"templates":[{"name":"Old","updatedAtMillis":123,"placements":[{"pageOneBased":1,"xRatio":0.2,"yRatio":0.3,"widthRatio":0.4}]}]}"""
        )
        val model = viewModel()
        assertEquals("Old", model.uiState.value.placementTemplates.single().name)
        model.loadPlacementTemplate("Old")
        assertEquals(1, model.uiState.value.placements.size)
        assertEquals(null, model.uiState.value.errorMessage)
    }
}
