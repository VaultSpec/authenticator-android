package com.vaultspec.authenticator.data.model

import com.google.gson.annotations.SerializedName

data class BackupPayload(
    @SerializedName("version") val version: Int = 1,
    @SerializedName("app") val app: String = "VaultSpec",
    @SerializedName("created") val created: String,
    @SerializedName("params") val params: BackupParams,
    @SerializedName("data") val data: String, // Base64(AES-256-GCM encrypted JSON of token list)
    @SerializedName("iv") val iv: String,     // Base64 IV
)

data class BackupParams(
    @SerializedName("n") val n: Int = 32768,
    @SerializedName("r") val r: Int = 8,
    @SerializedName("p") val p: Int = 1,
    @SerializedName("salt") val salt: String, // Hex-encoded salt
)

data class BackupTokenEntry(
    @SerializedName("issuer") val issuer: String,
    @SerializedName("account_name") val accountName: String,
    @SerializedName("secret") val secret: String, // Base32 plaintext secret
    @SerializedName("algorithm") val algorithm: String,
    @SerializedName("digits") val digits: Int,
    @SerializedName("period") val period: Int,
    @SerializedName("category") val category: String,
    @SerializedName("icon") val icon: String?,
)
