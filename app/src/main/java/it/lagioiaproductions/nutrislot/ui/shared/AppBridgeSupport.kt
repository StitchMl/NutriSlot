package it.lagioiaproductions.nutrislot.ui.shared

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal object AppBridgeSupport {

    data class ShoppingMutationResult(
        val state: AppBridgeUiState,
        val nextShoppingItemId: Long,
        val nextFeedbackId: Long
    )

    data class ParsedNutrition(
        val calories: Int,
        val protein: Int,
        val carbs: Int,
        val fibre: Int
    )

    fun addScannedProductToShopping(
        current: AppBridgeUiState,
        product: LinkedScannedProductUi,
        nextShoppingItemId: Long,
        nextFeedbackId: Long
    ): ShoppingMutationResult {
        val normalizedName = product.name.trim()
        val alreadyExists = current.shoppingItems.any {
            it.name.equals(normalizedName, ignoreCase = true)
        }

        return when {
            normalizedName.isBlank() -> mutationResult(
                state = current.copy(
                    latestScannedProduct = product,
                    shoppingFeedback = ShoppingFeedbackUi(
                        id = nextFeedbackId,
                        message = "Prodotto non valido per la lista della spesa."
                    )
                ),
                nextShoppingItemId = nextShoppingItemId,
                nextFeedbackId = nextFeedbackId
            )
            alreadyExists -> mutationResult(
                state = current.copy(
                    latestScannedProduct = product,
                    shoppingFeedback = ShoppingFeedbackUi(
                        id = nextFeedbackId,
                        message = "Il prodotto è già nella lista della spesa."
                    )
                ),
                nextShoppingItemId = nextShoppingItemId,
                nextFeedbackId = nextFeedbackId
            )
            else -> mutationResult(
                state = current.copy(
                    latestScannedProduct = product,
                    shoppingItems = listOf(
                        ShoppingListItemUi(
                            id = nextShoppingItemId,
                            name = normalizedName
                        )
                    ) + current.shoppingItems,
                    shoppingFeedback = ShoppingFeedbackUi(
                        id = nextFeedbackId,
                        message = "Prodotto aggiunto alla lista della spesa."
                    )
                ),
                nextShoppingItemId = nextShoppingItemId + 1,
                nextFeedbackId = nextFeedbackId
            )
        }
    }

    fun inferSectionFromScannerProduct(product: LinkedScannedProductUi): CalorieJournalSection {
        val source = "${product.name} ${product.subtitle}".lowercase()
        return when {
            listOf("colazione", "breakfast").any { source.contains(it) } -> CalorieJournalSection.BREAKFAST
            listOf("pranzo", "lunch").any { source.contains(it) } -> CalorieJournalSection.LUNCH
            listOf("cena", "dinner").any { source.contains(it) } -> CalorieJournalSection.DINNER
            else -> CalorieJournalSection.SNACK
        }
    }

    fun inferSectionFromSlotLabel(mealSlotLabel: String): CalorieJournalSection {
        val normalized = mealSlotLabel.lowercase()
        return when {
            normalized.contains("colazione") || normalized.contains("breakfast") -> CalorieJournalSection.BREAKFAST
            normalized.contains("pranzo") || normalized.contains("lunch") -> CalorieJournalSection.LUNCH
            normalized.contains("cena") || normalized.contains("dinner") -> CalorieJournalSection.DINNER
            else -> CalorieJournalSection.SNACK
        }
    }

    fun splitMealText(mealText: String): Pair<String, String> {
        val cleanedLines = stripMealNutritionFooter(mealText)
            .replace("\r\n", "\n")
            .replace("\r", "\n")
            .lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }

        if (cleanedLines.isEmpty()) {
            return "Pasto consumato" to ""
        }

        return cleanedLines.first() to cleanedLines.drop(1).joinToString(" • ")
    }

    fun parseNutritionFromMealText(mealText: String): ParsedNutrition {
        fun findFirstInt(vararg patterns: String): Int {
            return patterns.firstNotNullOfOrNull { pattern ->
                Regex(pattern, RegexOption.IGNORE_CASE)
                    .find(mealText)
                    ?.groupValues
                    ?.getOrNull(1)
                    ?.toIntOrNull()
            } ?: 0
        }

        return ParsedNutrition(
            calories = findFirstInt(
                """(\d{1,4})\s*kcal""",
                """kcal\s*[:=-]?\s*(\d{1,4})"""
            ),
            protein = findFirstInt(
                """(\d{1,3})\s*g\s*(?:di\s*)?(?:proteine|protein)""",
                """(?:proteine|protein)\s*[:=-]?\s*(\d{1,3})\s*g"""
            ),
            carbs = findFirstInt(
                """(\d{1,3})\s*g\s*(?:di\s*)?(?:carboidrati|carbs|carbo)""",
                """(?:carboidrati|carbs|carbo)\s*[:=-]?\s*(\d{1,3})\s*g"""
            ),
            fibre = findFirstInt(
                """(\d{1,3})\s*g\s*(?:di\s*)?(?:fibre|fiber|fibra)""",
                """(?:fibre|fiber|fibra)\s*[:=-]?\s*(\d{1,3})\s*g"""
            )
        )
    }

    fun currentTimeLabel(): String {
        return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
    }

    private fun mutationResult(
        state: AppBridgeUiState,
        nextShoppingItemId: Long,
        nextFeedbackId: Long
    ): ShoppingMutationResult {
        return ShoppingMutationResult(
            state = state,
            nextShoppingItemId = nextShoppingItemId,
            nextFeedbackId = nextFeedbackId + 1
        )
    }

    private fun stripMealNutritionFooter(
        mealText: String
    ): String {
        val markerIndex = mealText.indexOf("Nutrienti:", ignoreCase = true)
        return if (markerIndex >= 0) {
            mealText.substring(0, markerIndex).trim()
        } else {
            mealText.trim()
        }
    }
}
