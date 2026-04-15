package it.lagioiaproductions.nutrislot.ui.weeklyplan.state

import it.lagioiaproductions.nutrislot.domain.model.WeekDay

/** One-shot UI event that asks the calorie tracker to mirror a consumed meal. */
data class WeeklyPlanCalorieSyncUi(
    val id: Long,
    val consumptionId: String,
    val mealText: String,
    val mealSlotLabel: String,
    val targetDayOfWeek: WeekDay,
    val targetDayKey: String
)

/** One-shot UI event that asks the calorie tracker to undo a mirrored meal. */
data class WeeklyPlanCalorieUndoUi(
    val id: Long,
    val consumptionId: String
)
