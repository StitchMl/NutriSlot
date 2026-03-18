package it.lagioiaproductions.nutrislot.data.repository.planning

import it.lagioiaproductions.nutrislot.data.local.room.MealAssignmentEntity
import it.lagioiaproductions.nutrislot.data.local.room.MealConsumptionEntity
import it.lagioiaproductions.nutrislot.data.local.room.MealSlotEntity
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

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
        val zoneId = ZoneId.systemDefault()
        val date = Instant.ofEpochMilli(epochMillis).atZone(zoneId).toLocalDate()
        val today = LocalDate.now(zoneId)
        val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val nextWeekStart = weekStart.plusWeeks(1)

        return !date.isBefore(weekStart) && date.isBefore(nextWeekStart)
    }
}