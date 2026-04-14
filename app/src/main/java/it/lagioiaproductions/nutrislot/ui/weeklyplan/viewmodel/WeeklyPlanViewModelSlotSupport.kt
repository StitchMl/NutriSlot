package it.lagioiaproductions.nutrislot.ui.weeklyplan.viewmodel

import it.lagioiaproductions.nutrislot.domain.model.WeekDay
import it.lagioiaproductions.nutrislot.domain.model.WeeklyPlanSnapshot
import it.lagioiaproductions.nutrislot.ui.weeklyplan.checklist.WeeklyQuantityChecklistItemUi
import it.lagioiaproductions.nutrislot.ui.weeklyplan.checklist.hasLinkedWaterTracking
import it.lagioiaproductions.nutrislot.ui.weeklyplan.edit.WeeklyPlanCustomizationManager
import it.lagioiaproductions.nutrislot.ui.weeklyplan.edit.mergeMealTextWithNutritionSummary
import it.lagioiaproductions.nutrislot.ui.weeklyplan.edit.stripStoredMealNutrition
import it.lagioiaproductions.nutrislot.ui.weeklyplan.slot.EditSlotDialogUi
import it.lagioiaproductions.nutrislot.ui.weeklyplan.slot.EditableConsumptionTargetUi
import it.lagioiaproductions.nutrislot.ui.weeklyplan.slot.SlotActionDialogUi
import it.lagioiaproductions.nutrislot.ui.weeklyplan.slot.WeeklySlotUi
import it.lagioiaproductions.nutrislot.ui.weeklyplan.slot.buildSlotActionDialog
import it.lagioiaproductions.nutrislot.ui.weeklyplan.slot.buildWeeklySlotUis
import it.lagioiaproductions.nutrislot.ui.weeklyplan.slot.resolvePlannedConsumptionSource

internal data class PlannedSlotConsumptionCommand(
    val targetSlotId: String,
    val sourceSlotId: String,
    val targetDayOfWeek: WeekDay,
    val consumedMealText: String,
    val consumedMealSlotLabel: String,
    val usesCustomizedTargetMeal: Boolean
)

internal fun buildEditSlotDialog(
    slotUi: WeeklySlotUi,
    availableConsumptionTargets: List<EditableConsumptionTargetUi>
): EditSlotDialogUi {
    return EditSlotDialogUi(
        slotId = slotUi.slotId,
        dayLabel = slotUi.dayOfWeek.displayName,
        mealSlotLabel = slotUi.mealSlotType.displayName,
        mealText = stripStoredMealNutrition(slotUi.displayedMealText),
        nutritionText = slotUi.nutritionSummary.orEmpty(),
        selectedConsumptionTargetCanonicalKeys = slotUi.displayedConsumptionTargetCanonicalKeys,
        availableConsumptionTargets = availableConsumptionTargets,
        consumptionTargetSource = slotUi.displayedConsumptionTargetSource,
        canResetToOriginal = slotUi.hasCustomizations
    )
}

internal fun List<WeeklyQuantityChecklistItemUi>.toEditableConsumptionTargets(): List<EditableConsumptionTargetUi> {
    return filterNot { it.hasLinkedWaterTracking }
        .sortedBy { it.title }
        .map { item ->
            EditableConsumptionTargetUi(
                canonicalKey = item.id,
                title = item.title
            )
        }
}

internal fun WeeklyPlanSnapshot.resolveWeeklySlotUi(
    slotId: String,
    currentSlots: List<WeeklySlotUi>,
    customizationManager: WeeklyPlanCustomizationManager
): WeeklySlotUi? {
    return currentSlots.firstOrNull { it.slotId == slotId }
        ?: buildWeeklySlotUis(this, customizationManager).firstOrNull { it.slotId == slotId }
}

internal fun WeeklyPlanSnapshot.buildTargetSlotActionDialog(
    slotId: String,
    currentSlots: List<WeeklySlotUi>,
    customizationManager: WeeklyPlanCustomizationManager
): SlotActionDialogUi? {
    val targetUi = resolveWeeklySlotUi(
        slotId = slotId,
        currentSlots = currentSlots,
        customizationManager = customizationManager
    ) ?: return null

    return buildSlotActionDialog(
        snapshot = this,
        targetUi = targetUi,
        customizationManager = customizationManager
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
