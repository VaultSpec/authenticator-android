package com.vaultspec.authenticator.ui.screen.setup

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import com.vaultspec.authenticator.R
import com.vaultspec.authenticator.ui.component.AppIcons
import com.vaultspec.authenticator.ui.component.ErrorText
import com.vaultspec.authenticator.ui.component.GroupCard
import com.vaultspec.authenticator.ui.component.IconTile
import com.vaultspec.authenticator.ui.component.PlainButton
import com.vaultspec.authenticator.ui.component.PrimaryButton
import com.vaultspec.authenticator.ui.component.SectionHeader
import com.vaultspec.authenticator.ui.component.TintedButton
import com.vaultspec.authenticator.ui.component.VaultDialog
import com.vaultspec.authenticator.ui.component.VaultSwitch
import com.vaultspec.authenticator.ui.component.VaultTextField
import com.vaultspec.authenticator.ui.theme.LocalVaultSpecColors
import com.vaultspec.authenticator.ui.theme.VaultSpecDimens

@Composable
fun SetupScreen(
    onSetupComplete: () -> Unit,
    onCloudSignIn: () -> Unit,
    onConnectDevice: () -> Unit,
    cloudSignedIn: Boolean,
    onCloudSignedInHandled: () -> Unit,
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

    // Back from the account screen after signing in: continue the cloud restore
    LaunchedEffect(cloudSignedIn) {
        if (cloudSignedIn) {
            onCloudSignedInHandled()
            viewModel.onShowCloudRestore()
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = VaultSpecDimens.ScreenPadding, vertical = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        // VaultSpec logo
        Image(
            painter = painterResource(R.drawable.vaultspec_logo_mask),
            contentDescription = null,
            modifier = Modifier.size(88.dp),
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onBackground),
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Create Your Vault",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Secure your accounts with\nencrypted 2FA codes",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(32.dp))

        VaultTextField(
            value = state.password,
            onValueChange = viewModel::onPasswordChange,
            label = "Create Password",
            placeholder = "Minimum 6 characters",
            isPassword = true,
        )

        Spacer(modifier = Modifier.height(10.dp))

        VaultTextField(
            value = state.confirmPassword,
            onValueChange = viewModel::onConfirmPasswordChange,
            label = "Confirm Password",
            isPassword = true,
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Biometric toggle
        GroupCard {
            BiometricRow(
                title = "Biometric Unlock",
                checked = state.enableBiometric,
                onCheckedChange = viewModel::onBiometricToggle,
            )
        }

        // Error
        state.error?.let { error ->
            Spacer(modifier = Modifier.height(12.dp))
            ErrorText(error)
        }

        Spacer(modifier = Modifier.height(24.dp))

        PrimaryButton(
            text = "Create Vault",
            onClick = viewModel::onSetup,
            loading = state.isLoading,
        )

        Spacer(modifier = Modifier.height(8.dp))

        PlainButton(
            text = "Restore from Backup File",
            onClick = viewModel::onShowRestore,
        )

        PlainButton(
            text = "Restore from VaultSpec Cloud",
            onClick = {
                if (viewModel.isCloudSignedIn) viewModel.onShowCloudRestore() else onCloudSignIn()
            },
        )

        PlainButton(
            text = "Use Another Device",
            onClick = onConnectDevice,
        )

        Spacer(modifier = Modifier.height(24.dp))
    }

    // Cloud restore dialog
    if (state.showCloudRestoreDialog) {
        VaultDialog(
            title = "Restore from Cloud",
            message = "Enter the vault password your cloud backup was made with. It becomes the " +
                "password for this vault.",
            confirmText = "Restore",
            loading = state.isLoading,
            onConfirm = viewModel::onCloudRestore,
            onDismiss = viewModel::onDismissCloudRestore,
        ) {
            VaultTextField(
                value = state.cloudVaultPassword,
                onValueChange = viewModel::onCloudVaultPasswordChange,
                label = "Vault Password",
                isPassword = true,
                onSurface = true,
            )
            state.cloudRestoreError?.let { ErrorText(it) }
        }
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
private fun BiometricRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        IconTile(icon = AppIcons.Fingerprint, color = LocalVaultSpecColors.current.green)
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        VaultSwitch(checked = checked, onCheckedChange = onCheckedChange)
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
    VaultDialog(
        title = "Restore from Backup",
        confirmText = "Restore",
        loading = state.isLoading,
        onConfirm = onRestore,
        onDismiss = onDismiss,
    ) {
        TintedButton(
            text = if (state.restoreFileUri != null) "File Selected" else "Select Backup File",
            icon = if (state.restoreFileUri != null) AppIcons.CircleCheck else AppIcons.FileUp,
            onClick = onPickFile,
        )

        VaultTextField(
            value = state.restorePassword,
            onValueChange = onPasswordChange,
            label = "Backup Password",
            placeholder = "Password used when backing up",
            isPassword = true,
            onSurface = true,
        )

        Spacer(modifier = Modifier.height(4.dp))

        SectionHeader("Set up your new vault", modifier = Modifier.padding(bottom = 0.dp))

        VaultTextField(
            value = state.restoreNewPassword,
            onValueChange = onNewPasswordChange,
            label = "New Vault Password",
            isPassword = true,
            onSurface = true,
        )

        VaultTextField(
            value = state.restoreConfirmPassword,
            onValueChange = onConfirmPasswordChange,
            label = "Confirm New Password",
            isPassword = true,
            onSurface = true,
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 4.dp),
        ) {
            IconTile(icon = AppIcons.Fingerprint, color = LocalVaultSpecColors.current.green)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Enable Biometrics",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            VaultSwitch(
                checked = state.restoreEnableBiometric,
                onCheckedChange = onBiometricToggle,
            )
        }

        state.restoreError?.let { ErrorText(it) }
    }
}
