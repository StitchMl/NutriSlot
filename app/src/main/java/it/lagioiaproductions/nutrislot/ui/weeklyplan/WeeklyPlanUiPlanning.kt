package it.lagioiaproductions.nutrislot.ui.weeklyplan

import it.lagioiaproductions.nutrislot.data.repository.planning.ActiveWeekPlanning
import it.lagioiaproductions.nutrislot.domain.model.MealAssignment
import it.lagioiaproductions.nutrislot.domain.model.WeeklyPlanSnapshot

internal fun WeeklyPlanSnapshot.activeWeekAssignments(): List<MealAssignment> {
    return assignments.filter { assignment ->
        isInCurrentWeek(assignment.assignedAtEpochMillis)
    }
}

internal fun buildActiveWeekPlanning(
    snapshot: WeeklyPlanSnapshot
): ActiveWeekPlanning {
    val actualSourceByTarget = snapshot.buildActualConsumedSourceMap()
    val pendingSourceByTarget = snapshot.buildPendingAssignedSourceMap(actualSourceByTarget)

    return ActiveWeekPlanning(
        actualSourceByTarget = actualSourceByTarget,
        pendingSourceByTarget = pendingSourceByTarget,
        usedSourceByTarget = buildUsedSourceUsages(
            actualSourceByTarget = actualSourceByTarget,
            pendingSourceByTarget = pendingSourceByTarget
        )
    )
}
