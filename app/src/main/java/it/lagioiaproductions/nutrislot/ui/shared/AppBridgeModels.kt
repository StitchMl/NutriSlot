@file:Suppress("unused")

package it.lagioiaproductions.nutrislot.ui.shared

enum class CalorieJournalSection(
    val label: String,
    val emoji: String,
    val defaultTime: String
) {
    BREAKFAST("Colazione", "☀️", "08:00"),
    LUNCH("Pranzo", "🥗", "13:00"),
    DINNER("Cena", "🍽️", "20:00"),
    SNACK("Snack", "🍎", "16:30")
}

data class CalorieJournalEntryUi(
    val id: Long,
    val title: String,
    val subtitle: String,
    val calories: Int,
    val protein: Int,
    val carbs: Int,
    val fibre: Int,
    val section: CalorieJournalSection,
    val timeLabel: String,
    val sourceLabel: String,
    val plannerConsumptionId: String? = null
)

data class CalorieDayLogUi(
    val goalKcal: Int? = null,
    val entries: List<CalorieJournalEntryUi> = emptyList()
)

data class LinkedScannedProductUi(
    val name: String,
    val subtitle: String,
    val barcode: String,
    val calories: Int,
    val protein: Int,
    val carbs: Int,
    val fibre: Int
)

data class ShoppingListItemUi(
    val id: Long,
    val name: String,
    val isPurchased: Boolean = false
)

data class ShoppingFeedbackUi(
    val id: Long,
    val message: String
)

data class AppBridgeUiState(
    val latestScannedProduct: LinkedScannedProductUi? = null,
    val shoppingItems: List<ShoppingListItemUi> = emptyList(),
    val shoppingFeedback: ShoppingFeedbackUi? = null,
    val pendingCalorieProduct: LinkedScannedProductUi? = null,
    val calorieJournalByDate: Map<String, CalorieDayLogUi> = emptyMap()
)