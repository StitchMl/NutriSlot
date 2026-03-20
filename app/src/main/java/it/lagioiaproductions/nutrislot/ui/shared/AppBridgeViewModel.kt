@file:Suppress("unused")

package it.lagioiaproductions.nutrislot.ui.shared

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.core.content.edit

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

class AppBridgeViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    private val _uiState = MutableStateFlow(
        AppBridgeUiState(
            calorieJournalByDate = loadCalorieJournal()
        )
    )
    val uiState = _uiState.asStateFlow()

    private var nextShoppingItemId: Long = 1L
    private var nextFeedbackId: Long = 1L
    private var nextCalorieEntryId: Long = computeNextCalorieEntryId(
        _uiState.value.calorieJournalByDate
    )

    fun sendProductToShopping(product: LinkedScannedProductUi) {
        _uiState.update { current ->
            val normalizedName = product.name.trim()
            val alreadyExists = current.shoppingItems.any {
                it.name.equals(normalizedName, ignoreCase = true)
            }

            if (normalizedName.isBlank()) {
                current.copy(
                    latestScannedProduct = product,
                    shoppingFeedback = newFeedback("Prodotto non valido per la lista della spesa.")
                )
            } else if (alreadyExists) {
                current.copy(
                    latestScannedProduct = product,
                    shoppingFeedback = newFeedback("Il prodotto è già nella lista della spesa.")
                )
            } else {
                current.copy(
                    latestScannedProduct = product,
                    shoppingItems = listOf(
                        ShoppingListItemUi(
                            id = nextShoppingItemId++,
                            name = normalizedName
                        )
                    ) + current.shoppingItems,
                    shoppingFeedback = newFeedback("Prodotto aggiunto alla lista della spesa.")
                )
            }
        }
    }

    fun addShoppingItemsFromTexts(items: List<String>) {
        val cleanedItems = items
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }

        if (cleanedItems.isEmpty()) {
            _uiState.update { current ->
                current.copy(
                    shoppingFeedback = newFeedback("Nessun articolo valido da aggiungere.")
                )
            }
            return
        }

        _uiState.update { current ->
            val existingNames = current.shoppingItems
                .map { it.name.lowercase() }
                .toSet()

            val newNames = cleanedItems.filterNot { it.lowercase() in existingNames }

            if (newNames.isEmpty()) {
                current.copy(
                    shoppingFeedback = newFeedback("Tutti gli articoli selezionati sono già presenti.")
                )
            } else {
                val newItems = newNames.map { name ->
                    ShoppingListItemUi(
                        id = nextShoppingItemId++,
                        name = name
                    )
                }

                val message = when (newItems.size) {
                    1 -> "1 articolo aggiunto alla lista della spesa."
                    else -> "${newItems.size} articoli aggiunti alla lista della spesa."
                }

                current.copy(
                    shoppingItems = newItems + current.shoppingItems,
                    shoppingFeedback = newFeedback(message)
                )
            }
        }
    }

    fun addManualShoppingItem(text: String) {
        val normalized = text.trim()
        if (normalized.isBlank()) {
            _uiState.update { current ->
                current.copy(
                    shoppingFeedback = newFeedback("Inserisci un articolo valido.")
                )
            }
            return
        }

        _uiState.update { current ->
            val alreadyExists = current.shoppingItems.any {
                it.name.equals(normalized, ignoreCase = true)
            }

            if (alreadyExists) {
                current.copy(
                    shoppingFeedback = newFeedback("L'articolo è già nella lista della spesa.")
                )
            } else {
                current.copy(
                    shoppingItems = listOf(
                        ShoppingListItemUi(
                            id = nextShoppingItemId++,
                            name = normalized
                        )
                    ) + current.shoppingItems,
                    shoppingFeedback = newFeedback("Articolo aggiunto alla lista della spesa.")
                )
            }
        }
    }

    fun toggleShoppingItemPurchased(itemId: Long) {
        _uiState.update { current ->
            current.copy(
                shoppingItems = current.shoppingItems.map { item ->
                    if (item.id == itemId) {
                        item.copy(isPurchased = !item.isPurchased)
                    } else {
                        item
                    }
                }
            )
        }
    }

    fun removeShoppingItem(itemId: Long) {
        _uiState.update { current ->
            current.copy(
                shoppingItems = current.shoppingItems.filterNot { it.id == itemId },
                shoppingFeedback = newFeedback("Articolo rimosso dalla lista.")
            )
        }
    }

    fun clearShoppingFeedback() {
        _uiState.update { current ->
            current.copy(shoppingFeedback = null)
        }
    }

    fun sendProductToCalories(product: LinkedScannedProductUi) {
        _uiState.update { current ->
            current.copy(
                latestScannedProduct = product,
                pendingCalorieProduct = product
            )
        }
    }

    fun consumePendingCalorieProductForDay(dayKey: String) {
        val product = _uiState.value.pendingCalorieProduct ?: return
        val section = inferSectionFromScannerProduct(product)

        val entry = CalorieJournalEntryUi(
            id = nextCalorieEntryId++,
            title = product.name.ifBlank { "Prodotto scannerizzato" },
            subtitle = product.subtitle,
            calories = product.calories,
            protein = product.protein,
            carbs = product.carbs,
            fibre = product.fibre,
            section = section,
            timeLabel = currentTimeLabel(),
            sourceLabel = "Scanner"
        )

        val updatedJournal = appendEntryToJournal(
            currentJournal = _uiState.value.calorieJournalByDate,
            dayKey = dayKey,
            entry = entry
        )

        _uiState.update { current ->
            current.copy(
                pendingCalorieProduct = null,
                calorieJournalByDate = updatedJournal
            )
        }
    }

    fun addWeeklyPlanConsumptionToCalories(
        consumptionId: String,
        mealText: String,
        mealSlotLabel: String
    ) {
        val parsed = parseNutritionFromMealText(mealText)
        val section = inferSectionFromSlotLabel(mealSlotLabel)
        val titleSubtitle = splitMealText(mealText)

        val entry = CalorieJournalEntryUi(
            id = nextCalorieEntryId++,
            title = titleSubtitle.first.ifBlank { "${section.label} dal planner" },
            subtitle = titleSubtitle.second.ifBlank { "Da Weekly Plan" },
            calories = parsed.calories,
            protein = parsed.protein,
            carbs = parsed.carbs,
            fibre = parsed.fibre,
            section = section,
            timeLabel = currentTimeLabel(),
            sourceLabel = "Weekly Plan",
            plannerConsumptionId = consumptionId
        )

        val dayKey = todayDayKey()
        val updatedJournal = appendEntryToJournal(
            currentJournal = _uiState.value.calorieJournalByDate,
            dayKey = dayKey,
            entry = entry
        )

        _uiState.update { current ->
            current.copy(calorieJournalByDate = updatedJournal)
        }
    }

    fun removeWeeklyPlanConsumptionFromCalories(
        consumptionId: String
    ) {
        if (consumptionId.isBlank()) return

        val currentJournal = _uiState.value.calorieJournalByDate.toMutableMap()
        var changed = false

        val updatedJournal = currentJournal.mapValues { (_, dayLog) ->
            val filteredEntries = dayLog.entries.filterNot { entry ->
                entry.plannerConsumptionId == consumptionId
            }

            if (filteredEntries.size != dayLog.entries.size) {
                changed = true
            }

            dayLog.copy(entries = filteredEntries)
        }.filterValues { dayLog ->
            dayLog.goalKcal != null || dayLog.entries.isNotEmpty()
        }

        if (!changed) return

        persistCalorieJournal(updatedJournal)

        _uiState.update { current ->
            current.copy(calorieJournalByDate = updatedJournal)
        }
    }

    fun setCalorieGoalForDay(
        dayKey: String,
        goalKcal: Int?
    ) {
        val normalizedGoal = goalKcal?.takeIf { it > 0 }
        val currentJournal = _uiState.value.calorieJournalByDate.toMutableMap()
        val currentDay = currentJournal[dayKey] ?: CalorieDayLogUi()

        if (normalizedGoal == null && currentDay.entries.isEmpty()) {
            currentJournal.remove(dayKey)
        } else {
            currentJournal[dayKey] = currentDay.copy(goalKcal = normalizedGoal)
        }

        persistCalorieJournal(currentJournal)

        _uiState.update { current ->
            current.copy(calorieJournalByDate = currentJournal.toMap())
        }
    }

    fun removeCalorieEntry(
        dayKey: String,
        entryId: Long
    ) {
        val currentJournal = _uiState.value.calorieJournalByDate.toMutableMap()
        val currentDay = currentJournal[dayKey] ?: return
        val updatedEntries = currentDay.entries.filterNot { it.id == entryId }

        if (updatedEntries.isEmpty() && currentDay.goalKcal == null) {
            currentJournal.remove(dayKey)
        } else {
            currentJournal[dayKey] = currentDay.copy(entries = updatedEntries)
        }

        persistCalorieJournal(currentJournal)

        _uiState.update { current ->
            current.copy(calorieJournalByDate = currentJournal.toMap())
        }
    }

    fun resetCalorieDay(dayKey: String) {
        val currentJournal = _uiState.value.calorieJournalByDate.toMutableMap()
        val currentDay = currentJournal[dayKey] ?: return

        if (currentDay.goalKcal == null) {
            currentJournal.remove(dayKey)
        } else {
            currentJournal[dayKey] = currentDay.copy(entries = emptyList())
        }

        persistCalorieJournal(currentJournal)

        _uiState.update { current ->
            current.copy(calorieJournalByDate = currentJournal.toMap())
        }
    }

    private fun appendEntryToJournal(
        currentJournal: Map<String, CalorieDayLogUi>,
        dayKey: String,
        entry: CalorieJournalEntryUi
    ): Map<String, CalorieDayLogUi> {
        val mutableJournal = currentJournal.toMutableMap()
        val currentDay = mutableJournal[dayKey] ?: CalorieDayLogUi()

        mutableJournal[dayKey] = currentDay.copy(
            entries = listOf(entry) + currentDay.entries
        )

        persistCalorieJournal(mutableJournal)
        return mutableJournal.toMap()
    }

    private fun loadCalorieJournal(): Map<String, CalorieDayLogUi> {
        val raw = prefs.getString(KEY_CALORIE_JOURNAL, null) ?: return emptyMap()

        return runCatching {
            val root = JSONObject(raw)
            val result = linkedMapOf<String, CalorieDayLogUi>()
            val keys = root.keys()

            while (keys.hasNext()) {
                val dayKey = keys.next()
                val dayObject = root.optJSONObject(dayKey) ?: continue

                val goalKcal = if (dayObject.has("goalKcal") && !dayObject.isNull("goalKcal")) {
                    dayObject.optInt("goalKcal").takeIf { it > 0 }
                } else {
                    null
                }

                val entriesJson = dayObject.optJSONArray("entries") ?: JSONArray()
                val entries = buildList {
                    for (index in 0 until entriesJson.length()) {
                        val entryObject = entriesJson.optJSONObject(index) ?: continue

                        add(
                            CalorieJournalEntryUi(
                                id = entryObject.optLong("id"),
                                title = entryObject.optString("title"),
                                subtitle = entryObject.optString("subtitle"),
                                calories = entryObject.optInt("calories"),
                                protein = entryObject.optInt("protein"),
                                carbs = entryObject.optInt("carbs"),
                                fibre = entryObject.optInt("fibre"),
                                section = entryObject.optString("section")
                                    .takeIf { it.isNotBlank() }
                                    ?.let { enumName ->
                                        runCatching {
                                            CalorieJournalSection.valueOf(enumName)
                                        }.getOrNull()
                                    }
                                    ?: CalorieJournalSection.SNACK,
                                timeLabel = entryObject.optString("timeLabel"),
                                sourceLabel = entryObject.optString("sourceLabel"),
                                plannerConsumptionId = entryObject.optString("plannerConsumptionId")
                                    .takeIf { it.isNotBlank() }
                            )
                        )
                    }
                }

                result[dayKey] = CalorieDayLogUi(
                    goalKcal = goalKcal,
                    entries = entries.sortedByDescending { it.id }
                )
            }

            result.toMap()
        }.getOrDefault(emptyMap())
    }

    private fun persistCalorieJournal(journal: Map<String, CalorieDayLogUi>) {
        val root = JSONObject()

        journal.forEach { (dayKey, dayLog) ->
            val dayObject = JSONObject()
            if (dayLog.goalKcal != null) {
                dayObject.put("goalKcal", dayLog.goalKcal)
            } else {
                dayObject.put("goalKcal", JSONObject.NULL)
            }

            val entriesArray = JSONArray()
            dayLog.entries.forEach { entry ->
                entriesArray.put(
                    JSONObject().apply {
                        put("id", entry.id)
                        put("title", entry.title)
                        put("subtitle", entry.subtitle)
                        put("calories", entry.calories)
                        put("protein", entry.protein)
                        put("carbs", entry.carbs)
                        put("fibre", entry.fibre)
                        put("section", entry.section.name)
                        put("timeLabel", entry.timeLabel)
                        put("sourceLabel", entry.sourceLabel)
                        put("plannerConsumptionId", entry.plannerConsumptionId ?: JSONObject.NULL)
                    }
                )
            }

            dayObject.put("entries", entriesArray)
            root.put(dayKey, dayObject)
        }

        prefs.edit {
            putString(KEY_CALORIE_JOURNAL, root.toString())
        }
    }

    private fun computeNextCalorieEntryId(
        journal: Map<String, CalorieDayLogUi>
    ): Long {
        return journal.values
            .flatMap { it.entries }
            .maxOfOrNull { it.id }
            ?.plus(1L)
            ?: 1L
    }

    private fun newFeedback(message: String): ShoppingFeedbackUi {
        return ShoppingFeedbackUi(
            id = nextFeedbackId++,
            message = message
        )
    }

    private fun inferSectionFromScannerProduct(
        product: LinkedScannedProductUi
    ): CalorieJournalSection {
        val source = "${product.name} ${product.subtitle}".lowercase()

        return when {
            listOf("colazione", "breakfast").any { source.contains(it) } -> {
                CalorieJournalSection.BREAKFAST
            }
            listOf("pranzo", "lunch").any { source.contains(it) } -> {
                CalorieJournalSection.LUNCH
            }
            listOf("cena", "dinner").any { source.contains(it) } -> {
                CalorieJournalSection.DINNER
            }
            else -> CalorieJournalSection.SNACK
        }
    }

    private fun inferSectionFromSlotLabel(
        mealSlotLabel: String
    ): CalorieJournalSection {
        val normalized = mealSlotLabel.lowercase()

        return when {
            normalized.contains("colazione") || normalized.contains("breakfast") -> {
                CalorieJournalSection.BREAKFAST
            }
            normalized.contains("pranzo") || normalized.contains("lunch") -> {
                CalorieJournalSection.LUNCH
            }
            normalized.contains("cena") || normalized.contains("dinner") -> {
                CalorieJournalSection.DINNER
            }
            else -> CalorieJournalSection.SNACK
        }
    }

    private fun splitMealText(mealText: String): Pair<String, String> {
        val cleanedLines = mealText
            .replace("\r\n", "\n")
            .replace("\r", "\n")
            .lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }

        if (cleanedLines.isEmpty()) {
            return "Pasto consumato" to ""
        }

        val title = cleanedLines.first()
        val subtitle = cleanedLines.drop(1).joinToString(" • ")

        return title to subtitle
    }

    private data class ParsedNutrition(
        val calories: Int,
        val protein: Int,
        val carbs: Int,
        val fibre: Int
    )

    private fun parseNutritionFromMealText(
        mealText: String
    ): ParsedNutrition {
        fun findFirstInt(vararg patterns: String): Int {
            return patterns.firstNotNullOfOrNull { pattern ->
                Regex(pattern, RegexOption.IGNORE_CASE)
                    .find(mealText)
                    ?.groupValues
                    ?.getOrNull(1)
                    ?.toIntOrNull()
            } ?: 0
        }

        val calories = findFirstInt(
            """(\d{1,4})\s*kcal""",
            """kcal\s*[:=-]?\s*(\d{1,4})"""
        )

        val protein = findFirstInt(
            """(\d{1,3})\s*g\s*(?:di\s*)?(?:proteine|protein)""",
            """(?:proteine|protein)\s*[:=-]?\s*(\d{1,3})\s*g"""
        )

        val carbs = findFirstInt(
            """(\d{1,3})\s*g\s*(?:di\s*)?(?:carboidrati|carbs|carbo)""",
            """(?:carboidrati|carbs|carbo)\s*[:=-]?\s*(\d{1,3})\s*g"""
        )

        val fibre = findFirstInt(
            """(\d{1,3})\s*g\s*(?:di\s*)?(?:fibre|fiber|fibra)""",
            """(?:fibre|fiber|fibra)\s*[:=-]?\s*(\d{1,3})\s*g"""
        )

        return ParsedNutrition(
            calories = calories,
            protein = protein,
            carbs = carbs,
            fibre = fibre
        )
    }

    private fun currentTimeLabel(): String {
        return SimpleDateFormat("HH:mm", Locale.getDefault())
            .format(Date())
    }

    private fun todayDayKey(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            .format(Date())
    }

    companion object {
        private const val PREFS_NAME = "nutrislot_bridge_prefs"
        private const val KEY_CALORIE_JOURNAL = "calorie_journal_json"
    }
}