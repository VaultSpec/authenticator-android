package com.vaultspec.authenticator.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.vaultspec.authenticator.data.model.VaultState
import com.vaultspec.authenticator.data.repository.VaultRepository
import com.vaultspec.authenticator.ui.screen.addaccount.AddAccountScreen
import com.vaultspec.authenticator.ui.screen.addaccount.AddAccountViewModel
import com.vaultspec.authenticator.ui.screen.home.HomeScreen
import com.vaultspec.authenticator.ui.screen.scanner.QrScannerScreen
import com.vaultspec.authenticator.ui.screen.settings.SettingsScreen
import com.vaultspec.authenticator.ui.screen.setup.SetupScreen
import com.vaultspec.authenticator.ui.screen.unlock.UnlockScreen

@Composable
fun NavGraph(
    vaultRepository: VaultRepository,
    navController: NavHostController = rememberNavController(),
) {
    val vaultState by vaultRepository.state.collectAsState()

    val startDestination = when (vaultState) {
        is VaultState.NeedsSetup -> Routes.SETUP
        is VaultState.Locked -> Routes.UNLOCK
        is VaultState.Unlocked -> Routes.HOME
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
    ) {
        composable(Routes.SETUP) {
            SetupScreen(
                onSetupComplete = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.UNLOCK) {
            UnlockScreen(
                onUnlocked = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.HOME) {
            HomeScreen(
                onAddAccount = { navController.navigate(Routes.ADD_ACCOUNT) },
                onSettings = { navController.navigate(Routes.SETTINGS) },
                onLocked = {
                    navController.navigate(Routes.UNLOCK) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.ADD_ACCOUNT) { backStackEntry ->
            val addAccountViewModel: AddAccountViewModel = hiltViewModel()

            // Observe QR result from scanner screen
            val qrResult = backStackEntry.savedStateHandle
                .getStateFlow<String?>("qr_result", null)
                .collectAsState()

            LaunchedEffect(qrResult.value) {
                qrResult.value?.let { uri ->
                    addAccountViewModel.onQrScanned(uri)
                    backStackEntry.savedStateHandle["qr_result"] = null
                }
            }

            AddAccountScreen(
                onNavigateBack = { navController.popBackStack() },
                onScanQr = { navController.navigate(Routes.QR_SCANNER) },
                viewModel = addAccountViewModel,
            )
        }

        composable(Routes.QR_SCANNER) {
            QrScannerScreen(
                onQrScanned = { uri ->
                    // Navigate back to add account and pass QR result
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("qr_result", uri)
                    navController.popBackStack()
                },
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }
    }
}
