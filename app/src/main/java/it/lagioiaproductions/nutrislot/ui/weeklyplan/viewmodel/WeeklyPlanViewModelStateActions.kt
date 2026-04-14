package it.lagioiaproductions.nutrislot.ui.weeklyplan.viewmodel

import androidx.core.content.edit
import androidx.lifecycle.viewModelScope
import it.lagioiaproductions.nutrislot.domain.model.WeekDay
import it.lagioiaproductions.nutrislot.domain.model.WeeklyPlanSnapshot
import it.lagioiaproductions.nutrislot.ui.weeklyplan.WeeklyPlanPreferences
import it.lagioiaproductions.nutrislot.ui.weeklyplan.WeeklyPlanSnapshotStatePayload
import it.lagioiaproductions.nutrislot.ui.weeklyplan.WeeklyPlanViewModel
import it.lagioiaproductions.nutrislot.ui.weeklyplan.buildLoadFailureState
import it.lagioiaproductions.nutrislot.ui.weeklyplan.buildLoadResultState
import it.lagioiaproductions.nutrislot.ui.weeklyplan.currentWeekDay
import it.lagioiaproductions.nutrislot.ui.weeklyplan.toChecklistHydrationSnapshot
import it.lagioiaproductions.nutrislot.widget.MealCalendarWidgetProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal fun WeeklyPlanViewModel.loadLatestPlanInternal() {
    viewModelScope.launch {
        mutableUiState.update { state ->
            stateFactory.loadingState(state)
        }

        runCatching {
            planOperationMutex.withLock {
                withContext(Dispatchers.IO) {
                    waterRepository.ensureCurrentDay()
                    mutationExecutor.loadLatestSnapshot() to waterRepository.preferencesFlow
                        .first()
                        .toChecklistHydrationSnapshot()
                }
            }
        }.onSuccess { (snapshot, latestHydrationSnapshot) ->
            currentSnapshot = snapshot
            hydrationSnapshot = latestHydrationSnapshot

            val today = currentWeekDay()
            mutableUiState.value = buildLoadResultState(
                stateFactory = stateFactory,
                previousState = mutableUiState.value,
                snapshot = snapshot,
                referenceDay = today,
                hydrationSnapshot = latestHydrationSnapshot
            )
        }.onFailure { throwable ->
            currentSnapshot = null

            mutableUiState.value = buildLoadFailureState(
                stateFactory = stateFactory,
                previousState = mutableUiState.value,
                referenceDay = currentWeekDay(),
                throwable = throwable
            )
        }
    }
}

internal fun WeeklyPlanViewModel.consumePendingCalorieUndoEventInternal() {
    mutableUiState.update { state ->
        state.copy(pendingCalorieUndoEvent = null)
    }
}

internal fun WeeklyPlanViewModel.selectCalendarDayInternal(day: WeekDay) {
    mutableUiState.update { state ->
        state.copy(selectedCalendarDay = day)
    }
}

internal fun WeeklyPlanViewModel.toggleConsumedSlotsVisibilityInternal() {
    mutableUiState.update { state ->
        val nextValue = !state.showConsumedSlotsInCalendar
        preferences.edit {
            putBoolean(WeeklyPlanPreferences.PREF_SHOW_CONSUMED_SLOTS, nextValue)
        }

        state.copy(showConsumedSlotsInCalendar = nextValue)
    }
}

internal fun WeeklyPlanViewModel.consumePendingCalorieSyncEventInternal() {
    mutableUiState.update { state ->
        state.copy(pendingCalorieSyncEvent = null)
    }
}

internal fun WeeklyPlanViewModel.applyCustomizationUpdateInternal(
    snapshot: WeeklyPlanSnapshot,
    actionMessage: String
) {
    mutableUiState.update { state ->
        stateFactory.customizedState(
            snapshot = snapshot,
            previousState = state,
            actionMessage = actionMessage,
            hydrationSnapshot = hydrationSnapshot
        )
    }

    MealCalendarWidgetProvider.refresh(getApplication())
}

internal fun WeeklyPlanViewModel.applySnapshotUpdateInternal(
    snapshot: WeeklyPlanSnapshot,
    payload: WeeklyPlanSnapshotStatePayload
) {
    currentSnapshot = snapshot
    mutableUiState.value = stateFactory.snapshotState(
        snapshot = snapshot,
        previousState = mutableUiState.value,
        payload = payload,
        hydrationSnapshot = hydrationSnapshot
    )

    MealCalendarWidgetProvider.refresh(getApplication())
}

internal fun <T> WeeklyPlanViewModel.executePlanMutationInternal(
    fallbackErrorMessage: String,
    mutation: suspend () -> T,
    onSuccess: (T) -> Unit
) {
    viewModelScope.launch {
        mutableUiState.update { state ->
            stateFactory.actionInProgress(state)
        }

        runCatching {
            planOperationMutex.withLock {
                withContext(Dispatchers.IO) {
                    mutation()
                }
            }
        }.onSuccess { result ->
            onSuccess(result)
        }.onFailure { throwable ->
            mutableUiState.update { state ->
                stateFactory.actionFailure(
                    previousState = state,
                    throwable = throwable,
                    fallbackMessage = fallbackErrorMessage
                )
            }
        }
    }
}

internal fun WeeklyPlanViewModel.observeHydrationPreferencesInternal() {
    viewModelScope.launch {
        runCatching {
            withContext(Dispatchers.IO) {
                waterRepository.ensureCurrentDay()
            }
        }

        waterRepository.preferencesFlow.collect { storedPreferences ->
            hydrationSnapshot = storedPreferences.toChecklistHydrationSnapshot()
            rebuildCurrentStateWithLatestHydrationInternal()
        }
    }
}

internal fun WeeklyPlanViewModel.rebuildCurrentStateWithLatestHydrationInternal() {
    val snapshot = currentSnapshot ?: return

    mutableUiState.update { state ->
        stateFactory.snapshotState(
            snapshot = snapshot,
            previousState = state,
            payload = WeeklyPlanSnapshotStatePayload(
                actionMessage = state.actionMessage,
                actionErrorMessage = state.actionErrorMessage,
                isApplyingSlotAction = state.isApplyingSlotAction,
                slotActionDialog = state.slotActionDialog,
                currentWeekReferenceDay = state.currentWeekReferenceDay,
                selectedCalendarDay = state.selectedCalendarDay,
                pendingCalorieSyncEvent = state.pendingCalorieSyncEvent,
                pendingCalorieUndoEvent = state.pendingCalorieUndoEvent,
                editSlotDialog = state.editSlotDialog
            ),
            hydrationSnapshot = hydrationSnapshot
        )
    }
}
