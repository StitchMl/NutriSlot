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
    val reassignedFromMealSlotLabel: String? = null
)

data class ReplacementMealOptionUi(
    val sourceSlotId: String,
    val sourceDayLabel: String,
    val sourceMealSlotLabel: String,
    val mealText: String
)

data class ExtraCatalogMealOptionUi(
    val title: String?,
    val mealText: String,
    val sourceLabel: String,
    val pageNumber: Int?,
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
    val slotActionDialog: SlotActionDialogUi? = null,
    val isApplyingSlotAction: Boolean = false,
    val actionMessage: String? = null,
    val actionErrorMessage: String? = null,
    val errorMessage: String? = null
) {
    val isEmpty: Boolean
        get() = hasLoadedOnce && slots.isEmpty() && errorMessage == null

    val populatedSlotsCount: Int
        get() = slots.count { it.originalMealText.isNotBlank() }

    val orderedCalendarDays: List<WeekDay>
        get() {
            val ordered = WeekDay.orderedValues()
            val startIndex = ordered.indexOf(currentWeekReferenceDay).coerceAtLeast(0)
            return ordered.drop(startIndex) + ordered.take(startIndex)
        }
}