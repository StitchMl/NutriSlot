package it.lagioiaproductions.nutrislot.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import it.lagioiaproductions.nutrislot.data.local.room.NutriSlotDatabase
import it.lagioiaproductions.nutrislot.data.local.room.ShoppingListItemEntity
import kotlinx.coroutines.launch

internal data class ShoppingListQuickActions(
    val addItems: (List<String>) -> Unit,
    val addItem: (String) -> Unit
)

@Composable
internal fun rememberShoppingListQuickActions(): ShoppingListQuickActions {
    val context = LocalContext.current
    val shoppingDao = remember(context) {
        NutriSlotDatabase.getInstance(context).weeklyPlanDao()
    }
    val scope = rememberCoroutineScope()

    val addItems: (List<String>) -> Unit = remember(shoppingDao, scope) {
        { rawItems ->
            scope.launch {
                rawItems
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .distinct()
                    .forEachIndexed { index, item ->
                        shoppingDao.insertShoppingListItem(
                            ShoppingListItemEntity(
                                name = item,
                                isPurchased = false,
                                createdAtEpochMillis = System.currentTimeMillis() + index
                            )
                        )
                    }
            }
        }
    }

    val addItem: (String) -> Unit = remember(shoppingDao, scope) {
        { rawItem ->
            val cleaned = rawItem.trim()
            if (cleaned.isNotBlank()) {
                scope.launch {
                    shoppingDao.insertShoppingListItem(
                        ShoppingListItemEntity(
                            name = cleaned,
                            isPurchased = false,
                            createdAtEpochMillis = System.currentTimeMillis()
                        )
                    )
                }
            }
        }
    }

    return remember(addItems, addItem) {
        ShoppingListQuickActions(
            addItems = addItems,
            addItem = addItem
        )
    }
}
