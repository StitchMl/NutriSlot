@file:Suppress("SameParameterValue")

package it.lagioiaproductions.nutrislot.ui.weeklyplan

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import it.lagioiaproductions.nutrislot.data.local.room.NutriSlotDatabase
import it.lagioiaproductions.nutrislot.data.repository.WeeklyPlanRepository
import it.lagioiaproductions.nutrislot.domain.model.WeekDay
import it.lagioiaproductions.nutrislot.domain.model.WeeklyPlanSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.core.content.edit

class WeeklyPlanViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository = WeeklyPlanRepository(
        weeklyPlanDao = NutriSlotDatabase
            .getInstance(application)
            .weeklyPlanDao()
    )

    private val preferences = application.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )

    private var currentSnapshot: WeeklyPlanSnapshot? = null
    private var nextCalorieSyncEventId: Long = 1L
    private var nextCalorieUndoEventId: Long = 1L

    private val _uiState = MutableStateFlow(
        WeeklyPlanUiState(
            isLoading = true,
            showConsumedSlotsInCalendar = preferences.getBoolean(
                PREF_SHOW_CONSUMED_SLOTS,
                false
            ),
            pendingCalorieUndoEvent = null
        )
    )
    val uiState: StateFlow<WeeklyPlanUiState> = _uiState.asStateFlow()

    fun loadLatestPlan() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                    editSlotDialog = null
                )
            }

            runCatching {
                withContext(Dispatchers.IO) {
                    repository.getLatestWeeklyPlanSnapshot()
                }
            }.onSuccess { snapshot ->
                currentSnapshot = snapshot

                val today = currentWeekDay()
                val currentShowConsumed = _uiState.value.showConsumedSlotsInCalendar
                val currentSelectedDay = _uiState.value.selectedCalendarDay

                if (snapshot == null) {
                    _uiState.value = WeeklyPlanUiState(
                        isLoading = false,
                        hasLoadedOnce = true,
                        currentWeekReferenceDay = today,
                        selectedCalendarDay = today,
                        showConsumedSlotsInCalendar = currentShowConsumed,
                        slots = emptyList(),
                        errorMessage = null,
                        pendingCalorieSyncEvent = null,
                        pendingCalorieUndoEvent = null
                    )
                } else {
                    val baseState = snapshot.toUiState(
                        actionMessage = null,
                        actionErrorMessage = null,
                        isApplyingSlotAction = false,
                        slotActionDialog = null,
                        currentWeekReferenceDay = today,
                        selectedCalendarDay = currentSelectedDay,
                        showConsumedSlotsInCalendar = currentShowConsumed
                    ).copy(
                        pendingCalorieSyncEvent = null,
                        editSlotDialog = null
                    )

                    _uiState.value = applyManualDecorations(
                        snapshot = snapshot,
                        state = baseState
                    )
                }
            }.onFailure { throwable ->
                currentSnapshot = null

                _uiState.value = WeeklyPlanUiState(
                    isLoading = false,
                    hasLoadedOnce = true,
                    currentWeekReferenceDay = currentWeekDay(),
                    selectedCalendarDay = currentWeekDay(),
                    showConsumedSlotsInCalendar = _uiState.value.showConsumedSlotsInCalendar,
                    slots = emptyList(),
                    errorMessage = throwable.message
                        ?: "Errore sconosciuto durante il caricamento del piano.",
                    pendingCalorieSyncEvent = null,
                    pendingCalorieUndoEvent = null
                )
            }
        }
    }

    fun consumePendingCalorieUndoEvent() {
        _uiState.update { state ->
            state.copy(pendingCalorieUndoEvent = null)
        }
    }

    fun selectCalendarDay(day: WeekDay) {
        _uiState.update { state ->
            state.copy(selectedCalendarDay = day)
        }
    }

    fun toggleConsumedSlotsVisibility() {
        _uiState.update { state ->
            val nextValue = !state.showConsumedSlotsInCalendar
            preferences.edit {
                putBoolean(PREF_SHOW_CONSUMED_SLOTS, nextValue)
            }

            state.copy(showConsumedSlotsInCalendar = nextValue)
        }
    }

    fun openSlotAction(slotId: String) {
        val snapshot = currentSnapshot ?: return
        val targetUi = _uiState.value.slots
            .firstOrNull { it.slotId == slotId }
            ?: buildWeeklySlotUis(snapshot)
                .firstOrNull { it.slotId == slotId }
            ?: return

        val dialog = buildSlotActionDialog(
            snapshot = snapshot,
            targetUi = targetUi
        )

        _uiState.update { state ->
            state.copy(
                slotActionDialog = dialog,
                editSlotDialog = null,
                actionMessage = null,
                actionErrorMessage = null
            )
        }
    }

    fun dismissSlotAction() {
        _uiState.update { state ->
            state.copy(
                slotActionDialog = null,
                isApplyingSlotAction = false
            )
        }
    }

    fun openEditSlot(slotId: String) {
        val slotUi = _uiState.value.slots.firstOrNull { it.slotId == slotId } ?: return

        _uiState.update { state ->
            state.copy(
                slotActionDialog = null,
                editSlotDialog = EditSlotDialogUi(
                    slotId = slotUi.slotId,
                    dayLabel = slotUi.dayOfWeek.displayName,
                    mealSlotLabel = slotUi.mealSlotType.displayName,
                    mealText = slotUi.displayedMealText,
                    nutritionText = slotUi.nutritionSummary.orEmpty()
                )
            )
        }
    }

    fun dismissEditSlot() {
        _uiState.update { state ->
            state.copy(editSlotDialog = null)
        }
    }

    fun saveEditSlot(
        mealText: String,
        nutritionText: String
    ) {
        val snapshot = currentSnapshot ?: return
        val dialog = _uiState.value.editSlotDialog ?: return
        val planId = snapshot.plan.id

        preferences.edit {
            putString(slotMealPreferenceKey(planId, dialog.slotId), mealText.trim())
                .putString(slotNutritionPreferenceKey(planId, dialog.slotId), nutritionText.trim())
        }

        _uiState.update { state ->
            applyManualDecorations(
                snapshot = snapshot,
                state = state.copy(
                    editSlotDialog = null,
                    actionMessage = "Box aggiornato.",
                    actionErrorMessage = null
                )
            )
        }
    }

    fun resetEditSlot() {
        val snapshot = currentSnapshot ?: return
        val dialog = _uiState.value.editSlotDialog ?: return
        val planId = snapshot.plan.id

        preferences.edit {
            remove(slotMealPreferenceKey(planId, dialog.slotId))
                .remove(slotNutritionPreferenceKey(planId, dialog.slotId))
        }

        _uiState.update { state ->
            applyManualDecorations(
                snapshot = snapshot,
                state = state.copy(
                    editSlotDialog = null,
                    actionMessage = "Personalizzazione rimossa.",
                    actionErrorMessage = null
                )
            )
        }
    }

    fun consumeAsPlanned() {
        val snapshot = currentSnapshot ?: return
        val dialog = _uiState.value.slotActionDialog ?: return
        val assignedSourceSlotId = dialog.currentAssignedSourceSlotId ?: return

        applyConsumption(
            planId = snapshot.plan.id,
            targetSlotId = dialog.targetSlotId,
            sourceSlotId = assignedSourceSlotId,
            successMessage = "Pasto segnato come completato nella settimana corrente.",
            consumedMealText = dialog.currentDisplayedMealText,
            consumedMealSlotLabel = dialog.targetMealSlotLabel
        )
    }

    fun consumeReplacement(sourceSlotId: String) {
        val snapshot = currentSnapshot ?: return
        val dialog = _uiState.value.slotActionDialog ?: return

        applyReplacementAssignment(
            planId = snapshot.plan.id,
            targetSlotId = dialog.targetSlotId,
            sourceSlotId = sourceSlotId,
            successMessage = "Pasto riassegnato. Rimane nello slot finché non lo segni come completato."
        )
    }

    fun selectExtraCatalogOption(optionId: String) {
        val snapshot = currentSnapshot ?: return
        val dialog = _uiState.value.slotActionDialog ?: return

        applyCatalogOptionAssignment(
            planId = snapshot.plan.id,
            targetSlotId = dialog.targetSlotId,
            optionId = optionId,
            successMessage = "Opzione extra assegnata allo slot."
        )
    }

    fun undoCompletedMeal() {
        val snapshot = currentSnapshot ?: return
        val dialog = _uiState.value.slotActionDialog ?: return

        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(
                    isApplyingSlotAction = true,
                    actionErrorMessage = null,
                    actionMessage = null
                )
            }

            runCatching {
                withContext(Dispatchers.IO) {
                    val removedConsumptionId = repository.undoMealConsumption(
                        planId = snapshot.plan.id,
                        targetSlotId = dialog.targetSlotId
                    )

                    val updatedSnapshot = repository.getWeeklyPlanSnapshot(snapshot.plan.id)
                        ?: throw IllegalStateException(
                            "Impossibile ricaricare il piano dopo l'annullamento."
                        )

                    removedConsumptionId to updatedSnapshot
                }
            }.onSuccess { (removedConsumptionId, updatedSnapshot) ->
                currentSnapshot = updatedSnapshot

                val baseState = updatedSnapshot.toUiState(
                    actionMessage = "Consumo annullato con successo.",
                    actionErrorMessage = null,
                    isApplyingSlotAction = false,
                    slotActionDialog = null,
                    currentWeekReferenceDay = _uiState.value.currentWeekReferenceDay,
                    selectedCalendarDay = _uiState.value.selectedCalendarDay,
                    showConsumedSlotsInCalendar = _uiState.value.showConsumedSlotsInCalendar
                ).copy(
                    pendingCalorieSyncEvent = null,
                    pendingCalorieUndoEvent = WeeklyPlanCalorieUndoUi(
                        id = nextCalorieUndoEventId++,
                        consumptionId = removedConsumptionId
                    ),
                    editSlotDialog = null
                )

                _uiState.value = applyManualDecorations(
                    snapshot = updatedSnapshot,
                    state = baseState
                )
            }.onFailure { throwable ->
                _uiState.update { state ->
                    state.copy(
                        isApplyingSlotAction = false,
                        actionErrorMessage = throwable.message
                            ?: "Errore sconosciuto durante l'annullamento del consumo."
                    )
                }
            }
        }
    }

    fun consumePendingCalorieSyncEvent() {
        _uiState.update { state ->
            state.copy(pendingCalorieSyncEvent = null)
        }
    }

    private fun applyReplacementAssignment(
        planId: String,
        targetSlotId: String,
        sourceSlotId: String,
        successMessage: String
    ) {
        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(
                    isApplyingSlotAction = true,
                    actionErrorMessage = null,
                    actionMessage = null
                )
            }

            runCatching {
                withContext(Dispatchers.IO) {
                    repository.assignMealToSlot(
                        planId = planId,
                        targetSlotId = targetSlotId,
                        sourceSlotId = sourceSlotId
                    )

                    repository.getWeeklyPlanSnapshot(planId)
                        ?: throw IllegalStateException("Impossibile ricaricare il piano dopo l'aggiornamento.")
                }
            }.onSuccess { updatedSnapshot ->
                currentSnapshot = updatedSnapshot

                val baseState = updatedSnapshot.toUiState(
                    actionMessage = successMessage,
                    actionErrorMessage = null,
                    isApplyingSlotAction = false,
                    slotActionDialog = null,
                    currentWeekReferenceDay = _uiState.value.currentWeekReferenceDay,
                    selectedCalendarDay = _uiState.value.selectedCalendarDay,
                    showConsumedSlotsInCalendar = _uiState.value.showConsumedSlotsInCalendar
                ).copy(
                    pendingCalorieSyncEvent = null,
                    editSlotDialog = null
                )

                _uiState.value = applyManualDecorations(
                    snapshot = updatedSnapshot,
                    state = baseState
                )
            }.onFailure { throwable ->
                _uiState.update { state ->
                    state.copy(
                        isApplyingSlotAction = false,
                        actionErrorMessage = throwable.message
                            ?: "Errore sconosciuto durante l'aggiornamento dello slot."
                    )
                }
            }
        }
    }

    private fun applyConsumption(
        planId: String,
        targetSlotId: String,
        sourceSlotId: String,
        successMessage: String,
        consumedMealText: String,
        consumedMealSlotLabel: String
    ) {
        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(
                    isApplyingSlotAction = true,
                    actionErrorMessage = null,
                    actionMessage = null
                )
            }

            runCatching {
                withContext(Dispatchers.IO) {
                    val newConsumption = repository.recordMealConsumption(
                        planId = planId,
                        targetSlotId = targetSlotId,
                        sourceSlotId = sourceSlotId
                    )

                    val updatedSnapshot = repository.getWeeklyPlanSnapshot(planId)
                        ?: throw IllegalStateException("Impossibile ricaricare il piano dopo l'aggiornamento.")

                    newConsumption to updatedSnapshot
                }
            }.onSuccess { (newConsumption, updatedSnapshot) ->
                currentSnapshot = updatedSnapshot

                val baseState = updatedSnapshot.toUiState(
                    actionMessage = successMessage,
                    actionErrorMessage = null,
                    isApplyingSlotAction = false,
                    slotActionDialog = null,
                    currentWeekReferenceDay = _uiState.value.currentWeekReferenceDay,
                    selectedCalendarDay = _uiState.value.selectedCalendarDay,
                    showConsumedSlotsInCalendar = _uiState.value.showConsumedSlotsInCalendar
                ).copy(
                    pendingCalorieSyncEvent = WeeklyPlanCalorieSyncUi(
                        id = nextCalorieSyncEventId++,
                        consumptionId = newConsumption.id,
                        mealText = consumedMealText,
                        mealSlotLabel = consumedMealSlotLabel
                    ),
                    pendingCalorieUndoEvent = null,
                    editSlotDialog = null
                )

                _uiState.value = applyManualDecorations(
                    snapshot = updatedSnapshot,
                    state = baseState
                )
            }.onFailure { throwable ->
                _uiState.update { state ->
                    state.copy(
                        isApplyingSlotAction = false,
                        actionErrorMessage = throwable.message
                            ?: "Errore sconosciuto durante l'aggiornamento dello slot."
                    )
                }
            }
        }
    }

    private fun applyCatalogOptionAssignment(
        planId: String,
        targetSlotId: String,
        optionId: String,
        successMessage: String
    ) {
        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(
                    isApplyingSlotAction = true,
                    actionErrorMessage = null,
                    actionMessage = null
                )
            }

            runCatching {
                withContext(Dispatchers.IO) {
                    repository.assignCatalogOptionToSlot(
                        planId = planId,
                        targetSlotId = targetSlotId,
                        optionId = optionId
                    )

                    repository.getWeeklyPlanSnapshot(planId)
                        ?: throw IllegalStateException(
                            "Impossibile ricaricare il piano dopo l'aggiornamento."
                        )
                }
            }.onSuccess { updatedSnapshot ->
                currentSnapshot = updatedSnapshot

                val baseState = updatedSnapshot.toUiState(
                    actionMessage = successMessage,
                    actionErrorMessage = null,
                    isApplyingSlotAction = false,
                    slotActionDialog = null,
                    currentWeekReferenceDay = _uiState.value.currentWeekReferenceDay,
                    selectedCalendarDay = _uiState.value.selectedCalendarDay,
                    showConsumedSlotsInCalendar = _uiState.value.showConsumedSlotsInCalendar
                ).copy(
                    pendingCalorieSyncEvent = null,
                    editSlotDialog = null
                )

                _uiState.value = applyManualDecorations(
                    snapshot = updatedSnapshot,
                    state = baseState
                )
            }.onFailure { throwable ->
                _uiState.update { state ->
                    state.copy(
                        isApplyingSlotAction = false,
                        actionErrorMessage = throwable.message
                            ?: "Errore sconosciuto durante l'assegnazione dell'opzione extra."
                    )
                }
            }
        }
    }

    private fun applyManualDecorations(
        snapshot: WeeklyPlanSnapshot,
        state: WeeklyPlanUiState
    ): WeeklyPlanUiState {
        val nutritionSummaryBySlotType = snapshot.mealRules
            .groupBy { it.mealSlotType }
            .mapValues { (_, rules) ->
                rules.firstOrNull()
                    ?.requiredComponents
                    ?.takeIf { it.isNotEmpty() }
                    ?.joinToString(separator = " + ")
            }

        val decoratedSlots = state.slots.map { slot ->
            val customMealText = readStoredPreference(
                key = slotMealPreferenceKey(snapshot.plan.id, slot.slotId)
            )
            val customNutritionText = readStoredPreference(
                key = slotNutritionPreferenceKey(snapshot.plan.id, slot.slotId)
            )

            slot.copy(
                displayedMealText = customMealText ?: slot.displayedMealText,
                nutritionSummary = customNutritionText
                    ?: nutritionSummaryBySlotType[slot.mealSlotType],
                hasCustomizations = customMealText != null || customNutritionText != null
            )
        }

        return state.copy(slots = decoratedSlots)
    }

    private fun readStoredPreference(key: String): String? {
        return if (preferences.contains(key)) {
            preferences.getString(key, "") ?: ""
        } else {
            null
        }
    }

    private fun slotMealPreferenceKey(
        planId: String,
        slotId: String
    ): String {
        return "${PREF_SLOT_MEAL_PREFIX}_${planId}_$slotId"
    }

    private fun slotNutritionPreferenceKey(
        planId: String,
        slotId: String
    ): String {
        return "${PREF_SLOT_NUTRITION_PREFIX}_${planId}_$slotId"
    }

    private companion object {
        const val PREFERENCES_NAME = "weekly_plan_preferences"
        const val PREF_SHOW_CONSUMED_SLOTS = "show_consumed_slots_in_calendar"
        const val PREF_SLOT_MEAL_PREFIX = "slot_custom_meal"
        const val PREF_SLOT_NUTRITION_PREFIX = "slot_custom_nutrition"
    }
}