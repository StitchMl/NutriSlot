@file:Suppress("SameParameterValue")

package it.lagioiaproductions.nutrislot.ui.weeklyplan.edit

import it.lagioiaproductions.nutrislot.domain.model.ImportedMealNutrition

private const val NutritionMarker = "Nutrienti:"

internal fun stripStoredMealNutrition(
    text: String
): String {
    val normalized = text
        .replace("\r\n", "\n")
        .replace("\r", "\n")

    val markerIndex = normalized.indexOf(NutritionMarker, ignoreCase = true)
    val baseText = if (markerIndex >= 0) {
        normalized.substring(0, markerIndex)
    } else {
        normalized
    }

    return baseText
        .lines()
        .map(String::trim)
        .filter(String::isNotBlank)
        .joinToString(separator = "\n")
}

internal fun normalizeNutritionSummary(
    text: String
): String {
    val normalized = text
        .replace("\r\n", "\n")
        .replace("\r", "\n")
        .trim()
        .removePrefixIgnoreCase(NutritionMarker)
        .trim()

    return normalized
        .lines()
        .map(String::trim)
        .filter(String::isNotBlank)
        .joinToString(separator = " • ")
}

internal fun mergeMealTextWithNutritionSummary(
    mealText: String,
    nutritionSummary: String
): String {
    val cleanedMealText = stripStoredMealNutrition(mealText)
    val cleanedNutritionSummary = normalizeNutritionSummary(nutritionSummary)

    if (cleanedMealText.isBlank()) {
        return ""
    }

    return if (cleanedNutritionSummary.isBlank()) {
        cleanedMealText
    } else {
        "$cleanedMealText\n\n$NutritionMarker $cleanedNutritionSummary"
    }
}

internal fun extractStoredNutritionSummary(
    text: String
): String? {
    val normalized = text
        .replace("\r\n", "\n")
        .replace("\r", "\n")

    val markerIndex = normalized.indexOf(NutritionMarker, ignoreCase = true)
    if (markerIndex < 0) {
        return null
    }

    val summary = normalizeNutritionSummary(
        normalized.substring(markerIndex + NutritionMarker.length)
    )

    return summary.takeIf(String::isNotBlank)
}

internal fun ImportedMealNutrition.toNutritionSummary(): String? {
    if (!hasAnyValue) {
        return null
    }

    val parts = buildList {
        calories?.let { add("$it kcal") }
        proteinGrams?.let { add("$it g proteine") }
        carbsGrams?.let { add("$it g carboidrati") }
        fibreGrams?.let { add("$it g fibre") }
    }

    return parts
        .joinToString(separator = " • ")
        .takeIf(String::isNotBlank)
}

private fun String.removePrefixIgnoreCase(
    prefix: String
): String {
    return if (startsWith(prefix, ignoreCase = true)) {
        substring(prefix.length)
    } else {
        this
    }
}
