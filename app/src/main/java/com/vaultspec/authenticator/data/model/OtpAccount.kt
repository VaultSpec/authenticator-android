package com.vaultspec.authenticator.data.model

data class OtpAccount(
    val id: Long = 0,
    val issuer: String,
    val accountName: String,
    val secret: String, // Base32-encoded plaintext secret (only in-memory, never persisted directly)
    val algorithm: String = "SHA1",
    val digits: Int = 6,
    val period: Int = 30,
    val category: String = "All",
    val icon: String? = null,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
) {
    companion object {
        /**
         * Parse an otpauth:// URI into an OtpAccount.
         * Format: otpauth://totp/Issuer:account@email.com?secret=BASE32&issuer=Issuer&algorithm=SHA1&digits=6&period=30
         */
        fun fromUri(uri: String): OtpAccount {
            require(uri.startsWith("otpauth://totp/")) { "Only TOTP URIs are supported" }

            val withoutScheme = uri.removePrefix("otpauth://totp/")
            val pathAndQuery = withoutScheme.split("?", limit = 2)
            val path = java.net.URLDecoder.decode(pathAndQuery[0], "UTF-8")
            val params = if (pathAndQuery.size > 1) parseQueryParams(pathAndQuery[1]) else emptyMap()

            val (issuer, accountName) = if (":" in path) {
                val parts = path.split(":", limit = 2)
                parts[0].trim() to parts[1].trim()
            } else {
                (params["issuer"] ?: "") to path.trim()
            }

            val finalIssuer = params["issuer"] ?: issuer

            return OtpAccount(
                issuer = finalIssuer,
                accountName = accountName,
                secret = params["secret"] ?: throw IllegalArgumentException("Missing secret parameter"),
                algorithm = params["algorithm"]?.uppercase() ?: "SHA1",
                digits = params["digits"]?.toIntOrNull() ?: 6,
                period = params["period"]?.toIntOrNull() ?: 30,
            )
        }

        private fun parseQueryParams(query: String): Map<String, String> {
            return query.split("&").associate {
                val (key, value) = it.split("=", limit = 2)
                java.net.URLDecoder.decode(key, "UTF-8") to java.net.URLDecoder.decode(value, "UTF-8")
            }
        }
    }
}
