package com.vaultspec.authenticator.data.repository

import android.content.Context
import com.vaultspec.authenticator.crypto.MasterKeyManager
import com.vaultspec.authenticator.data.model.VaultState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.crypto.SecretKey
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VaultRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val masterKeyManager: MasterKeyManager,
) {

    private val _state = MutableStateFlow<VaultState>(determineInitialState())
    val state: StateFlow<VaultState> = _state.asStateFlow()

    val masterKey: SecretKey?
        get() = (_state.value as? VaultState.Unlocked)?.masterKey

    private fun determineInitialState(): VaultState {
        return if (masterKeyManager.isVaultSetUp(context)) {
            VaultState.Locked
        } else {
            VaultState.NeedsSetup
        }
    }

    fun setupVault(password: String): SecretKey {
        val key = masterKeyManager.setupVault(context, password)
        _state.value = VaultState.Unlocked(key)
        return key
    }

    fun unlockWithPassword(password: String): SecretKey {
        val key = masterKeyManager.unlockWithPassword(context, password)
        _state.value = VaultState.Unlocked(key)
        return key
    }

    fun unlockWithBiometric(cipher: javax.crypto.Cipher): SecretKey {
        val key = masterKeyManager.unlockWithBiometric(context, cipher)
        _state.value = VaultState.Unlocked(key)
        return key
    }

    fun lock() {
        _state.value = VaultState.Locked
    }

    fun isBiometricEnabled(): Boolean {
        return masterKeyManager.isBiometricEnabled(context)
    }

    fun enableBiometric(masterKey: SecretKey, authenticatedCipher: javax.crypto.Cipher) {
        masterKeyManager.finishBiometricEnrollment(context, masterKey, authenticatedCipher)
    }

    fun prepareBiometricEnrollment(): javax.crypto.Cipher {
        return masterKeyManager.prepareBiometricEnrollment()
    }

    fun disableBiometric() {
        masterKeyManager.disableBiometric(context)
    }

    fun changePassword(newPassword: String) {
        val key = masterKey ?: throw IllegalStateException("Vault must be unlocked")
        masterKeyManager.changePassword(context, key, newPassword)
    }

    fun deleteVault() {
        masterKeyManager.deleteVault(context)
        _state.value = VaultState.NeedsSetup
    }

    /** Used during restore to directly set the unlocked state with a provided master key. */
    fun setUnlocked(key: SecretKey) {
        _state.value = VaultState.Unlocked(key)
    }
}
