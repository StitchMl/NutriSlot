package it.lagioiaproductions.nutrislot.ui.scanner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import it.lagioiaproductions.nutrislot.ui.shared.LinkedScannedProductUi

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(
    onBackClick: () -> Unit,
    onAddToShoppingList: (LinkedScannedProductUi) -> Unit,
    onSendToCalorieTracker: (LinkedScannedProductUi) -> Unit
) {
    var selectedMode by remember { mutableStateOf(ScannerMode.BARCODE) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedProductId by remember { mutableStateOf<Int?>(null) }
    var scanCounter by remember { mutableIntStateOf(0) }
    val demoProducts = remember { scannerDemoProducts() }
    val uiState = rememberScannerUiState(
        demoProducts = demoProducts,
        searchQuery = searchQuery,
        selectedProductId = selectedProductId
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Scanner") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Indietro"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Impostazioni scanner"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ScannerIntroCard(
                scanCounter = scanCounter,
                selectedProduct = uiState.selectedProduct
            )
            ScannerSearchControls(
                selectedMode = selectedMode,
                searchQuery = searchQuery,
                onModeChange = { selectedMode = it },
                onSearchQueryChange = { searchQuery = it }
            )
            ScannerSimulationCard(
                selectedMode = selectedMode,
                onSimulateClick = {
                    scanCounter += 1
                    selectedProductId = uiState.filteredProducts.firstOrNull()?.id
                        ?: demoProducts.firstOrNull()?.id
                    }
            )
            ScannerResultsSection(
                filteredProducts = uiState.filteredProducts,
                selectedProductId = selectedProductId,
                onSelectProduct = { selectedProductId = it }
            )
            uiState.selectedProduct?.let { selectedProduct ->
                SelectedScannerProductCard(
                    selectedProduct = selectedProduct,
                    onAddToShoppingList = {
                        onAddToShoppingList(selectedProduct.toLinkedProduct())
                    },
                    onSendToCalorieTracker = {
                        onSendToCalorieTracker(selectedProduct.toLinkedProduct())
                    }
                )
            }
        }
    }
}