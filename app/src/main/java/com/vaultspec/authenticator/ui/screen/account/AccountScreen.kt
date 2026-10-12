package com.vaultspec.authenticator.ui.screen.account

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vaultspec.authenticator.ui.component.AppIcons
import com.vaultspec.authenticator.ui.component.ErrorText
import com.vaultspec.authenticator.ui.component.HeroIcon
import com.vaultspec.authenticator.ui.component.LargeTitle
import com.vaultspec.authenticator.ui.component.NavBar
import com.vaultspec.authenticator.ui.component.PlainButton
import com.vaultspec.authenticator.ui.component.PrimaryButton
import com.vaultspec.authenticator.ui.component.TintedButton
import com.vaultspec.authenticator.ui.component.VaultTextField
import com.vaultspec.authenticator.ui.theme.VaultSpecDimens

@Composable
fun AccountScreen(
    onNavigateBack: () -> Unit,
    onSignedIn: () -> Unit,
    viewModel: AccountViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.isSignedIn) {
        if (state.isSignedIn) onSignedIn()
    }

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
                .consumeWindowInsets(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = VaultSpecDimens.ScreenPadding)
        ) {
            if (state.mode == AccountMode.EnterCode) {
                EnterCodeContent(state, viewModel)
            } else {
                CredentialsContent(state, viewModel)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun CredentialsContent(state: AccountUiState, viewModel: AccountViewModel) {
    val signUp = state.mode == AccountMode.SignUp

    LargeTitle(text = if (signUp) "Create Account" else "Sign In")

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = if (signUp)
            "A VaultSpec account keeps encrypted backups in the cloud so you can restore on another device."
        else
            "Sign in to your VaultSpec account to back up and restore.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    Spacer(modifier = Modifier.height(24.dp))

    VaultTextField(
        value = state.email,
        onValueChange = viewModel::onEmailChange,
        label = "Email",
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
    )

    Spacer(modifier = Modifier.height(10.dp))

    VaultTextField(
        value = state.password,
        onValueChange = viewModel::onPasswordChange,
        label = "Account Password",
        placeholder = if (signUp) "Minimum 8 characters" else null,
        isPassword = true,
    )

    if (signUp) {
        Spacer(modifier = Modifier.height(10.dp))

        VaultTextField(
            value = state.confirmPassword,
            onValueChange = viewModel::onConfirmPasswordChange,
            label = "Confirm Account Password",
            isPassword = true,
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Use a different password from your vault password. Your vault password encrypts " +
                "your backups and is never sent to VaultSpec.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
    }

    state.error?.let { error ->
        Spacer(modifier = Modifier.height(12.dp))
        ErrorText(error, modifier = Modifier.padding(horizontal = 4.dp))
    }

    Spacer(modifier = Modifier.height(24.dp))

    PrimaryButton(
        text = if (signUp) "Create Account" else "Sign In",
        onClick = viewModel::onSubmit,
        loading = state.isLoading,
    )

    Spacer(modifier = Modifier.height(8.dp))

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(),
    ) {
        PlainButton(
            text = if (signUp) "I Already Have an Account" else "Create Account",
            onClick = { viewModel.onSwitchMode(if (signUp) AccountMode.SignIn else AccountMode.SignUp) },
        )
        if (!signUp) {
            PlainButton(
                text = "Forgot Password?",
                onClick = viewModel::onForgotPassword,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun EnterCodeContent(state: AccountUiState, viewModel: AccountViewModel) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 32.dp),
    ) {
        HeroIcon(icon = AppIcons.Mail)

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Enter Verification Code",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "We sent a 6-digit code to ${state.email.trim()}. It expires in 10 minutes.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(24.dp))

        VaultTextField(
            value = state.code,
            onValueChange = viewModel::onCodeChange,
            label = "Verification Code",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
        )

        state.error?.let { error ->
            Spacer(modifier = Modifier.height(12.dp))
            ErrorText(error)
        }

        Spacer(modifier = Modifier.height(24.dp))

        PrimaryButton(
            text = "Verify",
            onClick = viewModel::onSubmit,
            loading = state.isLoading,
        )

        Spacer(modifier = Modifier.height(10.dp))

        TintedButton(
            text = "Resend Code",
            onClick = viewModel::onResendCode,
            enabled = !state.isLoading,
        )

        Spacer(modifier = Modifier.height(8.dp))

        PlainButton(
            text = "Use a Different Email",
            onClick = { viewModel.onSwitchMode(AccountMode.SignIn) },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
