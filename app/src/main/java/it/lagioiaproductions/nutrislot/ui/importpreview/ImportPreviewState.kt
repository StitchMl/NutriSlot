package it.lagioiaproductions.nutrislot.ui.importpreview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import it.lagioiaproductions.nutrislot.domain.model.MealSlotType
import it.lagioiaproductions.nutrislot.domain.model.WeekDay
import it.lagioiaproductions.nutrislot.ui.importfile.EditableImportedMealCellUi
import it.lagioiaproductions.nutrislot.ui.importfile.ImportFileUiState

internal val PreviewSlotOrder = listOf(
    MealSlotType.BREAKFAST,
    MealSlotType.MORNING_SNACK,
    MealSlotType.LUNCH,
    MealSlotType.AFTERNOON_SNACK,
    MealSlotType.DINNER
)

internal data class ImportPreviewGridState(
    val visibleDays: List<WeekDay>,
    val filteredCells: List<EditableImportedMealCellUi>,
    val cellsByDayAndSlot: Map<WeekDay, Map<MealSlotType, EditableImportedMealCellUi?>>,
    val editingCell: EditableImportedMealCellUi?
)

@Composable
internal fun rememberImportPreviewGridState(
    uiState: ImportFileUiState,
    editingCellId: String?
): ImportPreviewGridState {
    val visibleDays = remember(uiState.selectedPreviewDay) {
        uiState.selectedPreviewDay?.let(::listOf) ?: WeekDay.orderedValues()
    }

    val filteredCells = remember(
        uiState.editableCells,
        visibleDays,
        uiState.showOnlyFilledSlots
    ) {
        uiState.editableCells.filter { cell ->
            cell.dayOfWeek in visibleDays &&
                    (!uiState.showOnlyFilledSlots || cell.mealText.isNotBlank())
        }
    }

    val cellsByDayAndSlot = remember(filteredCells, visibleDays) {
        visibleDays.associateWith { day ->
            PreviewSlotOrder.associateWith { slotType ->
                filteredCells.firstOrNull { cell ->
                    cell.dayOfWeek == day && cell.mealSlotType == slotType
                }
            }
        }
    }

    val editingCell = remember(uiState.editableCells, editingCellId) {
        uiState.editableCells.firstOrNull { it.id == editingCellId }
    }

    return ImportPreviewGridState(
        visibleDays = visibleDays,
        filteredCells = filteredCells,
        cellsByDayAndSlot = cellsByDayAndSlot,
        editingCell = editingCell
    )
}