package com.vaultspec.authenticator

import android.os.Bundle
import android.os.SystemClock
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.fragment.app.FragmentActivity
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
        prefs.initDarkModeFlow()
        updateScreenshotPolicy()
        enableEdgeToEdge()

        setContent {
            val isDarkMode by prefs.darkModeFlow.collectAsState()
            VaultSpecTheme(darkTheme = isDarkMode) {
                NavGraph(vaultRepository = vaultRepository)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateScreenshotPolicy()
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
