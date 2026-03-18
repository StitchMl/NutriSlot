package it.lagioiaproductions.nutrislot.ui.weeklyplan

import it.lagioiaproductions.nutrislot.domain.model.MealAssignment
import it.lagioiaproductions.nutrislot.domain.model.MealSlotType
import it.lagioiaproductions.nutrislot.domain.model.WeeklyPlanSnapshot

internal data class SourceUsage(
    val targetSlotId: String,
    val sourceSlotId: String
)

internal data class ActiveWeekPlanning(
    val actualSourceByTarget: Map<String, String>,
    val pendingSourceByTarget: Map<String, String>,
    val usedSourceByTarget: List<SourceUsage>
)

internal fun WeeklyPlanSnapshot.activeWeekAssignments(): List<MealAssignment> {
    return assignments.filter { assignment ->
        isInCurrentWeek(assignment.assignedAtEpochMillis)
    }
}

internal fun buildActiveWeekPlanning(
    snapshot: WeeklyPlanSnapshot
): ActiveWeekPlanning {
    val actualSourceByTarget = linkedMapOf<String, String>()
    snapshot.consumptions
        .filter { consumption -> isInCurrentWeek(consumption.consumedAtEpochMillis) }
        .sortedBy { it.consumedAtEpochMillis }
        .forEach { consumption ->
            actualSourceByTarget[consumption.targetSlotId] = consumption.sourceSlotId
        }

    val pendingSourceByTarget = linkedMapOf<String, String>()
    snapshot.activeWeekAssignments()
        .sortedBy { it.assignedAtEpochMillis }
        .forEach { assignment ->
            if (!actualSourceByTarget.containsKey(assignment.targetSlotId)) {
                pendingSourceByTarget[assignment.targetSlotId] = assignment.sourceSlotId
            }
        }

    val usedSourceByTarget = buildList {
        actualSourceByTarget.forEach { (targetSlotId, sourceSlotId) ->
            add(SourceUsage(targetSlotId = targetSlotId, sourceSlotId = sourceSlotId))
        }

        pendingSourceByTarget.forEach { (targetSlotId, sourceSlotId) ->
            add(SourceUsage(targetSlotId = targetSlotId, sourceSlotId = sourceSlotId))
        }
    }

    return ActiveWeekPlanning(
        actualSourceByTarget = actualSourceByTarget,
        pendingSourceByTarget = pendingSourceByTarget,
        usedSourceByTarget = usedSourceByTarget
    )
}

internal fun areMealSlotTypesCompatible(
    targetType: MealSlotType,
    sourceType: MealSlotType
): Boolean {
    if (targetType == sourceType) {
        return true
    }

    return when (targetType) {
        MealSlotType.LUNCH -> sourceType == MealSlotType.DINNER
        MealSlotType.DINNER -> sourceType == MealSlotType.LUNCH
        MealSlotType.MORNING_SNACK -> sourceType == MealSlotType.AFTERNOON_SNACK
        MealSlotType.AFTERNOON_SNACK -> sourceType == MealSlotType.MORNING_SNACK
        MealSlotType.BREAKFAST -> false
    }
}

internal fun isSourceAvailableForTarget(
    snapshot: WeeklyPlanSnapshot,
    targetSlotId: String,
    candidateSourceSlotId: String
): Boolean {
    val planning = buildActiveWeekPlanning(snapshot)

    val sourceUsedOrReservedByAnotherTarget = planning.usedSourceByTarget.any { usage ->
        usage.sourceSlotId == candidateSourceSlotId &&
                usage.targetSlotId != targetSlotId
    }

    return !sourceUsedOrReservedByAnotherTarget
}