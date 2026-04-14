package it.lagioiaproductions.nutrislot.ui.scanner.api

import android.graphics.Bitmap
import android.util.Log
import it.lagioiaproductions.nutrislot.ui.scanner.model.ScannedProductUi
import it.lagioiaproductions.nutrislot.ui.scanner.media.ScannerImageEncoder
import it.lagioiaproductions.nutrislot.ui.scanner.model.hasNutritionValues
import java.io.IOException

internal sealed interface GeminiScanResult {
    data class Success(val product: ScannedProductUi) : GeminiScanResult
    data class Error(val message: String) : GeminiScanResult
}

internal class GeminiProductScanner(
    private val apiKey: String,
    private val api: GeminiProductScanApi = GeminiProductScanApi(),
    private val responseParser: GeminiProductResponseParser = GeminiProductResponseParser()
) {

    val isConfigured: Boolean
        get() = apiKey.trim().isNotBlank()

    fun scan(bitmap: Bitmap): GeminiScanResult {
        val trimmedApiKey = apiKey.trim()
        if (trimmedApiKey.isBlank()) {
            return GeminiScanResult.Error("Gemini API key vuota.")
        }

        return try {
            val uploadBitmap = ScannerImageEncoder.prepareForUpload(bitmap)
            val imageBytes = ScannerImageEncoder.toJpeg(uploadBitmap)
            val scanResponse = api.scanProduct(
                apiKey = trimmedApiKey,
                imageBytes = imageBytes
            )

            val baseProduct = responseParser.parse(scanResponse)
                ?: return GeminiScanResult.Error("Gemini non ha restituito un prodotto leggibile.")

            GeminiScanResult.Success(enrichNutritionIfNeeded(trimmedApiKey, baseProduct))
        } catch (error: IOException) {
            Log.e(TAG, "Gemini product scan IO error", error)
            GeminiScanResult.Error("Errore di rete durante la chiamata a Gemini.")
        } catch (error: Exception) {
            Log.e(TAG, "Gemini product scan unexpected error", error)
            GeminiScanResult.Error("Errore inatteso durante l'analisi del prodotto.")
        }
    }

    private fun enrichNutritionIfNeeded(
        apiKey: String,
        baseProduct: ScannedProductUi
    ): ScannedProductUi {
        if (baseProduct.hasNutritionValues) return baseProduct

        val descriptor = responseParser.buildNutritionDescriptor(baseProduct)
        if (descriptor.isBlank()) return baseProduct

        val estimationResponse = api.estimateNutrition(
            apiKey = apiKey,
            descriptor = descriptor
        )
        val estimatedProduct = responseParser.parse(estimationResponse) ?: return baseProduct
        return responseParser.mergeEstimatedNutrition(baseProduct, estimatedProduct)
    }

    private companion object {
        const val TAG = "GeminiProductScanner"
    }
}
