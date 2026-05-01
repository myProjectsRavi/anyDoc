package com.docforge.core.pdf

import android.content.Context
import com.docforge.core.domain.settings.DocForgeOutputBucket
import com.docforge.core.domain.settings.DocForgeSettingsStore
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.encryption.AccessPermission
import com.tom_roush.pdfbox.pdmodel.encryption.InvalidPasswordException
import com.tom_roush.pdfbox.pdmodel.encryption.StandardProtectionPolicy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class PdfPasswordTool(
    private val context: Context
) {

    suspend fun protect(
        inputUri: android.net.Uri,
        outputName: String,
        userPassword: String,
        ownerPassword: String = userPassword
    ): PdfCreationResult = withContext(Dispatchers.IO) {
        require(userPassword.isNotBlank()) { "User password cannot be blank." }
        context.withUriCopiedToCacheFile(inputUri, prefix = "docforge_pwd_", suffix = ".pdf") { sourceFile ->
            loadPdfDocument(sourceFile).use { document ->
                require(document.numberOfPages > 0) { "Input PDF has no pages." }

                val permissions = AccessPermission()
                val policy = StandardProtectionPolicy(
                    ownerPassword.ifBlank { userPassword },
                    userPassword,
                    permissions
                ).apply {
                    setEncryptionKeyLength(128)
                    setPermissions(permissions)
                }

                document.protect(policy)

                val outputFile = outputFile(outputName, "protected")
                document.save(outputFile)

                PdfCreationResult(
                    outputFile = outputFile,
                    pageCount = document.numberOfPages,
                    outputSizeBytes = outputFile.length()
                )
            }
        }
    }

    suspend fun removePassword(
        inputUri: android.net.Uri,
        outputName: String,
        password: String
    ): PdfCreationResult = withContext(Dispatchers.IO) {
        require(password.isNotBlank()) { "Password cannot be blank for unlock." }
        context.withUriCopiedToCacheFile(inputUri, prefix = "docforge_unlock_", suffix = ".pdf") { sourceFile ->
            try {
                loadPdfDocument(sourceFile, password).use { document ->
                    require(document.numberOfPages > 0) { "Input PDF has no pages." }
                    require(document.isEncrypted) { "Selected PDF is not password protected." }

                    document.setAllSecurityToBeRemoved(true)

                    val outputFile = outputFile(outputName, "unlocked")
                    document.save(outputFile)

                    PdfCreationResult(
                        outputFile = outputFile,
                        pageCount = document.numberOfPages,
                        outputSizeBytes = outputFile.length()
                    )
                }
            } catch (_: InvalidPasswordException) {
                error("Incorrect password for this PDF.")
            }
        }
    }

    suspend fun isEncrypted(inputUri: android.net.Uri): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            context.withUriCopiedToCacheFile(inputUri, prefix = "docforge_probe_", suffix = ".pdf") { sourceFile ->
                loadPdfDocument(sourceFile).use { doc -> doc.isEncrypted }
            }
        }.getOrElse { true }
    }

    private fun outputFile(outputName: String, fallbackPrefix: String): File {
        val outputDir = DocForgeSettingsStore.resolveOutputDirectory(
            context = context,
            bucket = DocForgeOutputBucket.DOCUMENTS
        )

        val sanitized = outputName.ifBlank { "${fallbackPrefix}_${System.currentTimeMillis()}" }
            .replace(Regex("[^a-zA-Z0-9_-]"), "_")

        return File(outputDir, "$sanitized.pdf")
    }
}
