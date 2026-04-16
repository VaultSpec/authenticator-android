package com.vaultspec.authenticator.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.vaultspec.authenticator.data.model.VaultState
import com.vaultspec.authenticator.data.prefs.AppPreferencesManager
import com.vaultspec.authenticator.data.repository.VaultRepository
import com.vaultspec.authenticator.ui.screen.addaccount.AddAccountScreen
import com.vaultspec.authenticator.ui.screen.addaccount.AddAccountViewModel
import com.vaultspec.authenticator.ui.screen.home.HomeScreen
import com.vaultspec.authenticator.ui.screen.onboarding.OnboardingScreen
import com.vaultspec.authenticator.ui.screen.scanner.QrScannerScreen
import com.vaultspec.authenticator.ui.screen.settings.SettingsScreen
import com.vaultspec.authenticator.ui.screen.setup.SetupScreen
import com.vaultspec.authenticator.ui.screen.splash.SplashScreen
import com.vaultspec.authenticator.ui.screen.unlock.UnlockScreen

@Composable
fun NavGraph(
    vaultRepository: VaultRepository,
    prefs: AppPreferencesManager,
    navController: NavHostController = rememberNavController(),
) {
    val vaultState by vaultRepository.state.collectAsState()

    // Always start with splash
    val startDestination = Routes.SPLASH

    // React to vault lock: navigate to unlock/setup when vault state changes
    // Skip auto-navigation while on splash or onboarding screens
    LaunchedEffect(vaultState) {
        val currentRoute = navController.currentDestination?.route
        if (currentRoute == Routes.SPLASH || currentRoute == Routes.ONBOARDING) return@LaunchedEffect

        when (vaultState) {
            is VaultState.Locked -> {
                navController.navigate(Routes.UNLOCK) {
                    popUpTo(0) { inclusive = true }
                }
            }
            is VaultState.NeedsSetup -> {
                navController.navigate(Routes.SETUP) {
                    popUpTo(0) { inclusive = true }
                }
            }
            is VaultState.Unlocked -> {
                // Don't auto-navigate to HOME — let screens handle it explicitly
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
    ) {
        composable(Routes.SPLASH) {
            SplashScreen(
                onFinished = {
                    val next = if (!prefs.onboardingCompleted) {
                        Routes.ONBOARDING
                    } else {
                        when (vaultRepository.state.value) {
                            is VaultState.NeedsSetup -> Routes.SETUP
                            is VaultState.Locked -> Routes.UNLOCK
                            is VaultState.Unlocked -> Routes.HOME
                        }
                    }
                    navController.navigate(next) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onComplete = {
                    prefs.onboardingCompleted = true
                    val next = when (vaultRepository.state.value) {
                        is VaultState.NeedsSetup -> Routes.SETUP
                        is VaultState.Locked -> Routes.UNLOCK
                        is VaultState.Unlocked -> Routes.HOME
                    }
                    navController.navigate(next) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                },
            )
        }

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
