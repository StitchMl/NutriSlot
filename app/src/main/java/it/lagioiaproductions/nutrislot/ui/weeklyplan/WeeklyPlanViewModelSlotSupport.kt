package it.lagioiaproductions.nutrislot.ui.weeklyplan

import it.lagioiaproductions.nutrislot.domain.model.WeekDay
import it.lagioiaproductions.nutrislot.domain.model.WeeklyPlanSnapshot

internal data class PlannedSlotConsumptionCommand(
    val targetSlotId: String,
    val sourceSlotId: String,
    val targetDayOfWeek: WeekDay,
    val consumedMealText: String,
    val consumedMealSlotLabel: String,
    val usesCustomizedTargetMeal: Boolean
)

internal fun buildEditSlotDialog(slotUi: WeeklySlotUi): EditSlotDialogUi {
    return EditSlotDialogUi(
        slotId = slotUi.slotId,
        dayLabel = slotUi.dayOfWeek.displayName,
        mealSlotLabel = slotUi.mealSlotType.displayName,
        mealText = stripStoredMealNutrition(slotUi.displayedMealText),
        nutritionText = slotUi.nutritionSummary.orEmpty()
    )
}

internal fun WeeklyPlanSnapshot.resolveWeeklySlotUi(
    slotId: String,
    currentSlots: List<WeeklySlotUi>
): WeeklySlotUi? {
    return currentSlots.firstOrNull { it.slotId == slotId }
        ?: buildWeeklySlotUis(this).firstOrNull { it.slotId == slotId }
}

internal fun WeeklyPlanSnapshot.buildTargetSlotActionDialog(
    slotId: String,
    currentSlots: List<WeeklySlotUi>
): SlotActionDialogUi? {
    val targetUi = resolveWeeklySlotUi(
        slotId = slotId,
        currentSlots = currentSlots
    ) ?: return null

    return buildSlotActionDialog(
        snapshot = this,
        targetUi = targetUi
    )
}

internal fun WeeklyPlanSnapshot.buildPlannedSlotConsumptionCommand(
    targetSlotId: String,
    currentSlots: List<WeeklySlotUi>
): PlannedSlotConsumptionCommand? {
    val targetUi = currentSlots.firstOrNull { it.slotId == targetSlotId } ?: return null
    val targetSlot = slots.firstOrNull { it.id == targetSlotId } ?: return null
    val sourceResolution = resolvePlannedConsumptionSource(targetUi)
    val sourceSlotId = sourceResolution.sourceSlotId
        ?: return null

    return PlannedSlotConsumptionCommand(
        targetSlotId = targetSlotId,
        sourceSlotId = sourceSlotId,
        targetDayOfWeek = targetSlot.dayOfWeek,
        consumedMealText = mergeMealTextWithNutritionSummary(
            mealText = targetUi.displayedMealText,
            nutritionSummary = targetUi.nutritionSummary.orEmpty()
        ),
        consumedMealSlotLabel = targetUi.mealSlotType.displayName,
        usesCustomizedTargetMeal = sourceResolution.usesCustomizedTargetMeal
    )
}
