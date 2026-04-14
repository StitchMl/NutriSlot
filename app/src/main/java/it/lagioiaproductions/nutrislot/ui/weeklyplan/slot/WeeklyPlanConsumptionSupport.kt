package it.lagioiaproductions.nutrislot.ui.weeklyplan.slot

import it.lagioiaproductions.nutrislot.domain.model.WeeklyPlanSnapshot
import it.lagioiaproductions.nutrislot.ui.weeklyplan.calendar.buildActiveWeekPlanning
import it.lagioiaproductions.nutrislot.ui.weeklyplan.edit.stripStoredMealNutrition

internal data class PlannedConsumptionSource(
    val sourceSlotId: String?,
    val usesCustomizedTargetMeal: Boolean
)

internal fun WeeklyPlanSnapshot.resolvePlannedConsumptionSource(
    targetUi: WeeklySlotUi
): PlannedConsumptionSource {
    val targetSlot = slots.firstOrNull { slot -> slot.id == targetUi.slotId }
        ?: return PlannedConsumptionSource(
            sourceSlotId = null,
            usesCustomizedTargetMeal = false
        )

    val planning = buildActiveWeekPlanning(this)
    val assignedSourceSlotId = planning.pendingSourceByTarget[targetUi.slotId]
        ?: targetSlot.id.takeIf { targetSlot.plannedMealText.isNotBlank() }
    val assignedMealText = assignedSourceSlotId
        ?.let { slotId -> slotMealTextById(slotId) }
        .orEmpty()

    val usesCustomizedTargetMeal = stripStoredMealNutrition(targetUi.displayedMealText)
        .takeIf { it.isNotBlank() }
        ?.let { displayedMealText ->
            displayedMealText != stripStoredMealNutrition(assignedMealText)
        }
        ?: false

    val resolvedSourceSlotId = when {
        usesCustomizedTargetMeal -> targetSlot.id
        assignedSourceSlotId != null -> assignedSourceSlotId
        else -> null
    }

    return PlannedConsumptionSource(
        sourceSlotId = resolvedSourceSlotId,
        usesCustomizedTargetMeal = usesCustomizedTargetMeal
    )
}

private fun WeeklyPlanSnapshot.slotMealTextById(
    slotId: String
): String {
    return slots.firstOrNull { slot -> slot.id == slotId }
        ?.plannedMealText
        .orEmpty()
}
