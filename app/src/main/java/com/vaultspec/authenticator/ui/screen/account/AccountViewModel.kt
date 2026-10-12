package com.vaultspec.authenticator.ui.screen.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaultspec.authenticator.cloud.CloudApi
import com.vaultspec.authenticator.cloud.CloudException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

enum class AccountMode { SignIn, SignUp, EnterCode }

data class AccountUiState(
    val mode: AccountMode = AccountMode.SignIn,
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val code: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    val isSignedIn: Boolean = false,
)

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val api: CloudApi,
) : ViewModel() {

    private val _state = MutableStateFlow(AccountUiState())
    val state: StateFlow<AccountUiState> = _state.asStateFlow()

    fun onEmailChange(value: String) { _state.value = _state.value.copy(email = value, error = null) }
    fun onPasswordChange(value: String) { _state.value = _state.value.copy(password = value, error = null) }
    fun onConfirmPasswordChange(value: String) { _state.value = _state.value.copy(confirmPassword = value, error = null) }
    fun onCodeChange(value: String) {
        _state.value = _state.value.copy(code = value.filter(Char::isDigit).take(CODE_LENGTH), error = null)
    }
    fun clearMessage() { _state.value = _state.value.copy(message = null) }

    fun onSwitchMode(mode: AccountMode) {
        _state.value = _state.value.copy(mode = mode, confirmPassword = "", code = "", error = null)
    }

    fun onSubmit() {
        val s = _state.value
        val email = s.email.trim()
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _state.value = s.copy(error = "Enter a valid email address")
            return
        }
        when (s.mode) {
            AccountMode.SignUp -> {
                if (s.password.length < MIN_PASSWORD_LENGTH) {
                    _state.value = s.copy(error = "Account password must be at least $MIN_PASSWORD_LENGTH characters")
                    return
                }
                if (s.password != s.confirmPassword) {
                    _state.value = s.copy(error = "Passwords do not match")
                    return
                }
                run {
                    api.register(email, s.password)
                    _state.value = _state.value.copy(mode = AccountMode.EnterCode, code = "")
                }
            }
            AccountMode.SignIn -> {
                if (s.password.isEmpty()) {
                    _state.value = s.copy(error = "Enter your account password")
                    return
                }
                run {
                    api.requestLoginCode(email, s.password)
                    _state.value = _state.value.copy(mode = AccountMode.EnterCode, code = "")
                }
            }
            AccountMode.EnterCode -> {
                if (s.code.length != CODE_LENGTH) {
                    _state.value = s.copy(error = "Enter the $CODE_LENGTH-digit code from the email")
                    return
                }
                run {
                    api.verifyLoginCode(email, s.password, s.code)
                    _state.value = _state.value.copy(isSignedIn = true)
                }
            }
        }
    }

    fun onResendCode() = run {
        api.requestLoginCode(_state.value.email.trim(), _state.value.password)
        _state.value = _state.value.copy(code = "", message = "New code sent")
    }

    fun onForgotPassword() {
        val email = _state.value.email.trim()
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _state.value = _state.value.copy(error = "Enter your email address first")
            return
        }
        run {
            api.forgotPassword(email)
            _state.value = _state.value.copy(message = "If that address has an account, a reset link is on its way")
        }
    }

    /** Runs a blocking cloud call off the main thread with shared loading and error handling. */
    private fun run(block: () -> Unit) {
        _state.value = _state.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { block() }
                _state.value = _state.value.copy(isLoading = false)
            } catch (e: CancellationException) {
                throw e
            } catch (e: CloudException) {
                _state.value = _state.value.copy(isLoading = false, error = e.message)
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = "Something went wrong. Try again.")
            }
        }
    }

    private companion object {
        const val MIN_PASSWORD_LENGTH = 8
        const val CODE_LENGTH = 6
    }
}
