package com.vaultspec.authenticator.ui.screen.setup

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vaultspec.authenticator.R
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(
    onSetupComplete: () -> Unit,
    viewModel: SetupViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

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

    LaunchedEffect(state.isSetupComplete) {
        if (state.isSetupComplete) onSetupComplete()
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Spacer(modifier = Modifier.height(60.dp))

        // VaultSpec logo
        Image(
            painter = painterResource(R.mipmap.ic_launcher_foreground),
            contentDescription = null,
            modifier = Modifier.size(120.dp),
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "VaultSpec",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Secure your accounts with\nencrypted 2FA codes",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(40.dp))

        // Password field
        var passwordVisible by remember { mutableStateOf(false) }
        OutlinedTextField(
            value = state.password,
            onValueChange = viewModel::onPasswordChange,
            label = { Text("Create Password") },
            placeholder = { Text("Minimum 6 characters") },
            singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None
                else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.VisibilityOff
                            else Icons.Default.Visibility,
                        contentDescription = "Toggle password visibility",
                    )
                }
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Confirm password
        var confirmVisible by remember { mutableStateOf(false) }
        OutlinedTextField(
            value = state.confirmPassword,
            onValueChange = viewModel::onConfirmPasswordChange,
            label = { Text("Confirm Password") },
            singleLine = true,
            visualTransformation = if (confirmVisible) VisualTransformation.None
                else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { confirmVisible = !confirmVisible }) {
                    Icon(
                        imageVector = if (confirmVisible) Icons.Default.VisibilityOff
                            else Icons.Default.Visibility,
                        contentDescription = "Toggle confirm visibility",
                    )
                }
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Biometric toggle
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Fingerprint,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Enable Biometric Unlock",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = state.enableBiometric,
                onCheckedChange = viewModel::onBiometricToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    checkedBorderColor = MaterialTheme.colorScheme.primary,
                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                    uncheckedBorderColor = MaterialTheme.colorScheme.outline,
                ),
            )
        }

        // Error
        state.error?.let { error ->
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Setup button
        Button(
            onClick = viewModel::onSetup,
            enabled = !state.isLoading,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp,
                )
            } else {
                Text("Create Vault", fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Divider
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f))
            Text(
                text = "  OR  ",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            HorizontalDivider(modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Restore button
        OutlinedButton(
            onClick = viewModel::onShowRestore,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        ) {
            Icon(Icons.Default.Restore, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Restore from Backup", fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(40.dp))
    }

    // Restore dialog
    if (state.showRestoreDialog) {
        RestoreDialog(
            state = state,
            onDismiss = viewModel::onDismissRestore,
            onPickFile = { filePickerLauncher.launch(arrayOf("*/*")) },
            onPasswordChange = viewModel::onRestorePasswordChange,
            onNewPasswordChange = viewModel::onRestoreNewPasswordChange,
            onConfirmPasswordChange = viewModel::onRestoreConfirmPasswordChange,
            onBiometricToggle = viewModel::onRestoreBiometricToggle,
            onRestore = { viewModel.onRestore(context) },
        )
    }

    // Biometric enrollment via BiometricPrompt
    LaunchedEffect(state.showBiometricPrompt, state.biometricEnrollCipher) {
        if (state.showBiometricPrompt && state.biometricEnrollCipher != null) {
            val activity = context as? FragmentActivity ?: return@LaunchedEffect
            val cipher = state.biometricEnrollCipher ?: return@LaunchedEffect
            val executor = ContextCompat.getMainExecutor(context)
            val prompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    result.cryptoObject?.cipher?.let { viewModel.onBiometricEnrollSuccess(it) }
                        ?: viewModel.onBiometricEnrollFailed()
                }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    viewModel.onBiometricEnrollFailed()
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
private fun RestoreDialog(
    state: SetupUiState,
    onDismiss: () -> Unit,
    onPickFile: () -> Unit,
    onPasswordChange: (String) -> Unit,
    onNewPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onBiometricToggle: (Boolean) -> Unit,
    onRestore: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Restore from Backup") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // File selection
                OutlinedButton(
                    onClick = onPickFile,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.FileOpen, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (state.restoreFileUri != null) "File selected ✓"
                        else "Select backup file (.vsbk)"
                    )
                }

                // Backup password
                OutlinedTextField(
                    value = state.restorePassword,
                    onValueChange = onPasswordChange,
                    label = { Text("Backup Password") },
                    placeholder = { Text("Password used when backing up") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                )

                HorizontalDivider()

                Text(
                    "Set up your new vault:",
                    style = MaterialTheme.typography.labelLarge,
                )

                // New password
                OutlinedTextField(
                    value = state.restoreNewPassword,
                    onValueChange = onNewPasswordChange,
                    label = { Text("New Vault Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = state.restoreConfirmPassword,
                    onValueChange = onConfirmPasswordChange,
                    label = { Text("Confirm New Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Enable Biometrics", modifier = Modifier.weight(1f))
                    Switch(
                        checked = state.restoreEnableBiometric,
                        onCheckedChange = onBiometricToggle,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                            checkedBorderColor = MaterialTheme.colorScheme.primary,
                            uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                            uncheckedBorderColor = MaterialTheme.colorScheme.outline,
                        ),
                    )
                }

                state.restoreError?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onRestore,
                enabled = !state.isLoading,
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text("Restore")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
