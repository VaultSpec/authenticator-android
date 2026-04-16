package com.vaultspec.authenticator

import android.os.Bundle
import android.os.SystemClock
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentActivity
import com.vaultspec.authenticator.data.model.VaultState
import com.vaultspec.authenticator.data.prefs.AppPreferencesManager
import com.vaultspec.authenticator.data.repository.VaultRepository
import com.vaultspec.authenticator.ui.navigation.NavGraph
import com.vaultspec.authenticator.ui.theme.VaultSpecTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var vaultRepository: VaultRepository

    @Inject
    lateinit var prefs: AppPreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set window background early to prevent white flash in dark mode
        val themeModeAtLaunch = prefs.themeMode
        val isPitchBlackAtLaunch = prefs.pitchBlack
        if (themeModeAtLaunch == "dark") {
            window.decorView.setBackgroundColor(
                if (isPitchBlackAtLaunch) android.graphics.Color.BLACK
                else android.graphics.Color.parseColor("#0F0F0F")
            )
        }

        enableEdgeToEdge()

        prefs.initThemeFlows()
        updateScreenshotPolicy()

        setContent {
            val themeMode by prefs.themeModeFlow.collectAsState()
            val isPitchBlack by prefs.pitchBlackFlow.collectAsState()
            val systemDark = isSystemInDarkTheme()

            val isDarkMode = when (themeMode) {
                "dark" -> true
                "light" -> false
                else -> systemDark // "system"
            }

            // Keep window background and system bar appearance in sync with theme
            SideEffect {
                window.decorView.setBackgroundColor(
                    when {
                        isDarkMode && isPitchBlack -> android.graphics.Color.BLACK
                        isDarkMode -> android.graphics.Color.parseColor("#0F0F0F")
                        else -> android.graphics.Color.parseColor("#F5F7FA")
                    }
                )
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                insetsController.isAppearanceLightStatusBars = !isDarkMode
                insetsController.isAppearanceLightNavigationBars = !isDarkMode
            }

            VaultSpecTheme(darkTheme = isDarkMode, pitchBlack = isPitchBlack) {
                NavGraph(vaultRepository = vaultRepository, prefs = prefs)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateScreenshotPolicy()
    }

    override fun onPause() {
        super.onPause()
        // If session timeout is "Immediately", lock the vault when paused (including recents)
        if (prefs.sessionTimeoutSeconds == 0 && vaultRepository.state.value is VaultState.Unlocked) {
            vaultRepository.lock()
            // Temporarily set FLAG_SECURE to blank the recents thumbnail
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE,
            )
        }
    }

    override fun onStop() {
        super.onStop()
        prefs.lastBackgroundTimestamp = SystemClock.elapsedRealtime()
    }

    override fun onStart() {
        super.onStart()
        val lastBg = prefs.lastBackgroundTimestamp
        if (lastBg > 0L) {
            val timeoutMs = prefs.sessionTimeoutSeconds * 1000L
            val elapsed = SystemClock.elapsedRealtime() - lastBg
            if (elapsed >= timeoutMs) {
                vaultRepository.lock()
            }
        }
    }

    private fun updateScreenshotPolicy() {
        if (prefs.allowScreenshots) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE,
            )
        }
    }
}
