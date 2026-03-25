package com.vaultspec.authenticator.ui.screen.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaultspec.authenticator.backup.BackupManager
import com.vaultspec.authenticator.backup.RestoreManager
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
import javax.inject.Inject

data class SettingsUiState(
    val biometricEnabled: Boolean = false,
    val isLoading: Boolean = false,
    val message: String? = null,
    val error: String? = null,
    // Biometric enrollment
    val showBiometricPrompt: Boolean = false,
    val biometricEnrollCipher: javax.crypto.Cipher? = null,
    // Change password
    val showChangePassword: Boolean = false,
    val oldPassword: String = "",
    val newPassword: String = "",
    val confirmNewPassword: String = "",
    // Backup
    val backupPassword: String = "",
    val showBackupDialog: Boolean = false,
    // Restore
    val showRestoreDialog: Boolean = false,
    val restoreFileUri: Uri? = null,
    val restorePassword: String = "",
    // Behavior preferences
    val allowScreenshots: Boolean = false,
    val tapToReveal: Boolean = false,
    val revealTimeoutSeconds: Int = 30,
    val tapToCopy: Boolean = false,
    val highlightOnTap: Boolean = false,
    val passwordReminderDays: Int = 15,
    val darkMode: Boolean = false,
    val sessionTimeoutSeconds: Int = 0,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val vaultRepository: VaultRepository,
    private val backupManager: BackupManager,
    private val restoreManager: RestoreManager,
    private val prefs: AppPreferencesManager,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState(
        biometricEnabled = vaultRepository.isBiometricEnabled(),
        allowScreenshots = prefs.allowScreenshots,
        tapToReveal = prefs.tapToReveal,
        revealTimeoutSeconds = prefs.revealTimeoutSeconds,
        tapToCopy = prefs.tapToCopy,
        highlightOnTap = prefs.highlightOnTap,
        passwordReminderDays = prefs.passwordReminderDays,
        darkMode = prefs.darkMode,
        sessionTimeoutSeconds = prefs.sessionTimeoutSeconds,
    ))
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    // --- Biometric ---
    fun onBiometricToggle(enabled: Boolean) {
        if (enabled) {
            try {
                val cipher = vaultRepository.prepareBiometricEnrollment()
                _state.value = _state.value.copy(
                    showBiometricPrompt = true,
                    biometricEnrollCipher = cipher,
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = "Failed to prepare biometric: ${e.message ?: e.javaClass.simpleName}")
            }
        } else {
            try {
                vaultRepository.disableBiometric()
                _state.value = _state.value.copy(
                    biometricEnabled = false,
                    message = "Biometric unlock disabled",
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = "Failed: ${e.message}")
            }
        }
    }

    fun onBiometricPromptShown() {
        _state.value = _state.value.copy(showBiometricPrompt = false)
    }

    fun onBiometricEnrollSuccess(authenticatedCipher: javax.crypto.Cipher) {
        try {
            val masterKey = vaultRepository.masterKey ?: throw IllegalStateException("Vault locked")
            vaultRepository.enableBiometric(masterKey, authenticatedCipher)
            _state.value = _state.value.copy(
                biometricEnabled = true,
                biometricEnrollCipher = null,
                message = "Biometric unlock enabled",
            )
        } catch (e: Exception) {
            _state.value = _state.value.copy(
                biometricEnrollCipher = null,
                error = "Failed to enable biometric: ${e.message ?: e.javaClass.simpleName}",
            )
        }
    }

    fun onBiometricEnrollError(message: String) {
        _state.value = _state.value.copy(
            biometricEnrollCipher = null,
            error = message,
        )
    }

    // --- Change Password ---
    fun onShowChangePassword() {
        _state.value = _state.value.copy(showChangePassword = true, error = null)
    }

    fun onDismissChangePassword() {
        _state.value = _state.value.copy(
            showChangePassword = false, oldPassword = "", newPassword = "", confirmNewPassword = ""
        )
    }

    fun onOldPasswordChange(value: String) { _state.value = _state.value.copy(oldPassword = value, error = null) }
    fun onNewPasswordChange(value: String) { _state.value = _state.value.copy(newPassword = value, error = null) }
    fun onConfirmNewPasswordChange(value: String) { _state.value = _state.value.copy(confirmNewPassword = value, error = null) }

    fun onChangePassword() {
        val s = _state.value
        if (s.newPassword.length < 6) {
            _state.value = s.copy(error = "New password must be at least 6 characters")
            return
        }
        if (s.newPassword != s.confirmNewPassword) {
            _state.value = s.copy(error = "Passwords do not match")
            return
        }
        _state.value = s.copy(isLoading = true, error = null)
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    vaultRepository.changePassword(s.newPassword)
                }
                _state.value = _state.value.copy(
                    isLoading = false,
                    showChangePassword = false,
                    oldPassword = "", newPassword = "", confirmNewPassword = "",
                    message = "Password changed successfully",
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = "Failed: ${e.message}")
            }
        }
    }

    // --- Backup ---
    fun onShowBackupDialog() {
        _state.value = _state.value.copy(showBackupDialog = true, backupPassword = "", error = null)
    }

    fun onDismissBackupDialog() {
        _state.value = _state.value.copy(showBackupDialog = false)
    }

    fun onBackupPasswordChange(value: String) {
        _state.value = _state.value.copy(backupPassword = value, error = null)
    }

    fun onBackup(context: android.content.Context, folderUri: Uri) {
        val s = _state.value
        if (s.backupPassword.length < 6) {
            _state.value = s.copy(error = "Password must be at least 6 characters")
            return
        }
        val masterKey = vaultRepository.masterKey
        if (masterKey == null) {
            _state.value = s.copy(error = "Vault is locked")
            return
        }

        _state.value = s.copy(isLoading = true, error = null)
        viewModelScope.launch {
            try {
                val filename = withContext(Dispatchers.IO) {
                    backupManager.createBackup(context, folderUri, s.backupPassword, masterKey)
                }
                _state.value = _state.value.copy(
                    isLoading = false,
                    showBackupDialog = false,
                    message = "Backup saved: $filename",
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Backup failed: ${e.message}"
                )
            }
        }
    }

    // --- Restore ---
    fun onShowRestoreDialog() {
        _state.value = _state.value.copy(showRestoreDialog = true, restorePassword = "", restoreFileUri = null, error = null)
    }

    fun onDismissRestoreDialog() {
        _state.value = _state.value.copy(showRestoreDialog = false)
    }

    fun onRestoreFileSelected(uri: Uri) {
        _state.value = _state.value.copy(restoreFileUri = uri)
    }

    fun onRestorePasswordChange(value: String) {
        _state.value = _state.value.copy(restorePassword = value, error = null)
    }

    fun onRestore(context: android.content.Context) {
        val s = _state.value
        val fileUri = s.restoreFileUri
        if (fileUri == null) {
            _state.value = s.copy(error = "Select a backup file")
            return
        }
        if (s.restorePassword.isBlank()) {
            _state.value = s.copy(error = "Enter the backup password")
            return
        }

        _state.value = s.copy(isLoading = true, error = null)
        viewModelScope.launch {
            try {
                val count = withContext(Dispatchers.IO) {
                    restoreManager.restoreBackup(context, fileUri, s.restorePassword, replace = true)
                }
                _state.value = _state.value.copy(
                    isLoading = false,
                    showRestoreDialog = false,
                    message = "Restored $count accounts",
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Restore failed: ${e.message}"
                )
            }
        }
    }

    fun clearMessage() {
        _state.value = _state.value.copy(message = null, error = null)
    }

    // --- Behavior preferences ---
    fun onAllowScreenshotsToggle(enabled: Boolean) {
        prefs.allowScreenshots = enabled
        _state.value = _state.value.copy(allowScreenshots = enabled)
    }

    fun onTapToRevealToggle(enabled: Boolean) {
        prefs.tapToReveal = enabled
        _state.value = _state.value.copy(tapToReveal = enabled)
    }

    fun onRevealTimeoutChange(seconds: Int) {
        prefs.revealTimeoutSeconds = seconds
        _state.value = _state.value.copy(revealTimeoutSeconds = seconds)
    }

    fun onTapToCopyToggle(enabled: Boolean) {
        prefs.tapToCopy = enabled
        _state.value = _state.value.copy(tapToCopy = enabled)
    }

    fun onHighlightOnTapToggle(enabled: Boolean) {
        prefs.highlightOnTap = enabled
        _state.value = _state.value.copy(highlightOnTap = enabled)
    }

    fun onPasswordReminderDaysChange(days: Int) {
        prefs.passwordReminderDays = days
        _state.value = _state.value.copy(passwordReminderDays = days)
    }

    fun onDarkModeToggle(enabled: Boolean) {
        prefs.darkMode = enabled
        _state.value = _state.value.copy(darkMode = enabled)
    }

    fun onSessionTimeoutChange(seconds: Int) {
        prefs.sessionTimeoutSeconds = seconds
        _state.value = _state.value.copy(sessionTimeoutSeconds = seconds)
    }
}
