package com.vaultspec.authenticator.ui.screen.unlock

import androidx.biometric.BiometricPrompt
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import com.vaultspec.authenticator.R
import com.vaultspec.authenticator.ui.component.AppIcons
import com.vaultspec.authenticator.ui.component.ErrorText
import com.vaultspec.authenticator.ui.component.PrimaryButton
import com.vaultspec.authenticator.ui.component.TintedButton
import com.vaultspec.authenticator.ui.component.VaultTextField
import com.vaultspec.authenticator.ui.theme.VaultSpecDimens
import java.util.concurrent.Executors

@Composable
fun UnlockScreen(
    onUnlocked: () -> Unit,
    viewModel: UnlockViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(state.isUnlocked) {
        if (state.isUnlocked) onUnlocked()
    }

    // Auto-trigger biometric on first composition
    LaunchedEffect(state.biometricEnabled) {
        if (state.biometricEnabled) {
            viewModel.triggerBiometric()
        }
    }

    // Handle biometric prompt
    LaunchedEffect(state.showBiometricPrompt) {
        if (state.showBiometricPrompt) {
            viewModel.onBiometricPromptShown()
            val activity = context as? FragmentActivity ?: return@LaunchedEffect
            val cipher = viewModel.getDecryptCipher(context) ?: run {
                viewModel.onBiometricError("Biometric key unavailable")
                return@LaunchedEffect
            }
            showBiometricPrompt(activity, cipher, viewModel)
        }
    }

    // Entrance animation
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = VaultSpecDimens.ScreenPadding, vertical = 24.dp)
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(500)) + slideInVertically(tween(500)) { -40 },
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                    painter = painterResource(R.drawable.vaultspec_logo_mask),
                    contentDescription = null,
                    modifier = Modifier.size(96.dp),
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onBackground),
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Welcome Back",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = if (state.passwordReminderActive)
                        "Password required — periodic security check"
                    else
                        "Enter your password to unlock",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (state.passwordReminderActive)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }

        Spacer(modifier = Modifier.height(36.dp))

        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(500, delayMillis = 150)) + slideInVertically(tween(500, delayMillis = 150)) { 30 },
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                VaultTextField(
                    value = state.password,
                    onValueChange = viewModel::onPasswordChange,
                    label = "Password",
                    isPassword = true,
                )

                state.error?.let { error ->
                    Spacer(modifier = Modifier.height(8.dp))
                    ErrorText(error)
                }

                Spacer(modifier = Modifier.height(20.dp))

                PrimaryButton(
                    text = "Unlock",
                    onClick = viewModel::onUnlockWithPassword,
                    loading = state.isLoading,
                )

                if (state.biometricEnabled) {
                    Spacer(modifier = Modifier.height(12.dp))

                    TintedButton(
                        text = "Use Biometrics",
                        onClick = { viewModel.triggerBiometric() },
                        icon = AppIcons.Fingerprint,
                    )
                }
            }
        }
    }
}

private fun showBiometricPrompt(
    activity: FragmentActivity,
    cipher: javax.crypto.Cipher,
    viewModel: UnlockViewModel,
) {
    val executor = Executors.newSingleThreadExecutor()
    val callback = object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            val authedCipher = result.cryptoObject?.cipher ?: return
            viewModel.onBiometricSuccess(authedCipher)
        }

        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                viewModel.onBiometricError(errString.toString())
            }
        }
    }

    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle("Unlock VaultSpec")
        .setSubtitle("Use your biometrics to unlock")
        .setNegativeButtonText("Use Password")
        .build()

    val biometricPrompt = BiometricPrompt(activity, executor, callback)
    biometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
}
