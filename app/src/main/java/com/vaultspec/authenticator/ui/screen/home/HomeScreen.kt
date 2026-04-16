package com.vaultspec.authenticator.ui.screen.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vaultspec.authenticator.data.db.entity.TokenEntry
import com.vaultspec.authenticator.ui.component.CategoryTabs
import com.vaultspec.authenticator.ui.component.SearchBar
import com.vaultspec.authenticator.R
import com.vaultspec.authenticator.ui.component.TokenCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onAddAccount: () -> Unit,
    onSettings: () -> Unit,
    onLocked: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var tokenToDelete by remember { mutableStateOf<TokenEntry?>(null) }
    var showVendorFilter by remember { mutableStateOf(false) }

    if (tokenToDelete != null) {
        val deleteRed = Color(0xFFC93131)
        AlertDialog(
            onDismissRequest = { tokenToDelete = null },
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = deleteRed,
                    modifier = Modifier.size(28.dp),
                )
            },
            title = {
                Text(
                    "Delete Credential",
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                )
            },
            text = {
                Text(
                    "Are you sure you want to delete ${tokenToDelete!!.issuer}" +
                        if (tokenToDelete!!.accountName.isNotBlank()) " (${tokenToDelete!!.accountName})?" else "?" +
                        "\n\nThis action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.onDeleteToken(tokenToDelete!!)
                        tokenToDelete = null
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = deleteRed,
                        contentColor = Color.White,
                    ),
                ) {
                    Text(
                        "Delete",
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { tokenToDelete = null },
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(
                        "Cancel",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
        )
    }

    // Vendor Filter Bottom Sheet
    if (showVendorFilter && state.multiAccountVendors.isNotEmpty()) {
        ModalBottomSheet(
            onDismissRequest = { showVendorFilter = false },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp),
            ) {
                Text(
                    text = "Filter by Vendor",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp),
                )

                // "Show All" option
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            viewModel.onVendorFilterSelected(null)
                            showVendorFilter = false
                        }
                        .padding(vertical = 12.dp, horizontal = 12.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.ClearAll,
                        contentDescription = null,
                        tint = if (state.selectedVendorFilter == null)
                            MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Show All",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (state.selectedVendorFilter == null) FontWeight.Bold else FontWeight.Normal,
                        color = if (state.selectedVendorFilter == null)
                            MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface,
                    )
                }

                state.multiAccountVendors.forEach { vendor ->
                    val isSelected = state.selectedVendorFilter.equals(vendor, ignoreCase = true)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                viewModel.onVendorFilterSelected(vendor)
                                showVendorFilter = false
                            }
                            .padding(vertical = 12.dp, horizontal = 12.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Business,
                            contentDescription = null,
                            tint = if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = vendor,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddAccount,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add account")
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(bottom = 88.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Header
            item {
                Spacer(modifier = Modifier.height(20.dp))
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
                Spacer(modifier = Modifier.height(20.dp))
                SearchBar(
                    query = state.searchQuery,
                    onQueryChange = viewModel::onSearchQueryChange,
                )
            }

            // Category tabs
            item {
                Spacer(modifier = Modifier.height(16.dp))
                CategoryTabs(
                    categories = state.categories,
                    selectedCategory = state.selectedCategory,
                    onCategorySelected = viewModel::onCategorySelected,
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Token cards
            if (state.tokens.isEmpty()) {
                item {
                    EmptyState(isSearching = state.isSearching)
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
                                    .padding(horizontal = 20.dp, vertical = 8.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.error),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.onError,
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
                                horizontal = 20.dp,
                                vertical = 8.dp,
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
            .padding(horizontal = 20.dp)
    ) {
        // Logo
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
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Welcome Back",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        // Filter button (shown only when multi-account vendors exist)
        if (showFilterIcon) {
            IconButton(onClick = onFilterClick) {
                Icon(
                    imageVector = if (isFilterActive) Icons.Default.FilterAlt else Icons.Default.FilterList,
                    contentDescription = "Filter by vendor",
                    tint = if (isFilterActive) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Lock button
        IconButton(onClick = onLock) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Lock vault",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Settings button
        IconButton(onClick = onSettings) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun EmptyState(isSearching: Boolean) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 48.dp)
    ) {
        Icon(
            imageVector = if (isSearching) Icons.Default.SearchOff else Icons.Default.Shield,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
            modifier = Modifier.size(72.dp),
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = if (isSearching) "No services found"
                else "No accounts yet",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (isSearching) "Try a different search term"
                else "Tap + to add your first account",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        )
    }
}
