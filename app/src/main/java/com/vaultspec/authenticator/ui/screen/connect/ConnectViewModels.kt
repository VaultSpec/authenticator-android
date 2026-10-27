package com.vaultspec.authenticator.ui.screen.connect

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaultspec.authenticator.backup.CloudBackupManager
import com.vaultspec.authenticator.backup.RestoreManager
import com.vaultspec.authenticator.data.model.BackupPayload
import com.vaultspec.authenticator.data.repository.VaultRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

private const val CODE_LIFETIME_MS = 5 * 60 * 1000L

sealed interface ShowStage {
    data object Password : ShowStage
    data object Starting : ShowStage
    data class Showing(val id: String, val token: String, val expiresAt: Long) : ShowStage
    data object Expired : ShowStage
    data object Connected : ShowStage
}

data class ShowPairingUiState(
    val stage: ShowStage = ShowStage.Starting,
    val password: String = "",
    val working: Boolean = false,
    val error: String? = null,
)

/** Settings → Connect a Device: shows a particle code until a new device scans it. */
@HiltViewModel
class ShowPairingViewModel @Inject constructor(
    private val cloud: CloudBackupManager,
) : ViewModel() {
    private val _state = MutableStateFlow(ShowPairingUiState())
    val state: StateFlow<ShowPairingUiState> = _state.asStateFlow()
    private var watcher: Job? = null

    init {
        if (cloud.hasBackupKey) start(null) else _state.update { it.copy(stage = ShowStage.Password) }
    }

    fun onPasswordChange(value: String) = _state.update { it.copy(password = value, error = null) }

    fun onConfirmPassword() {
        if (_state.value.password.isEmpty() || _state.value.working) return
        start(_state.value.password)
    }

    fun onNewCode() = start(_state.value.password.ifEmpty { null })

    private fun start(password: String?) {
        watcher?.cancel()
        _state.update { it.copy(working = true, error = null, stage = if (password != null && !cloud.hasBackupKey) it.stage else ShowStage.Starting) }
        viewModelScope.launch {
            try {
                val (id, token) = cloud.startPairing(password)
                val showing = ShowStage.Showing(id, token, System.currentTimeMillis() + CODE_LIFETIME_MS)
                _state.update { it.copy(stage = showing, working = false) }
                watch(showing)
            } catch (e: CancellationException) {
                throw e
            } catch (e: IllegalArgumentException) {
                _state.update { it.copy(stage = ShowStage.Password, working = false, error = e.message) }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        stage = if (cloud.hasBackupKey || password != null) ShowStage.Expired else ShowStage.Password,
                        working = false,
                        error = e.message ?: "Couldn't create a code. Try again.",
                    )
                }
            }
        }
    }

    /** Polls the server while the code is on screen. */
    private fun watch(showing: ShowStage.Showing) {
        watcher = viewModelScope.launch {
            while (true) {
                delay(2000)
                if (System.currentTimeMillis() >= showing.expiresAt) {
                    _state.update { it.copy(stage = ShowStage.Expired) }
                    return@launch
                }
                when (runCatching { cloud.pairingStatus(showing.id) }.getOrNull()) {
                    "claimed" -> { _state.update { it.copy(stage = ShowStage.Connected) }; return@launch }
                    "expired" -> { _state.update { it.copy(stage = ShowStage.Expired) }; return@launch }
                }
            }
        }
    }

    override fun onCleared() {
        // Leaving before the code was used: retire it on the server
        (_state.value.stage as? ShowStage.Showing)?.let { showing ->
            CoroutineScope(Dispatchers.IO).launch { cloud.cancelPairing(showing.id) }
        }
    }
}

enum class ScanStage { Scanning, Connecting, Password }

data class ScanToConnectUiState(
    val stage: ScanStage = ScanStage.Scanning,
    val password: String = "",
    val working: Boolean = false,
    val error: String? = null,
    val done: Boolean = false,
)

/** Setup → Use Another Device: scan the particle code, then open the vault it brings. */
@HiltViewModel
class ScanToConnectViewModel @Inject constructor(
    private val cloud: CloudBackupManager,
    private val vaultRepository: VaultRepository,
    private val restoreManager: RestoreManager,
) : ViewModel() {
    private val _state = MutableStateFlow(ScanToConnectUiState())
    val state: StateFlow<ScanToConnectUiState> = _state.asStateFlow()
    private var payload: BackupPayload? = null

    /** Called from the camera analyzer thread with each decoded token. */
    fun onToken(token: String) {
        if (_state.value.stage != ScanStage.Scanning) return
        _state.update { it.copy(stage = ScanStage.Connecting, error = null) }
        viewModelScope.launch {
            try {
                payload = cloud.claimPairing(token)
                _state.update { it.copy(stage = ScanStage.Password) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(stage = ScanStage.Scanning, error = e.message ?: "Couldn't connect. Try again.") }
            }
        }
    }

    fun onPasswordChange(value: String) = _state.update { it.copy(password = value, error = null) }

    /** Opens the snapshot with the vault password, which then becomes this device's vault password. */
    fun onUnlock() {
        val snapshot = payload ?: return
        val password = _state.value.password
        if (password.isEmpty() || _state.value.working) return
        _state.update { it.copy(working = true, error = null) }
        viewModelScope.launch {
            try {
                val accounts = cloud.decrypt(snapshot, password)
                withContext(Dispatchers.IO) {
                    vaultRepository.setupVault(password)
                    restoreManager.importAccounts(accounts, replace = true)
                }
                // Keep backing up from this device under the same password
                cloud.adopt(password)
                _state.update { it.copy(working = false, done = true) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: IllegalArgumentException) {
                _state.update { it.copy(working = false, error = "That vault password doesn't open this vault.") }
            } catch (e: Exception) {
                _state.update { it.copy(working = false, error = e.message ?: "Couldn't open the vault. Try again.") }
            }
        }
    }
}
