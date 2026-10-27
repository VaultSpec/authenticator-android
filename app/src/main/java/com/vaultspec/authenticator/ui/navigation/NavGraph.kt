package com.vaultspec.authenticator.ui.navigation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.IntOffset
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.vaultspec.authenticator.data.model.VaultState
import com.vaultspec.authenticator.data.prefs.AppPreferencesManager
import com.vaultspec.authenticator.data.repository.VaultRepository
import com.vaultspec.authenticator.ui.screen.account.AccountScreen
import com.vaultspec.authenticator.ui.screen.addaccount.AddAccountScreen
import com.vaultspec.authenticator.ui.screen.addaccount.AddAccountViewModel
import com.vaultspec.authenticator.ui.screen.connect.ScanToConnectScreen
import com.vaultspec.authenticator.ui.screen.connect.ShowPairingCodeScreen
import com.vaultspec.authenticator.ui.screen.home.HomeScreen
import com.vaultspec.authenticator.ui.screen.onboarding.OnboardingScreen
import com.vaultspec.authenticator.ui.screen.scanner.QrScannerScreen
import com.vaultspec.authenticator.ui.screen.settings.SettingsScreen
import com.vaultspec.authenticator.ui.screen.setup.SetupScreen
import com.vaultspec.authenticator.ui.screen.splash.SplashScreen
import com.vaultspec.authenticator.ui.screen.unlock.UnlockScreen

private const val KEY_CLOUD_SIGNED_IN = "cloud_signed_in"

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

    // Pushed screens slide in over their parent; everything else cross-fades
    val pushed = setOf(Routes.ADD_ACCOUNT, Routes.SETTINGS, Routes.QR_SCANNER, Routes.ACCOUNT, Routes.CONNECT_SHOW, Routes.CONNECT_SCAN)
    val slide = tween<IntOffset>(durationMillis = 350, easing = FastOutSlowInEasing)
    val fade = tween<Float>(durationMillis = 250)

    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = {
            if (targetState.destination.route in pushed) slideInHorizontally(slide) { it }
            else fadeIn(fade)
        },
        exitTransition = {
            if (targetState.destination.route in pushed) slideOutHorizontally(slide) { -it / 4 } + fadeOut(fade)
            else fadeOut(fade)
        },
        popEnterTransition = {
            if (initialState.destination.route in pushed) slideInHorizontally(slide) { -it / 4 } + fadeIn(fade)
            else fadeIn(fade)
        },
        popExitTransition = {
            if (initialState.destination.route in pushed) slideOutHorizontally(slide) { it }
            else fadeOut(fade)
        },
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

        composable(Routes.SETUP) { backStackEntry ->
            // Set by the account screen when the user signs in to restore from the cloud
            val cloudSignedIn = backStackEntry.savedStateHandle
                .getStateFlow(KEY_CLOUD_SIGNED_IN, false)
                .collectAsState()

            SetupScreen(
                onSetupComplete = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onCloudSignIn = { navController.navigate(Routes.ACCOUNT) },
                onConnectDevice = { navController.navigate(Routes.CONNECT_SCAN) },
                cloudSignedIn = cloudSignedIn.value,
                onCloudSignedInHandled = { backStackEntry.savedStateHandle[KEY_CLOUD_SIGNED_IN] = false },
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
                onOpenAccount = { navController.navigate(Routes.ACCOUNT) },
                onConnectDevice = { navController.navigate(Routes.CONNECT_SHOW) },
            )
        }

        composable(Routes.CONNECT_SHOW) {
            ShowPairingCodeScreen(onDone = { navController.popBackStack() })
        }

        composable(Routes.CONNECT_SCAN) {
            ScanToConnectScreen(
                onCancel = { navController.popBackStack() },
                onConnected = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.ACCOUNT) {
            AccountScreen(
                onNavigateBack = { navController.popBackStack() },
                onSignedIn = {
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set(KEY_CLOUD_SIGNED_IN, true)
                    navController.popBackStack()
                },
            )
        }
    }
}
