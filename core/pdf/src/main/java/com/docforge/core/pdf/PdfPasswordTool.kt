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
        require(userPassword.length >= 8) { "Password must be at least 8 characters." }
        val outputDir = outputDirectory()
        val baseName = outputBaseName(outputName, "protected")
        val stagedResult = withStagedOutputFile(
            directory = outputDir,
            baseName = baseName,
            extension = "pdf"
        ) { stagedFile ->
            context.withUriCopiedToCacheFile(inputUri, prefix = "docforge_pwd_", suffix = ".pdf") { sourceFile ->
                loadPdfDocument(sourceFile).use { document ->
                    require(document.numberOfPages > 0) { "Input PDF has no pages." }

                    val permissions = AccessPermission()
                    val policy = StandardProtectionPolicy(
                        ownerPassword.ifBlank { userPassword },
                        userPassword,
                        permissions
                    ).apply {
                        setEncryptionKeyLength(256)
                        setPermissions(permissions)
                    }

                    document.protect(policy)
                    document.save(stagedFile)
                    document.numberOfPages
                }
            }
        }

        PdfCreationResult(
            outputFile = stagedResult.outputFile,
            pageCount = stagedResult.value,
            outputSizeBytes = stagedResult.outputFile.length()
        )
    }

    suspend fun removePassword(
        inputUri: android.net.Uri,
        outputName: String,
        password: String
    ): PdfCreationResult = withContext(Dispatchers.IO) {
        require(password.isNotBlank()) { "Password cannot be blank for unlock." }
        val outputDir = outputDirectory()
        val baseName = outputBaseName(outputName, "unlocked")
        try {
            val stagedResult = withStagedOutputFile(
                directory = outputDir,
                baseName = baseName,
                extension = "pdf"
            ) { stagedFile ->
                context.withUriCopiedToCacheFile(inputUri, prefix = "docforge_unlock_", suffix = ".pdf") { sourceFile ->
                    loadPdfDocument(sourceFile, password).use { document ->
                        require(document.numberOfPages > 0) { "Input PDF has no pages." }
                        require(document.isEncrypted) { "Selected PDF is not password protected." }

                        document.setAllSecurityToBeRemoved(true)
                        document.save(stagedFile)
                        document.numberOfPages
                    }
                }
            }

            PdfCreationResult(
                outputFile = stagedResult.outputFile,
                pageCount = stagedResult.value,
                outputSizeBytes = stagedResult.outputFile.length()
            )
        } catch (_: InvalidPasswordException) {
            error("Incorrect password for this PDF.")
        }
    }

    suspend fun isEncrypted(inputUri: android.net.Uri): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            context.withUriCopiedToCacheFile(inputUri, prefix = "docforge_probe_", suffix = ".pdf") { sourceFile ->
                loadPdfDocument(sourceFile).use { doc -> doc.isEncrypted }
            }
        }.getOrElse { true }
    }

    private fun outputDirectory(): File {
        return DocForgeSettingsStore.resolveOutputDirectory(
            context = context,
            bucket = DocForgeOutputBucket.DOCUMENTS
        )
    }

    private fun outputBaseName(outputName: String, fallbackPrefix: String): String {
        return outputName.ifBlank { "${fallbackPrefix}_${System.currentTimeMillis()}" }
            .replace(Regex("[^a-zA-Z0-9_-]"), "_")
    }
}
