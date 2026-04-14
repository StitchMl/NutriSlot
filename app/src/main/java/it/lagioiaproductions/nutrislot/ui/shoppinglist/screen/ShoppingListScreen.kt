package it.lagioiaproductions.nutrislot.ui.shoppinglist.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import it.lagioiaproductions.nutrislot.ui.shared.LinkedScannedProductUi
import it.lagioiaproductions.nutrislot.ui.shared.ShoppingListItemUi
import it.lagioiaproductions.nutrislot.ui.shoppinglist.components.AddShoppingItemSection
import it.lagioiaproductions.nutrislot.ui.shoppinglist.components.LatestScannedProductBanner
import it.lagioiaproductions.nutrislot.ui.shoppinglist.components.ShoppingFooterSummary
import it.lagioiaproductions.nutrislot.ui.shoppinglist.components.ShoppingListHeader
import it.lagioiaproductions.nutrislot.ui.shoppinglist.components.ShoppingItemsBoard

@Composable
fun ShoppingListScreen(
    onOpenScannerClick: () -> Unit,
    shoppingItems: List<ShoppingListItemUi>,
    latestScannedProduct: LinkedScannedProductUi?,
    onAddManualItem: (String) -> Unit,
    onTogglePurchased: (Long) -> Unit,
    onRemoveItem: (Long) -> Unit
) {
    var draftText by remember { mutableStateOf("") }

    val activeItems = remember(shoppingItems) { shoppingItems.filterNot { it.isPurchased } }
    val completedItems = remember(shoppingItems) { shoppingItems.filter { it.isPurchased } }
    val purchasedCount = completedItems.size
    val totalCount = shoppingItems.size

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            ShoppingListHeader()

            latestScannedProduct?.let { product ->
                LatestScannedProductBanner(
                    product = product,
                    onOpenScannerClick = onOpenScannerClick
                )
            }

            AddShoppingItemSection(
                draftText = draftText,
                onDraftChange = { draftText = it },
                onAddClick = {
                    val cleaned = draftText.trim()
                    if (cleaned.isNotEmpty()) {
                        onAddManualItem(cleaned)
                        draftText = ""
                    }
                },
                onOpenScannerClick = onOpenScannerClick
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
            )

            ShoppingItemsBoard(
                activeItems = activeItems,
                completedItems = completedItems,
                onTogglePurchased = onTogglePurchased,
                onRemoveItem = onRemoveItem
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
            )

            ShoppingFooterSummary(
                totalCount = totalCount,
                purchasedCount = purchasedCount
            )
        }
    }
}