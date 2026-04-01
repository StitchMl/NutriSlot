package it.lagioiaproductions.nutrislot.data.repository.planning

import it.lagioiaproductions.nutrislot.data.local.room.MealAssignmentEntity
import it.lagioiaproductions.nutrislot.data.local.room.MealConsumptionEntity
import it.lagioiaproductions.nutrislot.data.local.room.MealSlotEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WeeklyPlanningCalculatorTest {

    @Test
    fun buildActiveWeekPlanning_keepsPendingAssignmentsWhenSourceTargetWasConsumedWithDifferentMeal() {
        val planning = WeeklyPlanningCalculator.buildActiveWeekPlanning(
            slotEntities = slots(),
            actualConsumptions = listOf(
                MealConsumptionEntity(
                    id = "consumed-tuesday",
                    planId = PlanId,
                    targetSlotId = TuesdayLunchSlotId,
                    sourceSlotId = MondayLunchSlotId,
                    consumedAtEpochMillis = 20L
                )
            ),
            pendingAssignments = listOf(
                MealAssignmentEntity(
                    id = "pending-monday",
                    planId = PlanId,
                    targetSlotId = MondayLunchSlotId,
                    sourceSlotId = TuesdayLunchSlotId,
                    assignedAtEpochMillis = 10L
                )
            )
        )

        assertEquals(TuesdayLunchSlotId, planning.pendingSourceByTarget[MondayLunchSlotId])
        assertTrue(planning.isActualSourceConsumed(MondayLunchSlotId))
        assertFalse(planning.isActualSourceConsumed(TuesdayLunchSlotId))
    }

    @Test
    fun buildActiveWeekPlanning_discardsPendingAssignmentsWhoseSourceMealWasAlreadyConsumed() {
        val planning = WeeklyPlanningCalculator.buildActiveWeekPlanning(
            slotEntities = slots(),
            actualConsumptions = listOf(
                MealConsumptionEntity(
                    id = "consumed-wednesday",
                    planId = PlanId,
                    targetSlotId = WednesdayLunchSlotId,
                    sourceSlotId = TuesdayLunchSlotId,
                    consumedAtEpochMillis = 20L
                )
            ),
            pendingAssignments = listOf(
                MealAssignmentEntity(
                    id = "pending-monday",
                    planId = PlanId,
                    targetSlotId = MondayLunchSlotId,
                    sourceSlotId = TuesdayLunchSlotId,
                    assignedAtEpochMillis = 10L
                )
            )
        )

        assertTrue(planning.pendingSourceByTarget.isEmpty())
        assertTrue(planning.isActualSourceConsumed(TuesdayLunchSlotId))
    }

    private fun slots(): List<MealSlotEntity> {
        return listOf(
            MealSlotEntity(
                id = MondayLunchSlotId,
                planId = PlanId,
                dayOfWeek = "MONDAY",
                mealSlotType = "LUNCH",
                plannedMealText = "Pasta"
            ),
            MealSlotEntity(
                id = TuesdayLunchSlotId,
                planId = PlanId,
                dayOfWeek = "TUESDAY",
                mealSlotType = "LUNCH",
                plannedMealText = "Pollo"
            ),
            MealSlotEntity(
                id = WednesdayLunchSlotId,
                planId = PlanId,
                dayOfWeek = "WEDNESDAY",
                mealSlotType = "LUNCH",
                plannedMealText = "Pesce"
            )
        )
    }

    @Suppress("ConstPropertyName")
    private companion object {
        const val PlanId = "plan"
        const val MondayLunchSlotId = "plan_MONDAY_LUNCH"
        const val TuesdayLunchSlotId = "plan_TUESDAY_LUNCH"
        const val WednesdayLunchSlotId = "plan_WEDNESDAY_LUNCH"
    }
}
