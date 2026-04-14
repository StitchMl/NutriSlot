package it.lagioiaproductions.nutrislot.ui.importpreview

import it.lagioiaproductions.nutrislot.domain.model.CellRecognitionState
import it.lagioiaproductions.nutrislot.domain.model.MealSlotType
import it.lagioiaproductions.nutrislot.domain.model.WeekDay
import it.lagioiaproductions.nutrislot.ui.importfile.EditableImportedMealCellUi
import it.lagioiaproductions.nutrislot.ui.importfile.ImportFileUiState
import org.junit.Assert.assertEquals
import org.junit.Test

class ImportPreviewStateTest {

    @Test
    fun resolveImportPreviewVisibility_whenShowOnlyFilledSlots_keepsOnlyDaysAndSlotsWithMeals() {
        val uiState = ImportFileUiState(
            editableCells = listOf(
                editableCell(
                    day = WeekDay.MONDAY,
                    slot = MealSlotType.BREAKFAST,
                    mealText = "Yogurt"
                ),
                editableCell(
                    day = WeekDay.TUESDAY,
                    slot = MealSlotType.LUNCH,
                    mealText = "Riso"
                ),
                editableCell(
                    day = WeekDay.WEDNESDAY,
                    slot = MealSlotType.DINNER,
                    mealText = ""
                )
            ),
            showOnlyFilledSlots = true
        )

        val visibility = resolveImportPreviewVisibility(uiState)

        assertEquals(listOf(WeekDay.MONDAY, WeekDay.TUESDAY), visibility.visibleDays)
        assertEquals(
            listOf(MealSlotType.BREAKFAST, MealSlotType.LUNCH),
            visibility.visibleSlotTypes
        )
        assertEquals(2, visibility.displayedCells.size)
    }

    @Test
    fun resolveImportPreviewVisibility_whenSpecificDayIsSelected_respectsItEvenWithFilter() {
        val uiState = ImportFileUiState(
            editableCells = listOf(
                editableCell(
                    day = WeekDay.MONDAY,
                    slot = MealSlotType.BREAKFAST,
                    mealText = "Yogurt"
                ),
                editableCell(
                    day = WeekDay.TUESDAY,
                    slot = MealSlotType.LUNCH,
                    mealText = "Riso"
                )
            ),
            selectedPreviewDay = WeekDay.TUESDAY,
            showOnlyFilledSlots = true
        )

        val visibility = resolveImportPreviewVisibility(uiState)

        assertEquals(listOf(WeekDay.TUESDAY), visibility.visibleDays)
        assertEquals(listOf(MealSlotType.LUNCH), visibility.visibleSlotTypes)
        assertEquals(1, visibility.displayedCells.size)
        assertEquals(WeekDay.TUESDAY, visibility.displayedCells.single().dayOfWeek)
    }

    private fun editableCell(
        day: WeekDay,
        slot: MealSlotType,
        mealText: String
    ): EditableImportedMealCellUi {
        return EditableImportedMealCellUi(
            id = "${day.name}_${slot.name}",
            dayOfWeek = day,
            mealSlotType = slot,
            mealText = mealText,
            originalMealText = mealText,
            originalRecognitionState = CellRecognitionState.EMPTY
        )
    }
}
