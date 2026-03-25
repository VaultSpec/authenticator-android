package com.vaultspec.authenticator.ui.screen.setup

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaultspec.authenticator.backup.RestoreManager
import com.vaultspec.authenticator.data.repository.VaultRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class SetupUiState(
    val password: String = "",
    val confirmPassword: String = "",
    val enableBiometric: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSetupComplete: Boolean = false,
    // Biometric enrollment
    val showBiometricPrompt: Boolean = false,
    val biometricEnrollCipher: javax.crypto.Cipher? = null,
    // Restore state
    val showRestoreDialog: Boolean = false,
    val restoreFileUri: Uri? = null,
    val restorePassword: String = "",
    val restoreNewPassword: String = "",
    val restoreConfirmPassword: String = "",
    val restoreEnableBiometric: Boolean = false,
    val restoreError: String? = null,
    val restoreSuccess: Boolean = false,
    val restoredCount: Int = 0,
)

@HiltViewModel
class SetupViewModel @Inject constructor(
    private val vaultRepository: VaultRepository,
    private val restoreManager: RestoreManager,
) : ViewModel() {

    private val _state = MutableStateFlow(SetupUiState())
    val state: StateFlow<SetupUiState> = _state.asStateFlow()

    fun onPasswordChange(value: String) {
        _state.value = _state.value.copy(password = value, error = null)
    }

    fun onConfirmPasswordChange(value: String) {
        _state.value = _state.value.copy(confirmPassword = value, error = null)
    }

    fun onBiometricToggle(enabled: Boolean) {
        _state.value = _state.value.copy(enableBiometric = enabled)
    }

    fun onSetup() {
        val s = _state.value
        if (s.password.length < 6) {
            _state.value = s.copy(error = "Password must be at least 6 characters")
            return
        }
        if (s.password != s.confirmPassword) {
            _state.value = s.copy(error = "Passwords do not match")
            return
        }

        _state.value = s.copy(isLoading = true, error = null)
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    vaultRepository.setupVault(s.password)
                }
                if (s.enableBiometric) {
                    // Start biometric enrollment
                    val cipher = withContext(Dispatchers.IO) {
                        vaultRepository.prepareBiometricEnrollment()
                    }
                    _state.value = _state.value.copy(
                        isLoading = false,
                        showBiometricPrompt = true,
                        biometricEnrollCipher = cipher,
                    )
                } else {
                    _state.value = _state.value.copy(isLoading = false, isSetupComplete = true)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Setup failed: ${e.javaClass.simpleName}: ${e.message ?: e.cause?.message ?: "Unknown error"}"
                )
            }
        }
    }

    fun onBiometricEnrollSuccess(cipher: javax.crypto.Cipher) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val key = vaultRepository.masterKey
                        ?: throw IllegalStateException("Vault not unlocked")
                    vaultRepository.enableBiometric(key, cipher)
                }
            } catch (_: CancellationException) {
                throw CancellationException()
            } catch (_: Exception) { }
            _state.value = _state.value.copy(
                showBiometricPrompt = false,
                biometricEnrollCipher = null,
                isSetupComplete = true,
            )
        }
    }

    fun onBiometricEnrollFailed() {
        // Enrollment failed or cancelled — still complete setup without biometric
        _state.value = _state.value.copy(
            showBiometricPrompt = false,
            biometricEnrollCipher = null,
            isSetupComplete = true,
        )
    }

    // --- Restore flow ---
    fun onShowRestore() {
        _state.value = _state.value.copy(showRestoreDialog = true)
    }

    fun onDismissRestore() {
        _state.value = _state.value.copy(
            showRestoreDialog = false,
            restoreFileUri = null,
            restorePassword = "",
            restoreNewPassword = "",
            restoreConfirmPassword = "",
            restoreError = null,
        )
    }

    fun onRestoreFileSelected(uri: Uri) {
        _state.value = _state.value.copy(restoreFileUri = uri)
    }

    fun onRestorePasswordChange(value: String) {
        _state.value = _state.value.copy(restorePassword = value, restoreError = null)
    }

    fun onRestoreNewPasswordChange(value: String) {
        _state.value = _state.value.copy(restoreNewPassword = value, restoreError = null)
    }

    fun onRestoreConfirmPasswordChange(value: String) {
        _state.value = _state.value.copy(restoreConfirmPassword = value, restoreError = null)
    }

    fun onRestoreBiometricToggle(enabled: Boolean) {
        _state.value = _state.value.copy(restoreEnableBiometric = enabled)
    }

    fun onRestore(context: android.content.Context) {
        val s = _state.value
        val fileUri = s.restoreFileUri
        if (fileUri == null) {
            _state.value = s.copy(restoreError = "Please select a backup file")
            return
        }
        if (s.restorePassword.isBlank()) {
            _state.value = s.copy(restoreError = "Enter the backup password")
            return
        }
        if (s.restoreNewPassword.length < 6) {
            _state.value = s.copy(restoreError = "New password must be at least 6 characters")
            return
        }
        if (s.restoreNewPassword != s.restoreConfirmPassword) {
            _state.value = s.copy(restoreError = "New passwords do not match")
            return
        }

        _state.value = s.copy(isLoading = true, restoreError = null)
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    // 1. Setup vault with new password
                    vaultRepository.setupVault(s.restoreNewPassword)

                    // 2. Restore backup entries
                    val count = restoreManager.restoreBackup(
                        context = context,
                        fileUri = fileUri,
                        password = s.restorePassword,
                        replace = true,
                    )
                    count
                }.let { count ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        restoreSuccess = true,
                        restoredCount = count,
                        isSetupComplete = true,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    restoreError = "Restore failed: ${e.javaClass.simpleName}: ${e.message ?: "Unknown error"}"
                )
            }
        }
    }
}
