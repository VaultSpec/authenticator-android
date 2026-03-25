package com.vaultspec.authenticator.data.prefs

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppPreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val prefs: SharedPreferences by lazy {
        EncryptedSharedPreferences.create(
            "app_preferences",
            MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    var allowScreenshots: Boolean
        get() = prefs.getBoolean(KEY_ALLOW_SCREENSHOTS, false)
        set(value) = prefs.edit().putBoolean(KEY_ALLOW_SCREENSHOTS, value).apply()

    var tapToReveal: Boolean
        get() = prefs.getBoolean(KEY_TAP_TO_REVEAL, false)
        set(value) = prefs.edit().putBoolean(KEY_TAP_TO_REVEAL, value).apply()

    var revealTimeoutSeconds: Int
        get() = prefs.getInt(KEY_REVEAL_TIMEOUT, 30)
        set(value) = prefs.edit().putInt(KEY_REVEAL_TIMEOUT, value).apply()

    var tapToCopy: Boolean
        get() = prefs.getBoolean(KEY_TAP_TO_COPY, false)
        set(value) = prefs.edit().putBoolean(KEY_TAP_TO_COPY, value).apply()

    var highlightOnTap: Boolean
        get() = prefs.getBoolean(KEY_HIGHLIGHT_ON_TAP, false)
        set(value) = prefs.edit().putBoolean(KEY_HIGHLIGHT_ON_TAP, value).apply()

    var passwordReminderDays: Int
        get() = prefs.getInt(KEY_PASSWORD_REMINDER_DAYS, 15)
        set(value) = prefs.edit().putInt(KEY_PASSWORD_REMINDER_DAYS, value).apply()

    var lastPasswordAuthTimestamp: Long
        get() = prefs.getLong(KEY_LAST_PASSWORD_AUTH, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_PASSWORD_AUTH, value).apply()

    private val _darkModeFlow = MutableStateFlow(false)
    val darkModeFlow: StateFlow<Boolean> = _darkModeFlow.asStateFlow()

    var darkMode: Boolean
        get() = prefs.getBoolean(KEY_DARK_MODE, false)
        set(value) {
            prefs.edit().putBoolean(KEY_DARK_MODE, value).apply()
            _darkModeFlow.value = value
        }

    fun initDarkModeFlow() {
        _darkModeFlow.value = darkMode
    }

    var sessionTimeoutSeconds: Int
        get() = prefs.getInt(KEY_SESSION_TIMEOUT, 0)
        set(value) = prefs.edit().putInt(KEY_SESSION_TIMEOUT, value).apply()

    var lastBackgroundTimestamp: Long
        get() = prefs.getLong(KEY_LAST_BACKGROUND, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_BACKGROUND, value).apply()

    companion object {
        private const val KEY_ALLOW_SCREENSHOTS = "allow_screenshots"
        private const val KEY_TAP_TO_REVEAL = "tap_to_reveal"
        private const val KEY_REVEAL_TIMEOUT = "reveal_timeout_seconds"
        private const val KEY_TAP_TO_COPY = "tap_to_copy"
        private const val KEY_HIGHLIGHT_ON_TAP = "highlight_on_tap"
        private const val KEY_PASSWORD_REMINDER_DAYS = "password_reminder_days"
        private const val KEY_LAST_PASSWORD_AUTH = "last_password_auth_timestamp"
        private const val KEY_DARK_MODE = "dark_mode"
        private const val KEY_SESSION_TIMEOUT = "session_timeout_seconds"
        private const val KEY_LAST_BACKGROUND = "last_background_timestamp"
    }
}
