package it.lagioiaproductions.nutrislot.ui.weeklyplan

import it.lagioiaproductions.nutrislot.domain.model.WeekDay

data class WeeklyPlanUiState(
    val isLoading: Boolean = false,
    val hasLoadedOnce: Boolean = false,
    val planId: String? = null,
    val planTitle: String? = null,
    val sourceFileName: String? = null,
    val currentWeekReferenceDay: WeekDay = currentWeekDay(),
    val selectedCalendarDay: WeekDay = currentWeekDay(),
    val showConsumedSlotsInCalendar: Boolean = false,
    val slots: List<WeeklySlotUi> = emptyList(),
    val weeklyQuantityChecklist: List<WeeklyQuantityChecklistItemUi> = emptyList(),
    val slotActionDialog: SlotActionDialogUi? = null,
    val editSlotDialog: EditSlotDialogUi? = null,
    val isApplyingSlotAction: Boolean = false,
    val actionMessage: String? = null,
    val actionErrorMessage: String? = null,
    val errorMessage: String? = null,
    val pendingCalorieSyncEvent: WeeklyPlanCalorieSyncUi? = null,
    val pendingCalorieUndoEvent: WeeklyPlanCalorieUndoUi? = null
)
