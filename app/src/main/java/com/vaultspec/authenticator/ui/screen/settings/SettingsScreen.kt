package com.vaultspec.authenticator.ui.screen.settings

import android.content.Intent
import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.text.format.DateUtils
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import com.vaultspec.authenticator.backup.AutoBackupScheduler
import com.vaultspec.authenticator.ui.component.AppIcons
import com.vaultspec.authenticator.ui.component.ErrorText
import com.vaultspec.authenticator.ui.component.GroupCard
import com.vaultspec.authenticator.ui.component.GroupDivider
import com.vaultspec.authenticator.ui.component.IconTile
import com.vaultspec.authenticator.ui.component.LargeTitle
import com.vaultspec.authenticator.ui.component.NavBar
import com.vaultspec.authenticator.ui.component.SectionHeader
import com.vaultspec.authenticator.ui.component.SegmentedControl
import com.vaultspec.authenticator.ui.component.TintedButton
import com.vaultspec.authenticator.ui.component.VaultDialog
import com.vaultspec.authenticator.ui.component.VaultSwitch
import com.vaultspec.authenticator.ui.component.VaultTextField
import com.vaultspec.authenticator.ui.theme.LocalVaultSpecColors
import com.vaultspec.authenticator.ui.theme.VaultSpecDimens

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onOpenAccount: () -> Unit,
    onConnectDevice: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val extra = LocalVaultSpecColors.current
    val accent = MaterialTheme.colorScheme.primary
    val snackbarHostState = remember { SnackbarHostState() }

    // SAF folder picker for backup folder selection
    val backupFolderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            viewModel.onBackupFolderSelected(it)
        }
    }

    // SAF file picker for restore
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it, Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            viewModel.onRestoreFileSelected(it)
        }
    }

    // Automatic backups confirm with a notification: ask for permission when one is switched on
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* Backups work either way; a denial only means no confirmation */ }
    val askForNotifications = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Account state changes on the sign-in screen: re-read it whenever this screen comes back
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshCloudState() }

    // Show snackbar for messages
    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = { NavBar(onBack = onNavigateBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = VaultSpecDimens.ScreenPadding)
        ) {
            LargeTitle(text = "Settings")

            Spacer(modifier = Modifier.height(24.dp))

            // Security section
            SectionHeader("Security")
            GroupCard {
                SettingsItem(
                    icon = AppIcons.Key,
                    iconColor = accent,
                    title = "Change Password",
                    subtitle = "Update your vault password",
                    onClick = viewModel::onShowChangePassword,
                )
                GroupDivider()
                SettingsToggleItem(
                    icon = AppIcons.Fingerprint,
                    iconColor = extra.green,
                    title = "Biometric Unlock",
                    subtitle = "Use fingerprint or face to unlock",
                    checked = state.biometricEnabled,
                    onCheckedChange = viewModel::onBiometricToggle,
                )
                GroupDivider()
                SettingsItem(
                    icon = AppIcons.Timer,
                    iconColor = extra.orange,
                    title = "Session Timeout",
                    subtitle = "Lock after leaving the app",
                    value = formatTimeout(state.sessionTimeoutSeconds),
                    cycles = true,
                    onClick = {
                        val next = when (state.sessionTimeoutSeconds) {
                            0 -> 120
                            120 -> 300
                            300 -> 600
                            600 -> 900
                            else -> 0
                        }
                        viewModel.onSessionTimeoutChange(next)
                    },
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Local Backup section
            SectionHeader("Local Backup")
            GroupCard {
                SettingsItem(
                    icon = AppIcons.Folder,
                    iconColor = accent,
                    title = "Backup Folder",
                    subtitle = if (state.backupFolderUri != null) "Folder selected" else "Tap to select backup folder",
                    action = if (state.backupFolderUri != null) "Change" else "Set",
                    onClick = { backupFolderPickerLauncher.launch(null) },
                )
                GroupDivider()
                SettingsItem(
                    icon = AppIcons.LockKeyhole,
                    iconColor = extra.gray,
                    title = "Backup Password",
                    subtitle = if (state.hasBackupPassword) "Password set" else "Tap to set backup password",
                    action = if (state.hasBackupPassword) "Change" else "Set",
                    onClick = viewModel::onShowBackupPasswordDialog,
                )
                GroupDivider()
                SettingsItem(
                    icon = AppIcons.CloudUpload,
                    iconColor = extra.teal,
                    title = "Back Up Now",
                    subtitle = if (state.localLastBackupAt == 0L) "Save encrypted backup to selected folder"
                        else lastBackupLabel(state.localLastBackupAt),
                    onClick = { viewModel.onManualBackup(context) },
                )
                GroupDivider()
                SettingsToggleItem(
                    icon = AppIcons.RefreshCw,
                    iconColor = extra.green,
                    title = "Auto-backup",
                    subtitle = "Back up to the folder on a schedule",
                    checked = state.autoBackupEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled) askForNotifications()
                        viewModel.onAutoBackupToggle(enabled)
                    },
                )
                GroupDivider()
                SettingsItem(
                    icon = AppIcons.CalendarClock,
                    iconColor = extra.orange,
                    title = "Frequency",
                    subtitle = "Runs when you open VaultSpec",
                    value = AutoBackupScheduler.periodLabel(state.localBackupPeriodDays),
                    cycles = true,
                    enabled = state.autoBackupEnabled,
                    onClick = viewModel::onLocalBackupPeriodCycle,
                )
                GroupDivider()
                SettingsItem(
                    icon = AppIcons.History,
                    iconColor = extra.indigo,
                    title = "Restore from Backup",
                    subtitle = "Import accounts from a backup file",
                    onClick = viewModel::onShowRestoreDialog,
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Cloud Backup section
            SectionHeader("Cloud Backup")
            GroupCard {
                val email = state.cloudEmail
                if (email == null) {
                    SettingsItem(
                        icon = AppIcons.Cloud,
                        iconColor = accent,
                        title = "VaultSpec Account",
                        subtitle = "Sign in to keep encrypted backups in the cloud",
                        action = "Sign In",
                        onClick = onOpenAccount,
                    )
                } else {
                    SettingsItem(
                        icon = AppIcons.User,
                        iconColor = accent,
                        title = email,
                        subtitle = "VaultSpec account",
                        onClick = null,
                    )
                    GroupDivider()
                    SettingsToggleItem(
                        icon = AppIcons.Cloud,
                        iconColor = extra.green,
                        title = "Auto-backup",
                        subtitle = "Encrypted on this device with your vault password",
                        checked = state.cloudBackupEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled) askForNotifications()
                            viewModel.onCloudBackupToggle(enabled)
                        },
                        enabled = !state.cloudBusy,
                    )
                    GroupDivider()
                    SettingsItem(
                        icon = AppIcons.CalendarClock,
                        iconColor = extra.orange,
                        title = "Frequency",
                        subtitle = "Runs when you open VaultSpec",
                        value = AutoBackupScheduler.periodLabel(state.cloudBackupPeriodDays),
                        cycles = true,
                        enabled = state.cloudBackupEnabled,
                        onClick = viewModel::onCloudBackupPeriodCycle,
                    )
                    GroupDivider()
                    SettingsItem(
                        icon = AppIcons.CloudUpload,
                        iconColor = extra.teal,
                        title = "Back Up Now",
                        subtitle = when {
                            !state.cloudBackupEnabled -> "Turn on Auto-backup first"
                            state.cloudBusy -> "Working…"
                            state.cloudLastBackupAt == 0L -> "No backup yet"
                            else -> lastBackupLabel(state.cloudLastBackupAt)
                        },
                        enabled = state.cloudBackupEnabled && !state.cloudBusy,
                        onClick = viewModel::onCloudBackupNow,
                    )
                    GroupDivider()
                    SettingsItem(
                        icon = AppIcons.Smartphone,
                        iconColor = extra.indigo,
                        title = "Connect a Device",
                        subtitle = "Show a code to set up a new phone or iPhone",
                        onClick = onConnectDevice,
                    )
                    GroupDivider()
                    SettingsItem(
                        icon = AppIcons.CloudDownload,
                        iconColor = extra.indigo,
                        title = "Restore from Cloud",
                        subtitle = "Replace accounts with your latest cloud backup",
                        onClick = { viewModel.onShowCloudDialog(CloudDialog.Restore) },
                    )
                    GroupDivider()
                    SettingsItem(
                        icon = AppIcons.LogOut,
                        iconColor = extra.gray,
                        title = "Sign Out",
                        subtitle = "Cloud backups stay in your account",
                        onClick = viewModel::onCloudSignOut,
                    )
                    GroupDivider()
                    SettingsItem(
                        icon = AppIcons.UserX,
                        iconColor = extra.red,
                        title = "Delete Account",
                        subtitle = "Remove your account and all cloud backups",
                        titleColor = extra.red,
                        onClick = { viewModel.onShowCloudDialog(CloudDialog.DeleteAccount) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Appearance section
            SectionHeader("Appearance")
            GroupCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
                ) {
                    IconTile(icon = AppIcons.Moon, color = extra.indigo)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Theme", style = MaterialTheme.typography.bodyLarge)
                }
                SegmentedControl(
                    options = listOf("system" to "System", "light" to "Light", "dark" to "Dark"),
                    selected = state.themeMode,
                    onSelect = viewModel::onThemeModeChange,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
                GroupDivider()
                SettingsToggleItem(
                    icon = AppIcons.Contrast,
                    iconColor = extra.gray,
                    title = "Pitch Black",
                    subtitle = "AMOLED-friendly pure black theme",
                    checked = state.pitchBlack,
                    onCheckedChange = viewModel::onPitchBlackToggle,
                    enabled = state.themeMode == "dark",
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Behavior section
            SectionHeader("Behavior")
            GroupCard {
                SettingsToggleItem(
                    icon = AppIcons.Smartphone,
                    iconColor = extra.pink,
                    title = "Allow Screenshots",
                    subtitle = "Allow capturing screenshots of the app. Requires the app to be re-opened",
                    checked = state.allowScreenshots,
                    onCheckedChange = viewModel::onAllowScreenshotsToggle,
                )
                GroupDivider()
                SettingsToggleItem(
                    icon = AppIcons.Eye,
                    iconColor = extra.teal,
                    title = "Tap to Reveal Codes",
                    subtitle = "Codes are hidden until tapped (${state.revealTimeoutSeconds}s timeout)",
                    checked = state.tapToReveal,
                    onCheckedChange = viewModel::onTapToRevealToggle,
                )
                GroupDivider()
                SettingsToggleItem(
                    icon = AppIcons.Copy,
                    iconColor = accent,
                    title = "Tap to Copy Code",
                    subtitle = "Tap a token to copy code to clipboard",
                    checked = state.tapToCopy,
                    onCheckedChange = viewModel::onTapToCopyToggle,
                )
                GroupDivider()
                SettingsToggleItem(
                    icon = AppIcons.Highlighter,
                    iconColor = extra.orange,
                    title = "Highlight Token on Tap",
                    subtitle = "1st tap highlights, 2nd tap copies",
                    checked = state.highlightOnTap,
                    onCheckedChange = viewModel::onHighlightOnTapToggle,
                )
                GroupDivider()
                SettingsItem(
                    icon = AppIcons.CalendarClock,
                    iconColor = extra.red,
                    title = "Password Reminder",
                    subtitle = "Ask for the password periodically",
                    value = "${state.passwordReminderDays} days",
                    cycles = true,
                    onClick = {
                        // Cycle between 7, 15, 30
                        val next = when (state.passwordReminderDays) {
                            7 -> 15
                            15 -> 30
                            else -> 7
                        }
                        viewModel.onPasswordReminderDaysChange(next)
                    },
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // About section
            SectionHeader("About")
            GroupCard {
                SettingsItem(
                    icon = AppIcons.Info,
                    iconColor = extra.gray,
                    title = "VaultSpec Authenticator",
                    subtitle = "Local encrypted TOTP vault",
                    value = context.packageManager.getPackageInfo(context.packageName, 0).versionName,
                    onClick = null,
                )
                GroupDivider()
                SettingsItem(
                    icon = AppIcons.ShieldCheck,
                    iconColor = accent,
                    title = "Privacy Policy",
                    subtitle = "View our privacy policy",
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.vaultspec.in/privacy"))
                        context.startActivity(intent)
                    },
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Change Password Dialog
    if (state.showChangePassword) {
        VaultDialog(
            title = "Change Password",
            confirmText = "Change",
            onConfirm = viewModel::onChangePassword,
            onDismiss = viewModel::onDismissChangePassword,
        ) {
            VaultTextField(
                value = state.newPassword,
                onValueChange = viewModel::onNewPasswordChange,
                label = "New Password",
                isPassword = true,
                onSurface = true,
            )
            VaultTextField(
                value = state.confirmNewPassword,
                onValueChange = viewModel::onConfirmNewPasswordChange,
                label = "Confirm New Password",
                isPassword = true,
                onSurface = true,
            )
            state.error?.let { ErrorText(it) }
        }
    }

    // Backup Password Dialog
    if (state.showBackupPasswordDialog) {
        VaultDialog(
            title = if (state.hasBackupPassword) "Change Backup Password" else "Set Backup Password",
            message = if (state.hasBackupPassword)
                "New backups will be encrypted with this password. Older backups still need the previous one."
            else
                "This password will be used to encrypt your backups. You'll need it to restore.",
            confirmText = "Save",
            onConfirm = viewModel::onSaveBackupPassword,
            onDismiss = viewModel::onDismissBackupPasswordDialog,
        ) {
            VaultTextField(
                value = state.backupPassword,
                onValueChange = viewModel::onBackupPasswordChange,
                label = if (state.hasBackupPassword) "New Backup Password" else "Backup Password",
                isPassword = true,
                onSurface = true,
            )
            state.error?.let { ErrorText(it) }
        }
    }

    // Restore Dialog
    if (state.showRestoreDialog) {
        VaultDialog(
            title = "Restore from Backup",
            message = "This will replace all current accounts with the backup data.",
            confirmText = "Restore",
            destructive = true,
            loading = state.isLoading,
            onConfirm = { viewModel.onRestore(context) },
            onDismiss = viewModel::onDismissRestoreDialog,
        ) {
            TintedButton(
                text = if (state.restoreFileUri != null) "File Selected" else "Select Backup File",
                icon = if (state.restoreFileUri != null) AppIcons.CircleCheck else AppIcons.FileUp,
                onClick = { filePickerLauncher.launch(arrayOf("*/*")) },
            )
            VaultTextField(
                value = state.restorePassword,
                onValueChange = viewModel::onRestorePasswordChange,
                label = "Backup Password",
                isPassword = true,
                onSurface = true,
            )
            state.error?.let { ErrorText(it) }
        }
    }

    // Cloud dialogs
    when (state.cloudDialog) {
        CloudDialog.Enable -> VaultDialog(
            title = "Turn On Cloud Backup",
            message = "Enter your vault password. Backups are encrypted with it on this device, " +
                "and you'll need it to restore them.",
            confirmText = "Turn On",
            loading = state.cloudBusy,
            onConfirm = { viewModel.onCloudEnable() },
            onDismiss = viewModel::onDismissCloudDialog,
        ) {
            VaultTextField(
                value = state.cloudPassword,
                onValueChange = viewModel::onCloudPasswordChange,
                label = "Vault Password",
                isPassword = true,
                onSurface = true,
            )
            state.error?.let { ErrorText(it) }
        }

        CloudDialog.ConfirmReplace -> VaultDialog(
            title = "Replace Cloud Backup?",
            message = "This account already has a backup from another vault. Backing up from this " +
                "device deletes it. To keep those accounts, cancel and use Restore from Cloud first.",
            icon = AppIcons.CloudUpload,
            destructive = true,
            confirmText = "Replace",
            loading = state.cloudBusy,
            onConfirm = { viewModel.onCloudEnable(replaceExisting = true) },
            onDismiss = viewModel::onDismissCloudDialog,
        ) {
            state.error?.let { ErrorText(it) }
        }

        CloudDialog.Restore -> VaultDialog(
            title = "Restore from Cloud",
            message = "This replaces all accounts on this device with your latest cloud backup. " +
                "Enter the vault password the backup was made with.",
            destructive = true,
            confirmText = "Restore",
            loading = state.cloudBusy,
            onConfirm = viewModel::onCloudRestore,
            onDismiss = viewModel::onDismissCloudDialog,
        ) {
            VaultTextField(
                value = state.cloudPassword,
                onValueChange = viewModel::onCloudPasswordChange,
                label = "Vault Password",
                isPassword = true,
                onSurface = true,
            )
            state.error?.let { ErrorText(it) }
        }

        CloudDialog.DeleteAccount -> VaultDialog(
            title = "Delete Account?",
            message = "Your VaultSpec account and every cloud backup will be permanently deleted. " +
                "Accounts on this device are not affected.",
            icon = AppIcons.UserX,
            destructive = true,
            confirmText = "Delete",
            loading = state.cloudBusy,
            onConfirm = viewModel::onCloudDeleteAccount,
            onDismiss = viewModel::onDismissCloudDialog,
        ) {
            VaultTextField(
                value = state.cloudPassword,
                onValueChange = viewModel::onCloudPasswordChange,
                label = "Account Password",
                isPassword = true,
                onSurface = true,
            )
            state.error?.let { ErrorText(it) }
        }

        null -> Unit
    }

    // Biometric enrollment via BiometricPrompt
    LaunchedEffect(state.showBiometricPrompt, state.biometricEnrollCipher) {
        if (state.showBiometricPrompt && state.biometricEnrollCipher != null) {
            viewModel.onBiometricPromptShown()
            val activity = context as? FragmentActivity ?: return@LaunchedEffect
            val cipher = state.biometricEnrollCipher ?: return@LaunchedEffect
            val executor = ContextCompat.getMainExecutor(context)
            val prompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    result.cryptoObject?.cipher?.let { viewModel.onBiometricEnrollSuccess(it) }
                        ?: viewModel.onBiometricEnrollError("Authentication succeeded but no cipher returned")
                }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    viewModel.onBiometricEnrollError(errString.toString())
                }
                override fun onAuthenticationFailed() {
                    // Prompt stays open for retry
                }
            })
            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Enable Biometric Unlock")
                .setSubtitle("Authenticate to enable biometric unlock")
                .setNegativeButtonText("Cancel")
                .build()
            prompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
        }
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    trailing: @Composable RowScope.() -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        IconTile(
            icon = icon,
            color = iconColor,
            modifier = Modifier.alpha(if (enabled) 1f else 0.4f),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .alpha(if (enabled) 1f else 0.4f),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = titleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        trailing()
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)?,
    value: String? = null,
    action: String? = null,
    cycles: Boolean = false,
    enabled: Boolean = true,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    val extra = LocalVaultSpecColors.current
    SettingsRow(
        icon = icon,
        iconColor = iconColor,
        title = title,
        subtitle = subtitle,
        enabled = enabled,
        titleColor = titleColor,
        modifier = if (onClick != null) Modifier.clickable(enabled = enabled, onClick = onClick) else Modifier,
    ) {
        if (value != null) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        if (action != null) {
            Text(
                text = action,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
            )
        }
        if (onClick != null) {
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = if (cycles) AppIcons.ChevronsUpDown else AppIcons.ChevronRight,
                contentDescription = null,
                tint = extra.tertiaryLabel,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun SettingsToggleItem(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    SettingsRow(
        icon = icon,
        iconColor = iconColor,
        title = title,
        subtitle = subtitle,
        enabled = enabled,
    ) {
        VaultSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
        )
    }
}

private fun lastBackupLabel(timestamp: Long): String {
    val now = System.currentTimeMillis()
    if (now - timestamp < DateUtils.MINUTE_IN_MILLIS) return "Last backup just now"
    val relative = DateUtils.getRelativeTimeSpanString(timestamp, now, DateUtils.MINUTE_IN_MILLIS)
    return "Last backup " + relative.toString().replaceFirstChar { it.lowercase() }
}

private fun formatTimeout(seconds: Int): String = when (seconds) {
    0 -> "Immediately"
    60 -> "1 min"
    else -> "${seconds / 60} min"
}
