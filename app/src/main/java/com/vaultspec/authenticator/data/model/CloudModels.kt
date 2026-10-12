package com.vaultspec.authenticator.data.model

import com.google.gson.annotations.SerializedName

/** Response of a successful sign-in or token refresh. */
data class CloudSession(
    @SerializedName("email") val email: String?,
    @SerializedName("accessToken") val accessToken: String,
    @SerializedName("refreshToken") val refreshToken: String,
)

data class CloudBackupMeta(
    @SerializedName("id") val id: String,
    @SerializedName("keyId") val keyId: String,
    @SerializedName("sizeBytes") val sizeBytes: Int,
    @SerializedName("createdAt") val createdAt: String, // ISO-8601 instant
)

data class CloudBackupList(
    @SerializedName("backups") val backups: List<CloudBackupMeta>,
)

/** A stored backup; [payload] is the JSON of a [BackupPayload]. */
data class CloudBackup(
    @SerializedName("id") val id: String,
    @SerializedName("keyId") val keyId: String,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("payload") val payload: String,
)

data class CloudError(
    @SerializedName("error") val error: String?,
    @SerializedName("message") val message: String?,
)
