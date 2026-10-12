package com.vaultspec.authenticator.backup

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.vaultspec.authenticator.crypto.VaultCrypto
import com.vaultspec.authenticator.data.model.BackupPayload
import com.vaultspec.authenticator.data.model.BackupTokenEntry
import com.vaultspec.authenticator.data.model.OtpAccount
import com.vaultspec.authenticator.data.repository.TokenRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RestoreManager @Inject constructor(
    private val tokenRepository: TokenRepository,
) {

    private val gson = Gson()

    data class RestoreResult(
        val entriesCount: Int,
        val entries: List<OtpAccount>,
    )

    /**
     * Decrypt and parse a backup file, returning the accounts without importing them.
     */
    fun decryptBackup(context: Context, fileUri: Uri, password: String): RestoreResult {
        val payload = readBackupFile(context, fileUri)
        val accounts = decryptPayload(payload, password)
        return RestoreResult(entriesCount = accounts.size, entries = accounts)
    }

    /**
     * Restore accounts from a backup file into the database.
     * The vault must already be set up and unlocked.
     *
     * @param replace If true, deletes all existing tokens first. If false, merges (adds all).
     */
    suspend fun restoreBackup(
        context: Context,
        fileUri: Uri,
        password: String,
        replace: Boolean = true,
    ): Int {
        val result = decryptBackup(context, fileUri, password)
        return importAccounts(result.entries, replace)
    }

    /**
     * Write decrypted accounts into the database. The vault must be unlocked.
     *
     * @param replace If true, deletes all existing tokens first. If false, merges (adds all).
     */
    suspend fun importAccounts(accounts: List<OtpAccount>, replace: Boolean = true): Int {
        if (replace) {
            tokenRepository.deleteAll()
        }

        val entries = accounts.map { account ->
            tokenRepository.encryptAccount(account)
        }
        tokenRepository.insertAllEncrypted(entries)

        return accounts.size
    }

    /** Parse the JSON document of a backup (a .vsbk file or a cloud backup). */
    fun parsePayload(json: String): BackupPayload = gson.fromJson(json, BackupPayload::class.java)

    private fun readBackupFile(context: Context, fileUri: Uri): BackupPayload {
        val json = context.contentResolver.openInputStream(fileUri)?.use { input ->
            input.bufferedReader().readText()
        } ?: throw IllegalStateException("Cannot read backup file")

        return parsePayload(json)
    }

    fun decryptPayload(payload: BackupPayload, password: String): List<OtpAccount> {
        require(payload.version == 1) { "Unsupported backup version: ${payload.version}" }
        require(payload.app == "VaultSpec") { "Not a VaultSpec backup" }

        val salt = BackupManager.hexToBytes(payload.params.salt)
        val iv = Base64.decode(payload.iv, Base64.NO_WRAP)
        val ciphertext = Base64.decode(payload.data, Base64.NO_WRAP)

        val jsonBytes = VaultCrypto.decryptWithPassword(
            VaultCrypto.EncryptedPayload(ciphertext, iv), password, salt
        )

        val type = object : TypeToken<List<BackupTokenEntry>>() {}.type
        val backupEntries: List<BackupTokenEntry> = gson.fromJson(
            String(jsonBytes, Charsets.UTF_8), type
        )

        return backupEntries.map { entry ->
            OtpAccount(
                issuer = entry.issuer,
                accountName = entry.accountName,
                secret = entry.secret,
                algorithm = entry.algorithm,
                digits = entry.digits,
                period = entry.period,
                category = entry.category,
                icon = entry.icon,
            )
        }
    }
}
