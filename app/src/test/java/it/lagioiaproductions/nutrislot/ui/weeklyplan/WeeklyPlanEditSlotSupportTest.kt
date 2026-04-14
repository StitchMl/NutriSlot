package it.lagioiaproductions.nutrislot.ui.weeklyplan

import it.lagioiaproductions.nutrislot.domain.model.MealConsumptionTargetSource
import it.lagioiaproductions.nutrislot.domain.model.MealSlotType
import it.lagioiaproductions.nutrislot.domain.model.SlotDisplayState
import it.lagioiaproductions.nutrislot.domain.model.WeekDay
import it.lagioiaproductions.nutrislot.ui.weeklyplan.checklist.WeeklyQuantityChecklistItemUi
import it.lagioiaproductions.nutrislot.ui.weeklyplan.checklist.WeeklyQuantityChecklistMetricUi
import it.lagioiaproductions.nutrislot.ui.weeklyplan.checklist.WeeklyQuantityChecklistPeriodUi
import it.lagioiaproductions.nutrislot.ui.weeklyplan.slot.EditableConsumptionTargetUi
import it.lagioiaproductions.nutrislot.ui.weeklyplan.slot.WeeklySlotUi
import it.lagioiaproductions.nutrislot.ui.weeklyplan.viewmodel.buildEditSlotDialog
import it.lagioiaproductions.nutrislot.ui.weeklyplan.viewmodel.toEditableConsumptionTargets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WeeklyPlanEditSlotSupportTest {

    @Test
    fun buildEditSlotDialog_keepsCurrentTargetsAndSource() {
        val dialog = buildEditSlotDialog(
            slotUi = WeeklySlotUi(
                slotId = "plan_MONDAY_LUNCH",
                dayOfWeek = WeekDay.MONDAY,
                mealSlotType = MealSlotType.LUNCH,
                originalMealText = "Pasta e ceci",
                displayedMealText = "Pasta e ceci",
                displayState = SlotDisplayState.PlannedAvailable,
                isActuallyCompletedThisWeek = false,
                nutritionSummary = "520 kcal",
                displayedConsumptionTargetCanonicalKeys = listOf("legumi", "piatto unico"),
                displayedConsumptionTargetSource = MealConsumptionTargetSource.GEMINI
            ),
            availableConsumptionTargets = listOf(
                EditableConsumptionTargetUi(canonicalKey = "piatto unico", title = "Piatto Unico"),
                EditableConsumptionTargetUi(canonicalKey = "legumi", title = "Legumi")
            )
        )

        assertEquals(listOf("legumi", "piatto unico"), dialog.selectedConsumptionTargetCanonicalKeys)
        assertEquals(MealConsumptionTargetSource.GEMINI, dialog.consumptionTargetSource)
        assertEquals(2, dialog.availableConsumptionTargets.size)
    }

    @Test
    fun toEditableConsumptionTargets_filtersWaterAndSortsAlphabetically() {
        val targets = listOf(
            checklistItem(id = "pesce", title = "Pesce"),
            checklistItem(
                id = "acqua",
                title = "Acqua",
                metric = WeeklyQuantityChecklistMetricUi.MILLILITERS
            ),
            checklistItem(id = "affettati", title = "Affettati")
        ).toEditableConsumptionTargets()

        assertEquals(
            listOf("Affettati", "Pesce"),
            targets.map { it.title }
        )
        assertEquals(
            listOf("affettati", "pesce"),
            targets.map { it.canonicalKey }
        )
    }

    @Test
    fun buildEditSlotDialog_leavesSourceNullWhenSlotHasNoTaggedTargets() {
        val dialog = buildEditSlotDialog(
            slotUi = WeeklySlotUi(
                slotId = "plan_TUESDAY_DINNER",
                dayOfWeek = WeekDay.TUESDAY,
                mealSlotType = MealSlotType.DINNER,
                originalMealText = "Salmone",
                displayedMealText = "Salmone",
                displayState = SlotDisplayState.PlannedAvailable,
                isActuallyCompletedThisWeek = false
            ),
            availableConsumptionTargets = emptyList()
        )

        assertEquals(emptyList<String>(), dialog.selectedConsumptionTargetCanonicalKeys)
        assertNull(dialog.consumptionTargetSource)
    }

    private fun checklistItem(
        id: String,
        title: String,
        metric: WeeklyQuantityChecklistMetricUi = WeeklyQuantityChecklistMetricUi.OCCURRENCES
    ): WeeklyQuantityChecklistItemUi {
        return WeeklyQuantityChecklistItemUi(
            id = id,
            title = title,
            portionText = null,
            minimumTargetValue = 1,
            maximumTargetValue = null,
            consumedValue = 0,
            sourceLabel = "Test",
            period = WeeklyQuantityChecklistPeriodUi.WEEKLY,
            metric = metric
        )
    }
}
