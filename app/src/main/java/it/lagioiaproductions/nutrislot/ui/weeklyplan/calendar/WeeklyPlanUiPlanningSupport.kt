package it.lagioiaproductions.nutrislot.ui.weeklyplan

import it.lagioiaproductions.nutrislot.data.repository.planning.SourceUsage
import it.lagioiaproductions.nutrislot.domain.model.MealAssignment
import it.lagioiaproductions.nutrislot.domain.model.MealConsumption
import it.lagioiaproductions.nutrislot.domain.model.WeeklyPlanSnapshot

internal fun WeeklyPlanSnapshot.activeWeekConsumptions(): List<MealConsumption> {
    return consumptions.filter { consumption ->
        isInCurrentWeek(consumption.consumedAtEpochMillis)
    }
}

internal fun WeeklyPlanSnapshot.buildActualConsumedSourceMap(): Map<String, String> {
    return activeWeekConsumptions()
        .sortedBy { it.consumedAtEpochMillis }
        .associateLatestSourceByTarget(
            targetSlotIdOf = MealConsumption::targetSlotId,
            sourceSlotIdOf = MealConsumption::sourceSlotId
        )
}

internal fun WeeklyPlanSnapshot.buildPendingAssignedSourceMap(
    actualSourceByTarget: Map<String, String>
): Map<String, String> {
    val actualConsumedSourceIds = actualSourceByTarget.values.toSet()
    val slotById = slots.associateBy { it.id }

    return activeWeekAssignments()
        .sortedBy(MealAssignment::assignedAtEpochMillis)
        .filterNot { assignment ->
            actualSourceByTarget.containsKey(assignment.targetSlotId) ||
                    assignment.sourceSlotId in actualConsumedSourceIds
        }
        .filter { assignment ->
            slotById[assignment.sourceSlotId]
                ?.plannedMealText
                ?.isNotBlank() == true
        }
        .associateLatestSourceByTarget(
            targetSlotIdOf = MealAssignment::targetSlotId,
            sourceSlotIdOf = MealAssignment::sourceSlotId
        )
}

internal fun buildUsedSourceUsages(
    actualSourceByTarget: Map<String, String>,
    pendingSourceByTarget: Map<String, String>
): List<SourceUsage> {
    return buildList {
        actualSourceByTarget.forEach { (targetSlotId, sourceSlotId) ->
            add(SourceUsage(targetSlotId = targetSlotId, sourceSlotId = sourceSlotId))
        }

        pendingSourceByTarget.forEach { (targetSlotId, sourceSlotId) ->
            add(SourceUsage(targetSlotId = targetSlotId, sourceSlotId = sourceSlotId))
        }
    }
}

private fun <T> List<T>.associateLatestSourceByTarget(
    targetSlotIdOf: (T) -> String,
    sourceSlotIdOf: (T) -> String
): Map<String, String> {
    val result = linkedMapOf<String, String>()
    forEach { item ->
        result[targetSlotIdOf(item)] = sourceSlotIdOf(item)
    }
    return result
}
