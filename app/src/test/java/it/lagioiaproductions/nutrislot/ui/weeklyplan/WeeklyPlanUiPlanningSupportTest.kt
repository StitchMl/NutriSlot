@file:Suppress("ConstPropertyName")

package it.lagioiaproductions.nutrislot.ui.weeklyplan

import it.lagioiaproductions.nutrislot.domain.model.MealAssignment
import it.lagioiaproductions.nutrislot.domain.model.MealSlot
import it.lagioiaproductions.nutrislot.domain.model.MealSlotType
import it.lagioiaproductions.nutrislot.domain.model.WeekDay
import it.lagioiaproductions.nutrislot.domain.model.WeeklyPlan
import it.lagioiaproductions.nutrislot.domain.model.WeeklyPlanSnapshot
import it.lagioiaproductions.nutrislot.ui.weeklyplan.calendar.buildPendingAssignedSourceMap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WeeklyPlanUiPlanningSupportTest {

    @Test
    fun buildPendingAssignedSourceMap_ignoresAssignmentsWhoseSourceMealIsNowBlank() {
        val snapshot = snapshotWithAssignment(sourceMealText = "")

        val pendingSources = snapshot.buildPendingAssignedSourceMap(actualSourceByTarget = emptyMap())

        assertTrue(pendingSources.isEmpty())
    }

    @Test
    fun buildPendingAssignedSourceMap_keepsAssignmentsWithValidSourceMeal() {
        val snapshot = snapshotWithAssignment(sourceMealText = "Pollo")

        val pendingSources = snapshot.buildPendingAssignedSourceMap(actualSourceByTarget = emptyMap())

        assertEquals(TuesdayLunchSlotId, pendingSources[MondayLunchSlotId])
    }

    private fun snapshotWithAssignment(
        sourceMealText: String
    ): WeeklyPlanSnapshot {
        val now = System.currentTimeMillis()

        return WeeklyPlanSnapshot(
            plan = WeeklyPlan(
                id = PlanId,
                title = "Test",
                sourceFileName = null,
                createdAtEpochMillis = now
            ),
            slots = listOf(
                MealSlot(
                    id = MondayLunchSlotId,
                    planId = PlanId,
                    dayOfWeek = WeekDay.MONDAY,
                    mealSlotType = MealSlotType.LUNCH,
                    plannedMealText = "Pasta"
                ),
                MealSlot(
                    id = TuesdayLunchSlotId,
                    planId = PlanId,
                    dayOfWeek = WeekDay.TUESDAY,
                    mealSlotType = MealSlotType.LUNCH,
                    plannedMealText = sourceMealText
                )
            ),
            consumptions = emptyList(),
            assignments = listOf(
                MealAssignment(
                    id = "pending",
                    planId = PlanId,
                    targetSlotId = MondayLunchSlotId,
                    sourceSlotId = TuesdayLunchSlotId,
                    assignedAtEpochMillis = now
                )
            )
        )
    }

    private companion object {
        const val PlanId = "plan"
        const val MondayLunchSlotId = "plan_MONDAY_LUNCH"
        const val TuesdayLunchSlotId = "plan_TUESDAY_LUNCH"
    }
}
