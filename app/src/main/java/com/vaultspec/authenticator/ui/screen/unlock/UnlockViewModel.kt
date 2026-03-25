package com.vaultspec.authenticator.ui.screen.unlock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaultspec.authenticator.crypto.BiometricKeyManager
import com.vaultspec.authenticator.crypto.MasterKeyManager
import com.vaultspec.authenticator.data.prefs.AppPreferencesManager
import com.vaultspec.authenticator.data.repository.VaultRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.crypto.Cipher
import javax.inject.Inject

data class UnlockUiState(
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isUnlocked: Boolean = false,
    val biometricEnabled: Boolean = false,
    val showBiometricPrompt: Boolean = false,
    val passwordReminderActive: Boolean = false,
)

@HiltViewModel
class UnlockViewModel @Inject constructor(
    private val vaultRepository: VaultRepository,
    private val biometricKeyManager: BiometricKeyManager,
    private val masterKeyManager: MasterKeyManager,
    private val prefs: AppPreferencesManager,
) : ViewModel() {

    private val _state = MutableStateFlow(UnlockUiState())
    val state: StateFlow<UnlockUiState> = _state.asStateFlow()

    private val isPasswordReminderDue: Boolean
        get() {
            val lastAuth = prefs.lastPasswordAuthTimestamp
            if (lastAuth == 0L) return false // Never authenticated yet, first time
            val days = prefs.passwordReminderDays
            return System.currentTimeMillis() - lastAuth > days * 86_400_000L
        }

    init {
        val reminderDue = isPasswordReminderDue
        _state.value = _state.value.copy(
            biometricEnabled = vaultRepository.isBiometricEnabled() && !reminderDue,
            passwordReminderActive = reminderDue,
        )
    }

    fun onPasswordChange(value: String) {
        _state.value = _state.value.copy(password = value, error = null)
    }

    fun onUnlockWithPassword() {
        val s = _state.value
        if (s.password.isBlank()) {
            _state.value = s.copy(error = "Enter your password")
            return
        }

        _state.value = s.copy(isLoading = true, error = null)
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    vaultRepository.unlockWithPassword(s.password)
                }
                prefs.lastPasswordAuthTimestamp = System.currentTimeMillis()
                _state.value = _state.value.copy(isLoading = false, isUnlocked = true)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Wrong password"
                )
            }
        }
    }

    fun triggerBiometric() {
        if (vaultRepository.isBiometricEnabled()) {
            _state.value = _state.value.copy(showBiometricPrompt = true)
        }
    }

    fun onBiometricPromptShown() {
        _state.value = _state.value.copy(showBiometricPrompt = false)
    }

    fun getDecryptCipher(): Cipher? {
        return try {
            val header = masterKeyManager.getHeader(
                // Will be called from composable context
                null as? android.content.Context ?: return null
            )
            val iv = header?.biometricSlot?.iv ?: return null
            val ivBytes = android.util.Base64.decode(iv, android.util.Base64.NO_WRAP)
            biometricKeyManager.getDecryptCipher(ivBytes)
        } catch (e: Exception) {
            null
        }
    }

    fun getDecryptCipher(context: android.content.Context): Cipher? {
        return try {
            val header = masterKeyManager.getHeader(context)
            val iv = header?.biometricSlot?.iv ?: return null
            val ivBytes = android.util.Base64.decode(iv, android.util.Base64.NO_WRAP)
            biometricKeyManager.getDecryptCipher(ivBytes)
        } catch (e: Exception) {
            null
        }
    }

    fun onBiometricSuccess(cipher: Cipher) {
        viewModelScope.launch {
            try {
                vaultRepository.unlockWithBiometric(cipher)
                _state.value = _state.value.copy(isUnlocked = true)
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = "Biometric unlock failed")
            }
        }
    }

    fun onBiometricError(message: String) {
        _state.value = _state.value.copy(error = message)
    }
}
