package com.vaultspec.authenticator.ui.screen.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vaultspec.authenticator.R
import com.vaultspec.authenticator.data.db.entity.TokenEntry
import com.vaultspec.authenticator.ui.component.CategoryTabs
import com.vaultspec.authenticator.ui.component.CircleIconButton
import com.vaultspec.authenticator.ui.component.GroupCard
import com.vaultspec.authenticator.ui.component.GroupDivider
import com.vaultspec.authenticator.ui.component.HeroIcon
import com.vaultspec.authenticator.ui.component.SearchBar
import com.vaultspec.authenticator.ui.component.TintedButton
import com.vaultspec.authenticator.ui.component.TokenCard
import com.vaultspec.authenticator.ui.component.VaultDialog
import com.vaultspec.authenticator.ui.theme.LocalVaultSpecColors
import com.vaultspec.authenticator.ui.theme.VaultSpecDimens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onAddAccount: () -> Unit,
    onSettings: () -> Unit,
    onLocked: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val extra = LocalVaultSpecColors.current
    var tokenToDelete by remember { mutableStateOf<TokenEntry?>(null) }
    var showVendorFilter by remember { mutableStateOf(false) }

    tokenToDelete?.let { token ->
        val name = if (token.accountName.isNotBlank()) "${token.issuer} (${token.accountName})" else token.issuer
        VaultDialog(
            title = "Delete Account?",
            message = "$name will be removed from your vault. This action cannot be undone.",
            icon = Icons.Default.Delete,
            destructive = true,
            confirmText = "Delete",
            onConfirm = {
                viewModel.onDeleteToken(token)
                tokenToDelete = null
            },
            onDismiss = { tokenToDelete = null },
        )
    }

    // Vendor Filter Bottom Sheet
    if (showVendorFilter && state.multiAccountVendors.isNotEmpty()) {
        ModalBottomSheet(
            onDismissRequest = { showVendorFilter = false },
            containerColor = MaterialTheme.colorScheme.background,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            dragHandle = { BottomSheetDefaults.DragHandle(color = extra.tertiaryLabel) },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = VaultSpecDimens.ScreenPadding)
                    .padding(bottom = 32.dp),
            ) {
                Text(
                    text = "Filter by Vendor",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(bottom = 16.dp),
                )

                GroupCard {
                    VendorRow(
                        label = "Show All",
                        isSelected = state.selectedVendorFilter == null,
                        onClick = {
                            viewModel.onVendorFilterSelected(null)
                            showVendorFilter = false
                        },
                    )
                    state.multiAccountVendors.forEach { vendor ->
                        GroupDivider(startIndent = 16.dp)
                        VendorRow(
                            label = vendor,
                            isSelected = state.selectedVendorFilter.equals(vendor, ignoreCase = true),
                            onClick = {
                                viewModel.onVendorFilterSelected(vendor)
                                showVendorFilter = false
                            },
                        )
                    }
                }
            }
        }
    }

    Scaffold(
        floatingActionButton = {
            CircleIconButton(
                icon = Icons.Default.Add,
                contentDescription = "Add account",
                onClick = onAddAccount,
                tint = Color.White,
                containerColor = MaterialTheme.colorScheme.primary,
                size = 60.dp,
                modifier = Modifier.shadow(8.dp, CircleShape),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(bottom = 96.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Header
            item {
                HeaderSection(
                    onSettings = onSettings,
                    onLock = {
                        viewModel.lock()
                        onLocked()
                    },
                    showFilterIcon = state.multiAccountVendors.isNotEmpty(),
                    isFilterActive = state.selectedVendorFilter != null,
                    onFilterClick = { showVendorFilter = true },
                )
            }

            // Search bar
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SearchBar(
                    query = state.searchQuery,
                    onQueryChange = viewModel::onSearchQueryChange,
                    placeholder = "Search services",
                )
            }

            // Category tabs
            item {
                Spacer(modifier = Modifier.height(14.dp))
                CategoryTabs(
                    categories = state.categories,
                    selectedCategory = state.selectedCategory,
                    onCategorySelected = viewModel::onCategorySelected,
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Token cards
            if (state.tokens.isEmpty()) {
                item {
                    EmptyState(isSearching = state.isSearching, onAddAccount = onAddAccount)
                }
            } else {
                itemsIndexed(
                    items = state.tokens,
                    key = { _, item -> item.entry.id }
                ) { index, item ->
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { value ->
                            if (value == SwipeToDismissBoxValue.StartToEnd ||
                                value == SwipeToDismissBoxValue.EndToStart
                            ) {
                                tokenToDelete = item.entry
                            }
                            false
                        },
                    )

                    SwipeToDismissBox(
                        state = dismissState,
                        backgroundContent = {
                            val alignment = when (dismissState.dismissDirection) {
                                SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                                SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                                else -> Alignment.Center
                            }
                            Box(
                                contentAlignment = alignment,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = VaultSpecDimens.ScreenPadding, vertical = 6.dp)
                                    .clip(VaultSpecDimens.CardShape)
                                    .background(extra.red),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = Color.White,
                                    modifier = Modifier.padding(horizontal = 24.dp),
                                )
                            }
                        },
                        enableDismissFromStartToEnd = true,
                        enableDismissFromEndToStart = true,
                    ) {
                        TokenCard(
                            issuer = item.entry.issuer,
                            accountName = item.entry.accountName,
                            code = item.code,
                            secondsRemaining = item.secondsRemaining,
                            period = item.entry.period,
                            isFeatured = index == 0,
                            isRevealed = !state.tapToReveal || item.entry.id in state.revealedTokenIds,
                            isHighlighted = state.highlightedTokenId == item.entry.id,
                            onTap = { viewModel.onTokenTap(item.entry.id, item.code) },
                            modifier = Modifier.padding(
                                horizontal = VaultSpecDimens.ScreenPadding,
                                vertical = 6.dp,
                            ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderSection(
    onSettings: () -> Unit,
    onLock: () -> Unit,
    showFilterIcon: Boolean = false,
    isFilterActive: Boolean = false,
    onFilterClick: () -> Unit = {},
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = VaultSpecDimens.ScreenPadding)
            .padding(top = 12.dp),
    ) {
        Image(
            painter = painterResource(R.drawable.vaultspec_logo_mask),
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onBackground),
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "VaultSpec",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Welcome Back",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            // Filter button (shown only when multi-account vendors exist)
            if (showFilterIcon) {
                CircleIconButton(
                    icon = if (isFilterActive) Icons.Default.FilterAlt else Icons.Default.FilterList,
                    contentDescription = "Filter by vendor",
                    onClick = onFilterClick,
                    tint = if (isFilterActive) Color.White else MaterialTheme.colorScheme.onSurface,
                    containerColor = if (isFilterActive) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surface,
                )
            }

            CircleIconButton(
                icon = Icons.Default.Lock,
                contentDescription = "Lock vault",
                onClick = onLock,
                tint = MaterialTheme.colorScheme.onSurface,
            )

            CircleIconButton(
                icon = Icons.Default.Settings,
                contentDescription = "Settings",
                onClick = onSettings,
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun VendorRow(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = 52.dp)
            .padding(horizontal = 16.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun EmptyState(isSearching: Boolean, onAddAccount: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 40.dp, vertical = 56.dp)
    ) {
        HeroIcon(
            icon = if (isSearching) Icons.Default.SearchOff else Icons.Default.Shield,
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = if (isSearching) "No Results" else "No Accounts Yet",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = if (isSearching) "Try a different search term."
                else "Add your first account to start generating codes.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (!isSearching) {
            Spacer(modifier = Modifier.height(24.dp))
            TintedButton(
                text = "Add Account",
                onClick = onAddAccount,
                icon = Icons.Default.Add,
                modifier = Modifier.widthIn(min = 200.dp),
            )
        }
    }
}
