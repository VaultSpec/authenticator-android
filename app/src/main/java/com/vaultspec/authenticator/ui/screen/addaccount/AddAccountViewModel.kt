package com.vaultspec.authenticator.ui.screen.addaccount

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaultspec.authenticator.backup.BackupManager
import com.vaultspec.authenticator.data.db.CategoryDao
import com.vaultspec.authenticator.data.model.OtpAccount
import com.vaultspec.authenticator.data.repository.TokenRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

data class AddAccountUiState(
    val issuer: String = "",
    val accountName: String = "",
    val secret: String = "",
    val algorithm: String = "SHA1",
    val digits: Int = 6,
    val period: Int = 30,
    val category: String = "All",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSaved: Boolean = false,
    val showAddCategoryDialog: Boolean = false,
)

@HiltViewModel
class AddAccountViewModel @Inject constructor(
    private val tokenRepository: TokenRepository,
    private val categoryDao: CategoryDao,
    private val backupManager: BackupManager,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    private val _state = MutableStateFlow(AddAccountUiState())
    val state: StateFlow<AddAccountUiState> = _state.asStateFlow()

    val categories: StateFlow<List<String>> = categoryDao.observeAll()
        .map { list -> list.map { it.name } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("All"))

    fun onIssuerChange(value: String) {
        _state.value = _state.value.copy(issuer = value, error = null)
    }

    fun onAccountNameChange(value: String) {
        _state.value = _state.value.copy(accountName = value, error = null)
    }

    fun onSecretChange(value: String) {
        _state.value = _state.value.copy(secret = value.uppercase().replace(" ", ""), error = null)
    }

    fun onAlgorithmChange(value: String) {
        _state.value = _state.value.copy(algorithm = value)
    }

    fun onDigitsChange(value: Int) {
        _state.value = _state.value.copy(digits = value)
    }

    fun onPeriodChange(value: Int) {
        _state.value = _state.value.copy(period = value)
    }

    fun onCategoryChange(value: String) {
        _state.value = _state.value.copy(category = value)
    }

    fun onShowAddCategoryDialog() {
        _state.value = _state.value.copy(showAddCategoryDialog = true)
    }

    fun onDismissAddCategoryDialog() {
        _state.value = _state.value.copy(showAddCategoryDialog = false)
    }

    fun onAddCategory(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val category = com.vaultspec.authenticator.data.db.entity.Category(
                    name = name.trim(),
                    isDefault = false,
                )
                categoryDao.insert(category)
                _state.value = _state.value.copy(
                    category = name.trim(),
                    showAddCategoryDialog = false,
                )
            } catch (_: CancellationException) {
                throw CancellationException()
            } catch (_: Exception) { }
        }
    }

    fun onQrScanned(uri: String) {
        try {
            val account = OtpAccount.fromUri(uri)
            _state.value = _state.value.copy(
                issuer = account.issuer,
                accountName = account.accountName,
                secret = account.secret,
                algorithm = account.algorithm,
                digits = account.digits,
                period = account.period,
                error = null,
            )
        } catch (e: Exception) {
            _state.value = _state.value.copy(error = "Invalid QR code: ${e.message}")
        }
    }

    fun onSave() {
        val s = _state.value
        if (s.issuer.isBlank()) {
            _state.value = s.copy(error = "Issuer is required")
            return
        }
        if (s.secret.isBlank()) {
            _state.value = s.copy(error = "Secret key is required")
            return
        }

        _state.value = s.copy(isLoading = true, error = null)
        viewModelScope.launch {
            try {
                val account = OtpAccount(
                    issuer = s.issuer,
                    accountName = s.accountName,
                    secret = s.secret,
                    algorithm = s.algorithm,
                    digits = s.digits,
                    period = s.period,
                    category = s.category,
                )
                tokenRepository.addAccount(account)
                backupManager.triggerAutoBackupIfEnabled(appContext)
                _state.value = _state.value.copy(isLoading = false, isSaved = true)
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Failed to save: ${e.message}"
                )
            }
        }
    }
}
