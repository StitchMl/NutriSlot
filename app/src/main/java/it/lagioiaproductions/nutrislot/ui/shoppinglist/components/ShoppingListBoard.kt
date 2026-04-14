package it.lagioiaproductions.nutrislot.ui.shoppinglist.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import it.lagioiaproductions.nutrislot.ui.shared.ShoppingListItemUi

@Composable
internal fun ShoppingItemsBoard(
    activeItems: List<ShoppingListItemUi>,
    completedItems: List<ShoppingListItemUi>,
    onTogglePurchased: (Long) -> Unit,
    onRemoveItem: (Long) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        SectionLabel(
            title = "Da acquistare",
            count = activeItems.size
        )

        when {
            activeItems.isNotEmpty() -> {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    activeItems.forEach { item ->
                        CompactShoppingRow(
                            item = item,
                            onTogglePurchased = { onTogglePurchased(item.id) },
                            onRemoveClick = { onRemoveItem(item.id) }
                        )
                    }
                }
            }

            completedItems.isNotEmpty() -> {
                CompletedAllActiveState()
            }

            else -> {
                EmptyShoppingState()
            }
        }

        if (completedItems.isNotEmpty()) {
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
            )

            SectionLabel(
                title = "Acquistati",
                count = completedItems.size
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                completedItems.forEach { item ->
                    CompactShoppingRow(
                        item = item,
                        onTogglePurchased = { onTogglePurchased(item.id) },
                        onRemoveClick = { onRemoveItem(item.id) }
                    )
                }
            }
        }
    }
}