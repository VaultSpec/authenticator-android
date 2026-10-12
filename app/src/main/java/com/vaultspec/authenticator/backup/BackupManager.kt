package com.vaultspec.authenticator.backup

import android.content.Context
import android.net.Uri
import android.util.Base64
import androidx.documentfile.provider.DocumentFile
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.vaultspec.authenticator.crypto.VaultCrypto
import com.vaultspec.authenticator.data.db.entity.TokenEntry
import com.vaultspec.authenticator.data.model.BackupParams
import com.vaultspec.authenticator.data.model.BackupPayload
import com.vaultspec.authenticator.data.model.BackupTokenEntry
import com.vaultspec.authenticator.data.prefs.AppPreferencesManager
import com.vaultspec.authenticator.data.repository.TokenRepository
import com.vaultspec.authenticator.data.repository.VaultRepository
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import javax.crypto.SecretKey
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupManager @Inject constructor(
    private val tokenRepository: TokenRepository,
    private val prefs: AppPreferencesManager,
    private val vaultRepository: VaultRepository,
) {

    private val gson = Gson()

    /**
     * Create an encrypted backup of all tokens.
     *
     * @param context Application context
     * @param folderUri SAF folder URI chosen by the user
     * @param password Password to derive encryption key (the app's setup password)
     * @param masterKey Current unlocked master key to decrypt token secrets for backup
     * @return The filename of the created backup
     */
    suspend fun createBackup(
        context: Context,
        folderUri: Uri,
        password: String,
        masterKey: SecretKey,
    ): String {
        // 1-2. Decrypt all token secrets and serialize to JSON
        val jsonBytes = exportTokensJson(masterKey)

        // 3. Encrypt with password-derived key
        val salt = VaultCrypto.generateSalt()
        val encrypted = VaultCrypto.encryptWithPassword(jsonBytes, password, salt)

        // 4. Build backup payload
        val now = Instant.now()
        val payload = BackupPayload(
            version = 1,
            app = "VaultSpec",
            created = DateTimeFormatter.ISO_INSTANT.format(now),
            params = BackupParams(
                n = 32768, r = 8, p = 1,
                salt = bytesToHex(salt),
            ),
            data = Base64.encodeToString(encrypted.ciphertext, Base64.NO_WRAP),
            iv = Base64.encodeToString(encrypted.iv, Base64.NO_WRAP),
        )

        // 5. Write to SAF folder
        val timestamp = DateTimeFormatter
            .ofPattern("yyyyMMdd_HHmmss")
            .withZone(ZoneOffset.UTC)
            .format(now)
        val filename = "vaultspec_backup_$timestamp.vsbk"

        val folder = DocumentFile.fromTreeUri(context, folderUri)
            ?: throw IllegalStateException("Cannot access selected folder")
        val file = folder.createFile("application/octet-stream", filename)
            ?: throw IllegalStateException("Cannot create backup file")

        context.contentResolver.openOutputStream(file.uri)?.use { out ->
            out.write(gson.toJson(payload).toByteArray(Charsets.UTF_8))
        } ?: throw IllegalStateException("Cannot write to backup file")

        prefs.localLastBackupAt = System.currentTimeMillis()
        return filename
    }

    /**
     * All tokens with their secrets decrypted, as the JSON document that gets encrypted into a
     * backup. Shared by local file backups and cloud backups.
     */
    suspend fun exportTokensJson(masterKey: SecretKey): ByteArray {
        val backupEntries = tokenRepository.getAll().map { entry ->
            BackupTokenEntry(
                issuer = entry.issuer,
                accountName = entry.accountName,
                secret = tokenRepository.decryptSecret(entry, masterKey),
                algorithm = entry.algorithm,
                digits = entry.digits,
                period = entry.period,
                category = entry.category,
                icon = entry.icon,
            )
        }
        return gson.toJson(backupEntries).toByteArray(Charsets.UTF_8)
    }

    companion object {
        fun bytesToHex(bytes: ByteArray): String =
            bytes.joinToString("") { "%02x".format(it) }

        fun hexToBytes(hex: String): ByteArray =
            hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    }
}
