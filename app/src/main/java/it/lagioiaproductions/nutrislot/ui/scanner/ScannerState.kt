package it.lagioiaproductions.nutrislot.ui.scanner

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

internal data class ScannerUiState(
    val filteredProducts: List<ScannerProductUi>,
    val selectedProduct: ScannerProductUi?
)

@Composable
internal fun rememberScannerUiState(
    demoProducts: List<ScannerProductUi>,
    searchQuery: String,
    selectedProductId: Int?
): ScannerUiState {
    val filteredProducts = remember(demoProducts, searchQuery) {
        demoProducts.filter { product ->
            searchQuery.isBlank() ||
                    product.name.contains(searchQuery, ignoreCase = true) ||
                    product.subtitle.contains(searchQuery, ignoreCase = true) ||
                    product.barcode.contains(searchQuery)
        }
    }

    val selectedProduct = remember(filteredProducts, demoProducts, selectedProductId) {
        filteredProducts.firstOrNull { it.id == selectedProductId }
            ?: demoProducts.firstOrNull { it.id == selectedProductId }
            ?: filteredProducts.firstOrNull()
    }

    return ScannerUiState(
        filteredProducts = filteredProducts,
        selectedProduct = selectedProduct
    )
}