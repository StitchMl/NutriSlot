package it.lagioiaproductions.nutrislot.ui.shared

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class LinkedScannedProductUi(
    val name: String,
    val subtitle: String,
    val barcode: String,
    val calories: Int,
    val protein: Int,
    val carbs: Int,
    val fibre: Int
)

data class ShoppingListItemUi(
    val id: Long,
    val name: String,
    val isPurchased: Boolean = false
)

data class ShoppingFeedbackUi(
    val id: Long,
    val message: String
)

data class AppBridgeUiState(
    val latestScannedProduct: LinkedScannedProductUi? = null,
    val shoppingItems: List<ShoppingListItemUi> = emptyList(),
    val shoppingFeedback: ShoppingFeedbackUi? = null,
    val pendingCalorieProduct: LinkedScannedProductUi? = null
)

class AppBridgeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(AppBridgeUiState())
    val uiState = _uiState.asStateFlow()

    private var nextShoppingItemId: Long = 1L
    private var nextFeedbackId: Long = 1L

    fun sendProductToShopping(product: LinkedScannedProductUi) {
        _uiState.update { current ->
            val normalizedName = product.name.trim()
            val alreadyExists = current.shoppingItems.any {
                it.name.equals(normalizedName, ignoreCase = true)
            }

            if (normalizedName.isBlank()) {
                current.copy(
                    latestScannedProduct = product,
                    shoppingFeedback = newFeedback("Prodotto non valido per la lista della spesa.")
                )
            } else if (alreadyExists) {
                current.copy(
                    latestScannedProduct = product,
                    shoppingFeedback = newFeedback("Il prodotto è già nella lista della spesa.")
                )
            } else {
                current.copy(
                    latestScannedProduct = product,
                    shoppingItems = listOf(
                        ShoppingListItemUi(
                            id = nextShoppingItemId++,
                            name = normalizedName
                        )
                    ) + current.shoppingItems,
                    shoppingFeedback = newFeedback("Prodotto aggiunto alla lista della spesa.")
                )
            }
        }
    }

    fun addShoppingItemsFromTexts(items: List<String>) {
        val cleanedItems = items
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }

        if (cleanedItems.isEmpty()) {
            _uiState.update { current ->
                current.copy(
                    shoppingFeedback = newFeedback("Nessun articolo valido da aggiungere.")
                )
            }
            return
        }

        _uiState.update { current ->
            val existingNames = current.shoppingItems
                .map { it.name.lowercase() }
                .toSet()

            val newNames = cleanedItems.filterNot { it.lowercase() in existingNames }

            if (newNames.isEmpty()) {
                current.copy(
                    shoppingFeedback = newFeedback("Tutti gli articoli selezionati sono già presenti.")
                )
            } else {
                val newItems = newNames.map { name ->
                    ShoppingListItemUi(
                        id = nextShoppingItemId++,
                        name = name
                    )
                }

                val message = when (newItems.size) {
                    1 -> "1 articolo aggiunto alla lista della spesa."
                    else -> "${newItems.size} articoli aggiunti alla lista della spesa."
                }

                current.copy(
                    shoppingItems = newItems + current.shoppingItems,
                    shoppingFeedback = newFeedback(message)
                )
            }
        }
    }

    fun addManualShoppingItem(text: String) {
        val normalized = text.trim()
        if (normalized.isBlank()) {
            _uiState.update { current ->
                current.copy(
                    shoppingFeedback = newFeedback("Inserisci un articolo valido.")
                )
            }
            return
        }

        _uiState.update { current ->
            val alreadyExists = current.shoppingItems.any {
                it.name.equals(normalized, ignoreCase = true)
            }

            if (alreadyExists) {
                current.copy(
                    shoppingFeedback = newFeedback("L'articolo è già nella lista della spesa.")
                )
            } else {
                current.copy(
                    shoppingItems = listOf(
                        ShoppingListItemUi(
                            id = nextShoppingItemId++,
                            name = normalized
                        )
                    ) + current.shoppingItems,
                    shoppingFeedback = newFeedback("Articolo aggiunto alla lista della spesa.")
                )
            }
        }
    }

    fun toggleShoppingItemPurchased(itemId: Long) {
        _uiState.update { current ->
            current.copy(
                shoppingItems = current.shoppingItems.map { item ->
                    if (item.id == itemId) {
                        item.copy(isPurchased = !item.isPurchased)
                    } else {
                        item
                    }
                }
            )
        }
    }

    fun removeShoppingItem(itemId: Long) {
        _uiState.update { current ->
            current.copy(
                shoppingItems = current.shoppingItems.filterNot { it.id == itemId },
                shoppingFeedback = newFeedback("Articolo rimosso dalla lista.")
            )
        }
    }

    fun clearShoppingFeedback() {
        _uiState.update { current ->
            current.copy(shoppingFeedback = null)
        }
    }

    fun sendProductToCalories(product: LinkedScannedProductUi) {
        _uiState.update { current ->
            current.copy(
                latestScannedProduct = product,
                pendingCalorieProduct = product
            )
        }
    }

    fun consumePendingCalorieProduct() {
        _uiState.update { current ->
            current.copy(
                pendingCalorieProduct = null
            )
        }
    }

    private fun newFeedback(message: String): ShoppingFeedbackUi {
        return ShoppingFeedbackUi(
            id = nextFeedbackId++,
            message = message
        )
    }
}