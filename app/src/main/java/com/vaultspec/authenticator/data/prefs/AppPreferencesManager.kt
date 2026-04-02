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

    // Theme mode: "system", "light", "dark"
    private val _themeModeFlow = MutableStateFlow("system")
    val themeModeFlow: StateFlow<String> = _themeModeFlow.asStateFlow()

    private val _pitchBlackFlow = MutableStateFlow(false)
    val pitchBlackFlow: StateFlow<Boolean> = _pitchBlackFlow.asStateFlow()

    var themeMode: String
        get() = prefs.getString(KEY_THEME_MODE, null)
            ?: if (prefs.contains(KEY_DARK_MODE)) {
                // Migrate legacy boolean to new tri-state
                if (prefs.getBoolean(KEY_DARK_MODE, false)) "dark" else "light"
            } else "system"
        set(value) {
            prefs.edit().putString(KEY_THEME_MODE, value).apply()
            _themeModeFlow.value = value
        }

    // Legacy accessor kept for launch-time window background (no Compose context)
    val darkMode: Boolean
        get() = themeMode == "dark"

    var pitchBlack: Boolean
        get() = prefs.getBoolean(KEY_PITCH_BLACK, false)
        set(value) {
            prefs.edit().putBoolean(KEY_PITCH_BLACK, value).apply()
            _pitchBlackFlow.value = value
        }

    fun initThemeFlows() {
        _themeModeFlow.value = themeMode
        _pitchBlackFlow.value = pitchBlack
    }

    var sessionTimeoutSeconds: Int
        get() = prefs.getInt(KEY_SESSION_TIMEOUT, 0)
        set(value) = prefs.edit().putInt(KEY_SESSION_TIMEOUT, value).apply()

    var lastBackgroundTimestamp: Long
        get() = prefs.getLong(KEY_LAST_BACKGROUND, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_BACKGROUND, value).apply()

    var backupFolderUri: String?
        get() = prefs.getString(KEY_BACKUP_FOLDER_URI, null)
        set(value) { prefs.edit().putString(KEY_BACKUP_FOLDER_URI, value).apply() }

    var autoBackupEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_BACKUP, false)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_BACKUP, value).apply()

    var backupPassword: String?
        get() = prefs.getString(KEY_BACKUP_PASSWORD, null)
        set(value) {
            if (value != null) prefs.edit().putString(KEY_BACKUP_PASSWORD, value).apply()
            else prefs.edit().remove(KEY_BACKUP_PASSWORD).apply()
        }

    companion object {
        private const val KEY_ALLOW_SCREENSHOTS = "allow_screenshots"
        private const val KEY_TAP_TO_REVEAL = "tap_to_reveal"
        private const val KEY_REVEAL_TIMEOUT = "reveal_timeout_seconds"
        private const val KEY_TAP_TO_COPY = "tap_to_copy"
        private const val KEY_HIGHLIGHT_ON_TAP = "highlight_on_tap"
        private const val KEY_PASSWORD_REMINDER_DAYS = "password_reminder_days"
        private const val KEY_LAST_PASSWORD_AUTH = "last_password_auth_timestamp"
        private const val KEY_DARK_MODE = "dark_mode" // legacy
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_PITCH_BLACK = "pitch_black"
        private const val KEY_SESSION_TIMEOUT = "session_timeout_seconds"
        private const val KEY_LAST_BACKGROUND = "last_background_timestamp"
        private const val KEY_BACKUP_FOLDER_URI = "backup_folder_uri"
        private const val KEY_AUTO_BACKUP = "auto_backup_enabled"
        private const val KEY_BACKUP_PASSWORD = "backup_password"
    }
}
