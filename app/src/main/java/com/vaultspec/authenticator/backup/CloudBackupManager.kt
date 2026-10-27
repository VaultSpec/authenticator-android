package com.vaultspec.authenticator.backup

import android.content.Context
import android.util.Base64
import com.google.gson.Gson
import com.vaultspec.authenticator.cloud.CloudApi
import com.vaultspec.authenticator.cloud.CloudException
import com.vaultspec.authenticator.crypto.MasterKeyManager
import com.vaultspec.authenticator.crypto.VaultCrypto
import com.vaultspec.authenticator.data.model.BackupParams
import com.vaultspec.authenticator.data.model.BackupPayload
import com.vaultspec.authenticator.data.model.OtpAccount
import com.vaultspec.authenticator.data.prefs.AppPreferencesManager
import com.vaultspec.authenticator.data.repository.VaultRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.time.Instant
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/** The account already holds backups made by another vault; uploading would replace them. */
class ExistingCloudBackupException : Exception("This account already has a cloud backup.")

/**
 * Encrypted backups in the user's VaultSpec account.
 *
 * Backups are encrypted with a key derived from the vault password. The password itself is never
 * stored, so the derived key and its salt are kept in the encrypted preferences; that lets
 * automatic backups run after a biometric unlock. Changing the vault password derives a new key,
 * and the server drops every backup made with the old one (see [keyId]).
 */
@Singleton
class CloudBackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: CloudApi,
    private val prefs: AppPreferencesManager,
    private val backupManager: BackupManager,
    private val restoreManager: RestoreManager,
    private val vaultRepository: VaultRepository,
    private val masterKeyManager: MasterKeyManager,
) {
    private val gson = Gson()
    private val uploadLock = Mutex()

    val isEnabled: Boolean
        get() = prefs.cloudBackupEnabled && prefs.cloudKey != null && api.isSignedIn

    /**
     * Turns cloud backup on and uploads the first backup.
     *
     * @throws IllegalArgumentException if [vaultPassword] is wrong
     * @throws ExistingCloudBackupException if the account already has backups and
     *   [replaceExisting] is false — they belong to another vault and would be deleted
     */
    suspend fun enable(vaultPassword: String, replaceExisting: Boolean = false) = withContext(Dispatchers.IO) {
        try {
            masterKeyManager.unlockWithPassword(context, vaultPassword)
        } catch (e: Exception) {
            throw IllegalArgumentException("Incorrect vault password")
        }
        if (!replaceExisting && api.listBackups().isNotEmpty()) throw ExistingCloudBackupException()

        deriveKey(vaultPassword)
        prefs.cloudBackupEnabled = true
        upload()
    }

    /** Stops backing up from this device. Backups already in the cloud are left alone. */
    fun disable() {
        prefs.cloudBackupEnabled = false
        prefs.cloudBackupPending = false
        prefs.cloudKey = null
        prefs.cloudSalt = null
    }

    /** Uploads now and reports failures to the caller. */
    suspend fun backupNow() = withContext(Dispatchers.IO) {
        check(isEnabled) { "Cloud backup is off" }
        try {
            upload()
        } catch (e: Exception) {
            prefs.cloudBackupPending = true
            throw e
        }
    }

    /**
     * Re-keys cloud backups after the vault password changed and uploads a fresh backup, which
     * makes the server delete every backup made with the old password.
     *
     * @return true if the new backup was uploaded, false if it is queued for retry
     */
    suspend fun onVaultPasswordChanged(newPassword: String): Boolean = withContext(Dispatchers.IO) {
        if (!prefs.cloudBackupEnabled) return@withContext true
        deriveKey(newPassword)
        try {
            upload()
            true
        } catch (_: Exception) {
            prefs.cloudBackupPending = true
            false
        }
    }

    /** Downloads the newest cloud backup, or null if the account has none. */
    suspend fun fetchLatest(): BackupPayload? = withContext(Dispatchers.IO) {
        api.latestBackup()?.let { restoreManager.parsePayload(it.payload) }
    }

    /**
     * Decrypts a downloaded backup with the vault password it was made under.
     *
     * @throws IllegalArgumentException if the password does not match
     */
    suspend fun decrypt(payload: BackupPayload, vaultPassword: String): List<OtpAccount> =
        withContext(Dispatchers.Default) {
            try {
                restoreManager.decryptPayload(payload, vaultPassword)
            } catch (e: java.security.GeneralSecurityException) {
                throw IllegalArgumentException("Incorrect vault password")
            }
        }

    /**
     * Adopts [vaultPassword] as the cloud key on this device without uploading. Used right after
     * a vault was restored from the cloud with that password.
     */
    suspend fun adopt(vaultPassword: String) = withContext(Dispatchers.Default) {
        deriveKey(vaultPassword)
        prefs.cloudBackupEnabled = true
    }

    private fun deriveKey(vaultPassword: String) {
        val salt = VaultCrypto.generateSalt()
        val key = VaultCrypto.deriveKeyFromPassword(vaultPassword, salt)
        prefs.cloudSalt = BackupManager.bytesToHex(salt)
        prefs.cloudKey = Base64.encodeToString(key.encoded, Base64.NO_WRAP)
    }

    private suspend fun upload() = uploadLock.withLock {
        val saltHex = prefs.cloudSalt ?: throw IllegalStateException("Cloud backup is off")
        val keyBytes = Base64.decode(prefs.cloudKey ?: throw IllegalStateException("Cloud backup is off"), Base64.NO_WRAP)
        val payload = encryptVault(saltHex, keyBytes)

        try {
            api.uploadBackup(keyId(saltHex), gson.toJson(payload))
        } catch (e: CloudException) {
            // Signed out remotely (password reset, account deleted): stop trying
            if (e.code == "unauthorized") disable()
            throw e
        }
        prefs.cloudLastBackupAt = System.currentTimeMillis()
        prefs.cloudBackupPending = false
    }

    /** The unlocked vault as a backup document encrypted with [keyBytes] (zeroized afterwards). */
    private suspend fun encryptVault(saltHex: String, keyBytes: ByteArray): BackupPayload {
        val masterKey = vaultRepository.masterKey ?: throw IllegalStateException("Vault is locked")
        val jsonBytes = backupManager.exportTokensJson(masterKey)
        val encrypted = try {
            VaultCrypto.encrypt(jsonBytes, VaultCrypto.keyFromBytes(keyBytes))
        } finally {
            VaultCrypto.zeroize(jsonBytes)
            VaultCrypto.zeroize(keyBytes)
        }
        return BackupPayload(
            version = 1,
            app = "VaultSpec",
            created = DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
            params = BackupParams(n = 32768, r = 8, p = 1, salt = saltHex),
            data = Base64.encodeToString(encrypted.ciphertext, Base64.NO_WRAP),
            iv = Base64.encodeToString(encrypted.iv, Base64.NO_WRAP),
        )
    }

    // ── Scan & Connect ──

    /** Whether a pairing code can be made without asking for the vault password. */
    val hasBackupKey: Boolean get() = prefs.cloudKey != null && prefs.cloudSalt != null

    /**
     * Starts a pairing with a snapshot of the vault and returns its id and the token to draw. Uses the
     * cloud backup key when there is one, otherwise a key derived from [vaultPassword] that isn't kept.
     *
     * @throws IllegalArgumentException if [vaultPassword] is wrong
     */
    suspend fun startPairing(vaultPassword: String?): Pair<String, String> = withContext(Dispatchers.IO) {
        val saltHex: String
        val keyBytes: ByteArray
        if (hasBackupKey) {
            saltHex = prefs.cloudSalt!!
            keyBytes = Base64.decode(prefs.cloudKey!!, Base64.NO_WRAP)
        } else {
            requireNotNull(vaultPassword) { "Enter your vault password" }
            try {
                masterKeyManager.unlockWithPassword(context, vaultPassword)
            } catch (e: Exception) {
                throw IllegalArgumentException("Incorrect vault password")
            }
            val salt = VaultCrypto.generateSalt()
            saltHex = BackupManager.bytesToHex(salt)
            keyBytes = VaultCrypto.deriveKeyFromPassword(vaultPassword, salt).encoded
        }
        val pairing = api.startPairing(keyId(saltHex), gson.toJson(encryptVault(saltHex, keyBytes)))
        pairing.id to pairing.token
    }

    suspend fun pairingStatus(id: String): String = withContext(Dispatchers.IO) { api.pairingStatus(id) }

    suspend fun cancelPairing(id: String) = withContext(Dispatchers.IO) { api.cancelPairing(id) }

    /** The new device: claims a scanned token, which also signs in to the other device's account. */
    suspend fun claimPairing(token: String): BackupPayload = withContext(Dispatchers.IO) {
        restoreManager.parsePayload(api.claimPairing(token))
    }

    companion object {
        /**
         * Identifies which vault-password generation encrypted a backup, without revealing
         * anything about the key: the first 8 bytes of SHA-256(salt), hex-encoded.
         */
        fun keyId(saltHex: String): String {
            val digest = MessageDigest.getInstance("SHA-256").digest(BackupManager.hexToBytes(saltHex))
            return BackupManager.bytesToHex(digest.copyOf(8))
        }
    }
}
