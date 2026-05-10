package com.docforge.core.storage

import android.content.Context
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * AES-256-GCM encrypted document vault.
 * Files are encrypted with a user-provided passphrase and stored
 * in the app's private `filesDir/vault/` directory.
 *
 * Sprint 5 feature — AES-256 Encrypted Document Vault.
 */
class EncryptedDocumentVault(context: Context) {

    private val vaultDir = File(context.applicationContext.filesDir, "vault").apply { mkdirs() }

    companion object {
        private const val ALGORITHM = "AES/GCM/NoPadding"
        private const val KEY_ALGORITHM = "AES"
        private const val PBKDF_ALGORITHM = "PBKDF2WithHmacSHA256"
        private const val KEY_LENGTH_BITS = 256
        private const val PBKDF_ITERATIONS = 120_000
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 128
        private const val SALT_LENGTH = 16
    }

    data class VaultEntry(
        val encryptedFileName: String,
        val originalName: String,
        val sizeBytes: Long,
        val createdAtMillis: Long
    )

    /**
     * Encrypts [sourceFile] with [passphrase] and stores it in the vault.
     *
     * @return the vault entry metadata.
     */
    fun encrypt(sourceFile: File, originalName: String, passphrase: String): VaultEntry {
        require(sourceFile.exists() && sourceFile.length() > 0) { "Source file is empty or missing." }
        require(passphrase.length >= 8) { "Passphrase must be at least 8 characters." }

        val random = SecureRandom()
        val salt = ByteArray(SALT_LENGTH).also { random.nextBytes(it) }
        val iv = ByteArray(GCM_IV_LENGTH).also { random.nextBytes(it) }
        val key = deriveKey(passphrase, salt)

        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, iv))

        val encryptedName = "vault_${System.currentTimeMillis()}_${random.nextInt(100000)}.enc"
        val encryptedFile = File(vaultDir, encryptedName)

        FileOutputStream(encryptedFile).use { fos ->
            // Write header: salt (16 bytes) + IV (12 bytes)
            fos.write(salt)
            fos.write(iv)

            // Write original filename length + bytes
            val nameBytes = originalName.toByteArray(Charsets.UTF_8)
            fos.write(nameBytes.size shr 8)
            fos.write(nameBytes.size and 0xFF)
            fos.write(nameBytes)

            // Encrypt file content
            CipherOutputStream(fos, cipher).use { cos ->
                FileInputStream(sourceFile).use { fis ->
                    fis.copyTo(cos, bufferSize = 8192)
                }
            }
        }

        return VaultEntry(
            encryptedFileName = encryptedName,
            originalName = originalName,
            sizeBytes = encryptedFile.length(),
            createdAtMillis = System.currentTimeMillis()
        )
    }

    /**
     * Decrypts a vault entry to [outputFile] using [passphrase].
     */
    fun decrypt(encryptedFileName: String, passphrase: String, outputFile: File) {
        require(passphrase.length >= 8) { "Passphrase must be at least 8 characters." }
        val encryptedFile = File(vaultDir, encryptedFileName)
        require(encryptedFile.exists()) { "Vault entry not found: $encryptedFileName" }

        FileInputStream(encryptedFile).use { fis ->
            val salt = ByteArray(SALT_LENGTH)
            val iv = ByteArray(GCM_IV_LENGTH)
            fis.read(salt)
            fis.read(iv)

            // Read original filename (skip it — caller already knows the output path)
            val nameLenHigh = fis.read()
            val nameLenLow = fis.read()
            val nameLen = (nameLenHigh shl 8) or nameLenLow
            fis.skip(nameLen.toLong())

            val key = deriveKey(passphrase, salt)
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, iv))

            CipherInputStream(fis, cipher).use { cis ->
                FileOutputStream(outputFile).use { fos ->
                    cis.copyTo(fos, bufferSize = 8192)
                }
            }
        }
    }

    /**
     * Lists all vault entries.
     */
    fun listEntries(): List<VaultEntry> {
        return vaultDir.listFiles()
            ?.filter { it.name.endsWith(".enc") }
            ?.map { file ->
                val originalName = readOriginalName(file) ?: file.name
                VaultEntry(
                    encryptedFileName = file.name,
                    originalName = originalName,
                    sizeBytes = file.length(),
                    createdAtMillis = file.lastModified()
                )
            }
            ?.sortedByDescending { it.createdAtMillis }
            .orEmpty()
    }

    /**
     * Deletes a vault entry permanently.
     */
    fun delete(encryptedFileName: String): Boolean {
        return File(vaultDir, encryptedFileName).delete()
    }

    private fun readOriginalName(file: File): String? = runCatching {
        FileInputStream(file).use { fis ->
            fis.skip((SALT_LENGTH + GCM_IV_LENGTH).toLong())
            val nameLenHigh = fis.read()
            val nameLenLow = fis.read()
            val nameLen = (nameLenHigh shl 8) or nameLenLow
            if (nameLen in 1..512) {
                val nameBytes = ByteArray(nameLen)
                fis.read(nameBytes)
                String(nameBytes, Charsets.UTF_8)
            } else null
        }
    }.getOrNull()

    private fun deriveKey(passphrase: String, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(passphrase.toCharArray(), salt, PBKDF_ITERATIONS, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance(PBKDF_ALGORITHM)
        val keyBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, KEY_ALGORITHM)
    }
}
