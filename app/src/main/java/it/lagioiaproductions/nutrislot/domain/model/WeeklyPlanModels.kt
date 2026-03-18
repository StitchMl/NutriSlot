package it.lagioiaproductions.nutrislot.domain.model

data class WeeklyPlan(
    val id: String,
    val title: String?,
    val sourceFileName: String?,
    val createdAtEpochMillis: Long
)

data class MealSlot(
    val id: String,
    val planId: String,
    val dayOfWeek: WeekDay,
    val mealSlotType: MealSlotType,
    val plannedMealText: String
)

data class MealConsumption(
    val id: String,
    val planId: String,
    val targetSlotId: String,
    val sourceSlotId: String,
    val consumedAtEpochMillis: Long
)

data class MealAssignment(
    val id: String,
    val planId: String,
    val targetSlotId: String,
    val sourceSlotId: String,
    val assignedAtEpochMillis: Long
)