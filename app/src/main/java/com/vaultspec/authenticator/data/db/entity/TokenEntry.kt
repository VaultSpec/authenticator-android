package com.vaultspec.authenticator.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tokens")
data class TokenEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "issuer")
    val issuer: String,

    @ColumnInfo(name = "account_name")
    val accountName: String,

    /** Base32-encoded TOTP secret, encrypted with the master key (Base64 of ciphertext). */
    @ColumnInfo(name = "encrypted_secret")
    val encryptedSecret: String,

    /** IV used when encrypting the secret (Base64). */
    @ColumnInfo(name = "secret_iv")
    val secretIv: String,

    @ColumnInfo(name = "algorithm")
    val algorithm: String = "SHA1",

    @ColumnInfo(name = "digits")
    val digits: Int = 6,

    @ColumnInfo(name = "period")
    val period: Int = 30,

    @ColumnInfo(name = "category")
    val category: String = "All",

    @ColumnInfo(name = "icon")
    val icon: String? = null,

    @ColumnInfo(name = "sort_order")
    val sortOrder: Int = 0,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
)
