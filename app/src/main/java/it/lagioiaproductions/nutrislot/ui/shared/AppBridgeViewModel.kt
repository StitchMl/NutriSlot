@file:Suppress("unused")

package it.lagioiaproductions.nutrislot.ui.shared

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class AppBridgeViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    private val calorieJournalStore = AppBridgeCalorieJournalStore(
        prefs = prefs,
        key = KEY_CALORIE_JOURNAL
    )

    private val _uiState = MutableStateFlow(
        AppBridgeUiState(
            calorieJournalByDate = calorieJournalStore.load()
        )
    )
    val uiState = _uiState.asStateFlow()

    private var nextShoppingItemId: Long = 1L
    private var nextFeedbackId: Long = 1L
    private var nextCalorieEntryId: Long = calorieJournalStore.computeNextEntryId(
        _uiState.value.calorieJournalByDate
    )

    fun sendProductToShopping(product: LinkedScannedProductUi) {
        _uiState.update { current ->
            AppBridgeSupport.addScannedProductToShopping(
                current = current,
                product = product,
                nextShoppingItemId = nextShoppingItemId,
                nextFeedbackId = nextFeedbackId
            ).also(::applyShoppingMutation).state
        }
    }

    fun addShoppingItemsFromTexts(items: List<String>) {
        _uiState.update { current ->
            AppBridgeSupport.addShoppingItemsFromTexts(
                current = current,
                items = items,
                nextShoppingItemId = nextShoppingItemId,
                nextFeedbackId = nextFeedbackId
            ).also(::applyShoppingMutation).state
        }
    }

    fun addManualShoppingItem(text: String) {
        _uiState.update { current ->
            AppBridgeSupport.addManualShoppingItem(
                current = current,
                text = text,
                nextShoppingItemId = nextShoppingItemId,
                nextFeedbackId = nextFeedbackId
            ).also(::applyShoppingMutation).state
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
                shoppingFeedback = ShoppingFeedbackUi(
                    id = nextFeedbackId++,
                    message = "Articolo rimosso dalla lista."
                )
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
        val section = AppBridgeSupport.inferSectionFromScannerProduct(product)

        val entry = CalorieJournalEntryUi(
            id = nextCalorieEntryId++,
            title = product.name.ifBlank { "Prodotto scannerizzato" },
            subtitle = product.subtitle,
            calories = product.calories,
            protein = product.protein,
            carbs = product.carbs,
            fibre = product.fibre,
            section = section,
            timeLabel = AppBridgeSupport.currentTimeLabel(),
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
        val parsed = AppBridgeSupport.parseNutritionFromMealText(mealText)
        val section = AppBridgeSupport.inferSectionFromSlotLabel(mealSlotLabel)
        val titleSubtitle = AppBridgeSupport.splitMealText(mealText)

        val entry = CalorieJournalEntryUi(
            id = nextCalorieEntryId++,
            title = titleSubtitle.first.ifBlank { "${section.label} dal planner" },
            subtitle = titleSubtitle.second.ifBlank { "Da Weekly Plan" },
            calories = parsed.calories,
            protein = parsed.protein,
            carbs = parsed.carbs,
            fibre = parsed.fibre,
            section = section,
            timeLabel = AppBridgeSupport.currentTimeLabel(),
            sourceLabel = "Weekly Plan",
            plannerConsumptionId = consumptionId
        )

        val dayKey = AppBridgeSupport.todayDayKey()
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

        calorieJournalStore.persist(updatedJournal)

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

        calorieJournalStore.persist(currentJournal)

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

        calorieJournalStore.persist(currentJournal)

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

        calorieJournalStore.persist(currentJournal)

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

        calorieJournalStore.persist(mutableJournal)
        return mutableJournal.toMap()
    }

    private fun applyShoppingMutation(
        result: AppBridgeSupport.ShoppingMutationResult
    ): AppBridgeSupport.ShoppingMutationResult {
        nextShoppingItemId = result.nextShoppingItemId
        nextFeedbackId = result.nextFeedbackId
        return result
    }

    companion object {
        private const val PREFS_NAME = "nutrislot_bridge_prefs"
        private const val KEY_CALORIE_JOURNAL = "calorie_journal_json"
    }
}