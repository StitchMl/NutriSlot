package it.lagioiaproductions.nutrislot.ui.scanner

import androidx.compose.ui.graphics.Color

internal data class ScannerAssessmentUi(
    val score: Int,
    val label: String,
    val color: Color,
    val negativeMetrics: List<ScannerMetricUi>,
    val positiveMetrics: List<ScannerMetricUi>
)

internal data class ScannerMetricUi(
    val title: String,
    val description: String,
    val valueLabel: String,
    val color: Color,
    val symbol: String
)

internal fun ScannedProductUi.toAssessment(): ScannerAssessmentUi {
    val score = computeAssessmentScore()
    val negativeMetrics = buildNegativeMetrics()
    val positiveMetrics = buildPositiveMetrics()

    val resolvedPositives = if (positiveMetrics.isEmpty() && negativeMetrics.isEmpty()) {
        listOf(
            ScannerMetricUi(
                title = "Valutazione nutrizionale",
                description = "I dati disponibili non bastano per una lettura completa, ma il prodotto e stato riconosciuto.",
                valueLabel = "Parziale",
                color = Amber,
                symbol = "info"
            )
        )
    } else {
        positiveMetrics
    }

    return ScannerAssessmentUi(
        score = score,
        label = scoreToLabel(score),
        color = scoreToBadgeColor(score),
        negativeMetrics = negativeMetrics,
        positiveMetrics = resolvedPositives
    )
}

private fun ScannedProductUi.computeAssessmentScore(): Int {
    val caloriesPenalty = when {
        calories == null -> 0
        calories >= 500 -> 22
        calories >= 380 -> 14
        calories >= 280 -> 8
        else -> 0
    }

    val carbsPenalty = when {
        carbs == null -> 0
        carbs >= 65 -> 12
        carbs >= 50 -> 8
        carbs >= 35 -> 4
        else -> 0
    }

    val fibreBonus = when {
        fibre == null -> 0
        fibre >= 8 -> 18
        fibre >= 5 -> 12
        fibre >= 3 -> 7
        else -> 0
    }

    val proteinBonus = when {
        protein == null -> 0
        protein >= 12 -> 16
        protein >= 8 -> 11
        protein >= 5 -> 6
        else -> 0
    }

    return (52 - caloriesPenalty - carbsPenalty + fibreBonus + proteinBonus)
        .coerceIn(1, 100)
}

private fun ScannedProductUi.buildNegativeMetrics(): List<ScannerMetricUi> {
    return buildList {
        calories?.let { value ->
            if (value >= 280) {
                add(
                    ScannerMetricUi(
                        title = "Energia",
                        description = when {
                            value >= 500 -> "Molto calorico"
                            value >= 380 -> "Un po' troppo calorico"
                            else -> "Energia sopra la media"
                        },
                        valueLabel = "$value kcal",
                        color = if (value >= 500) Red else Amber,
                        symbol = "kcal"
                    )
                )
            }
        }

        carbs?.let { value ->
            if (value >= 35) {
                add(
                    ScannerMetricUi(
                        title = "Carboidrati",
                        description = when {
                            value >= 65 -> "Molti carboidrati"
                            value >= 50 -> "Carboidrati elevati"
                            else -> "Quota da tenere d'occhio"
                        },
                        valueLabel = "$value g",
                        color = if (value >= 65) Red else Amber,
                        symbol = "carb"
                    )
                )
            }
        }
    }
}

private fun ScannedProductUi.buildPositiveMetrics(): List<ScannerMetricUi> {
    return buildList {
        fibre?.let { value ->
            if (value > 0) {
                add(
                    ScannerMetricUi(
                        title = "Fibre",
                        description = when {
                            value >= 8 -> "Ottima quantita di fibre"
                            value >= 5 -> "Buona quantita di fibre"
                            value >= 3 -> "Discrete fibre"
                            else -> "Poche fibre ma presenti"
                        },
                        valueLabel = "$value g",
                        color = if (value >= 3) Green else Amber,
                        symbol = "fib"
                    )
                )
            }
        }

        protein?.let { value ->
            if (value > 0) {
                add(
                    ScannerMetricUi(
                        title = "Proteine",
                        description = when {
                            value >= 12 -> "Ottima quota proteica"
                            value >= 8 -> "Buona quota proteica"
                            value >= 5 -> "Un po' proteico"
                            else -> "Quota proteica leggera"
                        },
                        valueLabel = "$value g",
                        color = if (value >= 5) Green else Amber,
                        symbol = "pro"
                    )
                )
            }
        }
    }
}

private fun scoreToLabel(score: Int): String {
    return when {
        score >= 80 -> "Eccellente"
        score >= 65 -> "Buono"
        score >= 45 -> "Medio"
        else -> "Scarso"
    }
}

private fun scoreToBadgeColor(score: Int): Color {
    return when {
        score >= 65 -> Green
        score >= 45 -> Amber
        else -> Red
    }
}

private val Green = Color(0xFF22C55E)
private val Amber = Color(0xFFF59E0B)
private val Red = Color(0xFFEF4444)
