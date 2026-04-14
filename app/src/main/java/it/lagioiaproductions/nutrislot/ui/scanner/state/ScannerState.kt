package it.lagioiaproductions.nutrislot.ui.scanner

import android.graphics.Bitmap

internal data class ScannerUiState(
    val previewBitmap: Bitmap? = null,
    val isAnalyzing: Boolean = false,
    val infoMessage: String = "Scatta una foto del prodotto oppure carica un'immagine nitida dell'etichetta.",
    val errorMessage: String? = null,
    val scannedProduct: ScannedProductUi? = null
)