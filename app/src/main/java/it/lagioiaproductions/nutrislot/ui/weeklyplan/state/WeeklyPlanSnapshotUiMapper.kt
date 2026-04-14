package it.lagioiaproductions.nutrislot.ui.weeklyplan

import it.lagioiaproductions.nutrislot.domain.model.WeekDay
import it.lagioiaproductions.nutrislot.domain.model.WeeklyPlanSnapshot

internal fun WeeklyPlanSnapshot.toUiState(
    actionMessage: String?,
    actionErrorMessage: String?,
    isApplyingSlotAction: Boolean,
    slotActionDialog: SlotActionDialogUi?,
    currentWeekReferenceDay: WeekDay,
    selectedCalendarDay: WeekDay,
    showConsumedSlotsInCalendar: Boolean
): WeeklyPlanUiState {
    return WeeklyPlanUiState(
        isLoading = false,
        hasLoadedOnce = true,
        planId = plan.id,
        planTitle = plan.title,
        sourceFileName = plan.sourceFileName,
        currentWeekReferenceDay = currentWeekReferenceDay,
        selectedCalendarDay = selectedCalendarDay,
        showConsumedSlotsInCalendar = showConsumedSlotsInCalendar,
        slots = buildWeeklySlotUis(this),
        slotActionDialog = slotActionDialog,
        isApplyingSlotAction = isApplyingSlotAction,
        actionMessage = actionMessage,
        actionErrorMessage = actionErrorMessage,
        errorMessage = null
    )
}
