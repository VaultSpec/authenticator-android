package com.vaultspec.authenticator.ui.screen.addaccount

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vaultspec.authenticator.ui.component.AddPillChip
import com.vaultspec.authenticator.ui.component.AppIcons
import com.vaultspec.authenticator.ui.component.ErrorText
import com.vaultspec.authenticator.ui.component.LargeTitle
import com.vaultspec.authenticator.ui.component.NavBar
import com.vaultspec.authenticator.ui.component.PillChip
import com.vaultspec.authenticator.ui.component.PrimaryButton
import com.vaultspec.authenticator.ui.component.SectionHeader
import com.vaultspec.authenticator.ui.component.SegmentedControl
import com.vaultspec.authenticator.ui.component.TintedButton
import com.vaultspec.authenticator.ui.component.VaultDialog
import com.vaultspec.authenticator.ui.component.VaultTextField
import com.vaultspec.authenticator.ui.theme.VaultSpecDimens
import kotlinx.coroutines.delay

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddAccountScreen(
    onNavigateBack: () -> Unit,
    onScanQr: () -> Unit,
    viewModel: AddAccountViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val categories by viewModel.categories.collectAsState()

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) onNavigateBack()
    }

    Scaffold(
        topBar = { NavBar(onBack = onNavigateBack) },
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
            LargeTitle(text = "Add Account")

            Spacer(modifier = Modifier.height(24.dp))

            TintedButton(
                text = "Scan QR Code",
                onClick = onScanQr,
                icon = AppIcons.ScanQrCode,
            )

            Spacer(modifier = Modifier.height(28.dp))

            SectionHeader("Or enter manually")

            VaultTextField(
                value = state.issuer,
                onValueChange = viewModel::onIssuerChange,
                label = "Service Name",
                placeholder = "e.g., Google, GitHub",
            )

            Spacer(modifier = Modifier.height(10.dp))

            VaultTextField(
                value = state.accountName,
                onValueChange = viewModel::onAccountNameChange,
                label = "Account Name",
                placeholder = "e.g., user@email.com",
            )

            Spacer(modifier = Modifier.height(10.dp))

            VaultTextField(
                value = state.secret,
                onValueChange = viewModel::onSecretChange,
                label = "Secret Key",
                placeholder = "e.g., JBSWY3DPEHPK3PXP",
            )

            Spacer(modifier = Modifier.height(28.dp))

            SectionHeader("Algorithm")
            SegmentedControl(
                options = listOf("SHA1", "SHA256", "SHA512").map { it to it },
                selected = state.algorithm,
                onSelect = viewModel::onAlgorithmChange,
                onSurface = false,
            )

            Spacer(modifier = Modifier.height(28.dp))

            SectionHeader("Category")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // "All" is the home screen's filter, not something an account belongs to
                categories.filter { it != ALL_CATEGORY }.forEach { cat ->
                    PillChip(
                        text = cat,
                        selected = state.category == cat,
                        // Tapping the chosen category again leaves the account uncategorized
                        onClick = { viewModel.onCategoryChange(if (state.category == cat) ALL_CATEGORY else cat) },
                    )
                }
                AddPillChip(text = "New", onClick = viewModel::onShowAddCategoryDialog)
            }
            Text(
                text = "Optional. Every account also shows under All.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, top = 8.dp),
            )

            state.error?.let { error ->
                Spacer(modifier = Modifier.height(16.dp))
                ErrorText(error, modifier = Modifier.padding(horizontal = 16.dp))
            }

            Spacer(modifier = Modifier.height(32.dp))

            PrimaryButton(
                text = "Save Account",
                onClick = viewModel::onSave,
                loading = state.isLoading,
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Add Category Dialog
    if (state.showAddCategoryDialog) {
        var newCategoryName by remember { mutableStateOf("") }
        val focusRequester = remember { FocusRequester() }
        VaultDialog(
            title = "New Category",
            message = "Name a category to group accounts. It's saved for next time.",
            confirmText = "Add",
            confirmEnabled = newCategoryName.isNotBlank() && !newCategoryName.trim().equals(ALL_CATEGORY, ignoreCase = true),
            onConfirm = { viewModel.onAddCategory(newCategoryName) },
            onDismiss = viewModel::onDismissAddCategoryDialog,
        ) {
            VaultTextField(
                value = newCategoryName,
                onValueChange = { newCategoryName = it.take(24) },
                label = "Category Name",
                onSurface = true,
                modifier = Modifier.focusRequester(focusRequester),
            )
            LaunchedEffect(Unit) {
                delay(100)
                focusRequester.requestFocus()
            }
        }
    }
}
