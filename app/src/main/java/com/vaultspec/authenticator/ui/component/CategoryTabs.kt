package com.vaultspec.authenticator.ui.component

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vaultspec.authenticator.ui.theme.VaultSpecDimens

@Composable
fun CategoryTabs(
    categories: List<String>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = VaultSpecDimens.ScreenPadding)
    ) {
        categories.forEach { category ->
            PillChip(
                text = category,
                selected = category == selectedCategory,
                onClick = { onCategorySelected(category) },
            )
        }
    }
}
