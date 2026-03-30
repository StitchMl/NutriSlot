package it.lagioiaproductions.nutrislot.ui.shared

internal class AppBridgeShoppingManager {

    private var nextShoppingItemId: Long = 1L
    private var nextFeedbackId: Long = 1L

    fun addScannedProduct(
        current: AppBridgeUiState,
        product: LinkedScannedProductUi
    ): AppBridgeUiState {
        val result = AppBridgeSupport.addScannedProductToShopping(
            current = current,
            product = product,
            nextShoppingItemId = nextShoppingItemId,
            nextFeedbackId = nextFeedbackId
        )

        nextShoppingItemId = result.nextShoppingItemId
        nextFeedbackId = result.nextFeedbackId
        return result.state
    }

    fun clearFeedback(current: AppBridgeUiState): AppBridgeUiState {
        return current.copy(shoppingFeedback = null)
    }

    fun queueForCalories(
        current: AppBridgeUiState,
        product: LinkedScannedProductUi
    ): AppBridgeUiState {
        return current.copy(
            latestScannedProduct = product,
            pendingCalorieProduct = product
        )
    }
}
