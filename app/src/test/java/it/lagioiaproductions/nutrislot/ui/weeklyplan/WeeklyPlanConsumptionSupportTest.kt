package it.lagioiaproductions.nutrislot.ui.weeklyplan

import it.lagioiaproductions.nutrislot.domain.model.MealAssignment
import it.lagioiaproductions.nutrislot.domain.model.MealSlot
import it.lagioiaproductions.nutrislot.domain.model.MealSlotType
import it.lagioiaproductions.nutrislot.domain.model.SlotDisplayState
import it.lagioiaproductions.nutrislot.domain.model.WeekDay
import it.lagioiaproductions.nutrislot.domain.model.WeeklyPlan
import it.lagioiaproductions.nutrislot.domain.model.WeeklyPlanSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@Suppress("SameParameterValue", "ConstPropertyName")
class WeeklyPlanConsumptionSupportTest {

    @Test
    fun resolvePlannedConsumptionSource_keepsAssignedSourceWhenDisplayedMealMatchesIt() {
        val snapshot = snapshotWithPendingSwap()
        val targetUi = weeklySlotUi(
            slotId = MondayLunchSlotId,
            displayedMealText = "Pollo"
        )

        val result = snapshot.resolvePlannedConsumptionSource(targetUi)

        assertEquals(TuesdayLunchSlotId, result.sourceSlotId)
        assertFalse(result.usesCustomizedTargetMeal)
    }

    @Test
    fun resolvePlannedConsumptionSource_usesTargetSlotWhenMealWasCustomized() {
        val snapshot = snapshotWithPendingSwap()
        val targetUi = weeklySlotUi(
            slotId = MondayLunchSlotId,
            displayedMealText = "Burger vegetale"
        )

        val result = snapshot.resolvePlannedConsumptionSource(targetUi)

        assertEquals(MondayLunchSlotId, result.sourceSlotId)
        assertTrue(result.usesCustomizedTargetMeal)
    }

    private fun snapshotWithPendingSwap(): WeeklyPlanSnapshot {
        val now = System.currentTimeMillis()

        return WeeklyPlanSnapshot(
            plan = WeeklyPlan(
                id = PlanId,
                title = "Test",
                sourceFileName = null,
                createdAtEpochMillis = 0L
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
                    plannedMealText = "Pollo"
                )
            ),
            consumptions = emptyList(),
            assignments = listOf(
                MealAssignment(
                    id = "pending-monday",
                    planId = PlanId,
                    targetSlotId = MondayLunchSlotId,
                    sourceSlotId = TuesdayLunchSlotId,
                    assignedAtEpochMillis = now
                )
            )
        )
    }

    private fun weeklySlotUi(
        slotId: String,
        displayedMealText: String
    ): WeeklySlotUi {
        return WeeklySlotUi(
            slotId = slotId,
            dayOfWeek = WeekDay.MONDAY,
            mealSlotType = MealSlotType.LUNCH,
            originalMealText = "Pasta",
            displayedMealText = displayedMealText,
            displayState = SlotDisplayState.PlannedAvailable,
            isActuallyCompletedThisWeek = false
        )
    }

    private companion object {
        const val PlanId = "plan"
        const val MondayLunchSlotId = "plan_MONDAY_LUNCH"
        const val TuesdayLunchSlotId = "plan_TUESDAY_LUNCH"
    }
}
