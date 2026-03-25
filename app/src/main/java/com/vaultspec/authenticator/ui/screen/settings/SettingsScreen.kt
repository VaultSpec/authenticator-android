package com.vaultspec.authenticator.ui.screen.settings

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // SAF folder picker for backup
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            viewModel.onBackup(context, it)
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
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Security section
            SectionHeader("Security")

            // Change password
            SettingsItem(
                icon = Icons.Default.Key,
                title = "Change Password",
                subtitle = "Update your vault password",
                onClick = viewModel::onShowChangePassword,
            )

            // Biometric toggle
            SettingsToggleItem(
                icon = Icons.Default.Fingerprint,
                title = "Biometric Unlock",
                subtitle = "Use fingerprint or face to unlock",
                checked = state.biometricEnabled,
                onCheckedChange = viewModel::onBiometricToggle,
            )

            // Session timeout
            SettingsItem(
                icon = Icons.Default.Timer,
                title = "Session Timeout",
                subtitle = "Lock after: ${formatTimeout(state.sessionTimeoutSeconds)}",
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

            HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))

            // Backup & Restore section
            SectionHeader("Backup & Restore")

            SettingsItem(
                icon = Icons.Default.Backup,
                title = "Create Backup",
                subtitle = "Export encrypted backup to a folder",
                onClick = viewModel::onShowBackupDialog,
            )

            SettingsItem(
                icon = Icons.Default.Restore,
                title = "Restore from Backup",
                subtitle = "Import accounts from a backup file",
                onClick = viewModel::onShowRestoreDialog,
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))

            // Appearance section
            SectionHeader("Appearance")

            SettingsToggleItem(
                icon = Icons.Default.DarkMode,
                title = "Dark Mode",
                subtitle = "Use dark theme for the app",
                checked = state.darkMode,
                onCheckedChange = viewModel::onDarkModeToggle,
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))

            // Behavior section
            SectionHeader("Behavior")

            SettingsToggleItem(
                icon = Icons.Default.Screenshot,
                title = "Allow Screenshots",
                subtitle = "Allow capturing screenshots of the app",
                checked = state.allowScreenshots,
                onCheckedChange = viewModel::onAllowScreenshotsToggle,
            )

            SettingsToggleItem(
                icon = Icons.Default.Visibility,
                title = "Tap to Reveal Codes",
                subtitle = "Codes are hidden until tapped (${state.revealTimeoutSeconds}s timeout)",
                checked = state.tapToReveal,
                onCheckedChange = viewModel::onTapToRevealToggle,
            )

            SettingsToggleItem(
                icon = Icons.Default.ContentCopy,
                title = "Tap to Copy Code",
                subtitle = "Tap a token to copy code to clipboard",
                checked = state.tapToCopy,
                onCheckedChange = viewModel::onTapToCopyToggle,
            )

            SettingsToggleItem(
                icon = Icons.Default.Highlight,
                title = "Highlight Token on Tap",
                subtitle = "1st tap highlights, 2nd tap copies",
                checked = state.highlightOnTap,
                onCheckedChange = viewModel::onHighlightOnTapToggle,
            )

            SettingsItem(
                icon = Icons.Default.Schedule,
                title = "Password Reminder",
                subtitle = "Require password every ${state.passwordReminderDays} days",
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

            HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))

            // About section
            SectionHeader("About")

            SettingsItem(
                icon = Icons.Default.Info,
                title = "VaultSpec Authenticator",
                subtitle = "Version 1.0.0 — Local encrypted TOTP vault",
                onClick = {},
            )
        }
    }

    // Change Password Dialog
    if (state.showChangePassword) {
        AlertDialog(
            onDismissRequest = viewModel::onDismissChangePassword,
            title = { Text("Change Password") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = state.newPassword,
                        onValueChange = viewModel::onNewPasswordChange,
                        label = { Text("New Password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = state.confirmNewPassword,
                        onValueChange = viewModel::onConfirmNewPasswordChange,
                        label = { Text("Confirm New Password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    state.error?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(onClick = viewModel::onChangePassword) { Text("Change") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::onDismissChangePassword) { Text("Cancel") }
            },
        )
    }

    // Backup Dialog
    if (state.showBackupDialog) {
        AlertDialog(
            onDismissRequest = viewModel::onDismissBackupDialog,
            title = { Text("Create Encrypted Backup") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Enter a password to encrypt the backup. You'll need this password to restore.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    OutlinedTextField(
                        value = state.backupPassword,
                        onValueChange = viewModel::onBackupPasswordChange,
                        label = { Text("Backup Password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    state.error?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { folderPickerLauncher.launch(null) },
                    enabled = !state.isLoading,
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Select Folder & Backup")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::onDismissBackupDialog) { Text("Cancel") }
            },
        )
    }

    // Restore Dialog
    if (state.showRestoreDialog) {
        AlertDialog(
            onDismissRequest = viewModel::onDismissRestoreDialog,
            title = { Text("Restore from Backup") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "This will replace all current accounts with the backup data.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                    OutlinedButton(
                        onClick = { filePickerLauncher.launch(arrayOf("*/*")) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Default.FileOpen, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (state.restoreFileUri != null) "File selected ✓"
                            else "Select backup file (.vsbk)"
                        )
                    }
                    OutlinedTextField(
                        value = state.restorePassword,
                        onValueChange = viewModel::onRestorePasswordChange,
                        label = { Text("Backup Password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    state.error?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.onRestore(context) },
                    enabled = !state.isLoading,
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Restore")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::onDismissRestoreDialog) { Text("Cancel") }
            },
        )
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
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
    )
}

@Composable
private fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp),
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SettingsToggleItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    Surface(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                modifier = Modifier.size(24.dp),
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                )
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
        }
    }
}

private fun formatTimeout(seconds: Int): String = when (seconds) {
    0 -> "Immediately"
    60 -> "1 minute"
    120 -> "2 minutes"
    300 -> "5 minutes"
    600 -> "10 minutes"
    900 -> "15 minutes"
    else -> "${seconds / 60} minutes"
}
