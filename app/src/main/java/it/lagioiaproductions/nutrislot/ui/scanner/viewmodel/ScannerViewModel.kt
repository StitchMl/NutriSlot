package it.lagioiaproductions.nutrislot.ui.scanner.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import it.lagioiaproductions.nutrislot.BuildConfig
import it.lagioiaproductions.nutrislot.ui.scanner.api.GeminiProductScanner
import it.lagioiaproductions.nutrislot.ui.scanner.api.GeminiScanResult
import it.lagioiaproductions.nutrislot.ui.scanner.media.ScannerImageLoader
import it.lagioiaproductions.nutrislot.ui.scanner.model.ScannedProductUi
import it.lagioiaproductions.nutrislot.ui.scanner.state.ScannerUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class ScannerViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(ScannerUiState())
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    private val imageLoader = ScannerImageLoader(getApplication())
    private val scanner = GeminiProductScanner(apiKey = BuildConfig.GEMINI_API_KEY)

    fun onCameraBitmap(bitmap: Bitmap?) {
        if (bitmap == null) {
            showError("Nessuna foto acquisita dalla camera.")
            return
        }

        showPreview(
            bitmap = bitmap,
            message = "Foto acquisita. Premi Analizza con Gemini."
        )
    }

    fun onGalleryUri(uri: Uri?) {
        if (uri == null) return

        viewModelScope.launch(Dispatchers.IO) {
            val bitmap = imageLoader.decode(uri)
            if (bitmap == null) {
                showError("Impossibile leggere l'immagine selezionata.")
                return@launch
            }

            showPreview(
                bitmap = bitmap,
                message = "Immagine caricata. Premi Analizza con Gemini."
            )
        }
    }

    fun clearImage() {
        _uiState.update {
            it.copy(
                previewBitmap = null,
                scannedProduct = null,
                errorMessage = null,
                infoMessage = "Immagine rimossa. Puoi acquisirne un'altra."
            )
        }
    }

    fun analyzeCurrentImage() {
        val bitmap = _uiState.value.previewBitmap
        if (bitmap == null) {
            showError("Carica prima una foto del prodotto.")
            return
        }

        if (!scanner.isConfigured) {
            showError(
                "GEMINI_API_KEY mancante o vuota. Configurala in secrets.properties, local.properties, nella variabile d'ambiente GEMINI_API_KEY o nel gradle.properties utente."
            )
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            showAnalysisStarted()

            when (val result = scanner.scan(bitmap)) {
                is GeminiScanResult.Success -> showAnalysisSuccess(result.product)
                is GeminiScanResult.Error -> showAnalysisFailure(result.message)
            }
        }
    }

    private fun showPreview(
        bitmap: Bitmap,
        message: String
    ) {
        _uiState.update {
            it.copy(
                previewBitmap = bitmap,
                scannedProduct = null,
                errorMessage = null,
                infoMessage = message
            )
        }
    }

    private fun showAnalysisStarted() {
        _uiState.update {
            it.copy(
                isAnalyzing = true,
                scannedProduct = null,
                errorMessage = null,
                infoMessage = "Analisi immagine in corso con Gemini..."
            )
        }
    }

    private fun showAnalysisSuccess(product: ScannedProductUi) {
        _uiState.update {
            it.copy(
                isAnalyzing = false,
                scannedProduct = product,
                errorMessage = null,
                infoMessage = "Analisi completata."
            )
        }
    }

    private fun showAnalysisFailure(message: String) {
        _uiState.update {
            it.copy(
                isAnalyzing = false,
                scannedProduct = null,
                errorMessage = message,
                infoMessage = "Analisi non completata."
            )
        }
    }

    private fun showError(message: String) {
        _uiState.update { it.copy(errorMessage = message) }
    }
}
