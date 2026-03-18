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

data class AppBridgeUiState(
    val latestScannedProduct: LinkedScannedProductUi? = null,
    val pendingShoppingProduct: LinkedScannedProductUi? = null,
    val pendingCalorieProduct: LinkedScannedProductUi? = null
)

class AppBridgeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(AppBridgeUiState())
    val uiState = _uiState.asStateFlow()

    fun sendProductToShopping(product: LinkedScannedProductUi) {
        _uiState.update { current ->
            current.copy(
                latestScannedProduct = product,
                pendingShoppingProduct = product
            )
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

    fun consumePendingShoppingProduct() {
        _uiState.update { current ->
            current.copy(
                pendingShoppingProduct = null
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
}