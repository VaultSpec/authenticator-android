package com.vaultspec.authenticator.data.model

import javax.crypto.SecretKey

sealed class VaultState {
    data object NeedsSetup : VaultState()
    data object Locked : VaultState()
    data class Unlocked(val masterKey: SecretKey) : VaultState()
}
