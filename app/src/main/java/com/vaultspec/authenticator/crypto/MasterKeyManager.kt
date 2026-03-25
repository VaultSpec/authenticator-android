package com.vaultspec.authenticator.crypto

import android.content.Context
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the master key using an Aegis-inspired slot system.
 *
 * The master key is a random 256-bit AES key that encrypts all vault data.
 * It is itself encrypted ("wrapped") by credential-derived keys stored in "slots":
 *   - Password slot: scrypt(password, salt) → slot key → AES-GCM wraps master key
 *   - Biometric slot: Android Keystore key → AES-GCM wraps master key
 */
@Singleton
class MasterKeyManager @Inject constructor(
    private val biometricKeyManager: BiometricKeyManager
) {

    data class PasswordSlot(
        @SerializedName("salt") val salt: String,       // Base64
        @SerializedName("iv") val iv: String,           // Base64
        @SerializedName("encrypted_key") val encryptedKey: String, // Base64
    )

    data class BiometricSlot(
        @SerializedName("iv") val iv: String,           // Base64
        @SerializedName("encrypted_key") val encryptedKey: String, // Base64
    )

    data class VaultHeader(
        @SerializedName("version") val version: Int = 1,
        @SerializedName("password_slot") val passwordSlot: PasswordSlot? = null,
        @SerializedName("biometric_slot") val biometricSlot: BiometricSlot? = null,
    )

    private val gson = Gson()

    private fun getPrefs(context: Context) = EncryptedSharedPreferences.create(
        "vault_header",
        MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun isVaultSetUp(context: Context): Boolean {
        val prefs = getPrefs(context)
        return prefs.getString("vault_header", null) != null
    }

    fun getHeader(context: Context): VaultHeader? {
        val prefs = getPrefs(context)
        val json = prefs.getString("vault_header", null) ?: return null
        return gson.fromJson(json, VaultHeader::class.java)
    }

    private fun saveHeader(context: Context, header: VaultHeader) {
        val prefs = getPrefs(context)
        prefs.edit().putString("vault_header", gson.toJson(header)).apply()
    }

    /**
     * Create a fresh vault with a password-protected master key.
     * Returns the plaintext master key (caller must hold in memory, never persist).
     */
    fun setupVault(context: Context, password: String): SecretKey {
        val masterKey = VaultCrypto.generateMasterKey()
        val salt = VaultCrypto.generateSalt()
        val slotKey = VaultCrypto.deriveKeyFromPassword(password, salt)
        val encrypted = VaultCrypto.encrypt(masterKey.encoded, slotKey)

        val passwordSlot = PasswordSlot(
            salt = Base64.encodeToString(salt, Base64.NO_WRAP),
            iv = Base64.encodeToString(encrypted.iv, Base64.NO_WRAP),
            encryptedKey = Base64.encodeToString(encrypted.ciphertext, Base64.NO_WRAP),
        )

        saveHeader(context, VaultHeader(version = 1, passwordSlot = passwordSlot))
        return masterKey
    }

    /**
     * Unlock the vault's master key using a password.
     * Throws on wrong password (AES-GCM tag mismatch).
     */
    fun unlockWithPassword(context: Context, password: String): SecretKey {
        val header = getHeader(context) ?: throw IllegalStateException("Vault not set up")
        val slot = header.passwordSlot ?: throw IllegalStateException("No password slot")

        val salt = Base64.decode(slot.salt, Base64.NO_WRAP)
        val iv = Base64.decode(slot.iv, Base64.NO_WRAP)
        val ciphertext = Base64.decode(slot.encryptedKey, Base64.NO_WRAP)

        val slotKey = VaultCrypto.deriveKeyFromPassword(password, salt)
        val masterKeyBytes = VaultCrypto.decrypt(
            VaultCrypto.EncryptedPayload(ciphertext, iv), slotKey
        )
        return VaultCrypto.keyFromBytes(masterKeyBytes)
    }

    /**
     * Prepare biometric enrollment: generates a Keystore key and returns a Cipher
     * that must be authenticated via BiometricPrompt before use.
     */
    fun prepareBiometricEnrollment(): Cipher {
        biometricKeyManager.generateKey()
        return biometricKeyManager.getEncryptCipher()
    }

    /**
     * Finish biometric enrollment using an authenticated cipher from BiometricPrompt.
     * Wraps the master key with the biometric-protected Keystore key.
     */
    fun finishBiometricEnrollment(context: Context, masterKey: SecretKey, authenticatedCipher: Cipher) {
        val ciphertext = authenticatedCipher.doFinal(masterKey.encoded)
        val iv = authenticatedCipher.iv

        val biometricSlot = BiometricSlot(
            iv = Base64.encodeToString(iv, Base64.NO_WRAP),
            encryptedKey = Base64.encodeToString(ciphertext, Base64.NO_WRAP),
        )

        val header = getHeader(context) ?: throw IllegalStateException("Vault not set up")
        saveHeader(context, header.copy(biometricSlot = biometricSlot))
    }

    fun disableBiometric(context: Context) {
        biometricKeyManager.deleteKey()
        val header = getHeader(context) ?: return
        saveHeader(context, header.copy(biometricSlot = null))
    }

    fun isBiometricEnabled(context: Context): Boolean {
        return getHeader(context)?.biometricSlot != null
    }

    /**
     * Unlock master key using biometric-authenticated cipher.
     * The cipher must have been authenticated via BiometricPrompt.
     */
    fun unlockWithBiometric(context: Context, cipher: javax.crypto.Cipher): SecretKey {
        val header = getHeader(context) ?: throw IllegalStateException("Vault not set up")
        val slot = header.biometricSlot ?: throw IllegalStateException("No biometric slot")
        val ciphertext = Base64.decode(slot.encryptedKey, Base64.NO_WRAP)
        val masterKeyBytes = cipher.doFinal(ciphertext)
        return VaultCrypto.keyFromBytes(masterKeyBytes)
    }

    /**
     * Change the vault password. Re-wraps the master key with a new password-derived key.
     */
    fun changePassword(context: Context, masterKey: SecretKey, newPassword: String) {
        val salt = VaultCrypto.generateSalt()
        val slotKey = VaultCrypto.deriveKeyFromPassword(newPassword, salt)
        val encrypted = VaultCrypto.encrypt(masterKey.encoded, slotKey)

        val passwordSlot = PasswordSlot(
            salt = Base64.encodeToString(salt, Base64.NO_WRAP),
            iv = Base64.encodeToString(encrypted.iv, Base64.NO_WRAP),
            encryptedKey = Base64.encodeToString(encrypted.ciphertext, Base64.NO_WRAP),
        )

        val header = getHeader(context) ?: throw IllegalStateException("Vault not set up")
        saveHeader(context, header.copy(passwordSlot = passwordSlot))
    }

    /** Delete the entire vault (for restore scenarios). */
    fun deleteVault(context: Context) {
        biometricKeyManager.deleteKey()
        getPrefs(context).edit().clear().apply()
    }
}
