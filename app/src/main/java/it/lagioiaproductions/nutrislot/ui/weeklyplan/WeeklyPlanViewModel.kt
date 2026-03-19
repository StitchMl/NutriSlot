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

    private val _uiState = MutableStateFlow(
        WeeklyPlanUiState(
            isLoading = true
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
                        errorMessage = null
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
                        ?: "Errore sconosciuto durante il caricamento del piano."
                )
            }
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
            successMessage = "Pasto segnato come completato nella settimana corrente."
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

    fun undoCompletedMeal() {
        val snapshot = currentSnapshot ?: return
        val dialog = _uiState.value.slotActionDialog ?: return

        applyUndoConsumption(
            planId = snapshot.plan.id,
            targetSlotId = dialog.targetSlotId,
            successMessage = "Completamento annullato."
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

    private fun applyUndoConsumption(
        planId: String,
        targetSlotId: String,
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
                    repository.undoMealConsumption(
                        planId = planId,
                        targetSlotId = targetSlotId
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
                )
            }.onFailure { throwable ->
                _uiState.update { state ->
                    state.copy(
                        isApplyingSlotAction = false,
                        actionErrorMessage = throwable.message
                            ?: "Errore sconosciuto durante l'annullamento del completamento."
                    )
                }
            }
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
                    repository.recordMealConsumption(
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