@file:Suppress("SameParameterValue")

package it.lagioiaproductions.nutrislot.ui.weeklyplan

import android.app.Application
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

class WeeklyPlanViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository = WeeklyPlanRepository(
        weeklyPlanDao = NutriSlotDatabase
            .getInstance(application)
            .weeklyPlanDao()
    )

    private var currentSnapshot: WeeklyPlanSnapshot? = null
    private var nextCalorieSyncEventId: Long = 1L
    private var nextCalorieUndoEventId: Long = 1L

    private val _uiState = MutableStateFlow(
        WeeklyPlanUiState(
            isLoading = true,
            pendingCalorieUndoEvent = null
        )
    )
    val uiState: StateFlow<WeeklyPlanUiState> = _uiState.asStateFlow()

    fun loadLatestPlan() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null
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
                    _uiState.value = snapshot.toUiState(
                        actionMessage = null,
                        actionErrorMessage = null,
                        isApplyingSlotAction = false,
                        slotActionDialog = null,
                        currentWeekReferenceDay = today,
                        selectedCalendarDay = currentSelectedDay,
                        showConsumedSlotsInCalendar = currentShowConsumed
                    ).copy(
                        pendingCalorieSyncEvent = null
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
            state.copy(
                showConsumedSlotsInCalendar = !state.showConsumedSlotsInCalendar
            )
        }
    }

    fun openSlotAction(slotId: String) {
        val snapshot = currentSnapshot ?: return
        val targetUi = buildWeeklySlotUis(snapshot)
            .firstOrNull { it.slotId == slotId }
            ?: return

        val dialog = buildSlotActionDialog(
            snapshot = snapshot,
            targetUi = targetUi
        )

        _uiState.update { state ->
            state.copy(
                slotActionDialog = dialog,
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

                _uiState.value = updatedSnapshot.toUiState(
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
                    )
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

                _uiState.value = updatedSnapshot.toUiState(
                    actionMessage = successMessage,
                    actionErrorMessage = null,
                    isApplyingSlotAction = false,
                    slotActionDialog = null,
                    currentWeekReferenceDay = _uiState.value.currentWeekReferenceDay,
                    selectedCalendarDay = _uiState.value.selectedCalendarDay,
                    showConsumedSlotsInCalendar = _uiState.value.showConsumedSlotsInCalendar
                ).copy(
                    pendingCalorieSyncEvent = null
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

                _uiState.value = updatedSnapshot.toUiState(
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
                    pendingCalorieUndoEvent = null
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

                _uiState.value = updatedSnapshot.toUiState(
                    actionMessage = successMessage,
                    actionErrorMessage = null,
                    isApplyingSlotAction = false,
                    slotActionDialog = null,
                    currentWeekReferenceDay = _uiState.value.currentWeekReferenceDay,
                    selectedCalendarDay = _uiState.value.selectedCalendarDay,
                    showConsumedSlotsInCalendar = _uiState.value.showConsumedSlotsInCalendar
                ).copy(
                    pendingCalorieSyncEvent = null
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
}