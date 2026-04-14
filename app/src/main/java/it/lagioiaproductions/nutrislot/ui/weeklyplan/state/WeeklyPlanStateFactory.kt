package it.lagioiaproductions.nutrislot.ui.weeklyplan.state

import it.lagioiaproductions.nutrislot.domain.model.WeekDay
import it.lagioiaproductions.nutrislot.domain.model.WeeklyPlanSnapshot
import it.lagioiaproductions.nutrislot.ui.weeklyplan.checklist.WeeklyChecklistHydrationSnapshot
import it.lagioiaproductions.nutrislot.ui.weeklyplan.edit.WeeklyPlanCustomizationManager
import it.lagioiaproductions.nutrislot.ui.weeklyplan.slot.EditSlotDialogUi
import it.lagioiaproductions.nutrislot.ui.weeklyplan.slot.SlotActionDialogUi

internal data class WeeklyPlanSnapshotStatePayload(
    val actionMessage: String? = null,
    val actionErrorMessage: String? = null,
    val isApplyingSlotAction: Boolean = false,
    val slotActionDialog: SlotActionDialogUi? = null,
    val currentWeekReferenceDay: WeekDay? = null,
    val selectedCalendarDay: WeekDay? = null,
    val pendingCalorieSyncEvent: WeeklyPlanCalorieSyncUi? = null,
    val pendingCalorieUndoEvent: WeeklyPlanCalorieUndoUi? = null,
    val editSlotDialog: EditSlotDialogUi? = null
)

internal class WeeklyPlanStateFactory(
    private val customizationManager: WeeklyPlanCustomizationManager
) {

    fun initialState(
        showConsumedSlots: Boolean
    ): WeeklyPlanUiState {
        return WeeklyPlanUiState(
            isLoading = true,
            showConsumedSlotsInCalendar = showConsumedSlots,
            pendingCalorieUndoEvent = null
        )
    }

    fun loadingState(
        previousState: WeeklyPlanUiState
    ): WeeklyPlanUiState {
        return previousState.copy(
            isLoading = true,
            errorMessage = null,
            editSlotDialog = null
        )
    }

    fun emptyLoadedState(
        previousState: WeeklyPlanUiState,
        referenceDay: WeekDay
    ): WeeklyPlanUiState {
        return WeeklyPlanUiState(
            isLoading = false,
            hasLoadedOnce = true,
            currentWeekReferenceDay = referenceDay,
            selectedCalendarDay = referenceDay,
            showConsumedSlotsInCalendar = previousState.showConsumedSlotsInCalendar,
            slots = emptyList(),
            errorMessage = null,
            pendingCalorieSyncEvent = null,
            pendingCalorieUndoEvent = null
        )
    }

    fun errorState(
        previousState: WeeklyPlanUiState,
        referenceDay: WeekDay,
        message: String
    ): WeeklyPlanUiState {
        return WeeklyPlanUiState(
            isLoading = false,
            hasLoadedOnce = true,
            currentWeekReferenceDay = referenceDay,
            selectedCalendarDay = referenceDay,
            showConsumedSlotsInCalendar = previousState.showConsumedSlotsInCalendar,
            slots = emptyList(),
            errorMessage = message,
            pendingCalorieSyncEvent = null,
            pendingCalorieUndoEvent = null
        )
    }

    fun actionInProgress(
        previousState: WeeklyPlanUiState
    ): WeeklyPlanUiState {
        return previousState.copy(
            isApplyingSlotAction = true,
            actionErrorMessage = null,
            actionMessage = null
        )
    }

    fun actionFailure(
        previousState: WeeklyPlanUiState,
        throwable: Throwable,
        fallbackMessage: String
    ): WeeklyPlanUiState {
        return previousState.copy(
            isApplyingSlotAction = false,
            actionErrorMessage = throwable.message ?: fallbackMessage
        )
    }

    fun customizedState(
        snapshot: WeeklyPlanSnapshot,
        previousState: WeeklyPlanUiState,
        actionMessage: String,
        hydrationSnapshot: WeeklyChecklistHydrationSnapshot? = null
    ): WeeklyPlanUiState {
        return customizationManager.applyDecorations(
            snapshot = snapshot,
            state = previousState.copy(
                editSlotDialog = null,
                actionMessage = actionMessage,
                actionErrorMessage = null
            ),
            hydrationSnapshot = hydrationSnapshot
        )
    }

    fun snapshotState(
        snapshot: WeeklyPlanSnapshot,
        previousState: WeeklyPlanUiState,
        payload: WeeklyPlanSnapshotStatePayload = WeeklyPlanSnapshotStatePayload(),
        hydrationSnapshot: WeeklyChecklistHydrationSnapshot? = null
    ): WeeklyPlanUiState {
        val baseState = snapshot.toUiState(
            customizationManager = customizationManager,
            actionMessage = payload.actionMessage,
            actionErrorMessage = payload.actionErrorMessage,
            isApplyingSlotAction = payload.isApplyingSlotAction,
            slotActionDialog = payload.slotActionDialog,
            currentWeekReferenceDay = payload.currentWeekReferenceDay
                ?: previousState.currentWeekReferenceDay,
            selectedCalendarDay = payload.selectedCalendarDay
                ?: previousState.selectedCalendarDay,
            showConsumedSlotsInCalendar = previousState.showConsumedSlotsInCalendar
        ).copy(
            pendingCalorieSyncEvent = payload.pendingCalorieSyncEvent,
            pendingCalorieUndoEvent = payload.pendingCalorieUndoEvent,
            editSlotDialog = payload.editSlotDialog
        )

        return customizationManager.applyDecorations(
            snapshot = snapshot,
            state = baseState,
            hydrationSnapshot = hydrationSnapshot
        )
    }
}
