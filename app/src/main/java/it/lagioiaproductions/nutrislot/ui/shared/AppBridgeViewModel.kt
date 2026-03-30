package it.lagioiaproductions.nutrislot.ui.shared

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class AppBridgeViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    private val shoppingManager = AppBridgeShoppingManager()
    private val calorieJournalManager = AppBridgeCalorieJournalManager(
        store = AppBridgeCalorieJournalStore(
            prefs = prefs,
            key = KEY_CALORIE_JOURNAL
        )
    )
    private val weightManager = AppBridgeWeightManager(
        store = AppBridgeWeightStore(
            prefs = prefs,
            key = KEY_WEIGHT_JOURNAL
        )
    )

    private val _uiState = MutableStateFlow(
        AppBridgeUiState(
            calorieJournalByDate = calorieJournalManager.initialJournal,
            weightEntries = weightManager.initialEntries,
            weightSummary = weightManager.initialSummary
        )
    )
    val uiState: StateFlow<AppBridgeUiState> = _uiState.asStateFlow()

    fun sendProductToShopping(product: LinkedScannedProductUi) {
        _uiState.update { current ->
            shoppingManager.addScannedProduct(current, product)
        }
    }

    fun clearShoppingFeedback() {
        _uiState.update { current ->
            shoppingManager.clearFeedback(current)
        }
    }

    fun sendProductToCalories(product: LinkedScannedProductUi) {
        _uiState.update { current ->
            shoppingManager.queueForCalories(current, product)
        }
    }

    fun consumePendingCalorieProductForDay(dayKey: String) {
        mutateState {
            calorieJournalManager.consumePendingProduct(it, dayKey)
        }
    }

    fun addWeeklyPlanConsumptionToCalories(
        dayKey: String,
        consumptionId: String,
        mealText: String,
        mealSlotLabel: String
    ) {
        _uiState.update { current ->
            calorieJournalManager.addWeeklyPlanConsumption(
                current = current,
                dayKey = dayKey,
                consumptionId = consumptionId,
                mealText = mealText,
                mealSlotLabel = mealSlotLabel
            )
        }
    }

    fun removeWeeklyPlanConsumptionFromCalories(
        consumptionId: String
    ) {
        mutateState {
            calorieJournalManager.removeWeeklyPlanConsumption(it, consumptionId)
        }
    }

    fun setCalorieGoalForDay(
        dayKey: String,
        goalKcal: Int?
    ) {
        _uiState.update { current ->
            calorieJournalManager.setGoalForDay(current, dayKey, goalKcal)
        }
    }

    fun removeCalorieEntry(
        dayKey: String,
        entryId: Long
    ) {
        mutateState {
            calorieJournalManager.removeEntry(it, dayKey, entryId)
        }
    }

    fun resetCalorieDay(dayKey: String) {
        mutateState {
            calorieJournalManager.resetDay(it, dayKey)
        }
    }

    fun addWeightEntry(
        weightKg: Float,
        dateKey: String,
        note: String = ""
    ) {
        mutateState {
            weightManager.addEntry(it, weightKg, dateKey, note)
        }
    }

    fun removeWeightEntry(entryId: Long) {
        mutateState {
            weightManager.removeEntry(it, entryId)
        }
    }

    private inline fun mutateState(
        mutation: (AppBridgeUiState) -> AppBridgeUiState?
    ) {
        val updatedState = mutation(_uiState.value) ?: return
        _uiState.value = updatedState
    }

    companion object {
        private const val PREFS_NAME = "nutrislot_bridge_prefs"
        private const val KEY_CALORIE_JOURNAL = "calorie_journal_json"
        private const val KEY_WEIGHT_JOURNAL = "weight_journal_json"
    }
}
