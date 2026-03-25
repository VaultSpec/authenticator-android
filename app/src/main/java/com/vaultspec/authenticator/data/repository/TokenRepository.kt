package com.vaultspec.authenticator.data.repository

import android.util.Base64
import com.vaultspec.authenticator.crypto.VaultCrypto
import com.vaultspec.authenticator.data.db.TokenDao
import com.vaultspec.authenticator.data.db.entity.TokenEntry
import com.vaultspec.authenticator.data.model.OtpAccount
import dev.turingcomplete.kotlinonetimepassword.HmacAlgorithm
import dev.turingcomplete.kotlinonetimepassword.TimeBasedOneTimePasswordConfig
import dev.turingcomplete.kotlinonetimepassword.TimeBasedOneTimePasswordGenerator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit
import javax.crypto.SecretKey
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenRepository @Inject constructor(
    private val tokenDao: TokenDao,
    private val vaultRepository: VaultRepository,
) {

    fun observeAll(): Flow<List<TokenEntry>> = tokenDao.observeAll()

    fun observeByCategory(category: String): Flow<List<TokenEntry>> =
        if (category == "All") tokenDao.observeAll()
        else tokenDao.observeByCategory(category)

    fun search(query: String): Flow<List<TokenEntry>> = tokenDao.search(query)

    fun observeCategories(): Flow<List<String>> = tokenDao.observeCategories().map { cats ->
        listOf("All") + cats.filter { it != "All" }
    }

    suspend fun getAll(): List<TokenEntry> = tokenDao.getAll()

    suspend fun addAccount(account: OtpAccount): Long {
        val masterKey = requireMasterKey()
        val entry = encryptAndCreateEntry(account, masterKey)
        return tokenDao.insert(entry)
    }

    suspend fun addAccountFromUri(uri: String): Long {
        val account = OtpAccount.fromUri(uri)
        return addAccount(account)
    }

    suspend fun deleteToken(token: TokenEntry) {
        tokenDao.delete(token)
    }

    suspend fun deleteAll() {
        tokenDao.deleteAll()
    }

    suspend fun insertAllEncrypted(entries: List<TokenEntry>) {
        tokenDao.insertAll(entries)
    }

    /**
     * Decrypt a token's secret and generate the current TOTP code.
     */
    fun generateCode(entry: TokenEntry, timestamp: Long = System.currentTimeMillis()): String {
        val masterKey = requireMasterKey()
        val secretBase32 = decryptSecret(entry, masterKey)
        return generateTotpCode(secretBase32, entry.algorithm, entry.digits, entry.period, timestamp)
    }

    /**
     * Decrypt the Base32 secret from a token entry.
     */
    fun decryptSecret(entry: TokenEntry, masterKey: SecretKey): String {
        val ciphertext = Base64.decode(entry.encryptedSecret, Base64.NO_WRAP)
        val iv = Base64.decode(entry.secretIv, Base64.NO_WRAP)
        val plainBytes = VaultCrypto.decrypt(VaultCrypto.EncryptedPayload(ciphertext, iv), masterKey)
        return String(plainBytes, Charsets.UTF_8)
    }

    private fun encryptAndCreateEntry(account: OtpAccount, masterKey: SecretKey): TokenEntry {
        val encrypted = VaultCrypto.encrypt(account.secret.toByteArray(Charsets.UTF_8), masterKey)
        return TokenEntry(
            issuer = account.issuer,
            accountName = account.accountName,
            encryptedSecret = Base64.encodeToString(encrypted.ciphertext, Base64.NO_WRAP),
            secretIv = Base64.encodeToString(encrypted.iv, Base64.NO_WRAP),
            algorithm = account.algorithm,
            digits = account.digits,
            period = account.period,
            category = account.category,
            icon = account.icon,
            sortOrder = account.sortOrder,
            createdAt = account.createdAt,
        )
    }

    /** Re-encrypt a plaintext account using the master key and return a TokenEntry. */
    fun encryptAccount(account: OtpAccount): TokenEntry {
        val masterKey = requireMasterKey()
        return encryptAndCreateEntry(account, masterKey)
    }

    private fun requireMasterKey(): SecretKey {
        return vaultRepository.masterKey
            ?: throw IllegalStateException("Vault is locked — cannot access tokens")
    }

    companion object {
        fun generateTotpCode(
            secretBase32: String,
            algorithm: String,
            digits: Int,
            period: Int,
            timestamp: Long = System.currentTimeMillis()
        ): String {
            val secretBytes = decodeBase32(secretBase32)
            val hmacAlgorithm = when (algorithm.uppercase()) {
                "SHA256" -> HmacAlgorithm.SHA256
                "SHA512" -> HmacAlgorithm.SHA512
                else -> HmacAlgorithm.SHA1
            }
            val config = TimeBasedOneTimePasswordConfig(
                codeDigits = digits,
                hmacAlgorithm = hmacAlgorithm,
                timeStep = period.toLong(),
                timeStepUnit = TimeUnit.SECONDS,
            )
            val generator = TimeBasedOneTimePasswordGenerator(secretBytes, config)
            return generator.generate(timestamp)
        }

        private const val BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"

        fun decodeBase32(input: String): ByteArray {
            val cleaned = input.uppercase().replace("=", "").replace(" ", "")
            val bits = StringBuilder()
            for (c in cleaned) {
                val value = BASE32_ALPHABET.indexOf(c)
                if (value < 0) continue
                bits.append(value.toString(2).padStart(5, '0'))
            }
            val bytes = ByteArray(bits.length / 8)
            for (i in bytes.indices) {
                bytes[i] = bits.substring(i * 8, i * 8 + 8).toInt(2).toByte()
            }
            return bytes
        }
    }
}
