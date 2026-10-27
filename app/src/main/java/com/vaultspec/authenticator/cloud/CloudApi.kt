package com.vaultspec.authenticator.cloud

import android.os.Build
import com.google.gson.Gson
import com.vaultspec.authenticator.BuildConfig
import com.vaultspec.authenticator.data.model.CloudBackup
import com.vaultspec.authenticator.data.model.CloudBackupList
import com.vaultspec.authenticator.data.model.CloudBackupMeta
import com.vaultspec.authenticator.data.model.CloudError
import com.vaultspec.authenticator.data.model.CloudPairing
import com.vaultspec.authenticator.data.model.CloudPairingClaim
import com.vaultspec.authenticator.data.model.CloudPairingStatus
import com.vaultspec.authenticator.data.model.CloudSession
import com.vaultspec.authenticator.data.prefs.AppPreferencesManager
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** A failed cloud call. [code] is the server's error code, or "network" when it was unreachable. */
class CloudException(val code: String, message: String) : Exception(message)

/**
 * Client for the VaultSpec Cloud API (source: github.com/VaultSpec/authenticator-BE).
 * All calls block — run them on Dispatchers.IO. Only account credentials and already-encrypted
 * backups are sent; the vault password and vault keys never leave the device.
 */
@Singleton
class CloudApi @Inject constructor(
    private val prefs: AppPreferencesManager,
) {
    private val gson = Gson()
    private val json = "application/json; charset=utf-8".toMediaType()
    private val baseUrl = BuildConfig.CLOUD_BASE_URL.trimEnd('/')
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    private val refreshLock = Any()

    val isSignedIn: Boolean get() = prefs.cloudRefreshToken != null
    val email: String? get() = prefs.cloudEmail

    // ── Account ──

    /** Creates the account and emails a verification code; finish with [verifyLoginCode]. */
    fun register(email: String, password: String) {
        call("POST", "/v1/auth/register", mapOf("email" to email, "password" to password))
    }


    fun forgotPassword(email: String) {
        call("POST", "/v1/auth/forgot", mapOf("email" to email))
    }

    /** Sign-in step 1: checks the account password and emails a verification code. */
    fun requestLoginCode(email: String, password: String) {
        call("POST", "/v1/auth/login", mapOf("email" to email, "password" to password))
    }

    /** Sign-in step 2: exchanges the password and the emailed code for a session. */
    fun verifyLoginCode(email: String, password: String, code: String) {
        val body = mapOf("email" to email, "password" to password, "code" to code, "deviceName" to Build.MODEL)
        val session = gson.fromJson(call("POST", "/v1/auth/login/verify", body), CloudSession::class.java)
        prefs.cloudEmail = session.email ?: email
        prefs.cloudAccessToken = session.accessToken
        prefs.cloudRefreshToken = session.refreshToken
    }

    /** Signs out locally even if the server can't be reached. */
    fun logout() {
        val refreshToken = prefs.cloudRefreshToken
        clearSession()
        if (refreshToken != null) {
            runCatching { call("POST", "/v1/auth/logout", mapOf("refreshToken" to refreshToken)) }
        }
    }

    /** Deletes the account and every cloud backup. */
    fun deleteAccount(password: String) {
        call("DELETE", "/v1/account", mapOf("password" to password), auth = true)
        clearSession()
    }

    // ── Backups ──

    fun uploadBackup(keyId: String, payload: String): CloudBackupMeta {
        val body = mapOf("keyId" to keyId, "payload" to payload)
        return gson.fromJson(call("POST", "/v1/backups", body, auth = true), CloudBackupMeta::class.java)
    }

    fun listBackups(): List<CloudBackupMeta> =
        gson.fromJson(call("GET", "/v1/backups", auth = true), CloudBackupList::class.java).backups

    /** The newest backup, or null if the account has none. */
    fun latestBackup(): CloudBackup? = try {
        gson.fromJson(call("GET", "/v1/backups/latest", auth = true), CloudBackup::class.java)
    } catch (e: CloudException) {
        if (e.code == "not_found") null else throw e
    }

    // ── Scan & Connect ──

    /** Uploads a one-time vault snapshot for a new device; the returned token becomes the particle code. */
    fun startPairing(keyId: String, payload: String): CloudPairing {
        val body = mapOf("keyId" to keyId, "payload" to payload)
        return gson.fromJson(call("POST", "/v1/pairing", body, auth = true), CloudPairing::class.java)
    }

    /** "pending", "claimed" or "expired". */
    fun pairingStatus(id: String): String =
        gson.fromJson(call("GET", "/v1/pairing/$id", auth = true), CloudPairingStatus::class.java).status

    fun cancelPairing(id: String) {
        runCatching { call("DELETE", "/v1/pairing/$id", auth = true) }
    }

    /** The new device: claims a scanned token, signs in to the same account and returns the snapshot's JSON. */
    fun claimPairing(token: String): String {
        val body = mapOf("token" to token, "deviceName" to Build.MODEL)
        val claim = gson.fromJson(call("POST", "/v1/pairing/claim", body), CloudPairingClaim::class.java)
        prefs.cloudEmail = claim.email
        prefs.cloudAccessToken = claim.accessToken
        prefs.cloudRefreshToken = claim.refreshToken
        return claim.payload
    }

    // ── Transport ──

    private fun clearSession() {
        prefs.cloudEmail = null
        prefs.cloudAccessToken = null
        prefs.cloudRefreshToken = null
    }

    private fun call(method: String, path: String, body: Any? = null, auth: Boolean = false): String {
        if (!auth) return execute(method, path, body, token = null).orThrow()

        val token = prefs.cloudAccessToken ?: refreshAccessToken(staleToken = null)
        val first = execute(method, path, body, token)
        if (first.status != 401 || first.error?.error != "unauthorized") return first.orThrow()

        // Access token expired: refresh once and retry
        return execute(method, path, body, refreshAccessToken(staleToken = token)).orThrow()
    }

    /** Exchanges the refresh token for a new pair. Signs out locally if the server rejects it. */
    private fun refreshAccessToken(staleToken: String?): String = synchronized(refreshLock) {
        // Another thread may have refreshed while this one waited for the lock
        prefs.cloudAccessToken?.takeIf { it != staleToken }?.let { return it }

        val refreshToken = prefs.cloudRefreshToken
            ?: throw CloudException("unauthorized", "Sign in to your VaultSpec account.")
        val result = execute("POST", "/v1/auth/refresh", mapOf("refreshToken" to refreshToken), token = null)
        if (result.status == 401) {
            clearSession()
            throw CloudException("unauthorized", "Your session expired. Sign in again.")
        }
        val session = gson.fromJson(result.orThrow(), CloudSession::class.java)
        prefs.cloudAccessToken = session.accessToken
        prefs.cloudRefreshToken = session.refreshToken
        session.accessToken
    }

    private class Result(val status: Int, val body: String, val error: CloudError?) {
        fun orThrow(): String {
            if (status in 200..299) return body
            throw CloudException(
                error?.error ?: "server_error",
                error?.message ?: "VaultSpec Cloud returned an error ($status).",
            )
        }
    }

    private fun execute(method: String, path: String, body: Any?, token: String?): Result {
        val requestBody = when {
            body != null -> gson.toJson(body).toRequestBody(json)
            method == "GET" -> null
            else -> "{}".toRequestBody(json)
        }
        val request = Request.Builder()
            .url(baseUrl + path)
            .method(method, requestBody)
            .apply { if (token != null) header("Authorization", "Bearer $token") }
            .build()
        try {
            client.newCall(request).execute().use { response ->
                val text = response.body?.string().orEmpty()
                val error = if (response.isSuccessful) null
                    else runCatching { gson.fromJson(text, CloudError::class.java) }.getOrNull()
                return Result(response.code, text, error)
            }
        } catch (e: IOException) {
            throw CloudException("network", "Can't reach VaultSpec Cloud. Check your connection and try again.")
        }
    }
}
