package com.vaultspec.authenticator.ui.screen.settings

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
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
                    icon = Icons.Default.Key,
                    iconColor = accent,
                    title = "Change Password",
                    subtitle = "Update your vault password",
                    onClick = viewModel::onShowChangePassword,
                )
                GroupDivider()
                SettingsToggleItem(
                    icon = Icons.Default.Fingerprint,
                    iconColor = extra.green,
                    title = "Biometric Unlock",
                    subtitle = "Use fingerprint or face to unlock",
                    checked = state.biometricEnabled,
                    onCheckedChange = viewModel::onBiometricToggle,
                )
                GroupDivider()
                SettingsItem(
                    icon = Icons.Default.Timer,
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

            // Backup & Restore section
            SectionHeader("Backup & Restore")
            GroupCard {
                SettingsItem(
                    icon = Icons.Default.Folder,
                    iconColor = accent,
                    title = "Backup Folder",
                    subtitle = if (state.backupFolderUri != null) "Folder selected" else "Tap to select backup folder",
                    value = if (state.backupFolderUri != null) "Set" else null,
                    onClick = { backupFolderPickerLauncher.launch(null) },
                )
                GroupDivider()
                SettingsItem(
                    icon = Icons.Default.Lock,
                    iconColor = extra.gray,
                    title = "Backup Password",
                    subtitle = if (state.hasBackupPassword) "Password set" else "Tap to set backup password",
                    value = if (state.hasBackupPassword) "Set" else null,
                    onClick = viewModel::onShowBackupPasswordDialog,
                )
                GroupDivider()
                SettingsItem(
                    icon = Icons.Default.Backup,
                    iconColor = extra.teal,
                    title = "Backup Now",
                    subtitle = "Save encrypted backup to selected folder",
                    onClick = { viewModel.onManualBackup(context) },
                )
                GroupDivider()
                SettingsToggleItem(
                    icon = Icons.Default.Sync,
                    iconColor = extra.green,
                    title = "Auto-backup",
                    subtitle = "Automatically backup when tokens change",
                    checked = state.autoBackupEnabled,
                    onCheckedChange = viewModel::onAutoBackupToggle,
                )
                GroupDivider()
                SettingsItem(
                    icon = Icons.Default.Restore,
                    iconColor = extra.indigo,
                    title = "Restore from Backup",
                    subtitle = "Import accounts from a backup file",
                    onClick = viewModel::onShowRestoreDialog,
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Appearance section
            SectionHeader("Appearance")
            GroupCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
                ) {
                    IconTile(icon = Icons.Default.DarkMode, color = extra.indigo)
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
                    icon = Icons.Default.Contrast,
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
                    icon = Icons.Default.Screenshot,
                    iconColor = extra.pink,
                    title = "Allow Screenshots",
                    subtitle = "Allow capturing screenshots of the app. Requires the app to be re-opened",
                    checked = state.allowScreenshots,
                    onCheckedChange = viewModel::onAllowScreenshotsToggle,
                )
                GroupDivider()
                SettingsToggleItem(
                    icon = Icons.Default.Visibility,
                    iconColor = extra.teal,
                    title = "Tap to Reveal Codes",
                    subtitle = "Codes are hidden until tapped (${state.revealTimeoutSeconds}s timeout)",
                    checked = state.tapToReveal,
                    onCheckedChange = viewModel::onTapToRevealToggle,
                )
                GroupDivider()
                SettingsToggleItem(
                    icon = Icons.Default.ContentCopy,
                    iconColor = accent,
                    title = "Tap to Copy Code",
                    subtitle = "Tap a token to copy code to clipboard",
                    checked = state.tapToCopy,
                    onCheckedChange = viewModel::onTapToCopyToggle,
                )
                GroupDivider()
                SettingsToggleItem(
                    icon = Icons.Default.Highlight,
                    iconColor = extra.orange,
                    title = "Highlight Token on Tap",
                    subtitle = "1st tap highlights, 2nd tap copies",
                    checked = state.highlightOnTap,
                    onCheckedChange = viewModel::onHighlightOnTapToggle,
                )
                GroupDivider()
                SettingsItem(
                    icon = Icons.Default.Schedule,
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
                    icon = Icons.Default.Info,
                    iconColor = extra.gray,
                    title = "VaultSpec Authenticator",
                    subtitle = "Local encrypted TOTP vault",
                    value = context.packageManager.getPackageInfo(context.packageName, 0).versionName,
                    onClick = null,
                )
                GroupDivider()
                SettingsItem(
                    icon = Icons.Default.Policy,
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
            title = "Set Backup Password",
            message = "This password will be used to encrypt your backups. You'll need it to restore.",
            confirmText = "Save",
            onConfirm = viewModel::onSaveBackupPassword,
            onDismiss = viewModel::onDismissBackupPasswordDialog,
        ) {
            VaultTextField(
                value = state.backupPassword,
                onValueChange = viewModel::onBackupPasswordChange,
                label = "Backup Password",
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
                icon = if (state.restoreFileUri != null) Icons.Default.CheckCircle else Icons.Default.FileOpen,
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
                color = MaterialTheme.colorScheme.onSurface,
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
    cycles: Boolean = false,
) {
    val extra = LocalVaultSpecColors.current
    SettingsRow(
        icon = icon,
        iconColor = iconColor,
        title = title,
        subtitle = subtitle,
        modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier,
    ) {
        if (value != null) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        if (onClick != null) {
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = if (cycles) Icons.Default.UnfoldMore else Icons.Default.ChevronRight,
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

private fun formatTimeout(seconds: Int): String = when (seconds) {
    0 -> "Immediately"
    60 -> "1 min"
    else -> "${seconds / 60} min"
}
