package com.vaultspec.authenticator.backup

import android.content.Context
import android.net.Uri
import com.vaultspec.authenticator.data.prefs.AppPreferencesManager
import com.vaultspec.authenticator.data.repository.VaultRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Runs the automatic local and cloud backups on the schedule the user picked.
 *
 * A backup needs the unlocked vault key, so it cannot run while the app is closed. Instead,
 * [runDueBackups] is called whenever the vault is opened and backs up anything whose period has
 * elapsed since its last successful backup.
 */
@Singleton
class AutoBackupScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: AppPreferencesManager,
    private val vaultRepository: VaultRepository,
    private val backupManager: BackupManager,
    private val cloudBackupManager: CloudBackupManager,
    private val notifier: BackupNotifier,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val running = Mutex()

    /** Starts any backups that are due. Returns immediately; failures are retried on the next call. */
    fun runDueBackups() {
        scope.launch {
            if (!running.tryLock()) return@launch
            try {
                runLocalIfDue()
                runCloudIfDue()
            } finally {
                running.unlock()
            }
        }
    }

    private suspend fun runLocalIfDue() {
        if (!prefs.autoBackupEnabled) return
        val folderUri = prefs.backupFolderUri ?: return
        val password = prefs.backupPassword ?: return
        val masterKey = vaultRepository.masterKey ?: return
        if (!isDue(prefs.localLastBackupAt, prefs.localBackupPeriodDays)) return
        try {
            backupManager.createBackup(context, Uri.parse(folderUri), password, masterKey)
            notifier.localBackupDone()
        } catch (_: Exception) {
            // Left as due; tried again the next time the vault is opened
        }
    }

    private suspend fun runCloudIfDue() {
        if (!cloudBackupManager.isEnabled) return
        if (!prefs.cloudBackupPending && !isDue(prefs.cloudLastBackupAt, prefs.cloudBackupPeriodDays)) return
        try {
            cloudBackupManager.backupNow()
            notifier.cloudBackupDone()
        } catch (_: Exception) {
            // backupNow() marked it pending; tried again the next time the vault is opened
        }
    }

    private fun isDue(lastBackupAt: Long, periodDays: Int): Boolean {
        val elapsed = System.currentTimeMillis() - lastBackupAt
        // A clock set backwards makes elapsed negative: treat that as due rather than waiting
        return lastBackupAt == 0L || elapsed < 0 || elapsed >= TimeUnit.DAYS.toMillis(periodDays.toLong())
    }

    companion object {
        /** Selectable backup periods, in days. */
        val PERIODS = listOf(1, 7, 14, 30)

        fun nextPeriod(current: Int): Int = PERIODS[(PERIODS.indexOf(current) + 1) % PERIODS.size]

        fun periodLabel(days: Int): String = if (days == 1) "Every day" else "Every $days days"
    }
}
