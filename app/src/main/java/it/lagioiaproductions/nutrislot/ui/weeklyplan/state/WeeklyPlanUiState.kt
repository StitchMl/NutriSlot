package it.lagioiaproductions.nutrislot.ui.weeklyplan.state

import it.lagioiaproductions.nutrislot.domain.model.WeekDay
import it.lagioiaproductions.nutrislot.ui.weeklyplan.checklist.WeeklyQuantityChecklistItemUi
import it.lagioiaproductions.nutrislot.ui.weeklyplan.slot.EditSlotDialogUi
import it.lagioiaproductions.nutrislot.ui.weeklyplan.slot.SlotActionDialogUi
import it.lagioiaproductions.nutrislot.ui.weeklyplan.slot.WeeklySlotUi

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
