package it.lagioiaproductions.nutrislot.ui.shoppinglist

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import it.lagioiaproductions.nutrislot.data.local.room.NutriSlotDatabase
import it.lagioiaproductions.nutrislot.data.local.room.ShoppingListItemEntity
import it.lagioiaproductions.nutrislot.ui.shared.LinkedScannedProductUi
import it.lagioiaproductions.nutrislot.ui.shared.ShoppingListItemUi
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@Composable
fun ShoppingListRoute(
    onOpenScannerClick: () -> Unit,
    latestScannedProduct: LinkedScannedProductUi?
) {
    val context = LocalContext.current
    val dao = remember(context) {
        NutriSlotDatabase.getInstance(context).weeklyPlanDao()
    }
    val scope = rememberCoroutineScope()

    val shoppingItemsFlow = remember(dao) {
        dao.observeShoppingListItems().map { entities ->
            entities.map { entity ->
                entity.toUi()
            }
        }
    }

    val shoppingItems by shoppingItemsFlow.collectAsState(initial = emptyList())

    ShoppingListScreen(
        onOpenScannerClick = onOpenScannerClick,
        shoppingItems = shoppingItems,
        latestScannedProduct = latestScannedProduct,
        onAddManualItem = { rawText ->
            val cleaned = rawText.trim()
            if (cleaned.isBlank()) return@ShoppingListScreen

            scope.launch {
                dao.insertShoppingListItem(
                    ShoppingListItemEntity(
                        name = cleaned,
                        isPurchased = false,
                        createdAtEpochMillis = System.currentTimeMillis()
                    )
                )
            }
        },
        onTogglePurchased = { itemId ->
            scope.launch {
                dao.toggleShoppingListItemPurchased(itemId)
            }
        },
        onRemoveItem = { itemId ->
            scope.launch {
                dao.deleteShoppingListItemById(itemId)
            }
        }
    )
}

private fun ShoppingListItemEntity.toUi(): ShoppingListItemUi {
    return ShoppingListItemUi(
        id = id,
        name = name,
        isPurchased = isPurchased
    )
}