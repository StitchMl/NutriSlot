package it.lagioiaproductions.nutrislot.ui.weeklyplan.state

import it.lagioiaproductions.nutrislot.domain.model.WeekDay
import it.lagioiaproductions.nutrislot.domain.model.WeeklyPlanSnapshot
import it.lagioiaproductions.nutrislot.ui.weeklyplan.edit.WeeklyPlanCustomizationManager
import it.lagioiaproductions.nutrislot.ui.weeklyplan.slot.SlotActionDialogUi
import it.lagioiaproductions.nutrislot.ui.weeklyplan.slot.buildWeeklySlotUis

internal fun WeeklyPlanSnapshot.toUiState(
    customizationManager: WeeklyPlanCustomizationManager,
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
        slots = buildWeeklySlotUis(this, customizationManager),
        slotActionDialog = slotActionDialog,
        isApplyingSlotAction = isApplyingSlotAction,
        actionMessage = actionMessage,
        actionErrorMessage = actionErrorMessage,
        errorMessage = null
    )
}
