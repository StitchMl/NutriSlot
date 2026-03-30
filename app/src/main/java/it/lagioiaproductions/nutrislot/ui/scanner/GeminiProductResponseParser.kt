package it.lagioiaproductions.nutrislot.ui.scanner

import org.json.JSONObject

internal class GeminiProductResponseParser {

    fun parse(rawResponse: String): ScannedProductUi? {
        val jsonText = extractPayloadText(rawResponse) ?: return null
        val payload = JSONObject(jsonText)

        return ScannedProductUi(
            name = payload.nullableString("name") ?: "Prodotto riconosciuto",
            brand = payload.nullableString("brand"),
            subtitle = payload.nullableString("subtitle"),
            barcode = payload.nullableString("barcode")
                ?.filter(Char::isDigit)
                ?.takeIf { it.isNotBlank() },
            calories = payload.nullableInt("calories"),
            protein = payload.nullableInt("protein"),
            carbs = payload.nullableInt("carbs"),
            fibre = payload.nullableInt("fibre"),
            summary = payload.nullableString("summary"),
            nutritionSource = payload.nullableString("nutritionSource").toNutritionSource()
        )
    }

    fun buildNutritionDescriptor(product: ScannedProductUi): String {
        return buildList {
            add(product.name)
            product.brand?.takeIf { it.isNotBlank() }?.let { add("Marca: $it") }
            product.subtitle?.takeIf { it.isNotBlank() }?.let { add("Formato: $it") }
            product.summary?.takeIf { it.isNotBlank() }?.let { add("Contesto immagine: $it") }
        }.joinToString("\n")
    }

    fun mergeEstimatedNutrition(
        baseProduct: ScannedProductUi,
        estimatedProduct: ScannedProductUi
    ): ScannedProductUi {
        if (!estimatedProduct.hasNutritionValues) return baseProduct

        val mergedSummary = buildList {
            baseProduct.summary?.takeIf { it.isNotBlank() }?.let(::add)
            estimatedProduct.summary?.takeIf { it.isNotBlank() }?.let(::add)
        }.joinToString(" ").trim().takeIf { it.isNotBlank() }

        return baseProduct.copy(
            calories = estimatedProduct.calories ?: baseProduct.calories,
            protein = estimatedProduct.protein ?: baseProduct.protein,
            carbs = estimatedProduct.carbs ?: baseProduct.carbs,
            fibre = estimatedProduct.fibre ?: baseProduct.fibre,
            nutritionSource = estimatedProduct.nutritionSource.takeIf {
                it != NutritionSource.UNKNOWN
            } ?: NutritionSource.ESTIMATED,
            summary = mergedSummary ?: baseProduct.summary
        )
    }

    private fun extractPayloadText(rawResponse: String): String? {
        return JSONObject(rawResponse)
            .optJSONArray("candidates")
            ?.optJSONObject(0)
            ?.optJSONObject("content")
            ?.optJSONArray("parts")
            ?.optJSONObject(0)
            ?.optString("text")
            ?.trim()
            ?.takeIf { it.isNotBlank() }
    }

    private fun JSONObject.nullableString(name: String): String? {
        if (!has(name) || isNull(name)) return null
        return optString(name).trim().takeIf { it.isNotBlank() }
    }

    private fun JSONObject.nullableInt(name: String): Int? {
        if (!has(name) || isNull(name)) return null

        val value = opt(name)
        return when (value) {
            is Number -> value.toInt()
            is String -> value.toIntOrNull()
            else -> null
        }?.takeIf { it >= 0 }
    }

    private fun String?.toNutritionSource(): NutritionSource {
        return when (this?.lowercase()) {
            "label" -> NutritionSource.LABEL
            "estimated" -> NutritionSource.ESTIMATED
            else -> NutritionSource.UNKNOWN
        }
    }
}
