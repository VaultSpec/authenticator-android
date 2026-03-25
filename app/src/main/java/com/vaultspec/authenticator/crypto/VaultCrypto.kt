package com.vaultspec.authenticator.crypto

import org.bouncycastle.crypto.generators.SCrypt
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object VaultCrypto {

    private const val AES_GCM = "AES/GCM/NoPadding"
    private const val GCM_IV_LENGTH = 12
    private const val GCM_TAG_BITS = 128
    private const val KEY_LENGTH = 32 // 256 bits

    // scrypt parameters (matching Aegis defaults)
    private const val SCRYPT_N = 32768 // 2^15
    private const val SCRYPT_R = 8
    private const val SCRYPT_P = 1
    private const val SALT_LENGTH = 32

    private val secureRandom = SecureRandom()

    data class EncryptedPayload(
        val ciphertext: ByteArray,
        val iv: ByteArray,
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is EncryptedPayload) return false
            return ciphertext.contentEquals(other.ciphertext) && iv.contentEquals(other.iv)
        }
        override fun hashCode(): Int {
            return 31 * ciphertext.contentHashCode() + iv.contentHashCode()
        }
    }

    fun encrypt(plaintext: ByteArray, key: SecretKey): EncryptedPayload {
        val iv = ByteArray(GCM_IV_LENGTH).also { secureRandom.nextBytes(it) }
        val cipher = Cipher.getInstance(AES_GCM)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        val ciphertext = cipher.doFinal(plaintext)
        return EncryptedPayload(ciphertext = ciphertext, iv = iv)
    }

    fun decrypt(payload: EncryptedPayload, key: SecretKey): ByteArray {
        val cipher = Cipher.getInstance(AES_GCM)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, payload.iv))
        return cipher.doFinal(payload.ciphertext)
    }

    fun deriveKeyFromPassword(password: String, salt: ByteArray): SecretKey {
        val derived = SCrypt.generate(
            password.toByteArray(Charsets.UTF_8),
            salt,
            SCRYPT_N,
            SCRYPT_R,
            SCRYPT_P,
            KEY_LENGTH
        )
        return SecretKeySpec(derived, "AES")
    }

    fun generateSalt(): ByteArray {
        return ByteArray(SALT_LENGTH).also { secureRandom.nextBytes(it) }
    }

    fun generateMasterKey(): SecretKey {
        val keyBytes = ByteArray(KEY_LENGTH).also { secureRandom.nextBytes(it) }
        return SecretKeySpec(keyBytes, "AES")
    }

    fun keyFromBytes(bytes: ByteArray): SecretKey {
        return SecretKeySpec(bytes, "AES")
    }

    fun encryptWithPassword(plaintext: ByteArray, password: String, salt: ByteArray): EncryptedPayload {
        val key = deriveKeyFromPassword(password, salt)
        return encrypt(plaintext, key)
    }

    fun decryptWithPassword(payload: EncryptedPayload, password: String, salt: ByteArray): ByteArray {
        val key = deriveKeyFromPassword(password, salt)
        return decrypt(payload, key)
    }

    /** Securely zero a byte array to minimize time secrets spend in memory. */
    fun zeroize(bytes: ByteArray) {
        bytes.fill(0)
    }
}
