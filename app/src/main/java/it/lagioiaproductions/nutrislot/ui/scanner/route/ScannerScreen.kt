package it.lagioiaproductions.nutrislot.ui.scanner.route

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import it.lagioiaproductions.nutrislot.ui.scanner.viewmodel.ScannerViewModel
import it.lagioiaproductions.nutrislot.ui.scanner.components.ScannerErrorCard
import it.lagioiaproductions.nutrislot.ui.scanner.components.ScannerHeroCard
import it.lagioiaproductions.nutrislot.ui.scanner.components.ScannerLoadingCard
import it.lagioiaproductions.nutrislot.ui.scanner.components.ScannerStatusCard
import it.lagioiaproductions.nutrislot.ui.scanner.components.SelectedScannerProductCard
import it.lagioiaproductions.nutrislot.ui.scanner.model.toLinkedProduct
import it.lagioiaproductions.nutrislot.ui.shared.LinkedScannedProductUi

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(
    onBackClick: () -> Unit,
    onAddToShoppingList: (LinkedScannedProductUi) -> Unit,
    onSendToCalorieTracker: (LinkedScannedProductUi) -> Unit
) {
    val viewModel: ScannerViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        viewModel.onCameraBitmap(bitmap)
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        viewModel.onGalleryUri(uri)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Scanner") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Indietro"
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
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            ScannerHeroCard(
                previewBitmap = uiState.previewBitmap,
                isAnalyzing = uiState.isAnalyzing,
                onTakePhotoClick = { cameraLauncher.launch(null) },
                onPickFromGalleryClick = { galleryLauncher.launch("image/*") },
                onAnalyzeClick = viewModel::analyzeCurrentImage,
                onClearImageClick = viewModel::clearImage
            )

            ScannerStatusCard(text = uiState.infoMessage)

            if (uiState.isAnalyzing) {
                ScannerLoadingCard()
            }

            uiState.errorMessage?.let { errorMessage ->
                ScannerErrorCard(errorMessage = errorMessage)
            }

            uiState.scannedProduct?.let { selectedProduct ->
                SelectedScannerProductCard(
                    selectedProduct = selectedProduct,
                    previewBitmap = uiState.previewBitmap,
                    onAddToShoppingList = {
                        onAddToShoppingList(selectedProduct.toLinkedProduct())
                    },
                    onSendToCalorieTracker = {
                        onSendToCalorieTracker(selectedProduct.toLinkedProduct())
                    }
                )
            }

            Text(
                text = "Per ottenere una scheda più vicina alle app di scansione alimenti, fotografa il fronte del prodotto e poi la tabella nutrizionale in modo nitido.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
    }
}