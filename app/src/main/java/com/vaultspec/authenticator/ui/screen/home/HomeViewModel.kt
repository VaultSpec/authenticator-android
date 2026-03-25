package com.vaultspec.authenticator.ui.screen.home

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaultspec.authenticator.data.db.CategoryDao
import com.vaultspec.authenticator.data.db.entity.TokenEntry
import com.vaultspec.authenticator.data.prefs.AppPreferencesManager
import com.vaultspec.authenticator.data.repository.TokenRepository
import com.vaultspec.authenticator.data.repository.VaultRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TokenDisplayItem(
    val entry: TokenEntry,
    val code: String,
    val secondsRemaining: Int,
)

data class HomeUiState(
    val tokens: List<TokenDisplayItem> = emptyList(),
    val categories: List<String> = listOf("All"),
    val selectedCategory: String = "All",
    val searchQuery: String = "",
    val isSearching: Boolean = false,
    // Token interaction state
    val revealedTokenIds: Set<Long> = emptySet(),
    val highlightedTokenId: Long? = null,
    // Prefs
    val tapToReveal: Boolean = false,
    val tapToCopy: Boolean = false,
    val highlightOnTap: Boolean = false,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val tokenRepository: TokenRepository,
    private val vaultRepository: VaultRepository,
    private val categoryDao: CategoryDao,
    private val prefs: AppPreferencesManager,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    private val _selectedCategory = MutableStateFlow("All")
    private val _searchQuery = MutableStateFlow("")
    private val _tick = MutableStateFlow(System.currentTimeMillis())
    private val _revealedTokenIds = MutableStateFlow<Set<Long>>(emptySet())
    private val _highlightedTokenId = MutableStateFlow<Long?>(null)
    private var revealJobs = mutableMapOf<Long, Job>()

    private val _state = MutableStateFlow(HomeUiState(
        tapToReveal = prefs.tapToReveal,
        tapToCopy = prefs.tapToCopy,
        highlightOnTap = prefs.highlightOnTap,
    ))
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        // Tick every second to refresh TOTP codes
        viewModelScope.launch {
            while (true) {
                _tick.value = System.currentTimeMillis()
                delay(1000)
            }
        }

        // Combine token list + tick + category + search + categories from DB
        viewModelScope.launch {
            combine(
                tokenRepository.observeAll(),
                _tick,
                _selectedCategory,
                _searchQuery,
                categoryDao.observeAll().map { list -> list.map { it.name } },
            ) { allTokens, timestamp, category, query, categories ->
                val filteredTokens = allTokens
                    .filter { token ->
                        if (category != "All") token.category == category else true
                    }
                    .filter { token ->
                        if (query.isNotBlank()) {
                            token.issuer.contains(query, ignoreCase = true) ||
                            token.accountName.contains(query, ignoreCase = true)
                        } else true
                    }

                val displayItems = filteredTokens.map { entry ->
                    val code = try {
                        tokenRepository.generateCode(entry, timestamp)
                    } catch (e: Exception) {
                        "------"
                    }
                    val secondsRemaining = entry.period - ((timestamp / 1000) % entry.period).toInt()

                    TokenDisplayItem(
                        entry = entry,
                        code = code,
                        secondsRemaining = secondsRemaining,
                    )
                }

                _state.value.copy(
                    tokens = displayItems,
                    categories = categories,
                    selectedCategory = category,
                    searchQuery = query,
                    isSearching = query.isNotBlank(),
                    revealedTokenIds = _revealedTokenIds.value,
                    highlightedTokenId = _highlightedTokenId.value,
                    tapToReveal = prefs.tapToReveal,
                    tapToCopy = prefs.tapToCopy,
                    highlightOnTap = prefs.highlightOnTap,
                )
            }.collect { _state.value = it }
        }
    }

    fun onCategorySelected(category: String) {
        _selectedCategory.value = category
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onDeleteToken(entry: TokenEntry) {
        viewModelScope.launch {
            tokenRepository.deleteToken(entry)
        }
    }

    fun onTokenTap(tokenId: Long, code: String) {
        val tapToReveal = prefs.tapToReveal
        val highlightOnTap = prefs.highlightOnTap
        val tapToCopy = prefs.tapToCopy

        // Tap-to-reveal: first tap reveals
        if (tapToReveal && tokenId !in _revealedTokenIds.value) {
            val revealed = _revealedTokenIds.value + tokenId
            _revealedTokenIds.value = revealed
            _state.value = _state.value.copy(revealedTokenIds = revealed)

            // Auto-hide after timeout
            revealJobs[tokenId]?.cancel()
            revealJobs[tokenId] = viewModelScope.launch {
                delay(prefs.revealTimeoutSeconds * 1000L)
                val updated = _revealedTokenIds.value - tokenId
                _revealedTokenIds.value = updated
                _state.value = _state.value.copy(revealedTokenIds = updated)
            }
            return
        }

        // Highlight behavior: 1st tap highlights, 2nd tap copies
        if (highlightOnTap) {
            if (_highlightedTokenId.value == tokenId) {
                // 2nd tap on highlighted — copy
                copyToClipboard(code)
                _highlightedTokenId.value = null
                _state.value = _state.value.copy(highlightedTokenId = null)
            } else {
                // 1st tap — highlight
                _highlightedTokenId.value = tokenId
                _state.value = _state.value.copy(highlightedTokenId = tokenId)
            }
            return
        }

        // Simple tap-to-copy
        if (tapToCopy) {
            copyToClipboard(code)
            return
        }
    }

    private fun copyToClipboard(code: String) {
        val clipboard = appContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("TOTP Code", code)
        clipboard.setPrimaryClip(clip)
    }

    fun lock() {
        vaultRepository.lock()
    }
}
