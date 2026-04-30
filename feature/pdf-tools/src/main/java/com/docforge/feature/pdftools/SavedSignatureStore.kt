package com.docforge.feature.pdftools

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.io.FileOutputStream

data class SavedSignatureSlot(
    val slot: Int,
    val exists: Boolean,
    val updatedAtMillis: Long?
)

class SavedSignatureStore(
    context: Context
) {
    private val appContext = context.applicationContext
    private val signatureDir = File(appContext.filesDir, "saved_signatures").apply { mkdirs() }

    fun listSlots(): List<SavedSignatureSlot> {
        return (1..MAX_SLOTS).map { slot ->
            val file = slotFile(slot)
            SavedSignatureSlot(
                slot = slot,
                exists = file.exists(),
                updatedAtMillis = file.takeIf { it.exists() }?.lastModified()
            )
        }
    }

    fun save(slot: Int, bitmap: Bitmap) {
        require(slot in 1..MAX_SLOTS) { "Slot must be between 1 and $MAX_SLOTS." }
        val file = slotFile(slot)
        FileOutputStream(file).use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        }
    }

    fun load(slot: Int): Bitmap? {
        require(slot in 1..MAX_SLOTS) { "Slot must be between 1 and $MAX_SLOTS." }
        val file = slotFile(slot)
        if (!file.exists()) return null
        return BitmapFactory.decodeFile(file.absolutePath)
    }

    fun delete(slot: Int) {
        require(slot in 1..MAX_SLOTS) { "Slot must be between 1 and $MAX_SLOTS." }
        val file = slotFile(slot)
        if (file.exists()) {
            file.delete()
        }
    }

    private fun slotFile(slot: Int): File = File(signatureDir, "signature_slot_$slot.png")

    companion object {
        const val MAX_SLOTS: Int = 3
    }
}
