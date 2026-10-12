package com.vaultspec.authenticator.backup

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.vaultspec.authenticator.MainActivity
import com.vaultspec.authenticator.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Posts a notification when an automatic backup finishes. Silent if the user hasn't allowed notifications. */
@Singleton
class BackupNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun localBackupDone() = notify(ID_LOCAL, "Local backup complete", "Your accounts were saved to your backup folder.")

    fun cloudBackupDone() = notify(ID_CLOUD, "Cloud backup complete", "Your encrypted backup is stored in your VaultSpec account.")

    private fun notify(id: Int, title: String, text: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return

        context.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Backups", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Confirms when an automatic backup has finished"
            }
        )

        val openApp = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.vaultspec_logo_mask)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .build()
        try {
            manager.notify(id, notification)
        } catch (_: SecurityException) {
            // Permission revoked between the check and the post
        }
    }

    private companion object {
        const val CHANNEL_ID = "backups"
        const val ID_LOCAL = 1001
        const val ID_CLOUD = 1002
    }
}
