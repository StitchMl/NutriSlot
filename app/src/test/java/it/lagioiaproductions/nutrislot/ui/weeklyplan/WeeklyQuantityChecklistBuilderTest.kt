@file:Suppress("SameParameterValue")

package it.lagioiaproductions.nutrislot.ui.weeklyplan

import it.lagioiaproductions.nutrislot.domain.model.MealConsumptionTargetSource
import it.lagioiaproductions.nutrislot.domain.model.MealSlotType
import it.lagioiaproductions.nutrislot.domain.model.SlotDisplayState
import it.lagioiaproductions.nutrislot.domain.model.WeekDay
import it.lagioiaproductions.nutrislot.domain.model.WeeklyFrequencyTarget
import it.lagioiaproductions.nutrislot.ui.weeklyplan.checklist.WeeklyQuantityChecklistBuilder
import it.lagioiaproductions.nutrislot.ui.weeklyplan.slot.WeeklySlotUi
import org.junit.Assert.assertEquals
import org.junit.Test

class WeeklyQuantityChecklistBuilderTest {

    @Test
    fun build_countsCompletedMealUsingExplicitConsumptionTargetMapping() {
        val items = WeeklyQuantityChecklistBuilder.build(
            slots = listOf(
                weeklySlotUi(
                    displayedMealText = "Bowl speciale dello chef",
                    targetCanonicalKeys = listOf("pesce"),
                    targetSource = MealConsumptionTargetSource.GEMINI
                )
            ),
            weeklyTargets = listOf(
                weeklyTarget(
                    canonicalKey = "pesce",
                    title = "Pesce"
                )
            ),
            referenceDay = WeekDay.MONDAY
        )

        assertEquals(1, items.single().consumedValue)
    }

    @Test
    fun build_doesNotFallbackToTextMatchWhenManualSelectionExplicitlyClearsTarget() {
        val items = WeeklyQuantityChecklistBuilder.build(
            slots = listOf(
                weeklySlotUi(
                    displayedMealText = "Salmone con verdure",
                    targetCanonicalKeys = emptyList(),
                    targetSource = MealConsumptionTargetSource.MANUAL
                )
            ),
            weeklyTargets = listOf(
                weeklyTarget(
                    canonicalKey = "pesce",
                    title = "Pesce"
                )
            ),
            referenceDay = WeekDay.MONDAY
        )

        assertEquals(0, items.single().consumedValue)
    }

    private fun weeklySlotUi(
        displayedMealText: String,
        targetCanonicalKeys: List<String>,
        targetSource: MealConsumptionTargetSource?
    ): WeeklySlotUi {
        return WeeklySlotUi(
            slotId = "slot",
            dayOfWeek = WeekDay.MONDAY,
            mealSlotType = MealSlotType.LUNCH,
            originalMealText = displayedMealText,
            displayedMealText = displayedMealText,
            displayState = SlotDisplayState.ConsumedAsPlanned,
            isActuallyCompletedThisWeek = true,
            displayedConsumptionTargetCanonicalKeys = targetCanonicalKeys,
            displayedConsumptionTargetSource = targetSource
        )
    }

    private fun weeklyTarget(
        canonicalKey: String,
        title: String
    ): WeeklyFrequencyTarget {
        return WeeklyFrequencyTarget(
            id = canonicalKey,
            planId = "plan",
            title = title,
            canonicalKey = canonicalKey,
            minimumTimesPerWeek = 1,
            maximumTimesPerWeek = 2,
            matchTerms = listOf(title.lowercase()),
            sourceText = "$title 1-2 volte a settimana"
        )
    }
}
