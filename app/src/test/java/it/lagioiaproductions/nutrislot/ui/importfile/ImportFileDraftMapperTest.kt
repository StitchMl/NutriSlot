package it.lagioiaproductions.nutrislot.ui.importfile

import it.lagioiaproductions.nutrislot.domain.model.CellRecognitionState
import it.lagioiaproductions.nutrislot.domain.model.MealSlotType
import it.lagioiaproductions.nutrislot.domain.model.WeekDay
import it.lagioiaproductions.nutrislot.ui.importfile.draft.buildManualEditableUiCells
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ImportFileDraftMapperTest {

    @Test
    fun buildManualEditableUiCells_buildsFullWeeklyGridWithEmptyCells() {
        val manualCells = buildManualEditableUiCells()

        assertEquals(
            WeekDay.orderedValues().size * MealSlotType.orderedValues().size,
            manualCells.size
        )
        assertEquals(WeekDay.MONDAY, manualCells.first().dayOfWeek)
        assertEquals(MealSlotType.BREAKFAST, manualCells.first().mealSlotType)
        assertEquals(WeekDay.SUNDAY, manualCells.last().dayOfWeek)
        assertEquals(MealSlotType.DINNER, manualCells.last().mealSlotType)
        assertTrue(manualCells.all { it.mealText.isEmpty() })
        assertTrue(manualCells.all { it.originalMealText.isEmpty() })
        assertTrue(manualCells.all { it.originalRecognitionState == CellRecognitionState.EMPTY })
        assertTrue(manualCells.all { !it.wasManuallyEdited })
    }
}
