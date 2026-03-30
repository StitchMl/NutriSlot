package it.lagioiaproductions.nutrislot.domain.model

sealed interface SlotDisplayState {
    data object Empty : SlotDisplayState
    data object PlannedAvailable : SlotDisplayState
    data object ConsumedAsPlanned : SlotDisplayState
    data class ConsumedWithReplacement(
        val sourceSlotId: String
    ) : SlotDisplayState
    data object OriginalMealAlreadyUsedElsewhere : SlotDisplayState
}

data class WeeklyPlanSnapshot(
    val plan: WeeklyPlan,
    val slots: List<MealSlot>,
    val consumptions: List<MealConsumption>,
    val assignments: List<MealAssignment> = emptyList(),
    val mealOptions: List<MealOption> = emptyList(),
    val mealRules: List<MealRule> = emptyList(),
    val weeklyTargets: List<WeeklyFrequencyTarget> = emptyList()
)

@Suppress("unused")
data class SlotPresentation(
    val slot: MealSlot,
    val displayState: SlotDisplayState,
    val coveringConsumption: MealConsumption?
)

fun List<MealSlot>.sortedForWeeklyDisplay(): List<MealSlot> {
    return sortedWith(
        compareBy(
            { it.dayOfWeek.sortOrder },
            { it.mealSlotType.sortOrder }
        )
    )
}
