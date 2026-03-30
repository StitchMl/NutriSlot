package it.lagioiaproductions.nutrislot.ui.weeklyplan

import it.lagioiaproductions.nutrislot.domain.model.WeekDay

data class WeeklyPlanCalorieSyncUi(
    val id: Long,
    val consumptionId: String,
    val mealText: String,
    val mealSlotLabel: String,
    val targetDayOfWeek: WeekDay,
    val targetDayKey: String
)

data class WeeklyPlanCalorieUndoUi(
    val id: Long,
    val consumptionId: String
)
