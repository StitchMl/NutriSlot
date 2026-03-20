package it.lagioiaproductions.nutrislot.ui.weeklyplan

import it.lagioiaproductions.nutrislot.domain.model.MealSlotType
import it.lagioiaproductions.nutrislot.domain.model.SlotDisplayState
import it.lagioiaproductions.nutrislot.domain.model.WeekDay

data class WeeklySlotUi(
    val slotId: String,
    val dayOfWeek: WeekDay,
    val mealSlotType: MealSlotType,
    val originalMealText: String,
    val displayedMealText: String,
    val displayState: SlotDisplayState,
    val isActuallyCompletedThisWeek: Boolean,
    val reassignedFromDayLabel: String? = null,
    val reassignedFromMealSlotLabel: String? = null,
    val nutritionSummary: String? = null,
    val hasCustomizations: Boolean = false
)

data class ReplacementMealOptionUi(
    val sourceSlotId: String,
    val sourceDayLabel: String,
    val sourceMealSlotLabel: String,
    val mealText: String
)

data class ExtraCatalogMealOptionUi(
    val optionId: String,
    val title: String?,
    val mealText: String,
    val sourceLabel: String,
    val tags: List<String>
)

data class SlotActionDialogUi(
    val targetSlotId: String,
    val targetDayLabel: String,
    val targetMealSlotLabel: String,
    val currentDisplayedMealText: String,
    val currentAssignedSourceSlotId: String?,
    val targetDisplayState: SlotDisplayState,
    val isTargetActuallyCompletedThisWeek: Boolean,
    val reassignedFromDayLabel: String? = null,
    val reassignedFromMealSlotLabel: String? = null,
    val canConsumeAsPlanned: Boolean,
    val replacementOptions: List<ReplacementMealOptionUi>,
    val extraCatalogOptions: List<ExtraCatalogMealOptionUi>,
    val mealRuleSummary: String? = null
)

data class EditSlotDialogUi(
    val slotId: String,
    val dayLabel: String,
    val mealSlotLabel: String,
    val mealText: String,
    val nutritionText: String
)

data class WeeklyQuantityChecklistItemUi(
    val id: String,
    val title: String,
    val portionText: String?,
    val targetTimes: Int,
    val consumedTimes: Int
) {
    val isCompleted: Boolean
        get() = targetTimes in 1..consumedTimes
}

data class WeeklyPlanCalorieSyncUi(
    val id: Long,
    val consumptionId: String,
    val mealText: String,
    val mealSlotLabel: String
)

data class WeeklyPlanCalorieUndoUi(
    val id: Long,
    val consumptionId: String
)

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
) {
    val isEmpty: Boolean
        get() = hasLoadedOnce && slots.isEmpty() && errorMessage == null

    val orderedCalendarDays: List<WeekDay>
        get() = WeekDay.orderedValues()
}