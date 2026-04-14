package it.lagioiaproductions.nutrislot.ui.scanner.model

import it.lagioiaproductions.nutrislot.ui.shared.LinkedScannedProductUi

internal data class ScannedProductUi(
    val name: String,
    val brand: String? = null,
    val subtitle: String? = null,
    val barcode: String? = null,
    val calories: Int? = null,
    val protein: Int? = null,
    val carbs: Int? = null,
    val fibre: Int? = null,
    val summary: String? = null,
    val nutritionSource: NutritionSource = NutritionSource.UNKNOWN
)

internal enum class NutritionSource {
    LABEL,
    ESTIMATED,
    UNKNOWN
}

internal val ScannedProductUi.hasNutritionValues: Boolean
    get() = calories != null || protein != null || carbs != null || fibre != null

internal val ScannedProductUi.nutritionSourceLabel: String
    get() = when (nutritionSource) {
        NutritionSource.LABEL -> "Valori letti dall'etichetta"
        NutritionSource.ESTIMATED -> "Valori stimati da Gemini"
        NutritionSource.UNKNOWN -> "Valutazione parziale"
    }

internal fun ScannedProductUi.toLinkedProduct(): LinkedScannedProductUi {
    val computedSubtitle = buildList {
        brand?.trim()?.takeIf { it.isNotBlank() }?.let(::add)
        subtitle?.trim()?.takeIf { it.isNotBlank() }?.let(::add)
    }.joinToString(" - ")

    return LinkedScannedProductUi(
        name = name.ifBlank { "Prodotto scannerizzato" },
        subtitle = computedSubtitle,
        barcode = barcode.orEmpty(),
        calories = calories ?: 0,
        protein = protein ?: 0,
        carbs = carbs ?: 0,
        fibre = fibre ?: 0
    )
}
