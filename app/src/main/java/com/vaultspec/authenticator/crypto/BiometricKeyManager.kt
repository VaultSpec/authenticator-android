package com.vaultspec.authenticator.crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages a biometric-protected AES key in Android Keystore.
 * This key wraps/unwraps the vault master key.
 */
@Singleton
class BiometricKeyManager @Inject constructor() {

    companion object {
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val KEY_ALIAS = "vaultspec_biometric_key"
        private const val AES_GCM = "AES/GCM/NoPadding"
        private const val GCM_TAG_BITS = 128
    }

    private val keyStore: KeyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }

    fun generateKey() {
        val keyGen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
        keyGen.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setUserAuthenticationRequired(true)
                .setInvalidatedByBiometricEnrollment(true)
                .build()
        )
        keyGen.generateKey()
    }

    fun keyExists(): Boolean {
        return keyStore.containsAlias(KEY_ALIAS)
    }

    fun deleteKey() {
        if (keyStore.containsAlias(KEY_ALIAS)) {
            keyStore.deleteEntry(KEY_ALIAS)
        }
    }

    /** Get a Cipher initialized for encryption — pass this to BiometricPrompt. */
    fun getEncryptCipher(): Cipher {
        val key = keyStore.getKey(KEY_ALIAS, null) as SecretKey
        return Cipher.getInstance(AES_GCM).apply {
            init(Cipher.ENCRYPT_MODE, key)
        }
    }

    /** Get a Cipher initialized for decryption — pass this to BiometricPrompt as CryptoObject. */
    fun getDecryptCipher(iv: ByteArray): Cipher {
        val key = keyStore.getKey(KEY_ALIAS, null) as SecretKey
        return Cipher.getInstance(AES_GCM).apply {
            init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        }
    }
}
