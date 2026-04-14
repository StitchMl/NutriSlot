@file:Suppress("CanBeParameter")

package it.lagioiaproductions.nutrislot.ui.weeklyplan.slot

import it.lagioiaproductions.nutrislot.data.repository.mapper.areMealSlotTypesCompatible
import it.lagioiaproductions.nutrislot.data.repository.planning.isActualSourceConsumed
import it.lagioiaproductions.nutrislot.domain.model.MealSlot
import it.lagioiaproductions.nutrislot.domain.model.MealSlotType
import it.lagioiaproductions.nutrislot.domain.model.WeeklyPlanSnapshot
import it.lagioiaproductions.nutrislot.ui.weeklyplan.calendar.buildActiveWeekPlanning
import it.lagioiaproductions.nutrislot.ui.weeklyplan.edit.WeeklyPlanCustomizationManager

internal fun buildSlotActionDialog(
    snapshot: WeeklyPlanSnapshot,
    targetUi: WeeklySlotUi,
    customizationManager: WeeklyPlanCustomizationManager
): SlotActionDialogUi {
    return WeeklyPlanSlotActionDialogFactory(snapshot, customizationManager).build(targetUi)
}

private class WeeklyPlanSlotActionDialogFactory(
    private val snapshot: WeeklyPlanSnapshot,
    private val customizationManager: WeeklyPlanCustomizationManager
) {
    private val planning = buildActiveWeekPlanning(snapshot)
    private val weeklySlotUiById = buildWeeklySlotUis(snapshot, customizationManager).associateBy(WeeklySlotUi::slotId)

    fun build(targetUi: WeeklySlotUi): SlotActionDialogUi {
        val targetSlot = snapshot.slots.first { it.id == targetUi.slotId }
        val targetIsActuallyCompleted = planning.actualSourceByTarget.containsKey(targetUi.slotId)
        val currentAssignedSourceSlotId = if (targetIsActuallyCompleted) {
            null
        } else {
            snapshot.resolvePlannedConsumptionSource(targetUi).sourceSlotId
        }

        return SlotActionDialogUi(
            targetSlotId = targetUi.slotId,
            targetDayLabel = targetUi.dayOfWeek.displayName,
            targetMealSlotLabel = targetUi.mealSlotType.displayName,
            currentDisplayedMealText = targetUi.displayedMealText,
            currentAssignedSourceSlotId = currentAssignedSourceSlotId,
            targetDisplayState = targetUi.displayState,
            isTargetActuallyCompletedThisWeek = targetIsActuallyCompleted,
            reassignedFromDayLabel = targetUi.reassignedFromDayLabel,
            reassignedFromMealSlotLabel = targetUi.reassignedFromMealSlotLabel,
            canConsumeAsPlanned = !targetIsActuallyCompleted && currentAssignedSourceSlotId != null,
            replacementOptions = buildReplacementOptions(
                targetUi = targetUi,
                targetSlot = targetSlot,
                targetIsActuallyCompleted = targetIsActuallyCompleted
            ),
            extraCatalogOptions = buildExtraCatalogOptions(
                targetSlotType = targetSlot.mealSlotType,
                targetIsActuallyCompleted = targetIsActuallyCompleted
            ),
            mealRuleSummary = buildMealRuleSummary(targetSlot.mealSlotType)
        )
    }

    private fun buildReplacementOptions(
        targetUi: WeeklySlotUi,
        targetSlot: MealSlot,
        targetIsActuallyCompleted: Boolean
    ): List<ReplacementMealOptionUi> {
        if (targetIsActuallyCompleted) return emptyList()

        return snapshot.slots
            .asSequence()
            .filter { candidateSourceSlot ->
                canUseAsReplacement(
                    candidateSourceSlot = candidateSourceSlot,
                    targetUi = targetUi,
                    targetSlot = targetSlot
                )
            }
            .sortedWith(compareBy({ it.dayOfWeek.sortOrder }, { it.mealSlotType.sortOrder }))
            .map(::mapReplacementOption)
            .toList()
    }

    private fun canUseAsReplacement(
        candidateSourceSlot: MealSlot,
        targetUi: WeeklySlotUi,
        targetSlot: MealSlot
    ): Boolean {
        return candidateSourceSlot.id != targetUi.slotId &&
                candidateSourceSlot.plannedMealText.isNotBlank() &&
                !planning.isActualSourceConsumed(candidateSourceSlot.id) &&
                areMealSlotTypesCompatible(
                    targetType = targetSlot.mealSlotType,
                    sourceType = candidateSourceSlot.mealSlotType
                )
    }

    private fun mapReplacementOption(sourceSlot: MealSlot): ReplacementMealOptionUi {
        val displayedCandidateText = weeklySlotUiById[sourceSlot.id]
            ?.displayedMealText
            ?.takeIf { it.isNotBlank() }
            ?: sourceSlot.plannedMealText

        return ReplacementMealOptionUi(
            sourceSlotId = sourceSlot.id,
            sourceDayLabel = sourceSlot.dayOfWeek.displayName,
            sourceMealSlotLabel = sourceSlot.mealSlotType.displayName,
            mealText = displayedCandidateText
        )
    }

    private fun buildExtraCatalogOptions(
        targetSlotType: MealSlotType,
        targetIsActuallyCompleted: Boolean
    ): List<ExtraCatalogMealOptionUi> {
        if (targetIsActuallyCompleted) return emptyList()

        return snapshot.mealOptions
            .filter { option ->
                areMealSlotTypesCompatible(
                    targetType = targetSlotType,
                    sourceType = option.mealSlotType
                )
            }
            .sortedWith(
                compareBy(
                    { it.mealSlotType.sortOrder },
                    { it.title ?: "" },
                    { it.mealText }
                )
            )
            .map { option ->
                ExtraCatalogMealOptionUi(
                    optionId = option.id,
                    title = option.title,
                    mealText = option.mealText,
                    sourceLabel = option.sourceType.name
                        .replace('_', ' ')
                        .lowercase(),
                    tags = option.tags
                )
            }
            .distinctBy { option ->
                "${option.title}|${option.mealText}"
            }
    }

    private fun buildMealRuleSummary(targetSlotType: MealSlotType): String? {
        return snapshot.mealRules
            .firstOrNull { it.mealSlotType == targetSlotType }
            ?.requiredComponents
            ?.takeIf { it.isNotEmpty() }
            ?.joinToString(separator = " + ")
    }
}
