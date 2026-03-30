package com.vaultspec.authenticator.ui.screen.unlock

import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
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

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
    ) {
        Image(
            painter = painterResource(R.mipmap.ic_launcher_foreground),
            contentDescription = null,
            modifier = Modifier.size(100.dp),
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Welcome Back",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(modifier = Modifier.height(8.dp))

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

        Spacer(modifier = Modifier.height(32.dp))

        // Password field
        var passwordVisible by remember { mutableStateOf(false) }
        OutlinedTextField(
            value = state.password,
            onValueChange = viewModel::onPasswordChange,
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None
                else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.VisibilityOff
                            else Icons.Default.Visibility,
                        contentDescription = "Toggle visibility",
                    )
                }
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
        )

        state.error?.let { error ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = viewModel::onUnlockWithPassword,
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
                Text("Unlock", fontWeight = FontWeight.SemiBold)
            }
        }

        if (state.biometricEnabled) {
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = { viewModel.triggerBiometric() },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                Icon(Icons.Default.Fingerprint, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Use Biometrics", fontWeight = FontWeight.SemiBold)
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
