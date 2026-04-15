package it.lagioiaproductions.nutrislot.data.repository

import it.lagioiaproductions.nutrislot.data.local.room.MealAssignmentEntity
import it.lagioiaproductions.nutrislot.data.local.room.WeeklyPlanDao
import it.lagioiaproductions.nutrislot.data.repository.planning.ActiveWeekPlanning

/**
 * Centralizes assignment cleanup and source-slot lookup logic used by weekly plan mutations.
 */
internal class WeeklyPlanAssignmentSupport(
    private val weeklyPlanDao: WeeklyPlanDao
) {

    /**
     * Resolves the effective source slot for the target inside the active week.
     */
    fun currentEffectiveSourceSlotId(
        planning: ActiveWeekPlanning,
        targetSlotId: String
    ): String {
        return planning.pendingSourceByTarget[targetSlotId] ?: targetSlotId
    }

    /**
     * Deletes all current-week assignments that target the provided slot ids.
     */
    suspend fun deleteCurrentAssignmentsForTargets(
        targetSlotIds: Set<String>,
        assignments: List<MealAssignmentEntity>
    ) {
        val currentTargetAssignmentIds = assignments
            .filter { it.targetSlotId in targetSlotIds }
            .map { it.id }

        if (currentTargetAssignmentIds.isNotEmpty()) {
            weeklyPlanDao.deleteMealAssignmentsByIds(currentTargetAssignmentIds)
        }
    }
}
