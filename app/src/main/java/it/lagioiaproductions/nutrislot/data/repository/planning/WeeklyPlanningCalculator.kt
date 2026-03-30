package it.lagioiaproductions.nutrislot.data.repository.planning

import it.lagioiaproductions.nutrislot.data.local.room.MealAssignmentEntity
import it.lagioiaproductions.nutrislot.data.local.room.MealConsumptionEntity
import it.lagioiaproductions.nutrislot.data.local.room.MealSlotEntity
import it.lagioiaproductions.nutrislot.domain.model.currentWeekWindow

internal object WeeklyPlanningCalculator {

    fun buildActiveWeekPlanning(
        slotEntities: List<MealSlotEntity>,
        actualConsumptions: List<MealConsumptionEntity>,
        pendingAssignments: List<MealAssignmentEntity>
    ): ActiveWeekPlanning {
        val actualSourceByTarget = linkedMapOf<String, String>()
        actualConsumptions
            .sortedBy { it.consumedAtEpochMillis }
            .forEach { consumption ->
                actualSourceByTarget[consumption.targetSlotId] = consumption.sourceSlotId
            }

        val pendingSourceByTarget = linkedMapOf<String, String>()
        pendingAssignments
            .sortedBy { it.assignedAtEpochMillis }
            .forEach { assignment ->
                if (actualSourceByTarget.containsKey(assignment.targetSlotId)) {
                    return@forEach
                }

                val sourceSlot = slotEntities.firstOrNull { it.id == assignment.sourceSlotId }
                    ?: return@forEach

                if (sourceSlot.plannedMealText.isBlank()) {
                    return@forEach
                }

                pendingSourceByTarget[assignment.targetSlotId] = assignment.sourceSlotId
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

    fun isInCurrentWeek(epochMillis: Long): Boolean {
        return currentWeekWindow().contains(epochMillis)
    }
}
